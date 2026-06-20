"use client";

import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import {
  Car,
  CheckCircle2,
  Loader2,
  Radar,
  SearchX,
  Star,
  XCircle,
} from "lucide-react";

import { tripCancelPath } from "@/application/ride/endpoints";
import type { RideResponse, TripResponse } from "@/domain/ride/types";
import { api } from "@/lib/api/client";
import { Button } from "@/ui/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/ui/components/ui/card";

import { formatMoney } from "./FareQuoteCard";

type Props = {
  ride: RideResponse;
  trip: TripResponse | null;
  onReset: () => void;
};

/** Short, human-friendly form of a UUID for display ("a1b2c3d4"). */
function shortId(id: string): string {
  return id.split("-")[0] ?? id;
}

function DriverCard({ trip }: { trip: TripResponse | null }) {
  if (!trip) {
    return (
      <div className="flex items-center gap-2 rounded-md border bg-muted/30 px-4 py-3 text-sm text-muted-foreground">
        <Loader2 className="h-4 w-4 animate-spin" />
        Loading driver details…
      </div>
    );
  }
  return (
    <div className="flex items-center gap-3 rounded-md border p-4">
      <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary/10">
        <Car className="h-5 w-5 text-primary" />
      </div>
      <div className="flex-1">
        <p className="text-sm font-medium">Driver {shortId(trip.driverId)}</p>
        <p className="text-xs text-muted-foreground">{trip.vehicleType}</p>
      </div>
      {typeof trip.matchScore === "number" ? (
        <div className="flex items-center gap-1 text-sm text-muted-foreground">
          <Star className="h-4 w-4 text-amber-500" />
          {trip.matchScore.toFixed(2)}
        </div>
      ) : null}
    </div>
  );
}

export function ActiveRidePanel({ ride, trip, onReset }: Props) {
  // trip-service owns cancellation, but a Trip aggregate exists only after
  // assignment. While still REQUESTED we can only abandon tracking locally.
  const cancel = useMutation({
    mutationFn: () => api.post(tripCancelPath(ride.id), { reason: "RIDER_CANCELLED" }),
    onSuccess: () => {
      toast.success("Ride cancelled.");
      onReset();
    },
    onError: () => toast.error("Couldn't cancel the ride. Please try again."),
  });

  const { status } = ride;

  if (status === "REQUESTED") {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Radar className="h-5 w-5 animate-pulse text-primary" />
            Finding your driver
          </CardTitle>
          <CardDescription>Matching you with a nearby {ride.vehicleType.toLowerCase()} driver…</CardDescription>
        </CardHeader>
        <CardContent>
          <Button variant="outline" className="w-full" onClick={onReset}>
            Cancel
          </Button>
        </CardContent>
      </Card>
    );
  }

  if (status === "ASSIGNED" || status === "STARTED") {
    const started = status === "STARTED";
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Car className="h-5 w-5 text-primary" />
            {started ? "On your trip" : "Driver on the way"}
          </CardTitle>
          <CardDescription>
            {started ? "Enjoy the ride — you're on your way." : "Your driver is heading to the pickup point."}
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          <DriverCard trip={trip} />
          <Button
            variant="destructive"
            className="w-full"
            disabled={cancel.isPending}
            onClick={() => cancel.mutate()}
          >
            {cancel.isPending ? <Loader2 className="animate-spin" /> : <XCircle />}
            Cancel ride
          </Button>
        </CardContent>
      </Card>
    );
  }

  if (status === "COMPLETED") {
    const fare =
      typeof ride.fareTotal === "number" && ride.currency
        ? formatMoney(ride.currency, ride.fareTotal)
        : null;
    const km =
      typeof ride.finalDistanceMeters === "number"
        ? (ride.finalDistanceMeters / 1000).toFixed(1)
        : null;
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <CheckCircle2 className="h-5 w-5 text-green-600 dark:text-green-400" />
            Trip completed
          </CardTitle>
          <CardDescription>Thanks for riding with us.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          {fare ? (
            <div className="flex items-baseline justify-between rounded-md border p-4">
              <span className="text-sm text-muted-foreground">Total fare</span>
              <span className="text-2xl font-semibold tracking-tight">{fare}</span>
            </div>
          ) : null}
          {km ? <p className="text-xs text-muted-foreground">{km} km travelled</p> : null}
          <Button className="w-full" onClick={onReset}>
            Book another ride
          </Button>
        </CardContent>
      </Card>
    );
  }

  // CANCELLED / NO_DRIVERS_FOUND
  const noDrivers = status === "NO_DRIVERS_FOUND";
  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          {noDrivers ? (
            <SearchX className="h-5 w-5 text-amber-600 dark:text-amber-400" />
          ) : (
            <XCircle className="h-5 w-5 text-muted-foreground" />
          )}
          {noDrivers ? "No drivers available" : "Ride cancelled"}
        </CardTitle>
        <CardDescription>
          {noDrivers
            ? "We couldn't find a driver nearby. Try again in a moment."
            : "This ride was cancelled."}
        </CardDescription>
      </CardHeader>
      <CardContent>
        <Button className="w-full" onClick={onReset}>
          {noDrivers ? "Try again" : "Book another ride"}
        </Button>
      </CardContent>
    </Card>
  );
}
