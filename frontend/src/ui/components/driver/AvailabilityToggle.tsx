"use client";

import { Loader2, Power, PowerOff } from "lucide-react";

import { useAvailability } from "@/application/driver/useAvailability";
import type { DriverAvailability, DriverResponse } from "@/domain/driver/types";
import { Button } from "@/ui/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/ui/components/ui/card";
import { cn } from "@/lib/utils/cn";

const PILL: Record<DriverAvailability, string> = {
  ONLINE:  "bg-green-100 text-green-700 dark:bg-green-900/40 dark:text-green-300",
  OFFLINE: "bg-muted text-muted-foreground",
  ON_TRIP: "bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300",
};

const LABEL: Record<DriverAvailability, string> = {
  ONLINE:  "Online",
  OFFLINE: "Offline",
  ON_TRIP: "On a trip",
};

export function AvailabilityToggle({ driver }: { driver?: DriverResponse }) {
  const { goOnline, goOffline } = useAvailability(driver?.id);
  const availability: DriverAvailability = driver?.availability ?? "OFFLINE";
  const online = availability === "ONLINE";
  const onTrip = availability === "ON_TRIP";
  const pending = goOnline.isPending || goOffline.isPending;

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between gap-3">
          <div>
            <CardTitle>Availability</CardTitle>
            <CardDescription>Go online to start receiving ride offers.</CardDescription>
          </div>
          <span
            className={cn(
              "inline-flex shrink-0 rounded-full px-2.5 py-0.5 text-xs font-medium",
              PILL[availability],
            )}
          >
            {LABEL[availability]}
          </span>
        </div>
      </CardHeader>
      <CardContent className="space-y-2">
        <Button
          onClick={() => (online ? goOffline.mutate() : goOnline.mutate())}
          disabled={pending || onTrip || !driver}
          variant={online ? "outline" : "default"}
          className="w-full"
        >
          {pending ? (
            <Loader2 className="animate-spin" />
          ) : online ? (
            <PowerOff />
          ) : (
            <Power />
          )}
          {online ? "Go offline" : "Go online"}
        </Button>
        {onTrip ? (
          <p className="text-xs text-muted-foreground">
            Finish your current trip before changing availability.
          </p>
        ) : null}
      </CardContent>
    </Card>
  );
}
