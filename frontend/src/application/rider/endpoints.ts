import { env } from "@/config/env";

/**
 * Builds an absolute rider-service URL. Rider-service lives behind the
 * api-gateway (paths /api/v1/riders/**, which the gateway rewrites to
 * /v1/riders/**). When NEXT_PUBLIC_RIDER_API_BASE_URL points straight at
 * rider-service (:8081, the no-gateway fallback) there's no /api prefix, so we
 * detect that port and drop it.
 *
 * @param path sub-path after the riders collection, e.g. "/login" or "/{id}/rides".
 */
export function riderUrl(path: string): string {
  const base = env.NEXT_PUBLIC_RIDER_API_BASE_URL.replace(/\/+$/, "");
  const prefix = base.includes(":8081") ? "/v1/riders" : "/api/v1/riders";
  return `${base}${prefix}${path}`;
}
