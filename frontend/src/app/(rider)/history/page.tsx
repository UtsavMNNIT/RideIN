"use client";

import { useEffect, useMemo, useState } from "react";
import { Car, Clock, MapPin } from "lucide-react";

import { PAGE_SIZE, useRideHistory } from "@/application/rider/useRideHistory";
import type { RideResponse, RideStatus } from "@/domain/ride/types";
import { getSession } from "@/lib/auth/session";
import { cn } from "@/lib/utils/cn";
import { Button } from "@/ui/components/ui/button";
import { Card, CardContent } from "@/ui/components/ui/card";
import { Skeleton } from "@/ui/components/ui/skeleton";
import { formatMoney } from "@/ui/components/ride/FareQuoteCard";

const STATUS_STYLE: Record<RideStatus, string> = {
  REQUESTED:        "bg-blue-100 text-blue-700 dark:bg-blue-950 dark:text-blue-300",
  ASSIGNED:         "bg-blue-100 text-blue-700 dark:bg-blue-950 dark:text-blue-300",
  STARTED:          "bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300",
  COMPLETED:        "bg-green-100 text-green-700 dark:bg-green-900/40 dark:text-green-300",
  CANCELLED:        "bg-muted text-muted-foreground",
  NO_DRIVERS_FOUND: "bg-muted text-muted-foreground",
};

const STATUS_LABEL: Record<RideStatus, string> = {
  REQUESTED:        "Requested",
  ASSIGNED:         "Assigned",
  STARTED:          "In progress",
  COMPLETED:        "Completed",
  CANCELLED:        "Cancelled",
  NO_DRIVERS_FOUND: "No drivers",
};

function RideCard({ ride }: { ride: RideResponse }) {
  const fare =
    typeof ride.fareTotal === "number" && ride.currency
      ? formatMoney(ride.currency, ride.fareTotal)
      : null;
  const km =
    typeof ride.finalDistanceMeters === "number"
      ? `${(ride.finalDistanceMeters / 1000).toFixed(1)} km`
      : null;
  const date = new Date(ride.requestedAt).toLocaleString(undefined, {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });

  return (
    <Card>
      <CardContent className="flex items-start justify-between gap-3 p-4">
        <div className="flex items-start gap-3">
          <div className="mt-0.5 flex h-9 w-9 items-center justify-center rounded-full bg-primary/10">
            <Car className="h-4 w-4 text-primary" />
          </div>
          <div className="space-y-1">
            <p className="text-sm font-medium">{ride.vehicleType}</p>
            <p className="flex items-center gap-1 text-xs text-muted-foreground">
              <Clock className="h-3 w-3" />
              {date}
            </p>
            <p className="flex items-center gap-1 font-mono text-[11px] text-muted-foreground">
              <MapPin className="h-3 w-3 shrink-0" />
              {ride.dropoffLat.toFixed(4)}, {ride.dropoffLng.toFixed(4)}
            </p>
          </div>
        </div>
        <div className="flex flex-col items-end gap-1">
          <span
            className={cn(
              "inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium",
              STATUS_STYLE[ride.status],
            )}
          >
            {STATUS_LABEL[ride.status]}
          </span>
          {fare ? <span className="text-sm font-semibold">{fare}</span> : null}
          {km ? <span className="text-xs text-muted-foreground">{km}</span> : null}
        </div>
      </CardContent>
    </Card>
  );
}

export default function RiderHistoryPage() {
  const [riderId, setRiderId] = useState<string | undefined>(undefined);
  const [page, setPage] = useState(0);
  // Accumulate pages keyed by index so refetches replace (not duplicate) a page.
  const [pages, setPages] = useState<Record<number, RideResponse[]>>({});

  useEffect(() => {
    const session = getSession();
    if (session?.role === "RIDER") setRiderId(session.userId);
  }, []);

  const { data, isPending, isFetching, isError } = useRideHistory(riderId, page);

  useEffect(() => {
    if (data) setPages((prev) => ({ ...prev, [page]: data }));
  }, [data, page]);

  const rides = useMemo(
    () =>
      Object.keys(pages)
        .map(Number)
        .sort((a, b) => a - b)
        .flatMap((p) => pages[p]!),
    [pages],
  );

  const hasMore = (pages[page]?.length ?? 0) === PAGE_SIZE;
  const firstLoad = isPending && rides.length === 0;

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Ride history</h1>
        <p className="text-sm text-muted-foreground">Your past rides, newest first.</p>
      </div>

      {firstLoad ? (
        <div className="space-y-3">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-[88px] w-full rounded-lg" />
          ))}
        </div>
      ) : isError && rides.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center text-sm text-muted-foreground">
            Couldn&apos;t load your ride history. Please try again later.
          </CardContent>
        </Card>
      ) : rides.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center text-sm text-muted-foreground">
            No rides yet. Book your first ride to see it here.
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-3">
          {rides.map((ride) => (
            <RideCard key={ride.id} ride={ride} />
          ))}
          {hasMore ? (
            <Button
              variant="outline"
              className="w-full"
              disabled={isFetching}
              onClick={() => setPage((p) => p + 1)}
            >
              {isFetching ? "Loading…" : "Load more"}
            </Button>
          ) : null}
        </div>
      )}
    </div>
  );
}
