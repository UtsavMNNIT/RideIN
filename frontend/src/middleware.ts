import { NextResponse, type NextRequest } from "next/server";
import { jwtVerify } from "jose";

/**
 * Edge auth gate.
 *
 * Conventions:
 *   - {@code rf_role} cookie carries "RIDER" | "DRIVER" | "ADMIN" once logged in.
 *   - {@code rf_token} cookie carries the backend JWT (mirrored from the session
 *     store) so it can be verified here at the edge.
 *   - Route groups are protected by prefix match; unauthenticated users hitting
 *     a protected prefix are redirected to /login with a `next` param.
 *
 * RIDER/DRIVER sessions are verified cryptographically (HS256 signature + exp +
 * role claim) against the service secrets. ADMIN is demo-grade (cookie role
 * only) — there is no backend admin auth yet, and the admin pages consume only
 * public endpoints.
 */
const PROTECTED_PREFIXES: Record<string, ReadonlyArray<string>> = {
  RIDER:  ["/home", "/request", "/ride", "/history", "/payment"],
  DRIVER: ["/dashboard", "/earnings", "/ride"],
  ADMIN:  ["/overview", "/tariffs", "/surge"],
};

const PUBLIC_PATHS = new Set<string>([
  "/", "/login", "/login/credentials", "/register", "/forgot-password", "/about",
]);

// Service JWT secrets. Defaults match the gateway's dev secrets so verification
// works out of the box; override via server-only env in real environments.
const RIDER_SECRET = new TextEncoder().encode(
  process.env.RIDER_JWT_SECRET ?? "dev-only-rider-service-secret-change-me-please-0123456789",
);
const DRIVER_SECRET = new TextEncoder().encode(
  process.env.DRIVER_JWT_SECRET ?? "dev-only-driver-service-secret-change-me-please-0123456789",
);

/** Verify against either service key (like the gateway keyset); null if invalid/expired. */
async function verifyToken(token: string): Promise<{ role?: string } | null> {
  for (const secret of [RIDER_SECRET, DRIVER_SECRET]) {
    try {
      const { payload } = await jwtVerify(token, secret, { algorithms: ["HS256"] });
      return payload as { role?: string };
    } catch {
      /* try the next key */
    }
  }
  return null;
}

function redirectToLogin(req: NextRequest, pathname: string) {
  const url = req.nextUrl.clone();
  url.pathname = "/login";
  url.searchParams.set("next", pathname);
  return NextResponse.redirect(url);
}

export async function middleware(req: NextRequest) {
  const { pathname } = req.nextUrl;

  // Always allow framework + static assets.
  if (
    pathname.startsWith("/_next") ||
    pathname.startsWith("/api/") ||
    pathname.startsWith("/leaflet") ||
    pathname.startsWith("/icons") ||
    pathname.startsWith("/images") ||
    pathname === "/favicon.ico"
  ) {
    return NextResponse.next();
  }

  if (PUBLIC_PATHS.has(pathname)) {
    return NextResponse.next();
  }

  const requiresAuth = Object.values(PROTECTED_PREFIXES).some((prefixes) =>
    prefixes.some((p) => pathname.startsWith(p)),
  );
  if (!requiresAuth) return NextResponse.next();

  const role = req.cookies.get("rf_role")?.value;
  if (!role) return redirectToLogin(req, pathname);

  // Role-prefix gate: a session may only enter its own route group.
  const allowedPrefixes = PROTECTED_PREFIXES[role] ?? [];
  if (!allowedPrefixes.some((p) => pathname.startsWith(p))) {
    return redirectToLogin(req, pathname);
  }

  // Cryptographically verify real (RIDER/DRIVER) sessions. ADMIN stays demo-grade.
  if (role === "RIDER" || role === "DRIVER") {
    const token = req.cookies.get("rf_token")?.value;
    const claims = token ? await verifyToken(token) : null;
    if (!claims || claims.role !== role) {
      return redirectToLogin(req, pathname);
    }
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico).*)"],
};
