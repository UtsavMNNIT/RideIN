"use client";

import "leaflet/dist/leaflet.css";

import { useEffect } from "react";
import { MapContainer, Marker, Popup, TileLayer, useMap } from "react-leaflet";
import { MapPin } from "lucide-react";

import { applyLeafletIconFix } from "@/infrastructure/leaflet/icons";
import type { Coords } from "@/application/driver/useLocationHeartbeat";

// Runs once on the client when this (ssr:false) chunk loads.
applyLeafletIconFix();

/** Keeps the map centered on the driver as their position updates. */
function Recenter({ lat, lng }: { lat: number; lng: number }) {
  const map = useMap();
  useEffect(() => {
    map.setView([lat, lng], map.getZoom());
  }, [lat, lng, map]);
  return null;
}

/**
 * Live map of the driver's current position. Rendered only after the first GPS
 * fix; the empty/loading state is handled by the caller via `coords === null`.
 * Must be loaded with `next/dynamic({ ssr: false })` — Leaflet needs `window`.
 */
export default function DriverMap({ coords }: { coords: Coords | null }) {
  if (!coords) {
    return (
      <div className="flex h-[420px] w-full flex-col items-center justify-center gap-2 rounded-md border bg-muted/30 text-sm text-muted-foreground">
        <MapPin className="h-6 w-6" />
        Waiting for your location…
      </div>
    );
  }

  return (
    <div className="h-[420px] w-full overflow-hidden rounded-md border">
      <MapContainer
        center={[coords.lat, coords.lng]}
        zoom={15}
        scrollWheelZoom
        className="h-full w-full"
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <Marker position={[coords.lat, coords.lng]}>
          <Popup>You are here</Popup>
        </Marker>
        <Recenter lat={coords.lat} lng={coords.lng} />
      </MapContainer>
    </div>
  );
}
