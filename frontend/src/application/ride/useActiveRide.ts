"use client";

import { useCallback, useEffect, useState } from "react";

import { useTrip } from "@/application/trip/useTrip";
import type { RideResponse, RideStatus } from "@/domain/ride/types";
import { isTerminalRideStatus } from "@/domain/ride/types";
import { useWs } from "@/lib/ws/WsProvider";

const STORE_ACTIVE_RIDE = "rf_rider_active_ride";

/** WS notification type → the ride status it implies. */
const WS_TYPE_TO_STATUS: Record<string, RideStatus> = {
  RIDE_MATCHED:     "ASSIGNED",
  RIDE_STARTED:     "STARTED",
  RIDE_COMPLETED:   "COMPLETED",
  RIDE_CANCELLED:   "CANCELLED",
  NO_DRIVERS_FOUND: "NO_DRIVERS_FOUND",
};

/** Monotonic rank so we only ever advance a ride forward, never backward. */
const RANK: Record<RideStatus, number> = {
  REQUESTED:        0,
  ASSIGNED:         1,
  STARTED:          2,
  COMPLETED:        3,
  CANCELLED:        3,
  NO_DRIVERS_FOUND: 3,
};

function loadActiveRide(): RideResponse | null {
  if (typeof window === "undefined") return null;
  const raw = window.localStorage.getItem(STORE_ACTIVE_RIDE);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as RideResponse;
  } catch {
    return null;
  }
}

function saveActiveRide(ride: RideResponse | null): void {
  if (typeof window === "undefined") return;
  if (ride) {
    window.localStorage.setItem(STORE_ACTIVE_RIDE, JSON.stringify(ride));
  } else {
    window.localStorage.removeItem(STORE_ACTIVE_RIDE);
  }
}

/**
 * Single source of truth for the rider's in-flight ride. Hydrates from
 * localStorage (so a refresh resumes tracking), advances the status from
 * WebSocket frames addressed to this ride, and pulls richer detail (driver,
 * match score) from trip-service once assigned.
 */
export function useActiveRide() {
  const { messages } = useWs();
  const [ride, setRide] = useState<RideResponse | null>(null);

  // Hydrate once on the client — localStorage isn't available during SSR.
  useEffect(() => {
    setRide(loadActiveRide());
  }, []);

  const start = useCallback((next: RideResponse) => {
    saveActiveRide(next);
    setRide(next);
  }, []);

  const reset = useCallback(() => {
    saveActiveRide(null);
    setRide(null);
  }, []);

  const setStatus = useCallback((status: RideStatus) => {
    setRide((prev) => {
      if (!prev || prev.status === status) return prev;
      const updated = { ...prev, status, updatedAt: new Date().toISOString() };
      saveActiveRide(updated);
      return updated;
    });
  }, []);

  // Advance from WS frames. Order-independent: take the furthest-along status
  // among all frames for this ride and only move forward.
  useEffect(() => {
    if (!ride) return;
    let best = ride.status;
    for (const m of messages) {
      if (m.rideId !== ride.id) continue;
      const mapped = WS_TYPE_TO_STATUS[m.type];
      if (mapped && RANK[mapped] > RANK[best]) best = mapped;
    }
    if (best !== ride.status) setStatus(best);
  }, [messages, ride, setStatus]);

  // Trip detail is meaningful (and exists in trip-service) only mid-flight.
  const trackTrip = Boolean(ride) && (ride!.status === "ASSIGNED" || ride!.status === "STARTED");
  const trip = useTrip(ride?.id, trackTrip);

  return {
    ride,
    status: ride?.status ?? null,
    isTerminal: ride ? isTerminalRideStatus(ride.status) : false,
    trip: trip.data ?? null,
    start,
    reset,
  };
}
