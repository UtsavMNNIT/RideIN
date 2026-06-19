"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Mail, Smartphone } from "lucide-react";
import { Suspense, useState } from "react";
import { toast } from "sonner";

import {
  findUserByEmail,
  findUserByPhone,
  resetPassword,
} from "@/lib/auth/demoUsers";
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

type Channel = "EMAIL" | "PHONE";
type Step = "choose" | "send" | "verify" | "reset";

function parseRole(raw: string | null): Role {
  return raw === "DRIVER" ? "DRIVER" : "RIDER";
}

function generateOtp(): string {
  // Demo only — a real flow generates this server-side and never returns it.
  return String(Math.floor(100000 + Math.random() * 900000));
}

function ForgotPasswordFlow() {
  const router = useRouter();
  const params = useSearchParams();
  const role = parseRole(params.get("role"));

  const [step, setStep] = useState<Step>("choose");
  const [channel, setChannel] = useState<Channel>("EMAIL");
  const [destination, setDestination] = useState(""); // email or phone entered
  const [accountEmail, setAccountEmail] = useState(""); // resolved account email
  const [sentOtp, setSentOtp] = useState("");
  const [enteredOtp, setEnteredOtp] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState<string | null>(null);

  const pickChannel = (c: Channel) => {
    setChannel(c);
    setError(null);
    setStep("send");
  };

  const sendCode = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const user =
      channel === "EMAIL"
        ? findUserByEmail(destination)
        : findUserByPhone(destination);

    if (!user) {
      setError(
        channel === "EMAIL"
          ? "No account found with that email."
          : "No account found with that phone number.",
      );
      return;
    }

    const otp = generateOtp();
    setSentOtp(otp);
    setAccountEmail(user.email);
    setStep("verify");

    // Demo: there's no email/SMS provider, so surface the code in a toast.
    toast.success(
      `Demo OTP sent to your ${channel === "EMAIL" ? "email" : "phone"}: ${otp}`,
      { duration: 10000 },
    );
  };

  const verifyOtp = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    if (enteredOtp.trim() !== sentOtp) {
      setError("That code is incorrect. Check the code and try again.");
      return;
    }
    setStep("reset");
  };

  const submitReset = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    if (password.length < 6) {
      setError("Password must be at least 6 characters.");
      return;
    }
    if (password !== confirm) {
      setError("Passwords do not match.");
      return;
    }
    resetPassword(accountEmail, password);
    toast.success("Password updated. Please sign in.");
    router.push(`/login/credentials?role=${role}`);
  };

  return (
    <Card className="border-[#8cc63f]/30 shadow-lg">
      <CardHeader>
        <CardTitle>Reset your password</CardTitle>
        <CardDescription>
          {step === "choose" &&
            "Where should we send your one-time code?"}
          {step === "send" &&
            `Enter the ${channel === "EMAIL" ? "email" : "phone number"} on your account.`}
          {step === "verify" && "Enter the 6-digit code we just sent you."}
          {step === "reset" && "Choose a new password for your account."}
        </CardDescription>
      </CardHeader>

      <CardContent>
        {/* Step 1 — choose delivery channel */}
        {step === "choose" && (
          <div className="flex flex-col gap-3">
            <Button
              onClick={() => pickChannel("EMAIL")}
              className="w-full bg-[#8cc63f] text-white hover:bg-[#7ab32f]"
            >
              <Mail className="h-4 w-4" />
              Send OTP to Email
            </Button>
            <Button
              onClick={() => pickChannel("PHONE")}
              variant="outline"
              className="w-full border-[#8cc63f] text-[#5b8a1e] hover:bg-[#8cc63f]/10 hover:text-[#5b8a1e]"
            >
              <Smartphone className="h-4 w-4" />
              Send OTP to Phone
            </Button>
          </div>
        )}

        {/* Step 2 — enter destination, send code */}
        {step === "send" && (
          <form onSubmit={sendCode} className="flex flex-col gap-4">
            <div className="flex flex-col gap-1.5">
              <label htmlFor="destination" className="text-sm font-medium">
                {channel === "EMAIL" ? "Email" : "Phone number"}
              </label>
              <Input
                id="destination"
                type={channel === "EMAIL" ? "email" : "tel"}
                placeholder={
                  channel === "EMAIL" ? "you@example.com" : "+1 555 010 1234"
                }
                value={destination}
                onChange={(e) => setDestination(e.target.value)}
                required
              />
            </div>
            {error && (
              <p className="text-sm font-medium text-destructive">{error}</p>
            )}
            <Button
              type="submit"
              className="w-full bg-[#8cc63f] text-white hover:bg-[#7ab32f]"
            >
              Send code
            </Button>
            <button
              type="button"
              onClick={() => {
                setError(null);
                setStep("choose");
              }}
              className="text-center text-xs text-muted-foreground underline-offset-4 hover:underline"
            >
              Use a different method
            </button>
          </form>
        )}

        {/* Step 3 — verify OTP */}
        {step === "verify" && (
          <form onSubmit={verifyOtp} className="flex flex-col gap-4">
            <div className="flex flex-col gap-1.5">
              <label htmlFor="otp" className="text-sm font-medium">
                One-time code
              </label>
              <Input
                id="otp"
                inputMode="numeric"
                maxLength={6}
                placeholder="123456"
                className="tracking-[0.5em]"
                value={enteredOtp}
                onChange={(e) => setEnteredOtp(e.target.value)}
                required
              />
            </div>
            {error && (
              <p className="text-sm font-medium text-destructive">{error}</p>
            )}
            <Button
              type="submit"
              className="w-full bg-[#8cc63f] text-white hover:bg-[#7ab32f]"
            >
              Verify code
            </Button>
          </form>
        )}

        {/* Step 4 — set new password */}
        {step === "reset" && (
          <form onSubmit={submitReset} className="flex flex-col gap-4">
            <div className="flex flex-col gap-1.5">
              <label htmlFor="new-password" className="text-sm font-medium">
                New password
              </label>
              <Input
                id="new-password"
                type="password"
                autoComplete="new-password"
                placeholder="At least 6 characters"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <label htmlFor="confirm" className="text-sm font-medium">
                Confirm new password
              </label>
              <Input
                id="confirm"
                type="password"
                autoComplete="new-password"
                placeholder="Re-enter your password"
                value={confirm}
                onChange={(e) => setConfirm(e.target.value)}
                required
              />
            </div>
            {error && (
              <p className="text-sm font-medium text-destructive">{error}</p>
            )}
            <Button
              type="submit"
              className="w-full bg-[#8cc63f] text-white hover:bg-[#7ab32f]"
            >
              Update password
            </Button>
          </form>
        )}

        <p className="mt-4 text-center text-xs text-muted-foreground">
          <Link
            href={`/login/credentials?role=${role}`}
            className="underline-offset-4 hover:underline"
          >
            &larr; Back to sign in
          </Link>
        </p>
      </CardContent>
    </Card>
  );
}

export default function ForgotPasswordPage() {
  return (
    <Suspense fallback={null}>
      <ForgotPasswordFlow />
    </Suspense>
  );
}
