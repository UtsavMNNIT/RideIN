"use client";

import type { ReactNode } from "react";

import { WsProvider } from "@/lib/ws/WsProvider";

/**
 * Thin client wrapper that mounts the WebSocket provider for every driver
 * route, so the dashboard can surface live ride offers. Mirrors RiderShell:
 * the route group's layout stays a Server Component and delegates only the
 * live-data subtree to this client.
 */
export function DriverShell({ children }: { children: ReactNode }) {
  return <WsProvider>{children}</WsProvider>;
}
