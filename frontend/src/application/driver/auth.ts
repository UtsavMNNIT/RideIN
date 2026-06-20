import { api } from "@/lib/api/client";
import type { LoginResponse } from "@/domain/driver/types";

import { driverUrl } from "./endpoints";

/**
 * Authenticate a driver against the real backend (driver-service via the
 * gateway). Returns the JWT + the driver profile. Throws {@link ApiError} on
 * failure (401 = bad credentials) — callers map status → message.
 */
export function loginDriver(email: string, password: string): Promise<LoginResponse> {
  return api.post<LoginResponse>(driverUrl("/login"), { email, password });
}
