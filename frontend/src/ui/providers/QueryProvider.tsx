"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { useState, type ReactNode } from "react";

/**
 * React Query client + devtools (dev only).
 *
 * Defaults reflect the app's read/write profile:
 *   - `staleTime: 30s` so navigation doesn't trigger an immediate refetch.
 *   - `refetchOnWindowFocus: false` — refocus refetches are noisy in a real-time
 *     app where the WebSocket already keeps the cache fresh.
 *   - `retry`: only retry on network/5xx, never on 4xx (the request is bad).
 */
function makeClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        gcTime: 5 * 60_000,
        refetchOnWindowFocus: false,
        retry: (failureCount, error) => {
          const status = (error as { status?: number } | undefined)?.status;
          if (status && status >= 400 && status < 500) return false;
          return failureCount < 3;
        },
      },
      mutations: {
        retry: 0,
      },
    },
  });
}

export function QueryProvider({ children }: { children: ReactNode }) {
  // useState ensures the QueryClient is stable across renders without being
  // shared across server requests (which would leak data between users).
  const [client] = useState(makeClient);

  return (
    <QueryClientProvider client={client}>
      {children}
      {process.env.NODE_ENV !== "production" ? (
        <ReactQueryDevtools initialIsOpen={false} buttonPosition="bottom-left" />
      ) : null}
    </QueryClientProvider>
  );
}
