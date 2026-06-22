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

/** POST to cancel an active trip (rider- or driver-initiated). */
export function tripCancelPath(rideId: string): string {
  return `/api/v1/trips/${rideId}/cancel`;
}

/**
 * Driver trip state-machine transitions (trip-service). Identity comes from the
 * gateway-injected X-User-Id header (derived from the driver's JWT), so none of
 * these take a body except {@link tripCompletePath}. Each returns a TripResponse.
 */
export function tripAcceptPath(rideId: string): string {
  return `/api/v1/trips/${rideId}/accept`;
}

export function tripRejectPath(rideId: string): string {
  return `/api/v1/trips/${rideId}/reject`;
}

export function tripArrivedPath(rideId: string): string {
  return `/api/v1/trips/${rideId}/arrived`;
}

export function tripStartPath(rideId: string): string {
  return `/api/v1/trips/${rideId}/start`;
}

/** POST to complete a trip; body carries the final distance/duration. */
export function tripCompletePath(rideId: string): string {
  return `/api/v1/trips/${rideId}/complete`;
}
