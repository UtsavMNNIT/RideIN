import type { NextConfig } from "next";

/**
 * Local-dev REST + WebSocket proxy.
 *
 * In production (Vercel) the frontend hits the API Gateway directly with CORS;
 * the rewrites below keep the local-dev path same-origin so cookies, CORS, and
 * the WebSocket upgrade all "just work" against the docker-compose stack.
 *
 * The base URLs come from env so a deployed build can be pointed elsewhere.
 */
const apiBase = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

const nextConfig: NextConfig = {
  reactStrictMode: true,
  poweredByHeader: false,
  // Stabilized as a top-level option in Next 15.5 (and now Turbopack-compatible);
  // it previously lived under `experimental`.
  typedRoutes: true,
  async rewrites() {
    return [
      { source: "/api/gateway/:path*", destination: `${apiBase}/:path*` },
    ];
  },
};

export default nextConfig;
