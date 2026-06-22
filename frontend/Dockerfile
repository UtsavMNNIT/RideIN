# =============================================================================
# Frontend Dockerfile — used only when we self-host (Vercel is the default).
# Multi-stage build leveraging Next.js's `output: 'standalone'` to ship a
# minimal runtime image (~150 MB vs. ~1 GB for the full node_modules).
# =============================================================================

# Pin node by major + LTS minor. Alpine for size; if a native dep refuses to
# build, swap to `node:20-slim` (debian-based).
ARG NODE_VERSION=20-alpine

# ----------------------------- STAGE 1: deps --------------------------------
FROM node:${NODE_VERSION} AS deps
WORKDIR /app

# Cache npm install on the lockfile only — code changes don't invalidate.
COPY package.json package-lock.json* ./
# `npm install` (not `npm ci`): the lockfile is generated on the maintainer's
# Windows host, which omits some Linux-only optional native deps (e.g.
# @emnapi/*), so strict `npm ci` fails on this Alpine/Linux image. `npm install`
# reconciles those platform binaries; deps are still pinned by the lockfile.
RUN npm install --no-audit --no-fund

# ----------------------------- STAGE 2: build -------------------------------
FROM node:${NODE_VERSION} AS build
WORKDIR /app

ENV NEXT_TELEMETRY_DISABLED=1

# Build-time env: CI provides safe placeholders; Vercel overrides at deploy.
ARG NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
ARG NEXT_PUBLIC_WS_BASE_URL=ws://localhost:8086
ARG NEXT_PUBLIC_MAP_PROVIDER=osm
ARG NEXT_PUBLIC_APP_NAME=RideFlow
ARG NEXT_PUBLIC_APP_VERSION=0.0.0
ENV NEXT_PUBLIC_API_BASE_URL=$NEXT_PUBLIC_API_BASE_URL \
    NEXT_PUBLIC_WS_BASE_URL=$NEXT_PUBLIC_WS_BASE_URL  \
    NEXT_PUBLIC_MAP_PROVIDER=$NEXT_PUBLIC_MAP_PROVIDER \
    NEXT_PUBLIC_APP_NAME=$NEXT_PUBLIC_APP_NAME \
    NEXT_PUBLIC_APP_VERSION=$NEXT_PUBLIC_APP_VERSION

COPY --from=deps /app/node_modules ./node_modules
COPY . .

# `next build` reads `output: 'standalone'` from next.config — add it there
# when wiring the self-host path. Without it, this image still works but is
# larger because it copies the full node_modules.
RUN npm run build

# ----------------------------- STAGE 3: runtime -----------------------------
FROM node:${NODE_VERSION} AS runtime
WORKDIR /app

ENV NODE_ENV=production \
    NEXT_TELEMETRY_DISABLED=1 \
    PORT=3000 \
    HOSTNAME=0.0.0.0

# Non-root user — Node.js Alpine ships `node` (uid 1000).
RUN addgroup -S app && adduser -S app -G app

# Standalone output: minimal node_modules subset + server.js bootstrap.
COPY --from=build --chown=app:app /app/public           ./public
COPY --from=build --chown=app:app /app/.next/standalone ./
COPY --from=build --chown=app:app /app/.next/static     ./.next/static

USER app
EXPOSE 3000

LABEL org.opencontainers.image.title="rideflow-frontend"
LABEL org.opencontainers.image.description="RideFlow Next.js frontend"
LABEL org.opencontainers.image.licenses="MIT"

CMD ["node", "server.js"]
