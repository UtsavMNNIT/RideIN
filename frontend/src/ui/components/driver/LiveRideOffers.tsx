"use client";

import { Loader2, Wifi, WifiOff, Car } from "lucide-react";

import { useWs } from "@/lib/ws/WsProvider";
import type { WsMessage } from "@/lib/ws/WsClient";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/ui/components/ui/card";

const RIDE_TYPES = new Set([
  "RIDE_MATCHED",
  "RIDE_STARTED",
  "RIDE_COMPLETED",
  "RIDE_CANCELLED",
]);

const TYPE_LABEL: Record<string, string> = {
  RIDE_MATCHED:   "New ride offer",
  RIDE_STARTED:   "Trip started",
  RIDE_COMPLETED: "Trip completed",
  RIDE_CANCELLED: "Ride cancelled",
};

function num(payload: Record<string, unknown>, key: string): number | null {
  const v = payload[key];
  return typeof v === "number" ? v : null;
}

function fare(m: WsMessage): string | null {
  const amount = num(m.payload, "estimatedFare");
  if (amount === null) return null;
  const currency = typeof m.payload.currency === "string" ? m.payload.currency : "";
  return `${currency} ${amount.toFixed(2)}`.trim();
}

function coord(m: WsMessage, latKey: string, lngKey: string): string | null {
  const lat = num(m.payload, latKey);
  const lng = num(m.payload, lngKey);
  if (lat === null || lng === null) return null;
  return `${lat.toFixed(4)}, ${lng.toFixed(4)}`;
}

export function LiveRideOffers() {
  const { status, messages } = useWs();
  const offers = messages.filter((m) => RIDE_TYPES.has(m.type));

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <ConnectionDot status={status} />
          Ride offers
        </CardTitle>
        <CardDescription>
          Live offers arrive here while you&apos;re online.
        </CardDescription>
      </CardHeader>
      <CardContent>
        {offers.length === 0 ? (
          <p className="py-8 text-center text-sm text-muted-foreground">
            No offers yet. Go online and stay put — they&apos;ll show up here.
          </p>
        ) : (
          <ul className="divide-y">
            {offers.map((m) => {
              const pickup = coord(m, "pickupLat", "pickupLng");
              const dropoff = coord(m, "dropoffLat", "dropoffLng");
              const price = fare(m);
              return (
                <li key={m.id} className="flex items-start gap-3 py-3">
                  <Car className="mt-0.5 h-4 w-4 text-[#5b8a1e]" />
                  <div className="flex-1 space-y-1">
                    <div className="flex items-center justify-between gap-2">
                      <span className="text-sm font-medium">
                        {TYPE_LABEL[m.type] ?? m.type}
                      </span>
                      <span className="text-xs text-muted-foreground">
                        {new Date(m.createdAt).toLocaleTimeString()}
                      </span>
                    </div>
                    <div className="space-y-0.5 text-xs text-muted-foreground">
                      {pickup ? <p>Pickup: {pickup}</p> : null}
                      {dropoff ? <p>Dropoff: {dropoff}</p> : null}
                      {price ? (
                        <p className="font-medium text-foreground">Est. fare: {price}</p>
                      ) : null}
                    </div>
                  </div>
                </li>
              );
            })}
          </ul>
        )}
      </CardContent>
    </Card>
  );
}

function ConnectionDot({ status }: { status: string }) {
  if (status === "OPEN") {
    return <Wifi className="h-4 w-4 text-green-600 dark:text-green-400" />;
  }
  if (status === "CONNECTING" || status === "RECONNECTING") {
    return <Loader2 className="h-4 w-4 animate-spin text-amber-500" />;
  }
  return <WifiOff className="h-4 w-4 text-muted-foreground" />;
}
