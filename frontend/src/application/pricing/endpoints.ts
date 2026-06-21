/**
 * Pricing-service endpoint builders. Pricing rides on the api-gateway base
 * baked into the {@link import("@/lib/api/client").api} client (it prefixes
 * relative paths with NEXT_PUBLIC_API_BASE_URL), so these are plain
 * /api/v1/** paths — same convention as the quote/trip paths in
 * application/ride/endpoints.ts.
 *
 * Both endpoints below are public at the gateway (no JWT required), which is
 * why the admin tariffs/surge views work under the demo-grade ADMIN session.
 */

/** GET the list of per-vehicle rate cards (read-only). */
export function rateCardsPath(): string {
  return "/api/v1/rate-cards";
}
