"use client";

import dynamic from "next/dynamic";
import { AlertCircle } from "lucide-react";

import { useDriver } from "@/application/driver/useDriver";
import { useDriverTrip } from "@/application/driver/useDriverTrip";
import { useLocationHeartbeat } from "@/application/driver/useLocationHeartbeat";
import { AvailabilityToggle } from "@/ui/components/driver/AvailabilityToggle";
import { DriverActiveTripPanel } from "@/ui/components/driver/DriverActiveTripPanel";
import { LiveRideOffers } from "@/ui/components/driver/LiveRideOffers";
import { Skeleton } from "@/ui/components/ui/skeleton";

// Leaflet touches `window` at import time — load the map only on the client.
const DriverMap = dynamic(() => import("@/ui/components/map/DriverMap"), {
  ssr: false,
  loading: () => <Skeleton className="h-[420px] w-full rounded-md" />,
});

export default function DriverDashboardPage() {
  const { data: driver } = useDriver();
  const { coords, error } = useLocationHeartbeat(driver?.id, driver?.availability);
  const {
    activeTrip,
    trip,
    handled,
    accept,
    reject,
    arrived,
    start,
    complete,
    cancel,
    dismiss,
  } = useDriverTrip();

  // Offers for rides we've already acted on (or the in-flight trip) are hidden.
  const handledOffers = new Set(handled);
  if (activeTrip) handledOffers.add(activeTrip.rideId);

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">
          {driver ? `Welcome, ${driver.fullName.split(" ")[0]}` : "Driver dashboard"}
        </h1>
        <p className="text-sm text-muted-foreground">
          Go online when you&apos;re ready to drive.
        </p>
      </div>

      {error ? (
        <div className="flex items-center gap-2 rounded-md border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-amber-800 dark:border-amber-900/50 dark:bg-amber-950/40 dark:text-amber-300">
          <AlertCircle className="h-4 w-4 shrink-0" />
          {error}
        </div>
      ) : null}

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="space-y-6 lg:col-span-2">
          {driver ? (
            <AvailabilityToggle driver={driver} />
          ) : (
            <Skeleton className="h-40 w-full rounded-lg" />
          )}
          <DriverMap coords={coords} />
        </div>
        <div>
          {activeTrip ? (
            <DriverActiveTripPanel
              ctx={activeTrip}
              trip={trip}
              arrived={arrived}
              start={start}
              complete={complete}
              cancel={cancel}
              onDismiss={dismiss}
            />
          ) : (
            <LiveRideOffers
              handled={handledOffers}
              onAccept={(ctx) => accept.mutate(ctx)}
              onDecline={(rideId) => reject.mutate(rideId)}
              acceptingId={accept.isPending ? accept.variables?.rideId : undefined}
              decliningId={reject.isPending ? reject.variables : undefined}
              busy={accept.isPending || reject.isPending}
            />
          )}
        </div>
      </div>
    </div>
  );
}
