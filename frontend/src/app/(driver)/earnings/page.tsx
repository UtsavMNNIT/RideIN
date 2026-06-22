"use client";

import { useEffect, useState } from "react";
import { Car, Clock, TrendingUp, Wallet } from "lucide-react";
import type { LucideIcon } from "lucide-react";

import { useEarnings } from "@/application/driver/useEarnings";
import { getSession } from "@/lib/auth/session";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/ui/components/ui/card";
import { formatMoney } from "@/ui/components/ride/FareQuoteCard";

function StatCard({
  title,
  value,
  Icon,
}: {
  title: string;
  value: string;
  Icon: LucideIcon;
}) {
  return (
    <Card>
      <CardHeader className="pb-2">
        <div className="flex items-center justify-between">
          <CardTitle className="text-sm font-medium text-muted-foreground">{title}</CardTitle>
          <Icon className="h-4 w-4 text-muted-foreground" />
        </div>
      </CardHeader>
      <CardContent>
        <p className="text-2xl font-semibold tracking-tight">{value}</p>
      </CardContent>
    </Card>
  );
}

export default function DriverEarningsPage() {
  const [driverId, setDriverId] = useState<string | undefined>(undefined);
  // Session is client-only (cookie + sessionStorage); read after hydration.
  useEffect(() => {
    setDriverId(getSession()?.userId);
  }, []);

  const { data } = useEarnings(driverId);

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Earnings</h1>
        <p className="text-sm text-muted-foreground">Your trips and payouts.</p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          title="Total earnings"
          value={formatMoney(data.currency, data.totalEarnings)}
          Icon={Wallet}
        />
        <StatCard title="Rides completed" value={String(data.ridesCompleted)} Icon={Car} />
        <StatCard title="Hours driven" value={`${data.onlineHours.toFixed(1)} h`} Icon={Clock} />
        <StatCard
          title="Avg fare"
          value={formatMoney(data.currency, data.avgFare)}
          Icon={TrendingUp}
        />
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Recent trips</CardTitle>
          <CardDescription>Your latest completed rides.</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b text-left text-muted-foreground">
                  <th className="py-2 pr-3 font-medium">When</th>
                  <th className="py-2 pr-3 font-medium">Vehicle</th>
                  <th className="py-2 pr-3 text-right font-medium">Distance</th>
                  <th className="py-2 text-right font-medium">Fare</th>
                </tr>
              </thead>
              <tbody>
                {data.trips.map((t) => (
                  <tr key={t.id} className="border-b last:border-0">
                    <td className="py-2 pr-3">
                      {new Date(t.completedAt).toLocaleString(undefined, {
                        month: "short",
                        day: "numeric",
                        hour: "2-digit",
                        minute: "2-digit",
                      })}
                    </td>
                    <td className="py-2 pr-3">{t.vehicleType}</td>
                    <td className="py-2 pr-3 text-right">
                      {(t.distanceMeters / 1000).toFixed(1)} km
                    </td>
                    <td className="py-2 text-right font-medium">
                      {formatMoney(t.currency, t.fareTotal)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
