import Link from "next/link";
import { CarFront } from "lucide-react";
import type { ReactNode } from "react";

import { env } from "@/config/env";

/**
 * Auth chrome — intentionally chromeless. No app sidebar, no role badge,
 * no WS provider. Just a centered card slot. Real auth UI ships in F-2.
 */
export default function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <div className="grid min-h-screen place-items-center bg-muted/30 px-4">
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
