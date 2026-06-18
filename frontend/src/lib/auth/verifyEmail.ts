/**
 * Client helper for the server-side email verification proxy
 * (see app/api/auth/verify-email/route.ts).
 */

export type EmailVerdict = "DELIVERABLE" | "UNDELIVERABLE" | "UNKNOWN";

export type EmailCheck = {
  configured: boolean;
  deliverability: EmailVerdict;
  isDisposable?: boolean | null;
};

/** Cheap client-side format gate so we don't burn API quota on typos. */
export function isValidEmailFormat(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim());
}

/** Ask the server to verify deliverability. Never throws — returns UNKNOWN on error. */
export async function verifyEmail(email: string): Promise<EmailCheck> {
  try {
    const res = await fetch("/api/auth/verify-email", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email }),
    });
    if (!res.ok) return { configured: false, deliverability: "UNKNOWN" };
    return (await res.json()) as EmailCheck;
  } catch {
    return { configured: false, deliverability: "UNKNOWN" };
  }
}
