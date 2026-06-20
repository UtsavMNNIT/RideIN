"use client";

import { useEffect } from "react";
import { Loader2, MapPin, Navigation } from "lucide-react";

import { useQuote } from "@/application/ride/useQuote";
import { useRequestRide } from "@/application/ride/useRequestRide";
import type { LatLng } from "@/domain/geo/types";
import type { RideResponse, VehicleType } from "@/domain/ride/types";
import { Button } from "@/ui/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/ui/components/ui/card";

import { FareQuoteCard } from "./FareQuoteCard";
import { VehiclePicker } from "./VehiclePicker";

type Props = {
  riderId: string | undefined;
  pickup: LatLng | null;
  dropoff: LatLng | null;
  vehicleType: VehicleType;
  onVehicleChange: (v: VehicleType) => void;
  onRequested: (ride: RideResponse) => void;
};

export function RideRequestPanel({
  riderId,
  pickup,
  dropoff,
  vehicleType,
  onVehicleChange,
  onRequested,
}: Props) {
  const quote = useQuote();
  const request = useRequestRide(riderId);
  const ready = Boolean(pickup && dropoff);

  // Re-quote whenever the route or vehicle changes.
  const quoteMutate = quote.mutate;
  useEffect(() => {
    if (!pickup || !dropoff) return;
    quoteMutate({
      riderId,
      pickupLat: pickup.lat,
      pickupLng: pickup.lng,
      dropoffLat: dropoff.lat,
      dropoffLng: dropoff.lng,
      vehicleType,
    });
  }, [pickup, dropoff, vehicleType, riderId, quoteMutate]);

  const onSubmit = () => {
    if (!pickup || !dropoff) return;
    request.mutate(
      {
        pickupLat: pickup.lat,
        pickupLng: pickup.lng,
        dropoffLat: dropoff.lat,
        dropoffLng: dropoff.lng,
        vehicleType,
      },
      { onSuccess: onRequested },
    );
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Where to?</CardTitle>
        <CardDescription>
          {ready
            ? "Choose a ride and confirm."
            : "Tap the map to drop a destination pin."}
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex items-center gap-2 text-sm">
          <MapPin className="h-4 w-4 shrink-0 text-muted-foreground" />
          {dropoff ? (
            <span className="font-mono text-xs">
              {dropoff.lat.toFixed(5)}, {dropoff.lng.toFixed(5)}
            </span>
          ) : (
            <span className="text-muted-foreground">No destination set</span>
          )}
        </div>

        <VehiclePicker
          value={vehicleType}
          onChange={onVehicleChange}
          disabled={request.isPending}
        />

        {ready ? (
          <FareQuoteCard quote={quote.data ?? null} loading={quote.isPending} />
        ) : null}

        <Button
          className="w-full"
          disabled={!ready || !riderId || request.isPending}
          onClick={onSubmit}
        >
          {request.isPending ? <Loader2 className="animate-spin" /> : <Navigation />}
          Request ride
        </Button>
      </CardContent>
    </Card>
  );
}
