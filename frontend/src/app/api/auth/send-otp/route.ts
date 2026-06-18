import { NextResponse } from "next/server";

import { generateOtp, sendSms, storeOtp } from "@/lib/server/otp";

/**
 * POST { phone } → generate a 6-digit OTP, store it server-side, and send it
 * over SMS via Twilio. When Twilio isn't configured we return the code as
 * `devOtp` so the 2FA flow stays testable in local dev (demo only — a real
 * deployment must never return the code to the client).
 */
export const dynamic = "force-dynamic";

export async function POST(req: Request) {
  let phone = "";
  try {
    const body = (await req.json()) as { phone?: unknown };
    if (typeof body.phone === "string") phone = body.phone.trim();
  } catch {
    /* fall through */
  }

  if (!phone) {
    return NextResponse.json({ error: "phone is required" }, { status: 400 });
  }

  const code = generateOtp();
  storeOtp(phone, code);

  const result = await sendSms(phone, code);

  if (result.configured && !result.delivered) {
    return NextResponse.json(
      { sent: false, configured: true, error: result.error },
      { status: 502 },
    );
  }

  // Demo fallback: no SMS provider, so hand the code back for on-screen display.
  if (!result.configured) {
    return NextResponse.json({ sent: true, configured: false, devOtp: code });
  }

  return NextResponse.json({ sent: true, configured: true });
}
