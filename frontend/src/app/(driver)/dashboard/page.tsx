import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/ui/components/ui/card";

export const metadata = { title: "Driver" };

export default function DriverDashboardPage() {
  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Driver dashboard</h1>
        <p className="text-sm text-muted-foreground">Go online when you&apos;re ready to drive.</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Dashboard (placeholder)</CardTitle>
          <CardDescription>Phase F-3 — availability toggle, heartbeat map, earnings card.</CardDescription>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          The shell + nav are live; driver-specific widgets arrive next phase.
        </CardContent>
      </Card>
    </div>
  );
}
