/**
 * Minimal geo primitives shared across the ride-request flow (map, quote,
 * request). Kept provider-agnostic — just plain coordinates.
 */

export type LatLng = {
  lat: number;
  lng: number;
};
