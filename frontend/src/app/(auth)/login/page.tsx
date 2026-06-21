"use client";

import Image from "next/image";
import { useRouter } from "next/navigation";
import { Car, ShieldCheck, UserRound } from "lucide-react";

import { signIn } from "@/lib/auth/session";
import { Button } from "@/ui/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/ui/components/ui/card";

/**
 * Demo-grade sign-in: pick a role, get a per-tab UUID, redirect to the
 * role's landing page. Phase F-2 replaces this with the real
 * username/password form proxied through /api/auth/login.
 *
 * Styling takes its cue from the Cabpro reference: a lime-green hero banner
 * (the dashboard screenshot) sitting above the role picker.
 */
export default function LoginPage() {
  const router = useRouter();

  const handle = (role: "RIDER" | "DRIVER") => {
    // Hand off to the credentials screen; the chosen role rides along as a
    // query param so that screen knows which app to drop the user into.
    router.push(`/login/credentials?role=${role}`);
  };

  // The operator console has no backend auth yet, so admin access is demo-grade:
  // set the ADMIN role cookie and go straight to the overview. Its data sources
  // (rate-cards, quotes) are public at the gateway, so no JWT is required.
  const enterAdmin = () => {
    signIn("ADMIN");
    router.push("/overview");
  };

  return (
    <Card className="overflow-hidden border-[#8cc63f]/30 shadow-lg">
      {/* Reference dashboard image as the hero banner */}
      <div className="relative h-44 w-full bg-[#8cc63f]">
        <Image
          src="/images/cabpro.jpg"
          alt="Cabpro — local & affordable cab service"
          fill
          priority
          sizes="(max-width: 768px) 100vw, 28rem"
          className="object-cover object-top"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-black/55 via-black/10 to-transparent" />
        <div className="absolute bottom-3 left-4 right-4">
          <p className="text-2xl font-extrabold tracking-tight text-white drop-shadow">Cabpro</p>
          <p className="text-sm font-medium text-white/90 drop-shadow">
            Local &amp; affordable cab service
          </p>
        </div>
      </div>

      <CardHeader>
        <CardTitle>Sign in</CardTitle>
        <CardDescription>
          Demo mode — pick how you want to enter the app. A per-tab user id is
          generated for you and stored as a cookie so the WebSocket can route
          notifications back to this session.
        </CardDescription>
      </CardHeader>

      <CardContent className="flex flex-col gap-3">
        <Button
          onClick={() => handle("RIDER")}
          className="w-full bg-[#8cc63f] text-white hover:bg-[#7ab32f]"
        >
          <UserRound className="h-4 w-4" />
          Login as Rider
        </Button>
        <Button
          onClick={() => handle("DRIVER")}
          variant="outline"
          className="w-full border-[#8cc63f] text-[#5b8a1e] hover:bg-[#8cc63f]/10 hover:text-[#5b8a1e]"
        >
          <Car className="h-4 w-4" />
          Login as Driver
        </Button>

        <button
          type="button"
          onClick={enterAdmin}
          className="mt-1 inline-flex items-center justify-center gap-1.5 text-xs text-muted-foreground underline-offset-4 hover:text-foreground hover:underline"
        >
          <ShieldCheck className="h-3.5 w-3.5" />
          Enter operator console (demo)
        </button>
      </CardContent>
    </Card>
  );
}
