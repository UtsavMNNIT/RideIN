import { env } from "@/config/env";

/**
 * Minimal fetch wrapper for the demo flow.
 *
 * Production hardening (later phases): retry policy, auth header injection
 * from in-memory token, normalised ApiError, request-id correlation.
 */

export class ApiError extends Error {
  constructor(public status: number, message: string, public body?: unknown) {
    super(message);
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const url = path.startsWith("http") ? path : `${env.NEXT_PUBLIC_API_BASE_URL}${path}`;
  const res = await fetch(url, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
  });
  if (!res.ok) {
    let body: unknown;
    try { body = await res.json(); } catch { /* non-JSON error */ }
    throw new ApiError(res.status, `HTTP ${res.status} ${res.statusText}`, body);
  }
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

export const api = {
  get:  <T>(path: string)                       => request<T>(path),
  post: <T>(path: string, body?: unknown)       => request<T>(path, { method: "POST", body: JSON.stringify(body ?? {}) }),
};
