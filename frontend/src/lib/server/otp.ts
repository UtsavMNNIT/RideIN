import { randomInt } from "crypto";

/**
 * Server-side OTP engine for SMS two-factor auth.
 *
 * The code is generated AND verified here — the browser never sees it (except
 * in the unconfigured demo fallback, where there's no SMS provider to deliver
 * it). Codes live in an in-process Map with a short TTL and an attempt cap.
 *
 * NOTE: a module-level Map is fine for single-instance local dev but resets on
 * server restart / HMR and won't work across multiple instances. Production
 * would back this with Redis (keyed by phone, same TTL/attempt semantics).
 *
 * Env (server-only — never NEXT_PUBLIC):
 *   TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_FROM_NUMBER
 */

const TTL_MS = 5 * 60 * 1000; // 5 minutes
const MAX_ATTEMPTS = 5;

type Entry = { code: string; expiresAt: number; attempts: number };

// Survives HMR in dev by hanging off globalThis rather than module scope.
const g = globalThis as unknown as { __rfOtpStore?: Map<string, Entry> };
const store: Map<string, Entry> = g.__rfOtpStore ?? (g.__rfOtpStore = new Map());

/** Strip spaces/dashes so "+1 555-010" and "+1555010" key the same entry. */
export function normalizePhone(phone: string): string {
  return phone.replace(/[\s-]/g, "");
}

/** Cryptographically-random 6-digit code (100000–999999). */
export function generateOtp(): string {
  return String(randomInt(100000, 1000000));
}

export function storeOtp(phone: string, code: string): void {
  store.set(normalizePhone(phone), {
    code,
    expiresAt: Date.now() + TTL_MS,
    attempts: 0,
  });
}

export type VerifyResult =
  | { ok: true }
  | { ok: false; reason: "no-code" | "expired" | "too-many-attempts" | "mismatch" };

export function verifyOtp(phone: string, code: string): VerifyResult {
  const key = normalizePhone(phone);
  const entry = store.get(key);
  if (!entry) return { ok: false, reason: "no-code" };

  if (Date.now() > entry.expiresAt) {
    store.delete(key);
    return { ok: false, reason: "expired" };
  }
  if (entry.attempts >= MAX_ATTEMPTS) {
    store.delete(key);
    return { ok: false, reason: "too-many-attempts" };
  }

  entry.attempts += 1;
  if (entry.code !== code.trim()) return { ok: false, reason: "mismatch" };

  store.delete(key); // single-use
  return { ok: true };
}

export type SendResult =
  | { delivered: true; configured: true }
  | { delivered: false; configured: false } // demo mode, no provider
  | { delivered: false; configured: true; error: string };

/** Deliver the code via Twilio if configured; otherwise signal demo mode. */
export async function sendSms(phone: string, code: string): Promise<SendResult> {
  const sid = process.env.TWILIO_ACCOUNT_SID;
  const token = process.env.TWILIO_AUTH_TOKEN;
  const from = process.env.TWILIO_FROM_NUMBER;

  if (!sid || !token || !from) {
    return { delivered: false, configured: false };
  }

  try {
    const body = new URLSearchParams({
      From: from,
      To: phone,
      Body: `Your RideFlow verification code is ${code}. It expires in 5 minutes.`,
    });
    const res = await fetch(
      `https://api.twilio.com/2010-04-01/Accounts/${sid}/Messages.json`,
      {
        method: "POST",
        headers: {
          Authorization: `Basic ${Buffer.from(`${sid}:${token}`).toString("base64")}`,
          "Content-Type": "application/x-www-form-urlencoded",
        },
        body,
      },
    );
    if (!res.ok) {
      const detail = (await res.json().catch(() => ({}))) as { message?: string };
      return {
        delivered: false,
        configured: true,
        error: detail.message ?? `Twilio responded ${res.status}`,
      };
    }
    return { delivered: true, configured: true };
  } catch {
    return { delivered: false, configured: true, error: "Twilio unreachable" };
  }
}
