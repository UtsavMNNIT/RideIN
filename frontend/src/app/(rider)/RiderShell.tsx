"use client";

import type { ReactNode } from "react";

import { WsProvider } from "@/lib/ws/WsProvider";

/**
 * Thin client wrapper that mounts the WebSocket provider for every rider
 * route. The route group's layout stays a Server Component (zero JS for
 * the chrome) and delegates only the live-data subtree to this client.
 */
export function RiderShell({ children }: { children: ReactNode }) {
  return <WsProvider>{children}</WsProvider>;
}
