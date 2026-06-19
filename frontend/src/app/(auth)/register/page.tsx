"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useState } from "react";
import { toast } from "sonner";

import { registerRider } from "@/application/rider/auth";
import { ApiError } from "@/lib/api/client";
import { createUser } from "@/lib/auth/demoUsers";
import { signIn } from "@/lib/auth/session";
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

function RegisterForm() {
  const router = useRouter();
  const params = useSearchParams();
  const role = parseRole(params.get("role"));
  const roleLabel = role === "DRIVER" ? "Driver" : "Rider";

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [checking, setChecking] = useState(false);

  const landingFor = (r: Role) => (r === "DRIVER" ? "/dashboard" : "/home");

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!isValidEmailFormat(email)) {
      setError("Please enter a valid email address.");
      return;
    }
    // Riders register against the real backend, which enforces an 8-char minimum
    // (BCrypt). Keep the demo path's looser rule for any non-wired role.
    const minLength = role === "RIDER" ? 8 : 6;
    if (password.length < minLength) {
      setError(`Password must be at least ${minLength} characters.`);
      return;
    }
    if (password !== confirm) {
      setError("Passwords do not match.");
      return;
    }

    // Verify the email is actually deliverable before creating the account.
    setChecking(true);
    const check = await verifyEmail(email);
    setChecking(false);

    if (check.deliverability === "UNDELIVERABLE") {
      setError(
        "That email address doesn't appear to exist or can't receive mail. Please double-check it.",
      );
      return;
    }
    if (check.isDisposable) {
      setError("Disposable email addresses aren't allowed. Use a permanent address.");
      return;
    }
    if (!check.configured) {
      // Provider key not set — let signup proceed but make the gap visible.
      toast.warning("Email verification isn't configured — skipping deliverability check.");
    }

    // Riders are created in the real backend, then sign in (with 2FA) to obtain a
    // JWT — registration itself returns no token, so we route to the login step.
    if (role === "RIDER") {
      setChecking(true);
      try {
        await registerRider({ email, phone, fullName: name, password });
        toast.success("Account created — please sign in to continue.");
        router.push(`/login/credentials?role=RIDER`);
      } catch (err) {
        const status = err instanceof ApiError ? err.status : 0;
        if (status === 409) {
          setError("An account with that email already exists. Try signing in.");
        } else if (status === 400) {
          setError("Please check your details and try again.");
        } else {
          setError("Couldn't create your account right now. Please try again.");
        }
      } finally {
        setChecking(false);
      }
      return;
    }

    const user = createUser({ name, email, phone, password, role });
    if (!user) {
      setError("An account with that email already exists. Try signing in.");
      return;
    }

    signIn(role);
    toast.success(`Account created — welcome, ${user.name}!`);
    router.push(landingFor(role));
  };

  return (
    <Card className="border-[#8cc63f]/30 shadow-lg">
      <CardHeader>
        <CardTitle>Sign up as {roleLabel}</CardTitle>
        <CardDescription>
          Already have an account?{" "}
          <Link
            href={`/login/credentials?role=${role}`}
            className="font-medium text-[#5b8a1e] underline-offset-4 hover:underline"
          >
            Sign in
          </Link>
          .
        </CardDescription>
      </CardHeader>

      <CardContent>
        <form onSubmit={onSubmit} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <label htmlFor="name" className="text-sm font-medium">
              Full name
            </label>
            <Input
              id="name"
              autoComplete="name"
              placeholder="Jane Doe"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
            />
          </div>

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
            <label htmlFor="phone" className="text-sm font-medium">
              Phone number
            </label>
            <Input
              id="phone"
              type="tel"
              autoComplete="tel"
              placeholder="+1 555 010 1234"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              required
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <label htmlFor="password" className="text-sm font-medium">
              Password
            </label>
            <Input
              id="password"
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
              Confirm password
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
            disabled={checking}
            className="w-full bg-[#8cc63f] text-white hover:bg-[#7ab32f]"
          >
            {checking ? "Verifying email…" : "Create account"}
          </Button>
        </form>

        <p className="mt-4 text-center text-xs text-muted-foreground">
          <Link href="/login" className="underline-offset-4 hover:underline">
            &larr; Back to role selection
          </Link>
        </p>
      </CardContent>
    </Card>
  );
}

export default function RegisterPage() {
  return (
    <Suspense fallback={null}>
      <RegisterForm />
    </Suspense>
  );
}
