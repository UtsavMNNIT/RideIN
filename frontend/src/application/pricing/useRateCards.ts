"use client";

import { useQuery } from "@tanstack/react-query";

import type { RateCardResponse } from "@/domain/pricing/types";
import { api } from "@/lib/api/client";

import { rateCardsPath } from "./endpoints";

export const rateCardsQueryKey = () => ["rate-cards"] as const;

/**
 * The current per-vehicle rate cards from pricing-service. Cached for a minute
 * — rates change rarely (ops-controlled), so there's no value in hammering it.
 */
export function useRateCards() {
  return useQuery({
    queryKey: rateCardsQueryKey(),
    queryFn: () => api.get<RateCardResponse[]>(rateCardsPath()),
    staleTime: 60_000,
  });
}
