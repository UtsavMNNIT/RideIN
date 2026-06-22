"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";

import {
  tripAcceptPath,
  tripArrivedPath,
  tripCancelPath,
  tripCompletePath,
  tripRejectPath,
  tripStartPath,
} from "@/application/ride/endpoints";
import { tripQueryKey, useTrip } from "@/application/trip/useTrip";
import { useApplyDriver } from "@/application/driver/useDriver";
import type { TripResponse, TripStatus } from "@/domain/ride/types";
import { api, ApiError } from "@/lib/api/client";
import { getDriverProfile } from "@/lib/auth/session";

const STORE_ACTIVE_TRIP = "rf_driver_active_trip";

/** Statuses past which a trip no longer changes — stop driving the panel. */
const TERMINAL: readonly TripStatus[] = [
  "COMPLETED",
  "REJECTED",
  "EXPIRED",
  "CANCELLED",
];

/**
 * The bits of a ride offer the driver UI keeps locally after accepting. The
 * trip-service TripResponse carries status + timestamps but NOT the route
 * coordinates, so we stash them from the RIDE_MATCHED frame to (a) draw the
 * route and (b) compute the final distance at completion.
 */
export type DriverTripContext = {
  rideId:     string;
  pickupLat:  number;
  pickupLng:  number;
  dropoffLat: number;
  dropoffLng: number;
};

function loadActiveTrip(): DriverTripContext | null {
  if (typeof window === "undefined") return null;
  const raw = window.localStorage.getItem(STORE_ACTIVE_TRIP);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as DriverTripContext;
  } catch {
    return null;
  }
}

function saveActiveTrip(ctx: DriverTripContext | null): void {
  if (typeof window === "undefined") return;
  if (ctx) window.localStorage.setItem(STORE_ACTIVE_TRIP, JSON.stringify(ctx));
  else window.localStorage.removeItem(STORE_ACTIVE_TRIP);
}

/** Great-circle distance in metres — the backend wants a final distance ≥ 0. */
function haversineMeters(ctx: DriverTripContext): number {
  const R = 6_371_000;
  const toRad = (d: number) => (d * Math.PI) / 180;
  const dLat = toRad(ctx.dropoffLat - ctx.pickupLat);
  const dLng = toRad(ctx.dropoffLng - ctx.pickupLng);
  const s =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(ctx.pickupLat)) *
      Math.cos(toRad(ctx.dropoffLat)) *
      Math.sin(dLng / 2) ** 2;
  return Math.round(2 * R * Math.asin(Math.sqrt(s)));
}

function actionError(e: unknown): string {
  if (e instanceof ApiError) {
    if (e.status === 409) return "That ride is no longer available.";
    if (e.status === 404) return "This ride no longer exists.";
    if (e.status === 401 || e.status === 403)
      return "Your session expired. Please sign in again.";
    return `Action failed (HTTP ${e.status}).`;
  }
  return "Network error. Please try again.";
}

/**
 * Owns the driver's accept → arrive → start → complete trip lifecycle. Mirrors
 * the rider's useActiveRide: the active trip survives a refresh via localStorage,
 * its live status is polled from trip-service, and availability is updated
 * optimistically (there's no GET /drivers/{id}, and the server flips ON_TRIP /
 * ONLINE asynchronously over Kafka).
 */
