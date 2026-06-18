"use client";

import { useEffect, useState } from "react";
import { Loader2, Send, Wifi, WifiOff, Zap, AlertCircle, CheckCircle2 } from "lucide-react";

import { api, ApiError } from "@/lib/api/client";
import { getSession, type Session } from "@/lib/auth/session";
import { useWs } from "@/lib/ws/WsProvider";
import { Button } from "@/ui/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/ui/components/ui/card";

/**
 * End-to-end demo page. Two trigger paths:
 *   1. DIRECT — POST → use case → Redis pub/sub → WebSocket
 *   2. KAFKA  — POST → Kafka → consumer → use case → Redis pub/sub → WebSocket
 *
 * The notification list below shows live frames as they arrive.
 */
export default function RiderHomePage() {
  const { status, messages, clear } = useWs();
  const [session, setSession] = useState<Session | null>(null);
  const [busy, setBusy] = useState<"direct" | "kafka" | null>(null);
  const [error, setError] = useState<string | null>(null);

  // getSession reads cookies → must run on the client only.
  useEffect(() => setSession(getSession()), []);

  const trigger = async (path: "DIRECT" | "KAFKA") => {
    if (!session) return;
    setError(null);
    setBusy(path.toLowerCase() as "direct" | "kafka");
    try {
      await api.post("/v1/demo/notify", {
        userId:  session.userId,
        role:    session.role,
        type:    "RIDE_MATCHED",
        trigger: path,
      });
    } catch (e) {
      const msg = e instanceof ApiError ? `HTTP ${e.status}` : String(e);
      setError(`Trigger failed: ${msg}`);
    } finally {
      setBusy(null);
    }
  };

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Live notifications</h1>
        <p className="text-sm text-muted-foreground">
          Trigger a ride-matched event and watch it arrive over WebSocket.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <ConnectionDot status={status} />
            Connection: <span className="font-mono text-sm">{status}</span>
          </CardTitle>
          <CardDescription>
            User id:{" "}
            <span className="font-mono text-xs">{session?.userId ?? "—"}</span>
          </CardDescription>
        </CardHeader>
        <CardContent className="flex flex-wrap gap-3">
          <Button onClick={() => trigger("DIRECT")} disabled={!session || status !== "OPEN" || busy !== null}>
            {busy === "direct" ? <Loader2 className="animate-spin" /> : <Zap />}
            Trigger via direct path
          </Button>
          <Button
            onClick={() => trigger("KAFKA")}
            variant="outline"
            disabled={!session || status !== "OPEN" || busy !== null}
          >
            {busy === "kafka" ? <Loader2 className="animate-spin" /> : <Send />}
            Trigger via Kafka
          </Button>
          {messages.length > 0 ? (
            <Button onClick={clear} variant="ghost">Clear list</Button>
          ) : null}
        </CardContent>
        {error ? (
          <CardContent className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="h-4 w-4" /> {error}
          </CardContent>
        ) : null}
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Inbound stream</CardTitle>
          <CardDescription>
            Newest first. Frames arrive via {`{`}WebSocket → Redis pub/sub → consumer/use-case{`}`} —
            the exact production path.
          </CardDescription>
        </CardHeader>
        <CardContent>
          {messages.length === 0 ? (
            <p className="py-8 text-center text-sm text-muted-foreground">
              No messages yet. Trigger one above.
            </p>
          ) : (
            <ul className="divide-y">
              {messages.map((m) => (
                <li key={m.id} className="flex items-start gap-3 py-3">
                  <CheckCircle2 className="mt-0.5 h-4 w-4 text-green-600 dark:text-green-400" />
                  <div className="flex-1 space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="font-mono text-xs text-muted-foreground">{m.type}</span>
                      <span className="text-xs text-muted-foreground">
                        · {new Date(m.createdAt).toLocaleTimeString()}
                      </span>
                    </div>
                    <pre className="overflow-x-auto rounded-md bg-muted px-3 py-2 text-xs">
                      {JSON.stringify(m.payload, null, 2)}
                    </pre>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

function ConnectionDot({ status }: { status: string }) {
  if (status === "OPEN") {
    return <Wifi className="h-4 w-4 text-green-600 dark:text-green-400" />;
  }
  if (status === "CONNECTING" || status === "RECONNECTING") {
    return <Loader2 className="h-4 w-4 animate-spin text-amber-500" />;
  }
  return <WifiOff className="h-4 w-4 text-muted-foreground" />;
}
