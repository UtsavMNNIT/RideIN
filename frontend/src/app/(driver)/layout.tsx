import type { ReactNode } from "react";

import { AppHeader } from "@/ui/components/common/AppHeader";
import { AppSidebar } from "@/ui/components/common/AppSidebar";

import { DriverShell } from "./DriverShell";

/**
 * Driver shell. The chrome (header + sidebar) stays a Server Component; the
 * live-data subtree is wrapped in DriverShell, which mounts the WebSocket
 * provider so the dashboard can stream ride offers. The location-heartbeat
 * runs inside the dashboard page itself.
 */
export default function DriverLayout({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col">
      <AppHeader role="DRIVER" />
      <div className="flex flex-1">
        <AppSidebar role="DRIVER" />
        <main className="flex-1 overflow-y-auto p-4 md:p-6">
          <DriverShell>{children}</DriverShell>
        </main>
      </div>
    </div>
  );
}
