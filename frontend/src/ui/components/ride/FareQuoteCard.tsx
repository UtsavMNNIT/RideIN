"use client";

import { Loader2 } from "lucide-react";

import type { QuoteResponse } from "@/domain/ride/types";

/** Best-effort currency formatting; falls back to "USD 12.34" on bad codes. */
export function formatMoney(currency: string, amount: number): string {
  try {
    return new Intl.NumberFormat(undefined, { style: "currency", currency }).format(amount);
  } catch {
    return `${currency} ${amount.toFixed(2)}`;
  }
}

export function FareQuoteCard({
  quote,
  loading,
}: {
  quote: QuoteResponse | null;
  loading?: boolean;
}) {
  if (loading && !quote) {
    return (
      <div className="flex items-center gap-2 rounded-md border bg-muted/30 px-4 py-3 text-sm text-muted-foreground">
        <Loader2 className="h-4 w-4 animate-spin" />
        Estimating fare…
      </div>
    );
  }
  if (!quote) return null;

  const { fare } = quote;
  const surge = fare.surgeMultiplier > 1;

  return (
    <div className="space-y-2 rounded-md border p-4">
      <div className="flex items-baseline justify-between">
        <span className="text-sm text-muted-foreground">
          {loading ? "Updating…" : "Estimated fare"}
        </span>
        <span className="text-2xl font-semibold tracking-tight">
          {formatMoney(fare.currency, fare.total)}
        </span>
      </div>
      <div className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-muted-foreground">
        <span>{quote.estDistanceKm.toFixed(1)} km</span>
        <span>~{Math.round(quote.estDurationMin)} min</span>
        {surge ? (
          <span className="font-medium text-amber-600 dark:text-amber-400">
            {fare.surgeMultiplier.toFixed(1)}× surge
          </span>
        ) : null}
      </div>
    </div>
  );
}
