"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import type { ReactNode } from "react";

import { env } from "@/config/env";
import { getSession } from "@/lib/auth/session";
import { WsClient, type WsMessage, type WsStatus } from "@/lib/ws/WsClient";

type WsContextValue = {
  status:   WsStatus;
  messages: WsMessage[];
  clear:    () => void;
};

const WsContext = createContext<WsContextValue | null>(null);

const MAX_BUFFERED_MESSAGES = 50;

/**
 * Mounts a single {@link WsClient} per logged-in user, exposes the message
 * stream through React context. Re-mounts (and reconnects) when the session
 * userId changes.
 */
export function WsProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<WsStatus>("IDLE");
  const [messages, setMessages] = useState<WsMessage[]>([]);
  const clientRef = useRef<WsClient | null>(null);

  useEffect(() => {
    const session = getSession();
    if (!session) {
      setStatus("CLOSED");
      return;
    }

    const url =
      `${env.NEXT_PUBLIC_WS_BASE_URL}/ws/notifications` +
      `?userId=${encodeURIComponent(session.userId)}` +
      `&role=${encodeURIComponent(session.role)}`;

    const client = new WsClient(url, {
      onStatus: setStatus,
      onMessage: (msg) => {
        setMessages((prev) => {
          // Dedupe by id and cap buffer — protects against server redeliveries
          // and prevents an unbounded list during a long session.
          if (prev.some((m) => m.id === msg.id)) return prev;
          const next = [msg, ...prev];
          return next.length > MAX_BUFFERED_MESSAGES
            ? next.slice(0, MAX_BUFFERED_MESSAGES)
            : next;
        });
      },
    });

    clientRef.current = client;
    client.connect();

    return () => {
      client.close();
      clientRef.current = null;
    };
    // Intentionally empty deps: session is read at mount; if it changes the
    // parent re-mounts this provider by changing its key (see RiderShell).
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const clear = useCallback(() => setMessages([]), []);

  const value = useMemo<WsContextValue>(
    () => ({ status, messages, clear }),
    [status, messages, clear],
  );

  return <WsContext.Provider value={value}>{children}</WsContext.Provider>;
}

export function useWs(): WsContextValue {
  const ctx = useContext(WsContext);
  if (!ctx) throw new Error("useWs must be used inside <WsProvider>");
  return ctx;
}

/**
 * Like {@link useWs} but returns null instead of throwing when there's no
 * provider — for chrome (e.g. the header connection dot) that may render in
 * layouts without a WebSocket (the admin console is read-only, no WS).
 */
export function useWsOptional(): WsContextValue | null {
  return useContext(WsContext);
}
