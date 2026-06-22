"use client";

import { Info } from "lucide-react";

import { useRateCards } from "@/application/pricing/useRateCards";
import { Card, CardContent } from "@/ui/components/ui/card";
import { Skeleton } from "@/ui/components/ui/skeleton";
import { formatMoney } from "@/ui/components/ride/FareQuoteCard";

export default function AdminTariffsPage() {
  const { data: cards, isPending, isError } = useRateCards();

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Tariffs</h1>
        <p className="text-sm text-muted-foreground">
          Per-vehicle rate cards from pricing-service.
        </p>
      </div>

      <div className="flex items-center gap-2 rounded-md border bg-muted/40 px-3 py-2 text-xs text-muted-foreground">
        <Info className="h-4 w-4 shrink-0" />
        Rate cards are read-only here — edits are an ops-controlled migration
        (no write API yet).
      </div>

      {isPending ? (
        <Skeleton className="h-64 w-full rounded-lg" />
      ) : isError ? (
        <Card>
          <CardContent className="py-12 text-center text-sm text-muted-foreground">
            Couldn&apos;t load rate cards. Is pricing-service running?
          </CardContent>
        </Card>
      ) : !cards || cards.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center text-sm text-muted-foreground">
            No rate cards configured.
          </CardContent>
        </Card>
      ) : (
        <div className="overflow-x-auto rounded-lg border">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b bg-muted/50 text-left">
                <th className="p-3 font-medium">Vehicle</th>
                <th className="p-3 text-right font-medium">Base fare</th>
                <th className="p-3 text-right font-medium">Per km</th>
                <th className="p-3 text-right font-medium">Per min</th>
                <th className="p-3 text-right font-medium">Minimum</th>
                <th className="p-3 text-right font-medium">Booking fee</th>
              </tr>
            </thead>
            <tbody>
              {cards.map((c) => (
                <tr key={c.vehicleType} className="border-b last:border-0 hover:bg-muted/30">
                  <td className="p-3 font-medium">{c.vehicleType}</td>
                  <td className="p-3 text-right">{formatMoney(c.currency, c.baseFare)}</td>
                  <td className="p-3 text-right">{formatMoney(c.currency, c.perKm)}</td>
                  <td className="p-3 text-right">{formatMoney(c.currency, c.perMinute)}</td>
                  <td className="p-3 text-right">{formatMoney(c.currency, c.minimumFare)}</td>
                  <td className="p-3 text-right">{formatMoney(c.currency, c.bookingFee)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
