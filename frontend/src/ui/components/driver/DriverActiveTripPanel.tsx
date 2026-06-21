"use client";

import type { UseMutationResult } from "@tanstack/react-query";
import {
  CheckCircle2,
  Flag,
  Loader2,
  MapPin,
  Navigation,
  PlayCircle,
  User,
  XCircle,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";

import type { DriverTripContext } from "@/application/driver/useDriverTrip";
import type { TripResponse } from "@/domain/ride/types";
import { Button } from "@/ui/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/ui/components/ui/card";

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type TripMutation = UseMutationResult<TripResponse, any, any>;

type Props = {
  ctx:      DriverTripContext;
  trip:     TripResponse | null;
  arrived:  TripMutation;
  start:    TripMutation;
  complete: TripMutation;
  cancel:   TripMutation;
  onDismiss: () => void;
};

/** Short, human-friendly form of a UUID for display ("a1b2c3d4"). */
function shortId(id: string): string {
  return id.split("-")[0] ?? id;
}

function Coord({ label, lat, lng }: { label: string; lat: number; lng: number }) {
  return (
    <p className="flex items-center gap-1.5 text-xs text-muted-foreground">
      <MapPin className="h-3.5 w-3.5 shrink-0" />
      <span className="font-medium text-foreground">{label}:</span>
      <span className="font-mono">
        {lat.toFixed(4)}, {lng.toFixed(4)}
      </span>
    </p>
  );
}

function ActionButton({
  mutation,
  Icon,
  label,
}: {
  mutation: TripMutation;
  Icon: LucideIcon;
  label: string;
}) {
  return (
    <Button
      className="w-full"
      disabled={mutation.isPending}
      onClick={() => mutation.mutate(undefined)}
    >
      {mutation.isPending ? <Loader2 className="animate-spin" /> : <Icon />}
      {label}
    </Button>
  );
}

export function DriverActiveTripPanel({
  ctx,
  trip,
  arrived,
  start,
  complete,
  cancel,
  onDismiss,
}: Props) {
  const status = trip?.status ?? "ACCEPTED";

  if (status === "COMPLETED") {
    const km =
      typeof trip?.finalDistanceMeters === "number"
        ? (trip.finalDistanceMeters / 1000).toFixed(1)
        : null;
    const min =
      typeof trip?.finalDurationSeconds === "number"
        ? Math.round(trip.finalDurationSeconds / 60)
        : null;
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <CheckCircle2 className="h-5 w-5 text-green-600 dark:text-green-400" />
            Trip completed
          </CardTitle>
          <CardDescription>Nice work — you&apos;re back online.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          {km || min ? (
            <div className="flex gap-4 rounded-md border p-4 text-sm">
              {km ? (
                <div>
                  <p className="text-muted-foreground">Distance</p>
                  <p className="text-lg font-semibold">{km} km</p>
                </div>
              ) : null}
              {min !== null ? (
                <div>
                  <p className="text-muted-foreground">Duration</p>
                  <p className="text-lg font-semibold">{min} min</p>
                </div>
              ) : null}
            </div>
          ) : null}
          <Button className="w-full" onClick={onDismiss}>
            Done
          </Button>
        </CardContent>
      </Card>
    );
  }

  if (status === "CANCELLED" || status === "REJECTED" || status === "EXPIRED") {
    const cancelledByRider = trip?.cancelledBy === "RIDER";
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <XCircle className="h-5 w-5 text-muted-foreground" />
            {status === "CANCELLED" ? "Ride cancelled" : "Offer closed"}
          </CardTitle>
          <CardDescription>
            {cancelledByRider
              ? "The rider cancelled this ride."
              : "This ride is no longer active."}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Button className="w-full" onClick={onDismiss}>
            Dismiss
          </Button>
        </CardContent>
      </Card>
    );
  }

  // Live states: ACCEPTED → ARRIVED → STARTED (OFFERED only briefly post-accept).
  const headline =
    status === "STARTED"
      ? "Trip in progress"
      : status === "ARRIVED"
        ? "Waiting for rider"
        : "Head to pickup";
  const sub =
    status === "STARTED"
      ? "Drive safely to the destination."
      : status === "ARRIVED"
        ? "You're at the pickup point."
        : "Your rider is expecting you.";

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Navigation className="h-5 w-5 text-primary" />
          {headline}
        </CardTitle>
        <CardDescription>{sub}</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex items-center gap-3 rounded-md border p-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-full bg-primary/10">
            <User className="h-4 w-4 text-primary" />
          </div>
          <div className="text-sm">
            <p className="font-medium">Rider {shortId(ctx.rideId)}</p>
            {trip?.vehicleType ? (
              <p className="text-xs text-muted-foreground">{trip.vehicleType}</p>
            ) : null}
          </div>
        </div>

        <div className="space-y-1">
          <Coord label="Pickup" lat={ctx.pickupLat} lng={ctx.pickupLng} />
          <Coord label="Dropoff" lat={ctx.dropoffLat} lng={ctx.dropoffLng} />
        </div>

        {status === "STARTED" ? (
          <ActionButton mutation={complete} Icon={Flag} label="Complete trip" />
        ) : status === "ARRIVED" ? (
          <ActionButton mutation={start} Icon={PlayCircle} label="Start trip" />
        ) : (
          <ActionButton mutation={arrived} Icon={MapPin} label="I've arrived" />
        )}

        <Button
          variant="ghost"
          className="w-full text-muted-foreground"
          disabled={cancel.isPending}
          onClick={() => cancel.mutate(undefined)}
        >
          {cancel.isPending ? <Loader2 className="animate-spin" /> : <XCircle />}
          Cancel trip
        </Button>
      </CardContent>
    </Card>
  );
}
