# RideFlow — Project Overview & Interview Guide

> A plain-English guide to what RideFlow is, what it does, how it's built, why those
> choices were made, and a large interviewer-style Q&A to help you defend every decision.
> (For the hands-on "how to run it" instructions, see [`README.md`](./README.md).)

---

## 1. What is RideFlow? (the non-technical version)

RideFlow is a **ride-hailing platform** — think Uber, Ola, or Lyft — built from scratch as a
full, working product. There are three kinds of users:

- **Riders** book a trip from a pickup point to a destination, see the fare upfront, watch
  their assigned driver on a live map, and get a receipt when the trip ends.
- **Drivers** go online, share their location, receive trip offers, accept them, and drive
  the trip through to completion — then see their earnings.
- **Operators / Admins** watch live business metrics (active rides, drivers online, etc.)
  and view pricing rules.

The whole journey works end-to-end: a rider requests a ride → the system instantly finds the
nearest available driver → the driver accepts and drives the trip → the fare is charged
(simulated) → and both people get live notifications the entire time.

**In one sentence:** it's a production-patterned clone of the core Uber experience, where the
entire system — 8 backend services, a website, a message bus, databases, and live updates —
runs together on one machine with a single command.

### Why build something this big?

Anyone can build a CRUD app. RideFlow was built to demonstrate the **hard parts of real
distributed systems**: how independent services talk to each other reliably, how you match a
rider to a driver in real time, how you keep data consistent when things are spread across many
databases, and how you push live updates to thousands of browsers. It's a portfolio piece
designed to prove I can reason about systems, not just write endpoints.

---

## 2. Features implemented

| Area | What works |
|---|---|
| **Rider** | Register / login, request a ride, see an upfront fare quote, live trip tracking on a map, trip receipt, mock payment methods, ride history |
| **Driver** | Register / login, go online/offline, location heartbeat, receive live trip offers, accept → arrive → start → complete a trip, earnings view |
| **Matching** | Real-time dispatch of the nearest online driver using a geospatial search + a fair "radius-ladder" expansion |
| **Pricing** | Upfront fare quotes and final ride pricing (distance + time + surge multiplier) |
| **Payments** | Event-driven, *simulated* fare settlement (authorize → capture → settle), receipts, mock cards |
| **Notifications** | Live WebSocket updates to rider & driver browsers for every step of the journey, plus a notification history |
| **Admin** | Live operator metrics dashboard, read-only rate cards & surge view |
| **Quality** | ~245 backend unit tests; one-command local run via Docker Compose; GitHub Actions CI/CD |

**Honestly scoped (deliberately not "done"):** payments are simulated (no real Stripe),
security is demo-grade (shared-secret JWTs, not RS256/JWKS yet), and the Kubernetes manifests
are scaffolded but not wired. Being clear about this is intentional — see the
Q&A on "what's not production-ready."

---

## 3. How it works — the flow

### In plain language
1. A rider asks for a ride.
2. The system prices it and starts looking for a driver.
3. It finds the closest online driver and offers them the trip.
4. The driver accepts, drives to the rider, starts the trip, and completes it.
5. The fare is settled and a receipt is produced.
6. Throughout, both the rider's and driver's screens update live.

### The technical event flow
The services don't call each other directly for the lifecycle — they communicate by publishing
and consuming **events** on Kafka. This is what makes the system loosely coupled.

```
rider requests ride
   │  POST /v1/riders/{id}/rides            (rider-service, written via outbox)
   ▼  topic: rider.ride-requested
   ├─► pricing-service  → pricing.fare-quoted     (a fare is attached to the ride)
   │        └─► payment-service caches the fare    (creates a PENDING payment)
   ▼
matching-service   (finds nearest online driver via Redis GEOSEARCH + a distributed lock)
   │  topic: matching.ride-assigned
   ▼
trip-service   (creates the offer; driver accepts → arrives → starts → completes)
   │  topics: ride.accepted / ride.started / ride.completed
   ├─► payment-service: on ride.completed, simulate authorize→capture→settle
   │        └─► topic: payment.settled
   ▼
notification-service   (consumes every lifecycle + payment event)
   │  Redis PUBLISH notify:user:<id>  →  WebSocket frame
   ▼
rider & driver browsers update live (status, receipt, notifications)
```

