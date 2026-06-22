# =============================================================================
# Shared multi-stage Dockerfile for every RideFlow backend service.
#
# Usage:
#   docker buildx build \
#       --build-arg MODULE=notification-service \
#       -t rideflow-notification-service:dev backend/
#
# Build runs inside a maven:3.9 image — no local Maven required, no mvnw to
# maintain. Module is built with -am (also-make) so its `common-events` (and
# any future inter-module deps) are compiled in the right order.
# =============================================================================

ARG JAVA_VERSION=21

# ----------------------------- STAGE 1: build -------------------------------
FROM maven:3.9-eclipse-temurin-${JAVA_VERSION} AS build
ARG MODULE
WORKDIR /workspace

# Copy ONLY pom.xmls first so dependency resolution is cached separately from
# source. Edits to a .java file won't re-download Maven deps.
# All module poms first. The parent reactor lists every module, so Maven needs
# each pom present to build the project graph — even for a single -pl build.
COPY pom.xml                                ./
COPY shared/common-events/pom.xml           shared/common-events/
COPY notification-service/pom.xml           notification-service/
COPY driver-service/pom.xml                 driver-service/
COPY location-service/pom.xml               location-service/
COPY rider-service/pom.xml                  rider-service/
COPY matching-service/pom.xml               matching-service/
COPY pricing-service/pom.xml                pricing-service/
COPY trip-service/pom.xml                   trip-service/
COPY payment-service/pom.xml                payment-service/
COPY api-gateway/pom.xml                    api-gateway/

# Warm the local Maven repo for the WHOLE reactor, so this layer is stable
# across MODULE values. `--fail-never` so a transient miss doesn't abort the
# warm-up; the real build below catches genuine breakage.
RUN mvn -B -ntp dependency:go-offline -DskipTests --fail-never || true

# Now bring in source. Cached layers above are untouched on a code-only change.
COPY shared/             shared/
COPY notification-service/ notification-service/
COPY driver-service/     driver-service/
COPY location-service/   location-service/
COPY rider-service/      rider-service/
COPY matching-service/   matching-service/
COPY pricing-service/    pricing-service/
COPY trip-service/       trip-service/
COPY payment-service/    payment-service/
COPY api-gateway/        api-gateway/

# Build the requested module and its dependencies.
RUN test -n "${MODULE}" || (echo "ERROR: --build-arg MODULE is required" && exit 1) && \
    mvn -B -ntp -pl "${MODULE}" -am package -DskipTests

# Extract Spring Boot layers so the runtime stage can COPY them as distinct
# image layers — small deltas re-pull only the changed layer at deploy time.
RUN mkdir -p /workspace/extracted && \
    cd /workspace/extracted && \
    java -Djarmode=layertools -jar /workspace/${MODULE}/target/${MODULE}.jar extract

# ----------------------------- STAGE 2: runtime -----------------------------
FROM eclipse-temurin:${JAVA_VERSION}-jre AS runtime
ARG MODULE
WORKDIR /app

# Ordered from most-stable to least-stable layer for cache efficiency.
COPY --from=build /workspace/extracted/dependencies/          ./
COPY --from=build /workspace/extracted/spring-boot-loader/    ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/           ./

LABEL org.opencontainers.image.title="rideflow-${MODULE}"
LABEL org.opencontainers.image.description="RideFlow ${MODULE}"
LABEL org.opencontainers.image.licenses="MIT"

# Non-root user (Temurin image ships only `root`; add one).
RUN groupadd --system app && useradd --system --gid app --no-create-home app && \
    chown -R app:app /app
USER app

# JVM defaults tuned for containers — respects cgroup memory limit.
ENV JAVA_TOOL_OPTIONS="\
    -XX:MaxRAMPercentage=75.0 \
    -XX:+ExitOnOutOfMemoryError \
    -XX:+UseContainerSupport \
    -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
