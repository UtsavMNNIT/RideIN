"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useState } from "react";
import { toast } from "sonner";

import { loginDriver } from "@/application/driver/auth";
import { loginRider } from "@/application/rider/auth";
import type { LoginResponse } from "@/domain/driver/types";
import type { RiderLoginResponse } from "@/domain/rider/types";
import { ApiError } from "@/lib/api/client";
import { sendOtp, verifyOtp } from "@/lib/auth/otp";
import { signInDriver, signInRider } from "@/lib/auth/session";
import { isValidEmailFormat, verifyEmail } from "@/lib/auth/verifyEmail";
import type { Role } from "@/ui/components/common/RoleBadge";
import { Button } from "@/ui/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/ui/components/ui/card";
import { Input } from "@/ui/components/ui/input";

function parseRole(raw: string | null): Role {
  return raw === "DRIVER" ? "DRIVER" : "RIDER";
}

const OTP_ERRORS: Record<string, string> = {
  mismatch: "Incorrect code. Please try again.",
  expired: "That code has expired. Tap “Resend code”.",
  "too-many-attempts": "Too many attempts. Tap “Resend code” for a new one.",
  "no-code": "No active code. Tap “Resend code”.",
  network: "Network error. Please try again.",
};

function CredentialsForm() {
  const router = useRouter();
  const params = useSearchParams();
  const role = parseRole(params.get("role"));
  const roleLabel = role === "DRIVER" ? "Driver" : "Rider";

  // Step 1 (password) state
  const [step, setStep] = useState<"password" | "otp">("password");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [notFound, setNotFound] = useState(false);
  const [checking, setChecking] = useState(false);

  // Step 2 (2FA) state
  // The real backend login result carried across the 2FA step (one per role).
  const [driverLogin, setDriverLogin] = useState<LoginResponse | null>(null);
  const [riderLogin, setRiderLogin] = useState<RiderLoginResponse | null>(null);
  const [phone, setPhone] = useState("");
  const [otp, setOtp] = useState("");
  const [sending, setSending] = useState(false);
  const [verifying, setVerifying] = useState(false);

  const landingFor = (r: Role) => (r === "DRIVER" ? "/dashboard" : "/home");

  const deliverCode = async (to: string) => {
    if (!to.trim()) {
      setError("Enter a phone number to receive the code.");
      return;
    }
    setError(null);
    setSending(true);
    const res = await sendOtp(to);
    setSending(false);
    if (!res.sent) {
      setError(res.error);
      return;
    }
    if (res.configured) {
      toast.success(`A 6-digit code was sent to ${to}.`);
    } else {
      // Demo mode — no SMS provider, so show the code on screen.
      toast.info(`Demo mode — your code is ${res.devOtp}`, { duration: 12000 });
    }
  };

  const onPasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setNotFound(false);

    if (!isValidEmailFormat(email)) {
      setError("Please enter a valid email address.");
      return;
    }

    // Deliverability gate — block obviously dead addresses before the password.
    setChecking(true);
    const check = await verifyEmail(email);
    setChecking(false);
    if (check.deliverability === "UNDELIVERABLE") {
      setError("That email address doesn't appear to exist. Please check it.");
      return;
    }

    // Both roles authenticate against the real backend. Password is the first
    // factor; the SMS code (delivered to the account's phone) is the second.
    setChecking(true);
    try {
      let accountPhone: string;
      if (role === "DRIVER") {
        const res = await loginDriver(email, password);
        setDriverLogin(res);
        accountPhone = res.driver.phone;
      } else {
        const res = await loginRider(email, password);
        setRiderLogin(res);
        accountPhone = res.rider.phone;
      }
      setPhone(accountPhone);
      setOtp("");
      setStep("otp");
      await deliverCode(accountPhone);
    } catch (err) {
      const status = err instanceof ApiError ? err.status : 0;
      if (status === 401 || status === 403) {
        setError("Incorrect email or password.");
      } else if (status === 404) {
        setNotFound(true);
        setError(`No ${role.toLowerCase()} account found with that email. Sign up to get started.`);
      } else {
        setError("Couldn't sign you in right now. Please try again.");
      }
    } finally {
      setChecking(false);
    }
  };

  const onOtpSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setVerifying(true);
    const res = await verifyOtp(phone, otp);
    setVerifying(false);
    if (!res.ok) {
      setError(OTP_ERRORS[res.reason ?? "mismatch"] ?? "Couldn't verify that code.");
      return;
    }
    if (role === "DRIVER" && driverLogin) {
      // Persist the real backend session (driver UUID + JWT) and head to the dashboard.
      signInDriver(driverLogin.driver, driverLogin.accessToken);
      toast.success(`Welcome back, ${driverLogin.driver.fullName}!`);
      router.push("/dashboard");
      return;
    }
    if (riderLogin) {
      // Persist the real backend session (rider UUID + JWT) and head home.
      signInRider(riderLogin.rider, riderLogin.accessToken);
      toast.success(`Welcome back, ${riderLogin.rider.fullName}!`);
      router.push(landingFor(role));
      return;
    }
    setError("Your session expired. Please sign in again.");
    setStep("password");
  };

  return (
    <Card className="border-[#8cc63f]/30 shadow-lg">
      <CardHeader>
        <CardTitle>
          {step === "password" ? `Login as ${roleLabel}` : "Two-factor verification"}
        </CardTitle>
        <CardDescription>
          {step === "password" ? (
            <>
              Enter your credentials to continue. New here?{" "}
              <Link
                href={`/register?role=${role}`}
                className="font-medium text-[#5b8a1e] underline-offset-4 hover:underline"
              >
                Create an account
              </Link>
              .
            </>
          ) : (
            "Enter the 6-digit code we sent to your phone to finish signing in."
          )}
        </CardDescription>
      </CardHeader>

      <CardContent>
        {/* Step 1 — email + password */}
        {step === "password" && (
          <form onSubmit={onPasswordSubmit} className="flex flex-col gap-4">
            <div className="flex flex-col gap-1.5">
              <label htmlFor="email" className="text-sm font-medium">
                Email
              </label>
              <Input
                id="email"
                type="email"
                autoComplete="email"
                placeholder="you@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>

            <div className="flex flex-col gap-1.5">
              <div className="flex items-center justify-between">
                <label htmlFor="password" className="text-sm font-medium">
                  Password
                </label>
                <Link
                  href={`/forgot-password?role=${role}`}
                  className="text-xs font-medium text-[#5b8a1e] underline-offset-4 hover:underline"
                >
                  Forgot password?
                </Link>
              </div>
              <Input
                id="password"
                type="password"
                autoComplete="current-password"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>

            {error && <p className="text-sm font-medium text-destructive">{error}</p>}

            <Button
              type="submit"
              disabled={checking}
              className="w-full bg-[#8cc63f] text-white hover:bg-[#7ab32f]"
            >
              {checking ? "Verifying email…" : "Continue"}
            </Button>

            {notFound && (
              <Button
                type="button"
                variant="outline"
                className="w-full border-[#8cc63f] text-[#5b8a1e] hover:bg-[#8cc63f]/10 hover:text-[#5b8a1e]"
                onClick={() => router.push(`/register?role=${role}`)}
              >
                Sign up instead
              </Button>
            )}
          </form>
        )}

        {/* Step 2 — SMS OTP */}
        {step === "otp" && (
          <form onSubmit={onOtpSubmit} className="flex flex-col gap-4">
            <div className="flex flex-col gap-1.5">
              <label htmlFor="phone" className="text-sm font-medium">
                Phone number
              </label>
              <div className="flex gap-2">
                <Input
                  id="phone"
                  type="tel"
                  autoComplete="tel"
                  placeholder="+1 555 010 1234"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  required
                />
                <Button
                  type="button"
                  variant="outline"
                  disabled={sending}
                  onClick={() => deliverCode(phone)}
                  className="shrink-0 border-[#8cc63f] text-[#5b8a1e] hover:bg-[#8cc63f]/10 hover:text-[#5b8a1e]"
                >
                  {sending ? "Sending…" : "Resend code"}
                </Button>
              </div>
            </div>

            <div className="flex flex-col gap-1.5">
              <label htmlFor="otp" className="text-sm font-medium">
                6-digit code
              </label>
              <Input
                id="otp"
                inputMode="numeric"
                maxLength={6}
                placeholder="123456"
                className="tracking-[0.5em]"
                value={otp}
                onChange={(e) => setOtp(e.target.value.replace(/\D/g, ""))}
                required
              />
            </div>

            {error && <p className="text-sm font-medium text-destructive">{error}</p>}

            <Button
              type="submit"
              disabled={verifying || otp.length !== 6}
              className="w-full bg-[#8cc63f] text-white hover:bg-[#7ab32f]"
            >
              {verifying ? "Verifying…" : "Verify & sign in"}
            </Button>

            <button
              type="button"
              onClick={() => {
                setError(null);
                setOtp("");
                setStep("password");
              }}
              className="text-center text-xs text-muted-foreground underline-offset-4 hover:underline"
            >
              &larr; Use a different account
            </button>
          </form>
        )}

        <p className="mt-4 text-center text-xs text-muted-foreground">
          <Link href="/login" className="underline-offset-4 hover:underline">
            &larr; Back to role selection
          </Link>
        </p>
      </CardContent>
    </Card>
  );
}

export default function CredentialsPage() {
  return (
    <Suspense fallback={null}>
      <CredentialsForm />
    </Suspense>
  );
}
