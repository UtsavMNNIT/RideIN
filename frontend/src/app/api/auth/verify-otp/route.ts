import { NextResponse } from "next/server";

import { verifyOtp } from "@/lib/server/otp";

/**
 * POST { phone, code } → verify the OTP against the server-side store.
 * Returns { ok: true } on success, or { ok: false, reason } otherwise.
 */
export const dynamic = "force-dynamic";

export async function POST(req: Request) {
  let phone = "";
  let code = "";
  try {
    const body = (await req.json()) as { phone?: unknown; code?: unknown };
    if (typeof body.phone === "string") phone = body.phone.trim();
    if (typeof body.code === "string") code = body.code.trim();
  } catch {
    /* fall through */
  }

  if (!phone || !code) {
    return NextResponse.json({ error: "phone and code are required" }, { status: 400 });
  }

  const result = verifyOtp(phone, code);
  return NextResponse.json(result);
}
