"use client";

import "leaflet/dist/leaflet.css";

import { useEffect } from "react";
import {
  CircleMarker,
  MapContainer,
  Marker,
  Polyline,
  Popup,
  TileLayer,
  useMap,
  useMapEvents,
} from "react-leaflet";
import { MapPin } from "lucide-react";

import type { LatLng } from "@/domain/geo/types";
import { applyLeafletIconFix } from "@/infrastructure/leaflet/icons";

// Runs once on the client when this (ssr:false) chunk loads.
applyLeafletIconFix();

/** Turns a tap anywhere on the map into a dropoff selection. */
function ClickToSetDropoff({ onPick }: { onPick: (p: LatLng) => void }) {
  useMapEvents({
    click(e) {
      onPick({ lat: e.latlng.lat, lng: e.latlng.lng });
    },
  });
  return null;
}

/** Keeps both pickup and dropoff in view; falls back to centering on pickup. */
function FitView({ pickup, dropoff }: { pickup: LatLng; dropoff: LatLng | null }) {
  const map = useMap();
  useEffect(() => {
    if (dropoff) {
      map.fitBounds(
        [
          [pickup.lat, pickup.lng],
          [dropoff.lat, dropoff.lng],
        ],
        { padding: [48, 48] },
      );
    } else {
      map.setView([pickup.lat, pickup.lng], map.getZoom());
    }
  }, [map, pickup.lat, pickup.lng, dropoff]);
  return null;
}

export type RiderMapProps = {
  pickup: LatLng | null;
  dropoff: LatLng | null;
  driver?: LatLng | null;
  /** When set, tapping the map selects the dropoff. Omit to make the map read-only. */
  onPickDropoff?: (p: LatLng) => void;
};

/**
 * Map for the rider flow: a green dot at the pickup (the rider's location), a pin
 * at the tapped dropoff, and a dashed line between them. Optionally shows the
 * driver. Must be loaded via `next/dynamic({ ssr: false })` — Leaflet needs
 * `window`. The empty state is the caller's job (renders before the first fix).
 */
export default function RiderMap({ pickup, dropoff, driver, onPickDropoff }: RiderMapProps) {
  if (!pickup) {
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
        center={[pickup.lat, pickup.lng]}
        zoom={14}
        scrollWheelZoom
        className="h-full w-full"
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        <CircleMarker
          center={[pickup.lat, pickup.lng]}
          radius={8}
          pathOptions={{ color: "#16a34a", fillColor: "#16a34a", fillOpacity: 0.9 }}
        >
          <Popup>Pickup</Popup>
        </CircleMarker>

        {dropoff ? (
          <Marker position={[dropoff.lat, dropoff.lng]}>
            <Popup>Dropoff</Popup>
          </Marker>
        ) : null}

        {driver ? (
          <CircleMarker
            center={[driver.lat, driver.lng]}
            radius={8}
            pathOptions={{ color: "#2563eb", fillColor: "#2563eb", fillOpacity: 0.9 }}
          >
            <Popup>Your driver</Popup>
          </CircleMarker>
        ) : null}

        {dropoff ? (
          <Polyline
            positions={[
              [pickup.lat, pickup.lng],
              [dropoff.lat, dropoff.lng],
            ]}
            pathOptions={{ color: "#6b7280", weight: 3, dashArray: "6 8" }}
          />
        ) : null}

        {onPickDropoff ? <ClickToSetDropoff onPick={onPickDropoff} /> : null}
        <FitView pickup={pickup} dropoff={dropoff} />
      </MapContainer>
    </div>
  );
}
