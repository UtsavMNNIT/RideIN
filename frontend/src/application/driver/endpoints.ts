import { env } from "@/config/env";

/**
 * Builds an absolute driver-service URL. Driver-service lives behind the
 * api-gateway (paths /api/v1/drivers/**, which the gateway rewrites to
 * /v1/drivers/**). When NEXT_PUBLIC_DRIVER_API_BASE_URL points straight at
 * driver-service (:8083, the no-gateway fallback) there's no /api prefix, so we
 * detect that port and drop it.
 *
 * @param path sub-path after the drivers collection, e.g. "/login" or "/{id}/online".
 */
export function driverUrl(path: string): string {
  const base = env.NEXT_PUBLIC_DRIVER_API_BASE_URL.replace(/\/+$/, "");
  const prefix = base.includes(":8083") ? "/v1/drivers" : "/api/v1/drivers";
  return `${base}${prefix}${path}`;
}
