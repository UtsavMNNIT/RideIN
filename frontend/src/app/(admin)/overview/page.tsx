import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/ui/components/ui/card";

export const metadata = { title: "Overview" };

export default function AdminOverviewPage() {
  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Operator overview</h1>
        <p className="text-sm text-muted-foreground">Real-time system health and dispatch metrics.</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Overview (placeholder)</CardTitle>
          <CardDescription>Later phase — ride volume, dispatch latency, surge heatmap.</CardDescription>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          Admin shell is live; charts plug in once the metrics endpoints
          are exposed by the backend.
        </CardContent>
      </Card>
    </div>
  );
}
