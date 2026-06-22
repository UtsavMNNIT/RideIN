"use client";

import { useQueries } from "@tanstack/react-query";

import { quotePath } from "@/application/ride/endpoints";
import type { QuoteRequest, QuoteResponse, VehicleType } from "@/domain/ride/types";
import { api } from "@/lib/api/client";

/**
 * pricing-service has no surge endpoint — surge is a *calculated* field that
 * only surfaces inside a fare breakdown. But the backend's surge model is
 * location-agnostic (a flat multiplier per vehicle class), so quoting any fixed
 * route returns the true current multiplier. We fan out one quote per class and
 * read fare.surgeMultiplier — giving the operator a real, live surge view
 * without a dedicated API.
 */
const VEHICLE_TYPES: readonly VehicleType[] = ["STANDARD", "XL", "PREMIUM"];

// A short representative route. Coordinates are arbitrary (surge is flat), but a
// real distance keeps the quote valid (rejects zero-length trips).
const PROBE: Omit<QuoteRequest, "vehicleType"> = {
  pickupLat:  28.6139,
  pickupLng:  77.2090,
  dropoffLat: 28.6280,
  dropoffLng: 77.2410,
};

export type SurgeRow = {
  vehicleType: VehicleType;
  multiplier:  number;
  currency:    string;
};

export const surgeQueryKey = (vehicleType: VehicleType) =>
  ["surge", vehicleType] as const;

/**
 * Live surge multiplier per vehicle class, derived from probe quotes. Refetches
 * every 15s so the dynamic (demand-based) multiplier stays current.
 */
export function useSurge() {
  const queries = useQueries({
    queries: VEHICLE_TYPES.map((vehicleType) => ({
      queryKey: surgeQueryKey(vehicleType),
      queryFn: async (): Promise<SurgeRow> => {
        const quote = await api.post<QuoteResponse>(quotePath(), {
          ...PROBE,
          vehicleType,
        });
        return {
          vehicleType,
          multiplier: quote.fare.surgeMultiplier,
          currency:   quote.fare.currency,
        };
      },
      refetchInterval: 15_000,
      staleTime: 0,
    })),
  });

  return {
    rows: queries
      .map((q) => q.data)
      .filter((row): row is SurgeRow => Boolean(row)),
    isPending: queries.some((q) => q.isPending),
    isError:   queries.every((q) => q.isError),
  };
}
