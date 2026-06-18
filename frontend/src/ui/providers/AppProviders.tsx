"use client";

import { Toaster } from "sonner";
import type { ReactNode } from "react";

import { QueryProvider } from "@/ui/providers/QueryProvider";
import { ThemeProvider } from "@/ui/providers/ThemeProvider";

/**
 * Single mount point for every cross-cutting provider. Order matters:
 *   - ThemeProvider must wrap anything that reads CSS vars (Toaster does).
 *   - QueryProvider must wrap anything that uses React Query hooks.
 *
 * Real-time WS provider is intentionally NOT here — it belongs inside each
 * authenticated route group (rider / driver), where the role + user id are
 * known, not at the unauthenticated root.
 */
export function AppProviders({ children }: { children: ReactNode }) {
  return (
    <ThemeProvider>
      <QueryProvider>
        {children}
        <Toaster richColors position="top-right" closeButton />
      </QueryProvider>
    </ThemeProvider>
  );
}
