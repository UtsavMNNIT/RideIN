import Link from "next/link";
import { CarFront } from "lucide-react";
import type { CSSProperties, ReactNode } from "react";

import { env } from "@/config/env";

/**
 * Auth chrome — intentionally chromeless. No app sidebar, no role badge,
 * no WS provider. Just a centered card slot. Real auth UI ships in F-2.
 *
 * The auth screens are always light: we pin the relevant theme tokens here so
 * that even when the OS (and next-themes) is in dark mode, the cards render as
 * a soft grey block with white inputs instead of a pitch-black slab.
 */
const lightTokens: CSSProperties = {
  // grey "block" for cards, white inputs, near-black text — independent of theme
  ["--card" as string]: "214 14% 80%",
  ["--card-foreground" as string]: "222 47% 11%",
  ["--background" as string]: "0 0% 100%",
  ["--foreground" as string]: "222 47% 11%",
  ["--muted-foreground" as string]: "215 16% 42%",
  ["--border" as string]: "214 20% 82%",
  ["--input" as string]: "214 20% 80%",
};

export default function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <div
      style={lightTokens}
      className="grid min-h-screen place-items-center bg-gradient-to-b from-white from-60% to-[#d4eeb0] px-4 text-foreground"
    >
      <div className="w-full max-w-md">
        <Link href="/" className="mb-8 flex items-center justify-center gap-2 text-lg font-semibold">
          <CarFront className="h-5 w-5" />
          {env.NEXT_PUBLIC_APP_NAME}
        </Link>
        {children}
      </div>
    </div>
  );
}
