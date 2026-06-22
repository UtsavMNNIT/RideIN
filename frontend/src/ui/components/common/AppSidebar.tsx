"use client";

import Link from "next/link";
import type { Route } from "next";
import { usePathname } from "next/navigation";
import type { LucideIcon } from "lucide-react";
import {
  Home, MapPin, Clock, Wallet, Gauge, SlidersHorizontal, Flame, CreditCard,
} from "lucide-react";

import { cn } from "@/lib/utils/cn";
import type { Role } from "@/ui/components/common/RoleBadge";

type NavItem = { href: string; label: string; icon: LucideIcon };

const NAV_BY_ROLE: Record<Role, ReadonlyArray<NavItem>> = {
  RIDER: [
    { href: "/home",    label: "Home",    icon: Home },
    { href: "/request", label: "Request", icon: MapPin },
    { href: "/history", label: "History", icon: Clock },
    { href: "/payment", label: "Payment", icon: CreditCard },
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

/** Role-aware nav with active-route highlighting (Client — reads usePathname). */
export function AppSidebar({ role, className }: { role: Role; className?: string }) {
  const items = NAV_BY_ROLE[role];
  const pathname = usePathname();
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
          const active =
            pathname === item.href || pathname.startsWith(`${item.href}/`);
          return (
            <Link
              key={item.href}
              // Some nav targets (earnings, tariffs, surge) aren't built yet, so
              // href is a plain string cast to the typed-routes Route type.
              href={item.href as Route}
              aria-current={active ? "page" : undefined}
              className={cn(
                "flex items-center gap-2 rounded-md px-3 py-2 text-sm transition-colors",
                active
                  ? "bg-accent font-medium text-accent-foreground"
                  : "text-muted-foreground hover:bg-accent hover:text-accent-foreground",
              )}
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
