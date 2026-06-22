"use client";

import type { EarningsSummary } from "@/domain/driver/earnings";

/**
 * Driver earnings — PLACEHOLDER.
 *
 * driver-service / trip-service expose no earnings, payout, or completed-trip
 * history endpoint today, so there is nothing real to fetch. This hook returns
 * static, clearly-labeled sample data (flagged via `isPlaceholder`) so the
 * earnings UI can be built and reviewed now. The page renders a visible
 * "Preview — awaiting backend" banner whenever `isPlaceholder` is true.
 *
 * To make it real later: replace the constant below with a useQuery against
 * `GET /v1/drivers/{driverId}/earnings` and drop `isPlaceholder`. The shape
 * already matches {@link EarningsSummary}.
 */
const SAMPLE: EarningsSummary = {
  currency:       "INR",
  totalEarnings:  4820.5,
  ridesCompleted: 37,
  onlineHours:    22.5,
  avgFare:        130.28,
  trips: [
    { id: "t-1024", completedAt: "2026-06-21T09:14:00Z", vehicleType: "STANDARD", distanceMeters: 5400,  fareTotal: 142.0, currency: "INR" },
    { id: "t-1023", completedAt: "2026-06-21T08:02:00Z", vehicleType: "STANDARD", distanceMeters: 3120,  fareTotal: 98.5,  currency: "INR" },
    { id: "t-1021", completedAt: "2026-06-20T19:48:00Z", vehicleType: "XL",       distanceMeters: 11200, fareTotal: 286.0, currency: "INR" },
    { id: "t-1019", completedAt: "2026-06-20T17:33:00Z", vehicleType: "PREMIUM",  distanceMeters: 8700,  fareTotal: 312.5, currency: "INR" },
    { id: "t-1015", completedAt: "2026-06-20T13:05:00Z", vehicleType: "STANDARD", distanceMeters: 2400,  fareTotal: 74.0,  currency: "INR" },
  ],
};

export function useEarnings(_driverId: string | undefined) {
  return { data: SAMPLE, isPlaceholder: true as const };
}
