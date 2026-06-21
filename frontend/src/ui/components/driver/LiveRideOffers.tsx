"use client";

import { Check, Loader2, MapPin, Navigation, Wifi, WifiOff, X } from "lucide-react";

import type { DriverTripContext } from "@/application/driver/useDriverTrip";
import { useWs } from "@/lib/ws/WsProvider";
import type { WsMessage } from "@/lib/ws/WsClient";
import { Button } from "@/ui/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/ui/components/ui/card";

type Props = {
  /** rideIds already accepted/declined (or the active trip) — filtered out. */
  handled: Set<string>;
  onAccept: (ctx: DriverTripContext) => void;
  onDecline: (rideId: string) => void;
  /** rideId currently being accepted / declined, for per-row spinners. */
  acceptingId?: string;
  decliningId?: string;
  /** Any action in flight — disables the other buttons. */
  busy?: boolean;
};

function num(payload: Record<string, unknown>, key: string): number | null {
  const v = payload[key];
  return typeof v === "number" ? v : null;
}

/** Build the trip context from a RIDE_MATCHED frame, or null if coords missing. */
function toContext(m: WsMessage): DriverTripContext | null {
  const rideId = m.rideId ?? (typeof m.payload.rideId === "string" ? m.payload.rideId : null);
  const pickupLat = num(m.payload, "pickupLat");
  const pickupLng = num(m.payload, "pickupLng");
  const dropoffLat = num(m.payload, "dropoffLat");
  const dropoffLng = num(m.payload, "dropoffLng");
  if (
    !rideId ||
    pickupLat === null ||
    pickupLng === null ||
    dropoffLat === null ||
    dropoffLng === null
  ) {
    return null;
  }
  return { rideId, pickupLat, pickupLng, dropoffLat, dropoffLng };
}

export function LiveRideOffers({
  handled,
  onAccept,
  onDecline,
  acceptingId,
  decliningId,
  busy,
}: Props) {
  const { status, messages } = useWs();

  // Newest first; one card per ride (a ride can be re-offered), un-handled only.
  const seen = new Set<string>();
  const offers: { m: WsMessage; ctx: DriverTripContext }[] = [];
  for (const m of messages) {
    if (m.type !== "RIDE_MATCHED") continue;
    const ctx = toContext(m);
    if (!ctx || handled.has(ctx.rideId) || seen.has(ctx.rideId)) continue;
    seen.add(ctx.rideId);
    offers.push({ m, ctx });
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <ConnectionDot status={status} />
          Ride offers
        </CardTitle>
        <CardDescription>Live offers arrive here while you&apos;re online.</CardDescription>
      </CardHeader>
      <CardContent>
        {offers.length === 0 ? (
          <p className="py-8 text-center text-sm text-muted-foreground">
            No offers yet. Go online and stay put — they&apos;ll show up here.
          </p>
        ) : (
          <ul className="space-y-3">
            {offers.map(({ m, ctx }) => {
              const accepting = acceptingId === ctx.rideId;
              const declining = decliningId === ctx.rideId;
              return (
                <li key={m.id} className="rounded-md border p-3">
                  <div className="flex items-center justify-between gap-2">
                    <span className="flex items-center gap-1.5 text-sm font-medium">
                      <Navigation className="h-4 w-4 text-[#5b8a1e]" />
                      New ride offer
                    </span>
                    <span className="text-xs text-muted-foreground">
                      {new Date(m.createdAt).toLocaleTimeString()}
                    </span>
                  </div>
                  <div className="mt-1 space-y-0.5 text-xs text-muted-foreground">
                    <p className="flex items-center gap-1">
                      <MapPin className="h-3 w-3 shrink-0" />
                      Pickup: {ctx.pickupLat.toFixed(4)}, {ctx.pickupLng.toFixed(4)}
                    </p>
                    <p className="flex items-center gap-1">
                      <MapPin className="h-3 w-3 shrink-0" />
                      Dropoff: {ctx.dropoffLat.toFixed(4)}, {ctx.dropoffLng.toFixed(4)}
                    </p>
                  </div>
                  <div className="mt-3 flex gap-2">
                    <Button
                      className="flex-1"
                      disabled={busy}
                      onClick={() => onAccept(ctx)}
                    >
                      {accepting ? <Loader2 className="animate-spin" /> : <Check />}
                      Accept
                    </Button>
                    <Button
                      variant="outline"
                      className="flex-1"
                      disabled={busy}
                      onClick={() => onDecline(ctx.rideId)}
                    >
                      {declining ? <Loader2 className="animate-spin" /> : <X />}
                      Decline
                    </Button>
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
