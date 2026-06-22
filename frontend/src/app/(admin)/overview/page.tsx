"use client";

import { Activity, FlaskConical, Gauge, Timer, Users } from "lucide-react";
import type { LucideIcon } from "lucide-react";

import { useMetrics } from "@/application/admin/useMetrics";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/ui/components/ui/card";

function MetricCard({
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

export default function AdminOverviewPage() {
  const { data, isPlaceholder } = useMetrics();

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Operator overview</h1>
        <p className="text-sm text-muted-foreground">
          Real-time system health and dispatch metrics.
        </p>
      </div>

      {isPlaceholder ? (
        <div className="flex items-center gap-2 rounded-md border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-amber-800 dark:border-amber-900/50 dark:bg-amber-950/40 dark:text-amber-300">
          <FlaskConical className="h-4 w-4 shrink-0" />
          Preview — sample data. Live metrics arrive once the backend exposes an
          operator metrics endpoint.
        </div>
      ) : null}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
        <MetricCard title="Active rides" value={String(data.activeRides)} Icon={Gauge} />
        <MetricCard title="Drivers online" value={String(data.driversOnline)} Icon={Users} />
        <MetricCard title="Total riders" value={data.totalRiders.toLocaleString()} Icon={Users} />
        <MetricCard
          title="Completion rate"
          value={`${Math.round(data.completionRate * 100)}%`}
          Icon={Activity}
        />
        <MetricCard
          title="Avg dispatch"
          value={`${data.avgDispatchSecs}s`}
          Icon={Timer}
        />
      </div>
    </div>
  );
}
