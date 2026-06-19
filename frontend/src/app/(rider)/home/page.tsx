"use client";

import dynamic from "next/dynamic";
import { useEffect, useMemo, useState } from "react";
import { AlertCircle } from "lucide-react";

import { useActiveRide } from "@/application/ride/useActiveRide";
import type { LatLng } from "@/domain/geo/types";
import type { VehicleType } from "@/domain/ride/types";
import { getSession } from "@/lib/auth/session";
import { ActiveRidePanel } from "@/ui/components/ride/ActiveRidePanel";
import { RideRequestPanel } from "@/ui/components/ride/RideRequestPanel";
import { Skeleton } from "@/ui/components/ui/skeleton";

// Leaflet touches `window` at import time — load the map only on the client.
const RiderMap = dynamic(() => import("@/ui/components/map/RiderMap"), {
  ssr: false,
  loading: () => <Skeleton className="h-[420px] w-full rounded-md" />,
});

export default function RiderHomePage() {
  const { ride, trip, start, reset } = useActiveRide();

  const [riderId, setRiderId] = useState<string | undefined>(undefined);
  const [pickup, setPickup] = useState<LatLng | null>(null);
  const [dropoff, setDropoff] = useState<LatLng | null>(null);
  const [vehicleType, setVehicleType] = useState<VehicleType>("STANDARD");
  const [geoError, setGeoError] = useState<string | null>(null);

  // Session id (rider UUID) is in a cookie — read on the client only.
  useEffect(() => {
    const session = getSession();
    if (session?.role === "RIDER") setRiderId(session.userId);
  }, []);

  // Seed the pickup from the device location.
  useEffect(() => {
    if (typeof navigator === "undefined" || !navigator.geolocation) {
      setGeoError("Geolocation isn't available in this browser.");
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setPickup({ lat: pos.coords.latitude, lng: pos.coords.longitude });
        setGeoError(null);
      },
      (err) => {
        setGeoError(
          err.code === err.PERMISSION_DENIED
            ? "Location permission denied — enable it to set your pickup."
            : "Couldn't determine your location.",
        );
      },
      { enableHighAccuracy: true, timeout: 15_000 },
    );
  }, []);

  // The ride lifecycle drives the layout: request form, then live tracking.
  const tracking = Boolean(ride);

  // While requesting, the route pins come from local state; once a ride is
  // active, they come from the ride record so a refresh still draws the map.
  const mapPickup = useMemo<LatLng | null>(
    () => (ride ? { lat: ride.pickupLat, lng: ride.pickupLng } : pickup),
    [ride, pickup],
  );
  const mapDropoff = useMemo<LatLng | null>(
    () => (ride ? { lat: ride.dropoffLat, lng: ride.dropoffLng } : dropoff),
    [ride, dropoff],
  );

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Request a ride</h1>
        <p className="text-sm text-muted-foreground">
          {tracking ? "Tracking your ride." : "Set your destination and pick a ride."}
        </p>
      </div>

      {geoError && !tracking ? (
        <div className="flex items-center gap-2 rounded-md border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-amber-800 dark:border-amber-900/50 dark:bg-amber-950/40 dark:text-amber-300">
          <AlertCircle className="h-4 w-4 shrink-0" />
          {geoError}
        </div>
      ) : null}

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="lg:col-span-2">
          <RiderMap
            pickup={mapPickup}
            dropoff={mapDropoff}
            onPickDropoff={tracking ? undefined : setDropoff}
          />
        </div>
        <div>
          {ride ? (
            <ActiveRidePanel ride={ride} trip={trip} onReset={reset} />
          ) : (
            <RideRequestPanel
              riderId={riderId}
              pickup={pickup}
              dropoff={dropoff}
              vehicleType={vehicleType}
              onVehicleChange={setVehicleType}
              onRequested={start}
            />
          )}
        </div>
      </div>
    </div>
  );
}
