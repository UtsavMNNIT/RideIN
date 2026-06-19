"use client";

import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";

import type { RequestRideRequest, RideResponse } from "@/domain/ride/types";
import { api, ApiError } from "@/lib/api/client";

import { riderRidesUrl } from "./endpoints";

function errorMessage(e: unknown): string {
  if (e instanceof ApiError) {
    if (e.status === 409) return "You already have an active ride.";
    if (e.status === 401 || e.status === 403) return "Your session expired. Please sign in again.";
    if (e.status === 400) return "Those pickup/dropoff coordinates look invalid.";
    return `Couldn't request a ride (HTTP ${e.status}).`;
  }
  return "Network error. Please try again.";
}

/**
 * Request a ride. Returns the freshly created RideResponse (status REQUESTED);
 * the caller hands it to useActiveRide().start to begin live tracking.
 */
export function useRequestRide(riderId: string | undefined) {
  return useMutation({
    mutationFn: (body: RequestRideRequest) =>
      api.post<RideResponse>(riderRidesUrl(riderId!), body),
    onError: (e) => toast.error(errorMessage(e)),
  });
}
