"use client";

import { useQuery } from "@tanstack/react-query";

import type { Payment } from "@/domain/payment/types";
import { isTerminalPaymentStatus } from "@/domain/payment/types";
import { ApiError, api } from "@/lib/api/client";

import { ridePaymentPath } from "./endpoints";

export const ridePaymentQueryKey = (rideId: string | undefined) =>
  ["ride-payment", rideId] as const;

/**
 * Poll the settlement for a ride. Settlement is event-driven (off
 * `ride.completed`), so right after completion the payment may not exist yet —
 * a 404 is expected and surfaces as `null` while we keep polling. Once the
 * status is terminal (SETTLED/FAILED/CANCELLED) polling stops.
 *
 * Enable only once the ride is COMPLETED.
 */
export function useRidePayment(rideId: string | undefined, enabled: boolean) {
  return useQuery({
    queryKey: ridePaymentQueryKey(rideId),
    queryFn: async () => {
      try {
        return await api.get<Payment>(ridePaymentPath(rideId!));
      } catch (e) {
        // Not settled yet — keep polling rather than erroring the card.
        if (e instanceof ApiError && e.status === 404) return null;
        throw e;
      }
    },
    enabled: enabled && Boolean(rideId),
    // Keep polling until the payment exists and reaches a terminal state.
    refetchInterval: (query) => {
      if (!enabled) return false;
      const data = query.state.data as Payment | null | undefined;
      return data && isTerminalPaymentStatus(data.status) ? false : 2_000;
    },
    staleTime: 0,
  });
}