---

## 4. Tech stack — and *why* each choice

### Backend
| Tech | Role | Why this, and why not the alternative |
|---|---|---|
| **Java 21 + Spring Boot 3.3** | All backend services | Spring Boot is the industry standard for enterprise microservices: mature, huge ecosystem (Kafka, JPA, Gateway, Security all first-party). Java 21's virtual threads + records make modern, concise services. *Why not Node.js?* Node is great for I/O but the JVM gives stronger typing, better concurrency for CPU-bound matching logic, and a richer distributed-systems toolkit (Redisson, Spring Kafka). |
| **Apache Kafka** | Event backbone between services | Durable, replayable, ordered-per-partition log — ideal for an event-driven system where a dropped event means a lost ride. *Why not RabbitMQ?* Rabbit is a queue (messages disappear once consumed); Kafka is a log you can replay and have multiple independent consumers read — exactly what "one ride event, many interested services" needs. |
| **PostgreSQL 16 (schema-per-service)** | Each service's own database | Rock-solid relational DB with strong transactional guarantees — needed for the outbox pattern. Schema-per-service keeps services independent without running 8 separate DB servers locally. *Why not one shared DB?* That would couple services together (the #1 microservices anti-pattern). *Why not NoSQL?* The data is highly relational (rides, drivers, payments) and needs ACID transactions. |
| **Redis 7 (two instances: Geo + Cache)** | Geospatial driver index + caching + pub/sub | Redis `GEOSEARCH` does "find nearest drivers" in milliseconds — doing that in Postgres would be far slower. A separate cache instance backs rate-limiting and WebSocket pub/sub. *Why two instances?* Different eviction policies: the geo index must never evict data (`noeviction`), while the cache can drop old keys (`allkeys-lru`). |
| **Redisson** | Distributed lock in matching | When two ride requests arrive at once, a distributed lock stops the *same* driver being assigned to *both*. Redisson provides a battle-tested Redis-backed lock. |
| **Spring Cloud Gateway** | Single API entry point | One front door for routing, JWT auth, CORS, and rate limiting — so individual services don't each re-implement security. It's reactive (non-blocking), so it handles many concurrent connections cheaply. |

### Frontend
| Tech | Role | Why |
|---|---|---|
| **Next.js 15 (App Router)** | The web app | Server + client components, file-based routing, and great DX. SSR-capable for fast first paint; deploys trivially to Vercel. |
| **TypeScript (strict)** | Type safety | Catches whole classes of bugs at compile time; makes the API contract between front and back explicit. |
| **React Query (TanStack)** | Server-state / data fetching | Handles caching, refetching, and loading/error states so I don't hand-roll fetch logic everywhere. |
| **TailwindCSS + Shadcn UI** | Styling & components | Utility-first CSS + accessible, unstyled component primitives = fast, consistent UI without a heavy component library. |
| **React Leaflet (OpenStreetMap)** | Live map | Free, no API key needed for the demo (Mapbox is wired as an option). |

### Infrastructure
| Tech | Role | Why |
|---|---|---|
| **Docker Compose** | One-command local stack | Brings up all 9 services + Kafka + Postgres + Redis together, reproducibly, on any machine. |
| **Multi-stage Dockerfiles** | Small, layered images | Build in a fat JDK image, ship a slim JRE runtime — smaller, faster, more secure images. |
| **GitHub Actions** | CI/CD | Builds, tests, and publishes signed container images (with SBOM + Trivy scanning) automatically. |

---

## 5. Architecture & design patterns (explained simply)

Every backend service follows the same internal shape, and a few key patterns make the
distributed system reliable.

- **Hexagonal / Clean Architecture** — each service is split into `api` (controllers),
  `application` (use cases + ports), `domain` (pure business logic, no framework code), and
  `infrastructure` (DB, Kafka, Redis adapters). *Benefit:* the business rules don't depend on
  Spring or Postgres, so they're easy to test and swap implementations.

- **Transactional Outbox** — when a service needs to both save data *and* publish an event, it
  writes the event into an `outbox` table **inside the same database transaction** as the data.
  A background relay then publishes those rows to Kafka. *Why:* it's impossible to atomically
  "save to DB and send to Kafka" in one step — if you do them separately, a crash in between
  loses the event. The outbox guarantees the event is never lost.

