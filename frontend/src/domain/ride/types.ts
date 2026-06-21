/**
 * Ride / quote / trip domain types — the frontend mirror of the backend DTOs
 * across rider-service (RideResponse), pricing-service (QuoteResponse) and
 * trip-service (TripResponse). Field names match the backend JSON exactly.
 */

export type VehicleType = "STANDARD" | "XL" | "PREMIUM";

/** Lifecycle of a ride, authored by rider-service and advanced by events. */
export type RideStatus =
  | "REQUESTED"
  | "ASSIGNED"
  | "STARTED"
  | "COMPLETED"
  | "CANCELLED"
  | "NO_DRIVERS_FOUND";

/** Statuses past which a ride no longer changes — stop tracking. */
export const TERMINAL_RIDE_STATUSES: readonly RideStatus[] = [
  "COMPLETED",
  "CANCELLED",
  "NO_DRIVERS_FOUND",
];

export function isTerminalRideStatus(status: RideStatus): boolean {
  return TERMINAL_RIDE_STATUSES.includes(status);
}

/** Body of POST /v1/riders/{riderId}/rides. */
export type RequestRideRequest = {
  pickupLat:   number;
  pickupLng:   number;
  dropoffLat:  number;
  dropoffLng:  number;
  vehicleType: VehicleType;
};

/** Returned by the request-ride and ride-history endpoints. */
export type RideResponse = {
  id:                    string;
  riderId:               string;
  pickupLat:             number;
  pickupLng:             number;
  dropoffLat:            number;
  dropoffLng:            number;
  vehicleType:           VehicleType;
  status:                RideStatus;
  assignedDriverId?:     string | null;
  fareTotal?:            number | null;
  currency?:             string | null;
  finalDistanceMeters?:  number | null;
  finalDurationSeconds?: number | null;
  requestedAt:           string;
  updatedAt:             string;
};

/** Body of POST /v1/quotes. riderId optional (anonymous quotes allowed). */
export type QuoteRequest = {
  riderId?:    string;
  pickupLat:   number;
  pickupLng:   number;
  dropoffLat:  number;
  dropoffLng:  number;
  vehicleType: VehicleType;
};

export type FareBreakdown = {
  currency:        string;
  baseFare:        number;
  distanceFare:    number;
  timeFare:        number;
  subtotal:        number;
  surgeMultiplier: number;
  surgedSubtotal:  number;
  bookingFee:      number;
  total:           number;
};

/** Returned by POST /v1/quotes. */
export type QuoteResponse = {
  quoteId:        string;
  rideId?:        string | null;
  riderId?:       string | null;
  vehicleType:    VehicleType;
  estDistanceKm:  number;
  estDurationMin: number;
  fare:           FareBreakdown;
  validUntil:     string;
  createdAt:      string;
};

/** Post-assignment lifecycle, owned by trip-service. */
export type TripStatus =
  | "OFFERED"
  | "ACCEPTED"
  | "ARRIVED"
  | "STARTED"
  | "COMPLETED"
  | "REJECTED"
  | "EXPIRED"
  | "CANCELLED";

/** Returned by GET /v1/trips/{rideId} — richer detail for an active ride. */
export type TripResponse = {
  rideId:                string;
  riderId:               string;
  driverId:              string;
  vehicleType:           VehicleType;
  status:                TripStatus;
  matchScore?:           number | null;
  rejectReason?:         string | null;
  cancelledBy?:          string | null;
  cancelReason?:         string | null;
  finalDistanceMeters?:  number | null;
  finalDurationSeconds?: number | null;
  offeredAt?:            string | null;
  offerExpiresAt?:       string | null;
  acceptedAt?:           string | null;
  arrivedAt?:            string | null;
  startedAt?:            string | null;
  completedAt?:          string | null;
  rejectedAt?:           string | null;
  cancelledAt?:          string | null;
};

/** Body of POST /v1/trips/{rideId}/cancel (rider- or driver-initiated). */
export type CancelTripRequest = {
  reason?: string;
};

/** Body of POST /v1/trips/{rideId}/complete (driver-initiated). Both required. */
export type CompleteTripRequest = {
  finalDistanceMeters:  number;
  finalDurationSeconds: number;
};
