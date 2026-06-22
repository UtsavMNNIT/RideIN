import type { ReactNode } from "react";

import { AppHeader } from "@/ui/components/common/AppHeader";
import { AppSidebar } from "@/ui/components/common/AppSidebar";

import { RiderShell } from "./RiderShell";

/**
 * Rider shell. {@link RiderShell} (the Client WebSocket provider) wraps the
 * whole chrome so the header's connection indicator can read the live status;
 * the header/sidebar themselves stay Server Components, passed as children.
 */
export default function RiderLayout({ children }: { children: ReactNode }) {
  return (
    <RiderShell>
      <div className="flex min-h-screen flex-col">
        <AppHeader role="RIDER" />
        <div className="flex flex-1">
          <AppSidebar role="RIDER" />
          <main className="flex-1 overflow-y-auto p-4 md:p-6">{children}</main>
        </div>
      </div>
    </RiderShell>
  );
}
