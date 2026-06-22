"use client";

import { useQuery } from "@tanstack/react-query";

import type { OperatorMetrics } from "@/domain/admin/types";
import { api } from "@/lib/api/client";

/**
 * Operator metrics from rider-service (via the gateway at /api/v1/admin/metrics,
 * served from the ride read-model). The endpoint is public for the demo — there
 * is no ADMIN role yet. Polls every 10s so the dashboard stays live.
 *
 * `isPlaceholder` is kept (always false) so the overview page can keep its
 * "preview" banner logic unchanged; it simply never shows now.
 */
export function useMetrics() {
  const query = useQuery({
    queryKey: ["operator-metrics"],
    queryFn: () => api.get<OperatorMetrics>("/api/v1/admin/metrics"),
    refetchInterval: 10_000,
    staleTime: 5_000,
  });
  return { ...query, data: query.data, isPlaceholder: false as const };
}
