"use client";

import { useQuery } from "@tanstack/react-query";

import type { EarningsSummary } from "@/domain/driver/earnings";
import { api } from "@/lib/api/client";

/**
 * Driver earnings, served from rider-service (it owns the per-ride fare +
 * completion read-model) via the gateway at /api/v1/earnings/drivers/{id}.
 *
 * The backend returns aggregates plus a `recentTrips` sample; this hook maps
 * that to {@link EarningsSummary} (computing avg fare and deriving hours from
 * the summed trip durations). `isPlaceholder` is retained (always false) so the
 * page's banner logic is unchanged.
 */
type EarningsApiResponse = {
  driverId:             string;
  totalFare:            number;
  currency:             string;
  completedTrips:       number;
  totalDistanceMeters:  number;
  totalDurationSeconds: number;
  from:                 string | null;
  to:                   string | null;
  recentTrips: Array<{
    rideId:         string;
    completedAt:    string;
    vehicleType:    string;
    distanceMeters: number;
    fareTotal:      number;
    currency:       string;
  }>;
};

function toSummary(r: EarningsApiResponse): EarningsSummary {
  return {
    currency:       r.currency,
    totalEarnings:  r.totalFare,
    ridesCompleted: r.completedTrips,
    onlineHours:    r.totalDurationSeconds / 3600,
    avgFare:        r.completedTrips > 0 ? r.totalFare / r.completedTrips : 0,
    trips: r.recentTrips.map((t) => ({
      id:             t.rideId,
      completedAt:    t.completedAt,
      vehicleType:    t.vehicleType,
      distanceMeters: t.distanceMeters,
      fareTotal:      t.fareTotal,
      currency:       t.currency,
    })),
  };
}

const EMPTY: EarningsSummary = {
  currency: "INR",
  totalEarnings: 0,
  ridesCompleted: 0,
  onlineHours: 0,
  avgFare: 0,
  trips: [],
};

export function useEarnings(driverId: string | undefined) {
  const query = useQuery({
    queryKey: ["driver-earnings", driverId],
    queryFn: () =>
      api.get<EarningsApiResponse>(`/api/v1/earnings/drivers/${driverId}`).then(toSummary),
    enabled: Boolean(driverId),
    staleTime: 15_000,
  });
  return { data: query.data ?? EMPTY, isLoading: query.isLoading, isPlaceholder: false as const };
}
