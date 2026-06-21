"use client";

import { useQuery } from "@tanstack/react-query";

import { riderRidesUrl } from "@/application/ride/endpoints";
import type { RideResponse } from "@/domain/ride/types";
import { api } from "@/lib/api/client";

export const PAGE_SIZE = 20;

export const rideHistoryQueryKey = (riderId: string | undefined, page: number) =>
  ["ride-history", riderId, page] as const;

/**
 * One page of the rider's ride history, newest-first. rider-service returns a
 * plain array (not a Spring Page), so there's no total count — the caller drives
 * "load more" by paging until a short page (< PAGE_SIZE) comes back.
 */
export function useRideHistory(riderId: string | undefined, page = 0) {
  return useQuery({
    queryKey: rideHistoryQueryKey(riderId, page),
    queryFn: () =>
      api.get<RideResponse[]>(
        `${riderRidesUrl(riderId!)}?page=${page}&size=${PAGE_SIZE}`,
      ),
    enabled: Boolean(riderId),
    staleTime: 30_000,
  });
}
