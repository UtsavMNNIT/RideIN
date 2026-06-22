/**
 * Pricing domain types — the frontend mirror of pricing-service DTOs
 * (com.rideflow.pricing.api). Field names match the backend JSON exactly.
 */

import type { VehicleType } from "@/domain/ride/types";

/**
 * A per-vehicle-class rate card, returned by GET /v1/rate-cards. Read-only:
 * pricing-service exposes no write path (rates are an ops-controlled migration),
 * so the admin tariffs view renders these but cannot edit them.
 */
export type RateCardResponse = {
  vehicleType: VehicleType;
  currency:    string;
  baseFare:    number;
  perKm:       number;
  perMinute:   number;
  minimumFare: number;
  bookingFee:  number;
};
