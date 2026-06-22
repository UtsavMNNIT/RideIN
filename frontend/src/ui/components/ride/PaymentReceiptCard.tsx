"use client";

import { CheckCircle2, CreditCard, Loader2, XCircle } from "lucide-react";

import { useRidePayment } from "@/application/payment/useRidePayment";

import { formatMoney } from "./FareQuoteCard";

/**
 * Live payment receipt for a completed ride. Renders inside the "Trip
 * completed" panel: shows "Processing…" while settlement is in flight (the
 * payment-service settles off the `ride.completed` event), then the paid
 * amount once SETTLED.
 */
export function PaymentReceiptCard({ rideId, enabled = true }: { rideId: string; enabled?: boolean }) {
  const { data: payment } = useRidePayment(rideId, enabled);

  const settled = payment?.status === "SETTLED";
  const failed = payment?.status === "FAILED";

  return (
    <div className="rounded-md border p-4">
      <div className="flex items-center gap-2">
        {settled ? (
          <CheckCircle2 className="h-4 w-4 text-green-600 dark:text-green-400" />
        ) : failed ? (
          <XCircle className="h-4 w-4 text-destructive" />
        ) : (
          <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
        )}
        <span className="text-sm font-medium">
          {settled ? "Payment received" : failed ? "Payment failed" : "Processing payment…"}
        </span>
      </div>

      {settled && payment ? (
        <div className="mt-3 space-y-1.5">
          <div className="flex items-baseline justify-between">
            <span className="text-sm text-muted-foreground">Charged</span>
            <span className="text-lg font-semibold tracking-tight">
              {formatMoney(payment.currency, payment.amount)}
            </span>
          </div>
          <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <CreditCard className="h-3.5 w-3.5" />
            {payment.paymentMethodId ? "Saved card" : "Default payment method"}
            {payment.settledAt
              ? ` · ${new Date(payment.settledAt).toLocaleTimeString(undefined, {
                  hour: "2-digit",
                  minute: "2-digit",
                })}`
              : null}
          </div>
        </div>
      ) : failed ? (
        <p className="mt-2 text-xs text-muted-foreground">
          {payment?.failureReason ?? "We couldn't process this payment."}
        </p>
      ) : (
        <p className="mt-2 text-xs text-muted-foreground">Settling your fare…</p>
      )}
    </div>
  );
}
