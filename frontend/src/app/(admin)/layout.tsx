import type { ReactNode } from "react";

import { AppHeader } from "@/ui/components/common/AppHeader";
import { AppSidebar } from "@/ui/components/common/AppSidebar";

/**
 * Admin shell. No WS provider — admin tools are read-mostly and pull on
 * demand. Charts/tariff editors arrive in a later phase.
 */
export default function AdminLayout({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col">
      <AppHeader role="ADMIN" />
      <div className="flex flex-1">
        <AppSidebar role="ADMIN" />
        <main className="flex-1 overflow-y-auto p-4 md:p-6">{children}</main>
      </div>
    </div>
  );
}
