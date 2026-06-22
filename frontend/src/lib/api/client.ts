import { env } from "@/config/env";
import { getAccessToken } from "@/lib/auth/session";

/**
 * Fetch wrapper for the app.
 *
 * - Attaches the bearer token from the session store when present.
 * - Tags every request with an X-Request-Id for log correlation.
 * - Retries idempotent GETs on transient failures (network error / 5xx) with
 *   bounded exponential backoff. Non-idempotent verbs are never retried.
 * - Normalises failures into {@link ApiError}, lifting a human-readable message
 *   out of the backend's JSON error body when one is present.
 */

const MAX_GET_RETRIES = 2;
const RETRY_BASE_DELAY_MS = 300;

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public body?: unknown,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

/** Pull the most descriptive message out of a parsed backend error body. */
function messageFromBody(body: unknown, fallback: string): string {
  if (body && typeof body === "object") {
    const b = body as Record<string, unknown>;
    for (const key of ["message", "detail", "error", "title"]) {
      if (typeof b[key] === "string" && b[key]) return b[key] as string;
    }
  }
  return fallback;
}

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function newRequestId(): string {
  try {
    return crypto.randomUUID();
  } catch {
    return `${Date.now()}-${Math.round(Math.random() * 1e9)}`;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const url = path.startsWith("http") ? path : `${env.NEXT_PUBLIC_API_BASE_URL}${path}`;
  const method = (init?.method ?? "GET").toUpperCase();
  const token = getAccessToken();
  const canRetry = method === "GET";

  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    "X-Request-Id": newRequestId(),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...((init?.headers as Record<string, string>) ?? {}),
  };

  let lastError: unknown;
  for (let attempt = 0; attempt <= (canRetry ? MAX_GET_RETRIES : 0); attempt++) {
    if (attempt > 0) await delay(RETRY_BASE_DELAY_MS * 2 ** (attempt - 1));

    let res: Response;
    try {
      res = await fetch(url, { ...init, headers });
    } catch {
      // Network-level failure — retry GETs, otherwise surface immediately.
      lastError = new ApiError(0, "Network error. Please try again.");
      if (canRetry && attempt < MAX_GET_RETRIES) continue;
      throw lastError;
    }

    if (res.ok) {
      if (res.status === 204) return undefined as T;
      return (await res.json()) as T;
    }

    // 5xx is transient — retry GETs; 4xx is the caller's problem — fail fast.
    let body: unknown;
    try {
      body = await res.json();
    } catch {
      /* non-JSON error body */
    }
    const err = new ApiError(
      res.status,
      messageFromBody(body, `HTTP ${res.status} ${res.statusText}`),
      body,
    );
    if (canRetry && res.status >= 500 && attempt < MAX_GET_RETRIES) {
      lastError = err;
      continue;
    }
    throw err;
  }

  // Exhausted retries on a transient failure.
  throw lastError ?? new ApiError(0, "Request failed after retries.");
}

export const api = {
  get:  <T>(path: string)                 => request<T>(path),
  post: <T>(path: string, body?: unknown) => request<T>(path, { method: "POST", body: JSON.stringify(body ?? {}) }),
  put:  <T>(path: string, body?: unknown) => request<T>(path, { method: "PUT",  body: JSON.stringify(body ?? {}) }),
};
