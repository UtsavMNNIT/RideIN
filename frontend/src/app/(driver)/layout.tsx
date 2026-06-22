import type { ReactNode } from "react";

import { AppHeader } from "@/ui/components/common/AppHeader";
import { AppSidebar } from "@/ui/components/common/AppSidebar";

import { DriverShell } from "./DriverShell";

/**
 * Driver shell. {@link DriverShell} (the Client WebSocket provider) wraps the
 * whole chrome so the header's connection indicator can read the live status,
 * and the dashboard can stream ride offers. The header/sidebar stay Server
 * Components, passed as children. The location-heartbeat runs in the page.
 */
export default function DriverLayout({ children }: { children: ReactNode }) {
  return (
    <DriverShell>
      <div className="flex min-h-screen flex-col">
        <AppHeader role="DRIVER" />
        <div className="flex flex-1">
          <AppSidebar role="DRIVER" />
          <main className="flex-1 overflow-y-auto p-4 md:p-6">{children}</main>
        </div>
      </div>
    </DriverShell>
  );
}
