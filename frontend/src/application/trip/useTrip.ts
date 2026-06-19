"use client";

import { useQuery } from "@tanstack/react-query";

import { tripPath } from "@/application/ride/endpoints";
import type { TripResponse } from "@/domain/ride/types";
import { api } from "@/lib/api/client";

export const tripQueryKey = (rideId: string | undefined) => ["trip", rideId] as const;

/**
 * Poll the post-assignment trip detail (driver id, match score, status). Acts as
 * the resync fallback for the WebSocket: while enabled it refetches every 5s, so
 * a dropped frame still gets reconciled. Enable only once a ride is assigned —
 * trip-service has no Trip aggregate before then (it would 404).
 */
export function useTrip(rideId: string | undefined, enabled: boolean) {
  return useQuery({
    queryKey: tripQueryKey(rideId),
    queryFn: () => api.get<TripResponse>(tripPath(rideId!)),
    enabled: enabled && Boolean(rideId),
    refetchInterval: enabled ? 5_000 : false,
    staleTime: 0,
  });
}
