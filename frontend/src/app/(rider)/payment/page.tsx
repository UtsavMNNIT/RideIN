"use client";

import { useEffect, useState } from "react";
import { CreditCard, Loader2, Plus, Star } from "lucide-react";
import { toast } from "sonner";

import {
  useAddPaymentMethod,
  usePaymentMethods,
} from "@/application/payment/usePaymentMethods";
import { getSession } from "@/lib/auth/session";
import { Button } from "@/ui/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/ui/components/ui/card";
import { Input } from "@/ui/components/ui/input";
import { Skeleton } from "@/ui/components/ui/skeleton";

const BRANDS = ["Visa", "Mastercard", "Amex", "RuPay"] as const;

export default function PaymentMethodsPage() {
  const [userId, setUserId] = useState<string | undefined>(undefined);
  const [brand, setBrand] = useState<string>(BRANDS[0]);
  const [last4, setLast4] = useState("");

  // Session is client-only (cookie + sessionStorage); read after hydration.
  useEffect(() => {
    setUserId(getSession()?.userId);
  }, []);

  const { data: methods, isLoading } = usePaymentMethods(userId);
  const add = useAddPaymentMethod(userId);

  const last4Valid = /^[0-9]{4}$/.test(last4);

  function onAdd() {
    if (!userId || !last4Valid) return;
    const isDefault = !methods || methods.length === 0; // first card becomes default
    add.mutate(
      { userId, brand, last4, isDefault },
      {
        onSuccess: () => {
          toast.success(`${brand} •••• ${last4} added.`);
          setLast4("");
        },
        onError: () => toast.error("Couldn't add the card. Please try again."),
      },
    );
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Payment methods</h1>
        <p className="text-sm text-muted-foreground">
          Mock cards for the demo — no real card data is stored or charged. Your fare
          is settled to the default card when a trip completes.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <Plus className="h-4 w-4" /> Add a card
          </CardTitle>
          <CardDescription>Pick a brand and enter any 4 digits.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex flex-wrap gap-2">
            {BRANDS.map((b) => (
              <Button
                key={b}
                type="button"
                variant={b === brand ? "default" : "outline"}
                size="sm"
                onClick={() => setBrand(b)}
              >
                {b}
              </Button>
            ))}
          </div>
          <div className="flex items-end gap-3">
            <div className="flex-1">
              <label className="mb-1 block text-xs text-muted-foreground">Last 4 digits</label>
              <Input
                inputMode="numeric"
                maxLength={4}
                placeholder="4242"
                value={last4}
                onChange={(e) => setLast4(e.target.value.replace(/\D/g, "").slice(0, 4))}
              />
            </div>
            <Button onClick={onAdd} disabled={!userId || !last4Valid || add.isPending}>
              {add.isPending ? <Loader2 className="animate-spin" /> : <Plus />}
              Add card
            </Button>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Saved cards</CardTitle>
        </CardHeader>
        <CardContent className="space-y-2">
          {isLoading ? (
            <>
              <Skeleton className="h-12 w-full" />
              <Skeleton className="h-12 w-full" />
            </>
          ) : methods && methods.length > 0 ? (
            methods.map((m) => (
              <div key={m.id} className="flex items-center gap-3 rounded-md border p-3">
                <div className="flex h-9 w-9 items-center justify-center rounded-full bg-primary/10">
                  <CreditCard className="h-4 w-4 text-primary" />
                </div>
                <div className="flex-1">
                  <p className="text-sm font-medium">
                    {m.brand} •••• {m.last4}
                  </p>
                </div>
                {m.isDefault ? (
                  <span className="flex items-center gap-1 rounded-full bg-amber-100 px-2 py-0.5 text-xs text-amber-700 dark:bg-amber-900/40 dark:text-amber-300">
                    <Star className="h-3 w-3" /> Default
                  </span>
                ) : null}
              </div>
            ))
          ) : (
            <p className="py-4 text-center text-sm text-muted-foreground">
              No cards yet — add one above.
            </p>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
