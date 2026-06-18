import Link from "next/link";
import type { LucideIcon } from "lucide-react";
import {
  Home, MapPin, Clock, Wallet, Gauge, SlidersHorizontal, Flame,
} from "lucide-react";

import { cn } from "@/lib/utils/cn";
import type { Role } from "@/ui/components/common/RoleBadge";

type NavItem = { href: string; label: string; icon: LucideIcon };

const NAV_BY_ROLE: Record<Role, ReadonlyArray<NavItem>> = {
  RIDER: [
    { href: "/home",    label: "Home",    icon: Home },
    { href: "/request", label: "Request", icon: MapPin },
    { href: "/history", label: "History", icon: Clock },
  ],
  DRIVER: [
    { href: "/dashboard", label: "Dashboard", icon: Gauge },
    { href: "/earnings",  label: "Earnings",  icon: Wallet },
  ],
  ADMIN: [
    { href: "/overview", label: "Overview", icon: Gauge },
    { href: "/tariffs",  label: "Tariffs",  icon: SlidersHorizontal },
    { href: "/surge",    label: "Surge",    icon: Flame },
  ],
};

/**
 * Static role-aware nav. Server Component — active-link highlighting is
 * deferred to the moment we add `usePathname` (Client) in a future phase.
 * Keeping it Server now means zero client JS for the sidebar.
 */
export function AppSidebar({ role, className }: { role: Role; className?: string }) {
  const items = NAV_BY_ROLE[role];
  return (
    <aside
      className={cn(
        "hidden h-[calc(100vh-3.5rem)] w-56 shrink-0 border-r bg-card md:flex md:flex-col",
        className,
      )}
    >
      <nav className="flex flex-col gap-1 p-3">
        {items.map((item) => {
          const Icon = item.icon;
          return (
            <Link
              key={item.href}
              href={item.href}
              className="flex items-center gap-2 rounded-md px-3 py-2 text-sm text-muted-foreground transition-colors hover:bg-accent hover:text-accent-foreground"
            >
              <Icon className="h-4 w-4" />
              {item.label}
            </Link>
          );
        })}
      </nav>
    </aside>
  );
}
