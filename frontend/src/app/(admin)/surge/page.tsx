"use client";

import { Flame, Info } from "lucide-react";

import { useSurge } from "@/application/pricing/useSurge";
import { cn } from "@/lib/utils/cn";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/ui/components/ui/card";
import { Skeleton } from "@/ui/components/ui/skeleton";

export default function AdminSurgePage() {
  const { rows, isPending, isError } = useSurge();

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Surge pricing</h1>
        <p className="text-sm text-muted-foreground">
          Live multiplier per vehicle class. Refreshes every 15s.
        </p>
      </div>

      <div className="flex items-center gap-2 rounded-md border bg-muted/40 px-3 py-2 text-xs text-muted-foreground">
        <Info className="h-4 w-4 shrink-0" />
        Derived from live quotes — pricing-service computes surge per fare rather
        than exposing a config endpoint.
      </div>

      {isPending ? (
        <div className="grid gap-4 md:grid-cols-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-32 rounded-lg" />
          ))}
        </div>
      ) : isError && rows.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center text-sm text-muted-foreground">
            Couldn&apos;t compute surge. Is pricing-service running?
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-4 md:grid-cols-3">
          {rows.map((row) => {
            const surging = row.multiplier > 1;
            return (
              <Card
                key={row.vehicleType}
                className={cn(
                  surging &&
                    "border-amber-300 bg-amber-50 dark:border-amber-900/50 dark:bg-amber-950/20",
                )}
              >
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    {surging ? <Flame className="h-4 w-4 text-amber-600 dark:text-amber-400" /> : null}
                    {row.vehicleType}
                  </CardTitle>
                  <CardDescription>{surging ? "Surge active" : "No surge"}</CardDescription>
                </CardHeader>
                <CardContent>
                  <p className="text-3xl font-semibold tracking-tight">
                    {row.multiplier.toFixed(1)}×
                  </p>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}
