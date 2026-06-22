# RideFlow

A production-patterned Uber-style ride-hailing platform: 8 backend microservices, a Next.js 15 frontend, Kafka event backbone, Redis-geo dispatch, WebSocket live updates, and simulated payment settlement. The full product runs end-to-end locally with one command.

> **Status**: the entire ride lifecycle works end-to-end — a rider requests a ride, matching dispatches the nearest online driver, the driver accepts and completes the trip, the fare is settled (simulated), and both parties get live WebSocket notifications. All 8 services and the frontend are implemented and wired to real endpoints.

---

## Quickstart — one command

```bash
# 1. (optional) seed env — defaults work out of the box
cp .env.example .env

# 2. Bring up the whole stack: Postgres, Redis (geo + cache), Kafka,
#    all 8 services, and the frontend
docker compose up -d --build

# 3. Wait ~90s for the JVMs to boot + Flyway migrations to apply
docker compose ps        # everything should be healthy
```

Open **http://localhost:3000**, register a **driver** (go online, allow location), then in another browser/profile register a **rider** and request a ride. Watch the match, trip, payment receipt, and live notifications flow through.

| What | URL |
|---|---|
| Frontend | http://localhost:3000 |
| API Gateway | http://localhost:8080 |
| Kafka UI (optional) | `docker compose --profile tools up -d kafka-ui` → http://localhost:8089 |
| Postgres | `localhost:5432` (user `rideflow` / pass from `.env`) |

A scripted end-to-end smoke test (register driver + rider → request → match → trip → **payment settled** → notification → history) lives at `scratchpad/smoke.sh` in the working tree.

---

## Services & ports

| Service | Port | Role |
|---|---|---|
| api-gateway | 8080 | Single ingress: routing, JWT auth, Redis token-bucket rate limiting, CORS |
| rider-service | 8081 | Rider auth/registration, ride request (outbox), ride projection, driver earnings, admin metrics |
| location-service | 8082 | Redis-geo driver index, nearby-driver queries, stale-driver sweeper |
| driver-service | 8083 | Driver auth/registration, availability, location pings |
| matching-service | 8084 | Event-driven dispatch: radius-ladder GEOSEARCH + scoring + Redisson lock + outbox |
| pricing-service | 8085 | Fare quotes + ride pricing (Haversine + rate cards + surge) |
| notification-service | 8086 | Kafka → Redis pub/sub → WebSocket fan-out; notification history |
| trip-service | 8087 | Post-assignment trip state machine (offer → accept → start → complete → cancel) |
| payment-service | 8088 | **Simulated** fare settlement: consume fare-quoted + ride.completed → publish payment.settled |

---

## The end-to-end flow

```
rider requests ride
   │  POST /v1/riders/{id}/rides                (rider-service, via outbox)
   ▼  topic: rider.ride-requested
   ├─► pricing-service  → pricing.fare-quoted   (quote attached to the ride)
   │       └─► payment-service caches the fare (PENDING payment)
   ▼
matching-service  (GEOSEARCH nearest online driver, Redisson lock)
   │  topic: matching.ride-assigned
   ▼
trip-service  (creates the offer; driver accepts → starts → completes)
   │  topics: ride.accepted / ride.started / ride.completed
   ├─► payment-service: on ride.completed, simulate authorize→capture→settle
   │       └─► topic: payment.settled
   ▼
notification-service  (consumes every lifecycle + payment event)
   │  Redis PUBLISH notify:user:<id>  →  WebSocket frame
   ▼
rider & driver browsers update live (status, receipt, notifications)
```

Patterns visible in code:

- **Hexagonal / Clean Architecture** — `api`/`application`/`domain`/`infrastructure` in every service
- **Transactional outbox** — rider, pricing, matching, trip, payment services
- **Idempotent consumers** — per-service `processed_events` dedupe table
- **Distributed lock (Redisson)** — `matching-service` driver lock
- **DLQ + backoff** — every Kafka consumer config
- **Cross-replica WS fan-out** — notification-service Redis pub/sub
- **Server + Client Components split** — `frontend/src/app/(rider|driver|admin)/`

