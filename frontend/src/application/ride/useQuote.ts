"use client";

import { useMutation } from "@tanstack/react-query";

import type { QuoteRequest, QuoteResponse } from "@/domain/ride/types";
import { api } from "@/lib/api/client";

import { quotePath } from "./endpoints";

/**
 * Fetch an up-front fare estimate from pricing-service. Exposed as a mutation
 * (not a query) because the rider triggers it imperatively whenever the dropoff
 * or vehicle type changes — there's no stable key to cache against.
 */
export function useQuote() {
  return useMutation({
    mutationFn: (body: QuoteRequest) => api.post<QuoteResponse>(quotePath(), body),
  });
}
