"use client";

import type { OperatorMetrics } from "@/domain/admin/types";

/**
 * Operator metrics — PLACEHOLDER.
 *
 * No admin/metrics endpoint exists in any service, and the backend has no ADMIN
 * role. This hook returns static, clearly-labeled sample figures (flagged via
 * `isPlaceholder`) so the overview dashboard can be built now. The page renders
 * a visible "Preview — awaiting backend" banner whenever `isPlaceholder` is true.
 *
 * To make it real later: replace the constant with a useQuery against
 * `GET /v1/admin/metrics` and drop `isPlaceholder`. Shape matches
 * {@link OperatorMetrics}.
 */
const SAMPLE: OperatorMetrics = {
  activeRides:     142,
  driversOnline:   389,
  totalRiders:     12840,
  completionRate:  0.93,
  avgDispatchSecs: 28,
};

export function useMetrics() {
  return { data: SAMPLE, isPlaceholder: true as const };
}
