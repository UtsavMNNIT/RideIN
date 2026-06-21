"use client";

import { Loader2, Wifi, WifiOff } from "lucide-react";

import { useWsOptional } from "@/lib/ws/WsProvider";
import type { WsStatus } from "@/lib/ws/WsClient";

/** Wifi / spinner / WifiOff dot for a WebSocket status. Shared chrome. */
export function ConnectionDot({ status }: { status: WsStatus | string }) {
  if (status === "OPEN") {
    return <Wifi className="h-4 w-4 text-green-600 dark:text-green-400" />;
  }
  if (status === "CONNECTING" || status === "RECONNECTING") {
    return <Loader2 className="h-4 w-4 animate-spin text-amber-500" />;
  }
  return <WifiOff className="h-4 w-4 text-muted-foreground" />;
}

/**
 * Header connection indicator. Renders the live WebSocket status dot when a
 * WsProvider is present (rider/driver shells); renders nothing in layouts
 * without one (the admin console), since {@link useWsOptional} returns null.
 */
export function ConnectionIndicator() {
  const ws = useWsOptional();
  if (!ws) return null;
  return (
    <span title={`Live connection: ${ws.status.toLowerCase()}`} aria-label="Connection status">
      <ConnectionDot status={ws.status} />
    </span>
  );
}
