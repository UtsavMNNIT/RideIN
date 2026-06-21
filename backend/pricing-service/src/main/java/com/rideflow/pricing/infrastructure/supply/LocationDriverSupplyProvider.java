package com.rideflow.pricing.infrastructure.supply;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rideflow.pricing.application.port.out.DriverSupplyProvider;
import com.rideflow.pricing.domain.model.GeoPoint;
import com.rideflow.pricing.domain.model.VehicleType;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * HTTP adapter for {@link DriverSupplyProvider} backed by location-service's
 * {@code GET /api/v1/location/drivers/nearby} endpoint. The supply count is the
 * size of the returned nearby-driver list (already filtered to AVAILABLE drivers
 * of the requested vehicle class by the geo index).
 *
 * <p>Tight connect/read timeouts: surge is on the synchronous quote path, so a
 * slow or down location-service must not stall pricing. Any failure propagates
 * as a {@link RuntimeException}, which {@code DemandSurgeProvider} catches and
 * absorbs by falling back to the configured multiplier.
 */
@Component
public class LocationDriverSupplyProvider implements DriverSupplyProvider {

    /** Cap mirrors the location endpoint's own {@code limit} ceiling. */
    private static final int LOOKUP_LIMIT = 50;

    private final RestClient client;

    public LocationDriverSupplyProvider(
            @Value("${rideflow.pricing.location-service.base-url:http://location-service:8082}") String baseUrl,
            @Value("${rideflow.pricing.location-service.timeout-ms:300}") long timeoutMs) {

        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(Duration.ofMillis(timeoutMs));
        rf.setReadTimeout(Duration.ofMillis(timeoutMs));

        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(rf)
                .build();
    }

    @Override
    public int availableDriversNear(VehicleType vehicleType, GeoPoint pickup, int radiusMeters) {
        NearbyDriver[] body = client.get()
                .uri(uri -> uri.path("/api/v1/location/drivers/nearby")
                        .queryParam("lat", pickup.lat())
                        .queryParam("lng", pickup.lng())
                        .queryParam("radiusMeters", radiusMeters)
                        .queryParam("vehicleType", vehicleType.name())
                        .queryParam("limit", LOOKUP_LIMIT)
                        .build())
                .retrieve()
                .body(NearbyDriver[].class);

        return body == null ? 0 : body.length;
    }

    /** Only the count matters here; tolerate any extra fields the endpoint returns. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NearbyDriver() {}
}
