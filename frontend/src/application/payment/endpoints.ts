/**
 * Payment endpoint builders. payment-service sits behind the api-gateway at
 * /api/v1/payments/** (rewritten to /v1/payments/**), reached through the
 * gateway base baked into the api client — so these are plain relative paths,
 * matching the quote/trip convention in ../ride/endpoints.ts.
 */

/** GET the rider's saved (mock) cards. */
export function paymentMethodsPath(userId: string): string {
  return `/api/v1/payments/methods?userId=${encodeURIComponent(userId)}`;
}

/** POST to add a mock saved card. */
export function addPaymentMethodPath(): string {
  return "/api/v1/payments/methods";
}

/** GET the settlement / receipt for a ride. */
export function ridePaymentPath(rideId: string): string {
  return `/api/v1/payments/rides/${rideId}`;
}