---

## Payments (simulated)

`payment-service` is event-driven and stores no real card data:

- Consumes `pricing.fare-quoted` to learn each ride's fare (creates a `PENDING` payment).
- On `ride.completed`, simulates authorize → capture → settle and publishes `payment.settled` (via outbox). A configurable `rideflow.payment.simulated-failure-rate` (default `0`) can exercise the failure path.
- REST: `GET /v1/payments/methods?userId=…`, `POST /v1/payments/methods` (mock cards), `GET /v1/payments/rides/{rideId}` (receipt).

The frontend surfaces a live **receipt** on trip completion and a **payment-methods** page for adding mock cards. `notification-service` pushes a "payment received" notification to the rider.

---

## Tech stack

| Layer | Tech |
|---|---|
| Backend | Java 21, Spring Boot 3.3, Maven multi-module, Spring Kafka, Spring Data JPA + Hibernate 6, Lettuce, Redisson, Spring Cloud Gateway |
| Data | PostgreSQL 16 (schema per service), Redis 7 (Geo + cache), Apache Kafka |
| Frontend | Next.js 15 (App Router, standalone output), TypeScript strict, TailwindCSS, Shadcn UI, React Query, React Leaflet, next-themes |
| Infra | Docker Compose (full stack incl. frontend), multi-stage layered Dockerfiles, bash TCP healthchecks |
| CI/CD | GitHub Actions — backend-ci, frontend-ci, docker-publish (matrix, cosign-signed, SBOM, Trivy), CodeQL, Dependabot |

---

## What's implemented

| Area | Status |
|---|---|
| All 8 backend services (auth, ride, location, match, pricing, trip, notification, **payment**) | ✅ End-to-end |
| Frontend rider flow (book, map, fare quote, live tracking, **receipt**, **payment methods**, history) | ✅ |
| Frontend driver flow (availability, location heartbeat, live offers, trip state machine, **earnings**) | ✅ |
| Frontend admin (live operator **metrics**, rate cards, surge) | ✅ (metrics + read-only tariffs/surge) |
| Unit tests | ✅ 245 across the backend (domain + use cases) |
| One-command run (compose incl. frontend) | ✅ |
| Observability (Grafana dashboards), k8s manifests | 📐 Skeletons — not wired |
| Security hardening (RS256/JWKS, HttpOnly sessions, service-to-service auth) | 📐 Demo-grade today |
| Real payment processor (Stripe) | 📐 Simulated settlement only |

---

## Troubleshooting

| Symptom | Cause / fix |
|---|---|
| A service won't start; Flyway error | Postgres init hadn't finished on first boot. `docker compose down -v` to wipe volumes, then `up -d --build` again. |
| WebSocket shows `CLOSED` immediately | Check the WS origin allow-list includes `http://localhost:3000`. Browser DevTools → Network → WS tab shows the handshake. |
| Ride never gets matched | The driver must be **online** with a recent location near the pickup. Confirm `GET /api/v1/location/drivers/nearby?...` returns the driver. Topics: `docker exec rideflow-kafka kafka-topics --bootstrap-server localhost:9092 --list`. |
| Payment stays "Processing…" | Settlement is event-driven off `ride.completed`; give it a few seconds. Check `docker compose logs payment-service`. |
| Frontend env errors | Copy `frontend/.env.local.example` → `frontend/.env.local` (only needed when running the frontend outside compose). |

---

## Roadmap (next rounds)

1. **Observability**: Prometheus scrape + Grafana dashboards (dispatch latency, lock contention, WS connections, DLQ depth).
2. **Kubernetes**: Kustomize bases/overlays + Helm chart (the `k8s/` tree is scaffolded).
3. **Security hardening**: RS256/JWKS, HttpOnly cookie sessions, service-to-service auth.
4. **Real payments**: swap the simulated settlement for a Stripe sandbox integration.
5. **Integration/e2e tests**: Testcontainers across the consume → dispatch → settle → notify path.

---

## License

MIT.