- **Idempotent Consumers** — every consumer records each event ID it has processed in a
  `processed_events` table and skips duplicates. *Why:* Kafka guarantees *at-least-once*
  delivery, so the same event can arrive twice. Idempotency means processing it twice is safe
  (e.g., a driver never gets charged twice).

- **Distributed Lock (Redisson)** — matching locks a driver while assigning them, so concurrent
  requests can't double-book the same driver.

- **Dead Letter Queue (DLQ) + backoff** — if a consumer keeps failing on a "poison" message, it
  retries with backoff and eventually parks the message in a DLQ instead of blocking the whole
  stream.

- **Cross-replica WebSocket fan-out via Redis Pub/Sub** — if you run multiple
  notification-service instances, a user's browser is only connected to one of them. Publishing
  the notification to a Redis channel lets *whichever* instance holds that connection deliver it.

- **API Gateway** — centralizes auth (JWT verification), rate limiting (Redis token bucket),
  CORS, and routing, so the domain services stay focused on business logic.

---

## 6. Interview Q&A

> Grouped from high-level → deep. The answers are written so you can say them out loud.

### A. High-level / "tell me about your project"

**Q: Give me a 60-second overview of RideFlow.**
A: RideFlow is a ride-hailing platform — an Uber-style app. It has a Next.js frontend and a
backend of eight domain microservices behind an API gateway, communicating over Kafka. A rider
requests a ride, the system prices it, finds the nearest online driver using Redis geospatial
search, the driver runs the trip through a state machine, payment is settled, and both users get
live WebSocket updates the whole time. The entire stack runs locally with one Docker Compose
command. I built it to practice the genuinely hard parts of distributed systems — eventual
consistency, real-time matching, and reliable messaging — not just CRUD.

**Q: Why microservices and not a single monolith?**
A: Honestly, for this scale a monolith would be simpler — and I'd say that in a real job. I chose
microservices deliberately to *learn and demonstrate* the patterns: independent deployability,
service-owned databases, and event-driven communication. The services also map cleanly to real
bounded contexts (pricing, matching, trips, payments), so the split is natural rather than
arbitrary.

**Q: What was the hardest part?**
A: Getting the event-driven flow *reliable*. The naive version — save to your DB, then send a
Kafka message — silently loses events when the process crashes between the two steps. Solving
that with the transactional outbox, and then making every consumer idempotent because Kafka is
at-least-once, was the core challenge and the most valuable thing I learned.

### B. Architecture & communication

**Q: How do your services communicate?**
A: Two ways. For the ride *lifecycle*, they're fully decoupled and communicate asynchronously via
Kafka events — e.g. rider-service emits `rider.ride-requested`, and pricing, matching, etc.
react. For direct request/response needs (like the frontend fetching data), calls go through the
API gateway over HTTP. The rule of thumb: commands that trigger a workflow are events; reads are
HTTP.

**Q: Why event-driven instead of services calling each other directly (REST)?**
A: Direct REST chains create tight coupling and cascading failures — if matching is down, the
whole request fails. With events, rider-service just records the request and moves on; if
matching is temporarily down, the event waits in Kafka and is processed when it recovers. It also
lets multiple services react to one event (pricing *and* payment both care about a ride being
requested) without rider-service knowing they exist.

**Q: What's the role of the API gateway? Isn't it a single point of failure?**
A: It centralizes cross-cutting concerns: JWT verification, rate limiting, CORS, and routing, so
the eight domain services don't each re-implement them. Yes, it's a critical path — in
production you'd run multiple gateway replicas behind a load balancer so there's no single
instance to fail. It's stateless (the rate-limit state lives in Redis), so scaling it
horizontally is trivial.

### C. Data consistency (the favorite interviewer topic)

**Q: With a database per service, how do you keep data consistent?**
A: I don't use distributed transactions — they don't scale and create tight coupling. Instead I
rely on **eventual consistency** driven by events, and I make the event flow reliable with two
patterns: the **transactional outbox** (so an event is never lost when state changes) and
**idempotent consumers** (so processing an event twice is harmless). The system converges to a
consistent state even though no single transaction spans services.

