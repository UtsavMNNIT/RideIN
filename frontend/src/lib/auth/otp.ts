/**
 * Client helpers for SMS OTP two-factor auth
 * (see app/api/auth/send-otp + verify-otp).
 */

export type SendOtpResult =
  | { sent: true; configured: boolean; devOtp?: string }
  | { sent: false; error: string };

export type VerifyOtpResult =
  | { ok: true }
  | { ok: false; reason?: string };

/** Request a 6-digit code be sent to the given phone number. */
export async function sendOtp(phone: string): Promise<SendOtpResult> {
  try {
    const res = await fetch("/api/auth/send-otp", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ phone }),
    });
    const data = await res.json();
    if (!res.ok || !data.sent) {
      return { sent: false, error: data.error ?? "Couldn't send the code." };
    }
    return { sent: true, configured: !!data.configured, devOtp: data.devOtp };
  } catch {
    return { sent: false, error: "Network error while sending the code." };
  }
}

/** Verify a code the user entered. */
export async function verifyOtp(phone: string, code: string): Promise<VerifyOtpResult> {
  try {
    const res = await fetch("/api/auth/verify-otp", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ phone, code }),
    });
    return (await res.json()) as VerifyOtpResult;
  } catch {
    return { ok: false, reason: "network" };
  }
}
