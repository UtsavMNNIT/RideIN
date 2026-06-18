"use client";

import { useRouter } from "next/navigation";

import { signIn } from "@/lib/auth/session";
import { Button } from "@/ui/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/ui/components/ui/card";

/**
 * Demo-grade sign-in: pick a role, get a per-tab UUID, redirect to the
 * role's landing page. Phase F-2 replaces this with the real
 * username/password form proxied through /api/auth/login.
 */
export default function LoginPage() {
  const router = useRouter();

  const handle = (role: "RIDER" | "DRIVER") => {
    signIn(role);
    router.push(role === "RIDER" ? "/home" : "/dashboard");
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Sign in</CardTitle>
        <CardDescription>
          Demo mode — pick a role to enter the app. A per-tab user id is generated
          for you and stored as a cookie so the WebSocket can route notifications
          back to this session.
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        <Button onClick={() => handle("RIDER")} className="w-full">
          Continue as Rider
        </Button>
        <Button onClick={() => handle("DRIVER")} variant="outline" className="w-full">
          Continue as Driver
        </Button>
      </CardContent>
    </Card>
  );
}
