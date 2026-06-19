import { NextResponse } from "next/server";

/**
 * Server-side email verification proxy (ZeroBounce).
 *
 * Calls ZeroBounce's /v2/validate so the API key never reaches the browser.
 * The auth forms POST { email } here and decide what to do based on the
 * normalised `deliverability` verdict.
 *
 * Env (server-only — NOT prefixed NEXT_PUBLIC):
 *   EMAIL_VERIFY_API_KEY   — ZeroBounce API key (required to actually verify)
 *
 * Reality check: no provider can *guarantee* an inbox exists — Gmail accepts
 * all addresses at the SMTP layer to block enumeration. "DELIVERABLE" is a
 * strong signal (valid mailbox per ZeroBounce), not a certainty.
 */

// Don't statically optimise a route that hits an external API.
export const dynamic = "force-dynamic";

type Verdict = "DELIVERABLE" | "UNDELIVERABLE" | "UNKNOWN";

/** Map ZeroBounce `status` → our three-state verdict. */
function mapStatus(status?: string): Verdict {
  switch (status) {
    case "valid":
      return "DELIVERABLE";
    case "invalid":
      return "UNDELIVERABLE";
    // catch-all | unknown | spamtrap | abuse | do_not_mail → not safe to assert
    default:
      return "UNKNOWN";
  }
}

export async function POST(req: Request) {
  let email = "";
  try {
    const body = (await req.json()) as { email?: unknown };
    if (typeof body.email === "string") email = body.email.trim();
  } catch {
    /* fall through to the empty-email guard */
  }

  if (!email) {
    return NextResponse.json({ error: "email is required" }, { status: 400 });
  }

  const apiKey = process.env.EMAIL_VERIFY_API_KEY;
  if (!apiKey) {
    // Not wired up yet — tell the client so it can allow signup but warn.
    return NextResponse.json({
      configured: false,
      deliverability: "UNKNOWN" satisfies Verdict,
    });
  }

  const url =
    `https://api.zerobounce.net/v2/validate?api_key=${apiKey}` +
    `&email=${encodeURIComponent(email)}&ip_address=`;

  try {
    const res = await fetch(url, { cache: "no-store" });
    if (!res.ok) {
      return NextResponse.json({
        configured: true,
        deliverability: "UNKNOWN" satisfies Verdict,
        error: `provider responded ${res.status}`,
      });
    }

    const data = (await res.json()) as {
      status?: string;
      sub_status?: string;
      free_email?: boolean;
      mx_found?: string; // ZeroBounce returns "true"/"false" as strings
      error?: string;
    };

    // An auth/quota error comes back 200 with an `error` field populated.
    if (data.error) {
      return NextResponse.json({
        configured: true,
        deliverability: "UNKNOWN" satisfies Verdict,
        error: data.error,
      });
    }

    return NextResponse.json({
      configured: true,
      deliverability: mapStatus(data.status),
      status: data.status ?? null,
      subStatus: data.sub_status ?? null,
      isDisposable: data.sub_status === "disposable",
      isFreeEmail: data.free_email ?? null,
      hasMxRecord: data.mx_found === "true",
    });
  } catch {
    // Network/provider failure — don't hard-block the user on our outage.
    return NextResponse.json({
      configured: true,
      deliverability: "UNKNOWN" satisfies Verdict,
      error: "verification service unreachable",
    });
  }
}
