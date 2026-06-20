/**
 * Rider domain types — the frontend mirror of the backend rider-service DTOs
 * (com.rideflow.rider.api). Only the fields the rider flow actually reads.
 */

/** Returned by POST /v1/riders (register) and nested in the login response. */
export type RiderResponse = {
  id:        string;
  email:     string;
  phone:     string;
  fullName:  string;
  createdAt: string;
};

/** Body of POST /v1/riders (register). */
export type RegisterRiderRequest = {
  email:    string;
  phone:    string;
  fullName: string;
  password: string;
};

/** Returned by POST /v1/riders/login. */
export type RiderLoginResponse = {
  accessToken:      string;
  tokenType:        string;
  expiresInSeconds: number;
  expiresAt:        string;
  rider:            RiderResponse;
};
