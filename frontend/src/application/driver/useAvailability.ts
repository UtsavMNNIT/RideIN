"use client";

import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";

import type { DriverResponse } from "@/domain/driver/types";
import { api, ApiError } from "@/lib/api/client";

import { driverUrl } from "./endpoints";
import { useApplyDriver } from "./useDriver";

function errorMessage(e: unknown): string {
  if (e instanceof ApiError) {
    if (e.status === 409) return "Can't change availability right now — you may be on a trip.";
    if (e.status === 401 || e.status === 403) return "Your session expired. Please sign in again.";
    return `Request failed (HTTP ${e.status}).`;
  }
  return "Network error. Please try again.";
}

/**
 * Go online / offline. Each endpoint returns the updated DriverResponse, which
 * we write straight back into the shared driver cache. The backend rejects a
 * go-offline while ON_TRIP (409) — the UI also disables the button in that state.
 */
export function useAvailability(driverId: string | undefined) {
  const applyDriver = useApplyDriver();

  const goOnline = useMutation({
    mutationFn: () => api.post<DriverResponse>(driverUrl(`/${driverId}/online`)),
    onSuccess: (driver) => {
      applyDriver(driver);
      toast.success("You're online — watching for ride offers.");
    },
    onError: (e) => toast.error(errorMessage(e)),
  });

  const goOffline = useMutation({
    mutationFn: () => api.post<DriverResponse>(driverUrl(`/${driverId}/offline`)),
    onSuccess: (driver) => {
      applyDriver(driver);
      toast.success("You're offline.");
    },
    onError: (e) => toast.error(errorMessage(e)),
  });

  return { goOnline, goOffline };
}
