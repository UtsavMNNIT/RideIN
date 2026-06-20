import { api } from "@/lib/api/client";
import type {
  RegisterRiderRequest,
  RiderLoginResponse,
  RiderResponse,
} from "@/domain/rider/types";

import { riderUrl } from "./endpoints";

/**
 * Authenticate a rider against the real backend (rider-service via the gateway).
 * Returns the JWT + the rider profile. Throws {@link ApiError} on failure
 * (401 = bad credentials, 404 = no account) — callers map status → message.
 */
export function loginRider(email: string, password: string): Promise<RiderLoginResponse> {
  return api.post<RiderLoginResponse>(riderUrl("/login"), { email, password });
}

/**
 * Register a new rider. Returns the created profile (no token — the caller then
 * proceeds through the 2FA + login step). Throws {@link ApiError} (409 = email
 * already registered).
 */
export function registerRider(body: RegisterRiderRequest): Promise<RiderResponse> {
  return api.post<RiderResponse>(riderUrl(""), body);
}
