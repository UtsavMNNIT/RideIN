"use client";

import { useEffect, useRef, useState } from "react";

import type { DriverAvailability, DriverResponse } from "@/domain/driver/types";
import { api } from "@/lib/api/client";

import { driverUrl } from "./endpoints";
import { useApplyDriver } from "./useDriver";

const HEARTBEAT_MS = 5_000;

export type Coords = {
  lat:     number;
  lng:     number;
  heading: number | null;
  speed:   number | null;
};

export type HeartbeatState = {
  /** Latest GPS fix, or null before the first one. */
  coords: Coords | null;
  /** A human-readable problem (permission denied, unsupported), or null. */
  error: string | null;
};

/**
 * Watches the browser's geolocation and, while the driver is ONLINE/ON_TRIP,
 * pushes a location heartbeat to driver-service every {@link HEARTBEAT_MS}.
 * The returned DriverResponse refreshes the shared driver cache. Location pings
 * are skipped while OFFLINE (the backend would reject them anyway).
 */
export function useLocationHeartbeat(
  driverId: string | undefined,
  availability: DriverAvailability | undefined,
): HeartbeatState {
  const applyDriver = useApplyDriver();
  const [coords, setCoords] = useState<Coords | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Refs so the heartbeat interval reads the latest values without re-subscribing.
  const latest = useRef<Coords | null>(null);
  const availabilityRef = useRef(availability);
  availabilityRef.current = availability;

  // Continuously track position.
  useEffect(() => {
    if (typeof navigator === "undefined" || !navigator.geolocation) {
      setError("Geolocation isn't available in this browser.");
      return;
    }
    const watchId = navigator.geolocation.watchPosition(
      (pos) => {
        const next: Coords = {
          lat:     pos.coords.latitude,
          lng:     pos.coords.longitude,
          heading: Number.isFinite(pos.coords.heading) ? pos.coords.heading : null,
          speed:   Number.isFinite(pos.coords.speed) ? pos.coords.speed : null,
        };
        latest.current = next;
        setCoords(next);
        setError(null);
      },
      (err) => {
        setError(
          err.code === err.PERMISSION_DENIED
            ? "Location permission denied — enable it to go online."
            : "Couldn't determine your location.",
        );
      },
      { enableHighAccuracy: true, maximumAge: 5_000, timeout: 15_000 },
    );
    return () => navigator.geolocation.clearWatch(watchId);
  }, []);

  // Heartbeat: PUT location on an interval while available.
  useEffect(() => {
    if (!driverId) return;
    const interval = setInterval(async () => {
      const a = availabilityRef.current;
      const c = latest.current;
      if (!c || (a !== "ONLINE" && a !== "ON_TRIP")) return;
      try {
        const driver = await api.put<DriverResponse>(driverUrl(`/${driverId}/location`), {
          lat:            c.lat,
          lng:            c.lng,
          headingDegrees: c.heading ?? undefined,
          speedMps:       c.speed ?? undefined,
          capturedAt:     new Date().toISOString(),
        });
        applyDriver(driver);
      } catch {
        // Transient failure — the next tick retries; don't spam the user.
      }
    }, HEARTBEAT_MS);
    return () => clearInterval(interval);
  }, [driverId, applyDriver]);

  return { coords, error };
}
