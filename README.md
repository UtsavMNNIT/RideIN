# RideFlow

A production-patterned Uber-style ride-matching platform: 7 backend microservices, a Next.js 15 frontend, Kafka event backbone, Redis-geo dispatch, WebSocket live updates. **Built as a resume showcase** — designed for production rigor, scoped pragmatically.

> **Status**: notification-service runs end-to-end (Kafka consume → Redis pub/sub fan-out → WebSocket push). Frontend rider home demonstrates the live flow with a one-click trigger. Other backend services are designed and scaffolded; see [What's implemented vs. designed](#whats-implemented-vs-designed).

---

## Quickstart — 5 minutes

```bash
# 1. Bring up the backend stack (Postgres, Redis, Kafka, notification-service)
docker compose up -d --build

# 2. Wait ~60s for the JVM to boot and Flyway migrations to apply
docker compose logs -f notification-service | grep -m1 "Started NotificationServiceApplication"

# 3. Install + run the frontend
cd frontend
npm install
npm run dev
```

Open **http://localhost:3000**, click **Continue as Rider**, then on the home page click either trigger button. Notifications appear live in the inbound stream.

| What | URL |
|---|---|
| Frontend | http://localhost:3000 |
| Notification API | http://localhost:8086 |
| Kafka UI (optional) | `docker compose --profile tools up -d kafka-ui` → http://localhost:8089 |
| Postgres | `localhost:5432` (user `rideflow` / pass `rideflow`) |

---

## What this demonstrates

The single working flow exercises **the exact production pipeline**, not a toy mock:

```
[ Browser ]
    │ click "Trigger via Kafka"
    ▼
[ POST /v1/demo/notify ]                  (Next.js → notification-service)
    │
    ▼
[ KafkaTemplate.send ]                    (idempotent producer, acks=all)
    │
    ▼
[ topic: matching.ride-assigned ]
    │
    ▼
[ @KafkaListener consumer ]               (manual ack, exponential backoff + DLQ)
    │
    ▼
[ PublishNotificationUseCase ]            (idempotency claim + persist, one TX)
    │
    ├─ Postgres: notifications + processed_events  (durable + dedupe)
    │
    ▼
[ Redis PUBLISH notify:user:<id> ]        (cross-replica fan-out)
    │
    ▼
[ RedisNotificationSubscriber ]           (any replica that holds the WS session)
    │
    ▼
[ WebSocket frame to the browser ]        (per-session lock, backpressure-safe)
```

Patterns visible in code (the link is to the most-implemented service):

- **Hexagonal / Clean Architecture** — `api`/`application`/`domain`/`infrastructure` layout in `backend/notification-service/src/main/java/com/rideflow/notification/`
- **Transactional outbox** schema — `backend/matching-service/src/main/resources/db/migration/V1__init.sql`
- **Idempotent consumer** — `notification-service/.../application/usecase/PublishNotificationUseCase.java`
- **Distributed lock with Redisson** — `backend/matching-service/src/main/java/com/rideflow/matching/infrastructure/cache/redis/RedissonDriverLockService.java`
- **DLQ with retry policy** — `notification-service/.../infrastructure/messaging/kafka/topology/KafkaConsumerConfig.java`
- **Cross-replica WS fan-out** — `notification-service/.../infrastructure/cache/redis/RedisNotificationBroadcaster.java`
- **Server Components + Client Components** split — `frontend/src/app/(rider)/layout.tsx` and `frontend/src/app/(rider)/RiderShell.tsx`

---

## Tech stack

| Layer | Tech |
|---|---|
| Backend | Java 21, Spring Boot 3.3, Maven multi-module, Spring Kafka, Spring Data JPA + Hibernate 6, Lettuce, Redisson |
| Data | PostgreSQL 16 (schema per service), Redis 7 (Geo + cache), Apache Kafka |
| Frontend | Next.js 15 (App Router), TypeScript strict, TailwindCSS, Shadcn UI, React Query, next-themes |
| Infra | Docker Compose, multi-stage layered Dockerfile (eclipse-temurin distroless-ish), bash TCP healthchecks |
| CI/CD | GitHub Actions — backend-ci, frontend-ci, docker-publish (matrix, cosign-signed, SBOM, Trivy), CodeQL, Dependabot |

---

## Project structure

```
backend/
├── pom.xml                       parent POM (BOM + plugin management)
├── Dockerfile                    shared multi-stage build, parameterised by MODULE
├── shared/
│   └── common-events/            EventEnvelope, Topics, EventTypes (the cross-service contract)
├── notification-service/         IMPLEMENTED — Kafka consumer + WebSocket fan-out + REST backfill
├── matching-service/             Distributed lock implemented; dispatcher designed
├── pricing-service/              Designed (formula, APIs, lifecycle)
├── location-service/             Redis-geo adapter + Kafka consumer on disk
├── api-gateway/                  Skeleton
├── rider-service/                Skeleton
└── driver-service/               Skeleton

frontend/
└── src/
    ├── app/                      App Router — (auth), (rider), (driver), (admin) route groups
    ├── ui/                       components, providers, styles
    ├── lib/                      ws/, api/, auth/, utils/, query/
    ├── config/                   typed env via Zod (fails-fast on missing vars)
    └── middleware.ts             edge auth gate (role-prefix protection)

infra/docker/                     Postgres init scripts, Kafka topic bootstrap, Redis configs
.github/workflows/                CI/CD (see below)
```

---

## What's implemented vs. designed

Honest accounting — a senior reviewer should know what they're looking at.

| Asset | Status |
|---|---|
| **notification-service** | ✅ End-to-end (35 files): domain, app, JPA, Redis pub/sub, WebSocket, Kafka consumer, demo controller |
| **matching-service** distributed lock | ✅ Redisson port + adapter + config + metrics |
| **matching-service** dispatcher (M-1 → M-6) | 📐 Designed (see commit history / docs); not yet wired |
| **pricing-service** | 📐 Designed end-to-end; no code |
| **rider/driver/api-gateway/location** services | 📐 Skeletons with sibling-consistent folder layout |
| **Frontend Phase F-1 (Layout)** | ✅ Routing, providers, Shadcn theme, role-aware shell |
| **Frontend demo (rider home)** | ✅ Live WebSocket + trigger buttons |
| **Frontend F-2 → F-5** (real auth, real ride flow, maps, full tracking) | 📐 Designed |
| **CI/CD pipeline** | ✅ 6 workflows + Dependabot — backend-ci, frontend-ci, docker-publish (cosign + SBOM + Trivy), release, CodeQL, dep-review |
| **Tests** | ❌ Skeleton directories exist; no specs yet |
| **Observability** | ❌ Actuator/Prometheus endpoint exposed; Grafana dashboards not wired |

Design responses for the unbuilt slices are preserved in the commit history and can be turned into ADRs on request.

---

## Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `notification-service` won't start; Flyway error | Postgres init hadn't finished. `docker compose down -v` to wipe volumes, then `up -d --build` again. |
| WebSocket shows `CLOSED` immediately | Check `WS_ALLOWED_ORIGINS` includes `http://localhost:3000`. Browser DevTools → Network → WS tab shows the handshake. |
| `Trigger via Kafka` button returns 200 but no message arrives | Verify topic exists: `docker exec rideflow-kafka kafka-topics --bootstrap-server localhost:9092 --list`. The `kafka-init` container creates them on first boot. |
| TypeScript errors in IDE before `npm install` | Expected — types aren't on disk until `cd frontend && npm install`. |
| Frontend build complains about env vars | Copy `frontend/.env.local.example` → `frontend/.env.local`. Defaults work for local Docker. |

---

## Roadmap (next slices)

1. **Auth**: `common-security` JWT verifier, replace dev-mode `?userId=` handshake
2. **API gateway**: rate limiting (Redis token bucket), JWT verification, CORS allow-list
3. **matching-service**: finish M-1 → M-6 (domain + use case + persistence + consumer + producer)
4. **Frontend F-3 → F-5**: real ride request form, React Leaflet map, ride tracking
5. **Observability**: Prometheus scrape + Grafana dashboards for dispatch latency, lock contention, WS connection count, DLQ depth
6. **Tests**: Testcontainers integration tests for the consume → broadcast happy path

---

## License

MIT.
# RideIN
