"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";

import type { DriverResponse } from "@/domain/driver/types";
import { getDriverProfile, setDriverProfile } from "@/lib/auth/session";

export const driverQueryKey = (id: string | undefined) => ["driver", id] as const;

/**
 * The current driver, shared across dashboard widgets via the React Query cache.
 *
 * There's no `GET /drivers/{id}` on the backend yet, so the source of truth is
 * the profile persisted at login; every online/offline/location response is
 * written back into this cache (see {@link applyDriver}) to keep it fresh.
 */
export function useDriver() {
  const seed = getDriverProfile();
  return useQuery({
    queryKey: driverQueryKey(seed?.id),
    queryFn: async () => {
      const profile = getDriverProfile();
      if (!profile) throw new Error("No driver session");
      return profile;
    },
    initialData: seed ?? undefined,
    enabled: Boolean(seed),
    staleTime: Infinity,
  });
}

/** Hook returning a callback that writes a fresh DriverResponse to cache + storage. */
export function useApplyDriver() {
  const qc = useQueryClient();
  return (driver: DriverResponse) => {
    setDriverProfile(driver);
    qc.setQueryData(driverQueryKey(driver.id), driver);
  };
}