**Q: Explain the transactional outbox and why you need it.**
A: When rider-service accepts a ride request, it must do two things: save the ride and publish
`rider.ride-requested`. You can't do both atomically — the DB and Kafka are separate systems. If
you save then publish and crash in between, the ride exists but no one is ever told. So instead I
write the event into an `outbox` table *in the same DB transaction* as the ride. Either both
commit or neither does. A separate relay polls the outbox and publishes to Kafka. Now the event
is guaranteed to eventually be sent.

**Q: If the relay publishes an event but crashes before marking it sent, you'll send it twice. Isn't that a bug?**
A: That's expected, and it's why every consumer is **idempotent**. Each consumer records the
event ID in a `processed_events` table; if it sees the same ID again, it skips it. So
at-least-once delivery plus idempotent consumers gives effectively-once *processing*. Duplicate
delivery is fine because duplicate *handling* is a no-op.

**Q: How do you prevent the same driver being assigned to two riders at once?**
A: A distributed lock in the matching service, via Redisson on Redis. Before assigning a driver,
matching acquires a lock keyed on that driver's ID. A second concurrent request for the same
driver can't get the lock and moves on to the next-best driver. The lock has a timeout so a
crashed holder doesn't deadlock the system.

### D. Real-time matching & location

**Q: How do you find the nearest driver quickly?**
A: Drivers send periodic location pings that get stored in a Redis geospatial index. Redis's
`GEOSEARCH` returns drivers within a radius sorted by distance in milliseconds. Matching uses a
"radius ladder" — it searches a small radius first, and if no suitable driver is found, expands
the radius — so nearby riders get the closest driver but no one is left unmatched.

**Q: Why Redis for location and not Postgres with PostGIS?**
A: Two reasons: speed and write volume. Driver locations update constantly (every few seconds per
driver) and the "find nearest" query runs on every ride request — that's a high-frequency,
low-latency workload that fits Redis's in-memory geo commands perfectly. PostGIS is powerful but
disk-based and heavier for this hot path. Postgres still owns the durable data; Redis is the fast
index.

**Q: How do live updates reach the browser?**
A: WebSockets. The notification-service consumes every lifecycle and payment event from Kafka and
pushes a frame to the relevant user's browser. To work across multiple notification-service
replicas, it uses Redis pub/sub: the event is published to a `notify:user:<id>` channel, and
whichever replica holds that user's WebSocket connection delivers it.

### E. Scaling & reliability

**Q: How would this scale to millions of users?**
A: The services are stateless, so they scale horizontally behind load balancers. Kafka scales by
adding partitions and consumer instances (consumers in a group split partitions). Postgres scales
with read replicas and, if needed, partitioning per region. Redis can be clustered. The matching
service is the trickiest — you'd likely partition by geographic region so each instance only
handles its area's drivers.

**Q: What happens if a service crashes mid-flow?**
A: Because the flow is event-driven, in-flight events stay in Kafka and are processed when the
service restarts (Kafka tracks consumer offsets). The outbox ensures no event was lost on the
producer side, and idempotency ensures replays don't double-process. A "poison" message that
always fails gets retried with backoff and eventually moved to a dead-letter queue so it doesn't
block the stream.

**Q: How do you handle a message that can never be processed (poison pill)?**
A: Each consumer is configured with retry + exponential backoff, and after N failures the message
is routed to a DLQ topic. That isolates the bad message for manual inspection while the rest of
the stream keeps flowing.

### F. Security

**Q: How does authentication work?**
A: Users log in and receive a signed JWT. The API gateway verifies the token on every request and
forwards the user's identity and role to downstream services via headers, so the services trust
the gateway rather than re-verifying. The edge middleware in the Next.js frontend also checks the
token to guard protected routes.

**Q: What are the security weaknesses, and how would you fix them?**
A: It's demo-grade today, and I'm upfront about that. The JWTs use shared HMAC secrets (HS256);
in production I'd move to RS256/JWKS so services verify with a public key and only the auth
service holds the private key. Sessions should be HttpOnly cookies, and I'd add service-to-service
auth (mTLS or signed internal tokens) so a leaked gateway isn't a free pass. None of that is
conceptually hard — it's scoped out to keep the demo runnable.

