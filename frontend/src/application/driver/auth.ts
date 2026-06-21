import { api } from "@/lib/api/client";
import type {
  DriverResponse,
  LoginResponse,
  RegisterDriverRequest,
} from "@/domain/driver/types";

import { driverUrl } from "./endpoints";

/**
 * Authenticate a driver against the real backend (driver-service via the
 * gateway). Returns the JWT + the driver profile. Throws {@link ApiError} on
 * failure (401 = bad credentials) — callers map status → message.
 */
export function loginDriver(email: string, password: string): Promise<LoginResponse> {
  return api.post<LoginResponse>(driverUrl("/login"), { email, password });
}

/**
 * Register a new driver. Returns the created profile (no token — the caller then
 * proceeds through the 2FA + login step, exactly like rider registration).
 * Throws {@link ApiError} (409 = email already registered).
 */
export function registerDriver(body: RegisterDriverRequest): Promise<DriverResponse> {
  return api.post<DriverResponse>(driverUrl(""), body);
}
