import Link from "next/link";
import { CarFront } from "lucide-react";

import { env } from "@/config/env";
import { cn } from "@/lib/utils/cn";
import { RoleBadge, type Role } from "@/ui/components/common/RoleBadge";

/**
 * Top chrome rendered inside every authenticated layout. Header is a Server
 * Component — no client state lives here. Interactive bits (user menu,
 * connection indicator) are mounted in later phases as Client subtrees.
 */
export function AppHeader({ role, className }: { role: Role; className?: string }) {
  return (
    <header
      className={cn(
        "sticky top-0 z-30 flex h-14 w-full items-center justify-between border-b bg-background/95 px-4 backdrop-blur supports-[backdrop-filter]:bg-background/60",
        className,
      )}
    >
      <Link href="/" className="flex items-center gap-2 font-semibold">
        <CarFront className="h-5 w-5" />
        <span>{env.NEXT_PUBLIC_APP_NAME}</span>
      </Link>

      <div className="flex items-center gap-3">
        <RoleBadge role={role} />
        {/* user menu, theme toggle, connection indicator — Phase F-2 / F-5 */}
      </div>
    </header>
  );
}
