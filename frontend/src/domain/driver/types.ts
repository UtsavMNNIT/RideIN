/**
 * Driver domain types — the frontend mirror of the backend driver-service DTOs
 * (com.rideflow.driver.api). Kept deliberately small: only the fields the
 * dashboard actually reads. Extend as more of the driver surface is built.
 */

/** Lifecycle state of a driver. Matches the backend DriverAvailability enum. */
export type DriverAvailability = "OFFLINE" | "ONLINE" | "ON_TRIP";

export type VehicleType = "STANDARD" | "XL" | "PREMIUM";

/** Returned by register / online / offline / location endpoints. */
export type DriverResponse = {
  id:              string;
  email:           string;
  phone:           string;
  fullName:        string;
  vehicleType:     VehicleType;
  vehiclePlate:    string;
  availability:    DriverAvailability;
  lastLat?:        number | null;
  lastLng?:        number | null;
  lastLocationAt?: string | null;
  createdAt:       string;
};

/** Returned by POST /v1/drivers/login. */
export type LoginResponse = {
  accessToken:      string;
  tokenType:        string;
  expiresInSeconds: number;
  expiresAt:        string;
  driver:           DriverResponse;
};

/** Body of PUT /v1/drivers/{id}/location. */
export type UpdateLocationRequest = {
  lat:             number;
  lng:             number;
  headingDegrees?: number | null;
  speedMps?:       number | null;
  capturedAt?:     string;
};
