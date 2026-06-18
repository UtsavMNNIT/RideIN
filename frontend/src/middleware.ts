import { NextResponse, type NextRequest } from "next/server";

/**
 * Edge auth gate. Phase F-1: pass-through with role detection from cookie.
 * Phase F-2 (Authentication) will replace the trivial cookie check with JWT
 * verification using the WebCrypto-friendly `jose` library and a JWKS fetch
 * from common-security.
 *
 * Conventions:
 *   - {@code rf_role} cookie carries "RIDER" | "DRIVER" | "ADMIN" once logged in.
 *   - Route groups are protected by prefix match; unauthenticated users hitting
 *     a protected prefix are redirected to /login with a `next` param.
 */
const PROTECTED_PREFIXES: Record<string, ReadonlyArray<string>> = {
  RIDER:  ["/home", "/request", "/ride", "/history"],
  DRIVER: ["/dashboard", "/earnings", "/ride"],
  ADMIN:  ["/overview", "/tariffs", "/surge"],
};

const PUBLIC_PATHS = new Set<string>([
  "/", "/login", "/register", "/about",
]);

export function middleware(req: NextRequest) {
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

  const role = req.cookies.get("rf_role")?.value;
  const requiresAuth = Object.values(PROTECTED_PREFIXES).some((prefixes) =>
    prefixes.some((p) => pathname.startsWith(p)),
  );

  if (requiresAuth && !role) {
    const url = req.nextUrl.clone();
    url.pathname = "/login";
    url.searchParams.set("next", pathname);
    return NextResponse.redirect(url);
  }

  // Role-mismatch (e.g., DRIVER trying to hit /overview): block.
  if (role && requiresAuth) {
    const allowedPrefixes = PROTECTED_PREFIXES[role] ?? [];
    const allowed = allowedPrefixes.some((p) => pathname.startsWith(p));
    if (!allowed) {
      const url = req.nextUrl.clone();
      url.pathname = "/login";
      return NextResponse.redirect(url);
    }
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico).*)"],
};