### G. Frontend

**Q: Why Next.js, and how does the frontend talk to the backend?**
A: Next.js gives me server and client components, file-based routing, and easy deployment to
Vercel. It talks to the backend through the API gateway over HTTP, with React Query managing
caching and loading states, and a WebSocket connection to notification-service for live updates.
All backend URLs come from environment variables, so the same build can point at local Docker or
a deployed backend.

**Q: You deployed the frontend to Vercel but not the backend. Why?**
A: Vercel is purpose-built for frontends and serverless functions — it can't host always-on JVM
services, Kafka, or databases. So the right split is frontend on Vercel, backend on a
container host (a VM running Docker Compose, or Railway/Render with managed Postgres/Redis/Kafka).
The frontend reads its API URL from an env var, so pointing it at the hosted backend is a config
change, not a code change.

### H. "Doubt" / gotcha questions an interviewer might probe

**Q: Isn't all this (outbox, idempotency, locks, Kafka) overkill for a portfolio project?**
A: For *traffic*, absolutely — a monolith would serve this load fine. But the point wasn't to
handle scale today; it was to demonstrate that I understand the failure modes of distributed
systems and the standard patterns that solve them. I'd rather over-engineer a learning project to
show depth than ship a shallow one.

**Q: Your payments are simulated — does that mean payments don't really work?**
A: The *flow* is fully real — payment-service consumes the fare quote, creates a pending payment,
and on trip completion runs an authorize→capture→settle state machine that emits a
`payment.settled` event the rest of the system reacts to. What's simulated is the external
processor call; swapping in a Stripe sandbox is an adapter change behind the same port, because of
the hexagonal design. There's even a configurable failure rate to exercise the failure path.

**Q: How did you test this?**
A: ~245 backend unit tests focused on the domain and use-case layers — the pure business logic,
which is fast and deterministic to test thanks to the hexagonal split. The honest gap is
integration/end-to-end tests across the Kafka flow; the next step there is Testcontainers to spin
up real Kafka/Postgres/Redis and assert the consume→dispatch→settle→notify path.

**Q: If you started over, what would you change?**
A: I'd start as a modular monolith and only split out services when a real boundary demanded it —
you get most of the architectural cleanliness with far less operational overhead. I'd also add the
integration tests earlier, and wire observability (metrics/tracing) from day one, because in an
event-driven system "where did this ride get stuck?" is hard to answer without it.

**Q: Walk me through exactly what happens when a rider taps "Request Ride."**
A: The frontend POSTs to the gateway, which auth-checks and routes to rider-service.
rider-service saves the ride and an outbox row in one transaction, returns 201, and the relay
publishes `rider.ride-requested`. pricing-service consumes it, computes a fare, and emits
`pricing.fare-quoted`; payment-service caches that fare as a pending payment. matching-service
consumes the request, runs a Redis `GEOSEARCH` for the nearest online driver, locks that driver,
and emits `matching.ride-assigned`. trip-service creates an offer; the driver accepts/arrives/
starts/completes, each step emitting an event. On `ride.completed`, payment-service settles and
emits `payment.settled`. notification-service consumes every one of these and pushes live
WebSocket updates to the rider and driver. That's the whole loop.

---

## 7. Quick reference — services & ports

| Service | Port | Responsibility |
|---|---|---|
| api-gateway | 8080 | Ingress: routing, JWT auth, rate limiting, CORS |
| rider-service | 8081 | Rider auth, ride requests (outbox), ride read-model, earnings, admin metrics |
| location-service | 8082 | Redis geo index, nearby-driver queries |
| driver-service | 8083 | Driver auth, availability, location pings |
| matching-service | 8084 | Event-driven dispatch (GEOSEARCH + scoring + lock + outbox) |
| pricing-service | 8085 | Fare quotes + ride pricing |
| notification-service | 8086 | Kafka → Redis pub/sub → WebSocket fan-out |
| trip-service | 8087 | Trip state machine (offer → accept → start → complete) |
| payment-service | 8088 | Simulated fare settlement |
| frontend | 3000 | Next.js web app |

---

*This document is the "explain it to a human" companion to the technical `README.md`. Keep both
in sync as the project evolves.*
