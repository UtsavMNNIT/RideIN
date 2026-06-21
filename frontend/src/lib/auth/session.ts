/**
 * Demo-grade session: a userId + role kept in cookies (and mirrored in
 * sessionStorage for tab-local reads without re-parsing the cookie header).
 *
 * Phase F-2 will replace this with a real JWT in an HttpOnly cookie set by a
 * Next route handler that proxies the backend's /auth/login.
 */

import type { DriverResponse } from "@/domain/driver/types";
import type { RiderResponse } from "@/domain/rider/types";
import type { Role } from "@/ui/components/common/RoleBadge";

export type Session = { userId: string; role: Role };

const COOKIE_ROLE = "rf_role";
const COOKIE_USER = "rf_userId";
// The JWT is mirrored into a cookie (in addition to localStorage, which the api
// client reads) so the edge middleware can cryptographically verify it. Not
// HttpOnly — the client still needs to read the bearer for fetch; this is no
// more XSS-exposed than the existing localStorage copy. A fully-hardened setup
// (HttpOnly cookie + API calls proxied server-side) is a later refactor.
const COOKIE_TOKEN = "rf_token";
const COOKIE_MAX_AGE = 60 * 60 * 24 * 7; // 7 days

// Demo-grade token + profile storage. A real build keeps the JWT in an HttpOnly
// cookie set server-side; here we mirror it in localStorage so the client-side
// api wrapper can attach the bearer header. Documented throwaway, not prod.
//
// One user is signed in per browser, so the JWT lives under a single role-agnostic
// key that both driver and rider sign-in write and getAccessToken() reads. Profiles
// stay per-role because their shapes differ (DriverResponse vs RiderResponse).
const STORE_TOKEN          = "rf_token";
const STORE_DRIVER_PROFILE = "rf_driver_profile";
const STORE_RIDER_PROFILE  = "rf_rider_profile";

function readCookie(name: string): string | null {
  if (typeof document === "undefined") return null;
  const parts = document.cookie.split("; ");
  for (const part of parts) {
    const [k, v] = part.split("=");
    if (k === name && v) return decodeURIComponent(v);
  }
  return null;
}

function writeCookie(name: string, value: string, maxAgeSec: number) {
  document.cookie = `${name}=${encodeURIComponent(value)}; path=/; max-age=${maxAgeSec}; SameSite=Lax`;
}

function deleteCookie(name: string) {
  document.cookie = `${name}=; path=/; max-age=0; SameSite=Lax`;
}

export function getSession(): Session | null {
  const role   = readCookie(COOKIE_ROLE) as Role | null;
  const userId = readCookie(COOKIE_USER);
  if (!role || !userId) return null;
  return { userId, role };
}

export function signIn(role: Role): Session {
  // Stable per-tab id — refreshing the page keeps the same demo user, so the
  // backend's persisted notifications list still belongs to "me".
  const existing = readCookie(COOKIE_USER);
  const userId   = existing ?? crypto.randomUUID();
  writeCookie(COOKIE_ROLE, role,   COOKIE_MAX_AGE);
  writeCookie(COOKIE_USER, userId, COOKIE_MAX_AGE);
  return { userId, role };
}

/**
 * Sign in a driver authenticated against the real backend. The session userId
 * is the *backend* driver UUID (so middleware + the WS notification channel both
 * route to the real driver), and the JWT + profile are persisted for the api
 * wrapper and the dashboard's initial render.
 */
export function signInDriver(driver: DriverResponse, token: string): Session {
  writeCookie(COOKIE_ROLE, "DRIVER", COOKIE_MAX_AGE);
  writeCookie(COOKIE_USER, driver.id, COOKIE_MAX_AGE);
  writeCookie(COOKIE_TOKEN, token, COOKIE_MAX_AGE);
  if (typeof window !== "undefined") {
    window.localStorage.setItem(STORE_TOKEN, token);
    window.localStorage.setItem(STORE_DRIVER_PROFILE, JSON.stringify(driver));
  }
  return { userId: driver.id, role: "DRIVER" };
}

/**
 * Sign in a rider authenticated against the real backend (rider-service via the
 * gateway). The session userId is the *backend* rider UUID so the request-ride
 * endpoint and the WS notification channel both route to the real rider.
 */
export function signInRider(rider: RiderResponse, token: string): Session {
  writeCookie(COOKIE_ROLE, "RIDER", COOKIE_MAX_AGE);
  writeCookie(COOKIE_USER, rider.id, COOKIE_MAX_AGE);
  writeCookie(COOKIE_TOKEN, token, COOKIE_MAX_AGE);
  if (typeof window !== "undefined") {
    window.localStorage.setItem(STORE_TOKEN, token);
    window.localStorage.setItem(STORE_RIDER_PROFILE, JSON.stringify(rider));
  }
  return { userId: rider.id, role: "RIDER" };
}

/** The bearer token for the logged-in user, or null. Safe to call on the server. */
export function getAccessToken(): string | null {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem(STORE_TOKEN);
}

/** The driver profile cached at login, or null. Used to seed the dashboard query. */
export function getDriverProfile(): DriverResponse | null {
  if (typeof window === "undefined") return null;
  const raw = window.localStorage.getItem(STORE_DRIVER_PROFILE);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as DriverResponse;
  } catch {
    return null;
  }
}

/** Overwrite the cached driver profile (e.g. after an online/offline/location response). */
export function setDriverProfile(driver: DriverResponse): void {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(STORE_DRIVER_PROFILE, JSON.stringify(driver));
}

/** The rider profile cached at login, or null. Used to seed rider views. */
export function getRiderProfile(): RiderResponse | null {
  if (typeof window === "undefined") return null;
  const raw = window.localStorage.getItem(STORE_RIDER_PROFILE);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as RiderResponse;
  } catch {
    return null;
  }
}

/** Overwrite the cached rider profile. */
export function setRiderProfile(rider: RiderResponse): void {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(STORE_RIDER_PROFILE, JSON.stringify(rider));
}

export function signOut() {
  deleteCookie(COOKIE_ROLE);
  deleteCookie(COOKIE_USER);
  deleteCookie(COOKIE_TOKEN);
  if (typeof window !== "undefined") {
    window.localStorage.removeItem(STORE_TOKEN);
    window.localStorage.removeItem(STORE_DRIVER_PROFILE);
    window.localStorage.removeItem(STORE_RIDER_PROFILE);
  }
}