export function useDriverTrip() {
  const qc = useQueryClient();
  const applyDriver = useApplyDriver();

  const [activeTrip, setActiveTrip] = useState<DriverTripContext | null>(null);
  // Offers the driver already accepted or declined — filtered out of the feed.
  const [handled, setHandled] = useState<Set<string>>(new Set());

  // Hydrate once on the client.
  useEffect(() => {
    setActiveTrip(loadActiveTrip());
  }, []);

  // Poll live trip status while a trip is active (also catches rider cancels).
  const tripQuery = useTrip(activeTrip?.rideId, Boolean(activeTrip));
  const trip = tripQuery.data ?? null;

  const setAvailability = useCallback(
    (availability: "ON_TRIP" | "ONLINE") => {
      const driver = getDriverProfile();
      if (driver && driver.availability !== availability) {
        applyDriver({ ...driver, availability });
      }
    },
    [applyDriver],
  );

  const markHandled = useCallback((rideId: string) => {
    setHandled((prev) => {
      const next = new Set(prev);
      next.add(rideId);
      return next;
    });
  }, []);

  const begin = useCallback(
    (ctx: DriverTripContext, result: TripResponse) => {
      saveActiveTrip(ctx);
      setActiveTrip(ctx);
      qc.setQueryData(tripQueryKey(ctx.rideId), result);
      markHandled(ctx.rideId);
      setAvailability("ON_TRIP");
    },
    [qc, markHandled, setAvailability],
  );

  const dismiss = useCallback(() => {
    saveActiveTrip(null);
    setActiveTrip(null);
  }, []);

  // When the trip reaches a terminal status (incl. a rider-side cancel observed
  // via polling), release availability back to ONLINE.
  useEffect(() => {
    if (trip && TERMINAL.includes(trip.status)) setAvailability("ONLINE");
  }, [trip, setAvailability]);

  const accept = useMutation({
    mutationFn: (ctx: DriverTripContext) =>
      api.post<TripResponse>(tripAcceptPath(ctx.rideId)),
    onSuccess: (result, ctx) => {
      begin(ctx, result);
      toast.success("Ride accepted — head to the pickup.");
    },
    onError: (e) => toast.error(actionError(e)),
  });

  const reject = useMutation({
    mutationFn: (rideId: string) =>
      api.post<TripResponse>(tripRejectPath(rideId)),
    onSuccess: (_result, rideId) => {
      markHandled(rideId);
      toast.success("Offer declined.");
    },
    onError: (e) => toast.error(actionError(e)),
  });

  const advance = useCallback(
    (result: TripResponse) => {
      if (activeTrip) qc.setQueryData(tripQueryKey(activeTrip.rideId), result);
    },
    [qc, activeTrip],
  );

  const arrived = useMutation({
    mutationFn: () => api.post<TripResponse>(tripArrivedPath(activeTrip!.rideId)),
    onSuccess: advance,
    onError: (e) => toast.error(actionError(e)),
  });

  const start = useMutation({
    mutationFn: () => api.post<TripResponse>(tripStartPath(activeTrip!.rideId)),
    onSuccess: advance,
    onError: (e) => toast.error(actionError(e)),
  });

  const complete = useMutation({
    mutationFn: () => {
      const ctx = activeTrip!;
      const startedAt = trip?.startedAt ? new Date(trip.startedAt).getTime() : null;
      const finalDurationSeconds = startedAt
        ? Math.max(1, Math.round((Date.now() - startedAt) / 1000))
        : 1;
      return api.post<TripResponse>(tripCompletePath(ctx.rideId), {
        finalDistanceMeters: haversineMeters(ctx),
        finalDurationSeconds,
      });
    },
    onSuccess: (result) => {
      advance(result);
      setAvailability("ONLINE");
      toast.success("Trip completed.");
    },
    onError: (e) => toast.error(actionError(e)),
  });

  const cancel = useMutation({
    mutationFn: (reason?: string) =>
      api.post<TripResponse>(tripCancelPath(activeTrip!.rideId), { reason }),
    onSuccess: (result) => {
      advance(result);
      setAvailability("ONLINE");
      toast.success("Trip cancelled.");
    },
    onError: (e) => toast.error(actionError(e)),
  });

  const isTerminal = trip ? TERMINAL.includes(trip.status) : false;

  return useMemo(
    () => ({
      activeTrip,
      trip,
      isTerminal,
      handled,
      accept,
      reject,
      arrived,
      start,
      complete,
      cancel,
      dismiss,
    }),
    [activeTrip, trip, isTerminal, handled, accept, reject, arrived, start, complete, cancel, dismiss],
  );
}
