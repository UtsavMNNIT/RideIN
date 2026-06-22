"use client";

import { useEffect, useMemo, useState } from "react";
import { AlertCircle, History, Navigation } from "lucide-react";

import { useActiveRide } from "@/application/ride/useActiveRide";
import { useRideHistory } from "@/application/rider/useRideHistory";
import type { LatLng } from "@/domain/geo/types";
import type { VehicleType } from "@/domain/ride/types";
import { getSession } from "@/lib/auth/session";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/ui/components/ui/card";
import { Input } from "@/ui/components/ui/input";
import { ActiveRidePanel } from "@/ui/components/ride/ActiveRidePanel";
import { RideRequestPanel } from "@/ui/components/ride/RideRequestPanel";

/** Round a coordinate so near-identical dropoffs collapse into one rebook chip. */
function destKey(lat: number, lng: number): string {
  return `${lat.toFixed(3)},${lng.toFixed(3)}`;
}

export default function RiderRequestPage() {
  const { ride, trip, start, reset } = useActiveRide();

  const [riderId, setRiderId] = useState<string | undefined>(undefined);
  const [pickup, setPickup] = useState<LatLng | null>(null);
  const [dropoff, setDropoff] = useState<LatLng | null>(null);
  const [latStr, setLatStr] = useState("");
  const [lngStr, setLngStr] = useState("");
  const [vehicleType, setVehicleType] = useState<VehicleType>("STANDARD");
  const [geoError, setGeoError] = useState<string | null>(null);

  useEffect(() => {
    const session = getSession();
    if (session?.role === "RIDER") setRiderId(session.userId);
  }, []);

  // Seed pickup from the device location (same as /home).
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

  // Keep the dropoff pin in sync with the manual lat/lng inputs.
  useEffect(() => {
    const lat = Number(latStr);
    const lng = Number(lngStr);
    if (
      latStr.trim() !== "" &&
      lngStr.trim() !== "" &&
      Number.isFinite(lat) &&
      Number.isFinite(lng) &&
      lat >= -90 &&
      lat <= 90 &&
      lng >= -180 &&
      lng <= 180
    ) {
      setDropoff({ lat, lng });
    } else {
      setDropoff(null);
    }
  }, [latStr, lngStr]);

  const setDest = (lat: number, lng: number) => {
    setLatStr(String(lat));
    setLngStr(String(lng));
  };

  // Recent distinct dropoffs for one-tap rebooking.
  const { data: history } = useRideHistory(riderId, 0);
  const recentDests = useMemo(() => {
    if (!history) return [];
    const seen = new Set<string>();
    const out: LatLng[] = [];
    for (const r of history) {
      const key = destKey(r.dropoffLat, r.dropoffLng);
      if (seen.has(key)) continue;
      seen.add(key);
      out.push({ lat: r.dropoffLat, lng: r.dropoffLng });
      if (out.length >= 5) break;
    }
    return out;
  }, [history]);

  if (ride) {
    return (
      <div className="mx-auto max-w-2xl space-y-6">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Your ride</h1>
          <p className="text-sm text-muted-foreground">Tracking your active booking.</p>
        </div>
        <ActiveRidePanel ride={ride} trip={trip} onReset={reset} />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Request a ride</h1>
        <p className="text-sm text-muted-foreground">
          Enter a destination or rebook a recent trip.
        </p>
      </div>

      {geoError ? (
        <div className="flex items-center gap-2 rounded-md border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-amber-800 dark:border-amber-900/50 dark:bg-amber-950/40 dark:text-amber-300">
          <AlertCircle className="h-4 w-4 shrink-0" />
          {geoError}
        </div>
      ) : null}

      <Card>
        <CardHeader>
          <CardTitle>Destination</CardTitle>
          <CardDescription>
            Pickup is your current location. Enter the dropoff coordinates.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1">
              <label htmlFor="dropoff-lat" className="text-xs text-muted-foreground">
                Dropoff latitude
              </label>
              <Input
                id="dropoff-lat"
                inputMode="decimal"
                placeholder="28.6280"
                value={latStr}
                onChange={(e) => setLatStr(e.target.value)}
              />
            </div>
            <div className="space-y-1">
              <label htmlFor="dropoff-lng" className="text-xs text-muted-foreground">
                Dropoff longitude
              </label>
              <Input
                id="dropoff-lng"
                inputMode="decimal"
                placeholder="77.2410"
                value={lngStr}
                onChange={(e) => setLngStr(e.target.value)}
              />
            </div>
          </div>

          {recentDests.length > 0 ? (
            <div className="space-y-1.5">
              <p className="flex items-center gap-1 text-xs font-medium text-muted-foreground">
                <History className="h-3 w-3" />
                Recent destinations
              </p>
              <div className="flex flex-wrap gap-2">
                {recentDests.map((d) => (
                  <button
                    key={destKey(d.lat, d.lng)}
                    type="button"
                    onClick={() => setDest(d.lat, d.lng)}
                    className="inline-flex items-center gap-1 rounded-full border border-input px-3 py-1 font-mono text-xs transition-colors hover:bg-accent hover:text-accent-foreground"
                  >
                    <Navigation className="h-3 w-3" />
                    {d.lat.toFixed(4)}, {d.lng.toFixed(4)}
                  </button>
                ))}
              </div>
            </div>
          ) : null}
        </CardContent>
      </Card>

      <RideRequestPanel
        riderId={riderId}
        pickup={pickup}
        dropoff={dropoff}
        vehicleType={vehicleType}
        onVehicleChange={setVehicleType}
        onRequested={start}
      />
    </div>
  );
}
