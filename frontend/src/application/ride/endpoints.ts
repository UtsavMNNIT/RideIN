import { riderUrl } from "@/application/rider/endpoints";

/**
 * Ride / quote / trip endpoint builders.
 *
 * - Rides belong to rider-service, so they reuse {@link riderUrl} (gateway-aware).
 * - Quotes (pricing-service) and trips (trip-service) are reached through the
 *   gateway base baked into the api client, so they're plain relative
 *   /api/v1/** paths that the client prefixes with NEXT_PUBLIC_API_BASE_URL.
 */

/** POST to request a ride / GET the rider's ride history. */
export function riderRidesUrl(riderId: string): string {
  return riderUrl(`/${riderId}/rides`);
}

/** POST to compute an up-front fare estimate. */
export function quotePath(): string {
  return "/api/v1/quotes";
}

/** GET the post-assignment trip detail (driver, ETA, status). */
export function tripPath(rideId: string): string {
  return `/api/v1/trips/${rideId}`;
}

/** POST to cancel an active trip (rider-initiated). */
export function tripCancelPath(rideId: string): string {
  return `/api/v1/trips/${rideId}/cancel`;
}
