/**
 * Demo-grade session: a userId + role kept in cookies (and mirrored in
 * sessionStorage for tab-local reads without re-parsing the cookie header).
 *
 * Phase F-2 will replace this with a real JWT in an HttpOnly cookie set by a
 * Next route handler that proxies the backend's /auth/login.
 */

import type { Role } from "@/ui/components/common/RoleBadge";

export type Session = { userId: string; role: Role };

const COOKIE_ROLE = "rf_role";
const COOKIE_USER = "rf_userId";
const COOKIE_MAX_AGE = 60 * 60 * 24 * 7; // 7 days

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

export function signOut() {
  deleteCookie(COOKIE_ROLE);
  deleteCookie(COOKIE_USER);
}
