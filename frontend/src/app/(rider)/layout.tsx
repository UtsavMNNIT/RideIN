import type { ReactNode } from "react";

import { AppHeader } from "@/ui/components/common/AppHeader";
import { AppSidebar } from "@/ui/components/common/AppSidebar";

import { RiderShell } from "./RiderShell";

/**
 * Rider shell. The layout itself is a Server Component (zero JS for the
 * header/sidebar chrome); {@link RiderShell} is the small Client subtree
 * that mounts the WebSocket provider.
 */
export default function RiderLayout({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col">
      <AppHeader role="RIDER" />
      <div className="flex flex-1">
        <AppSidebar role="RIDER" />
        <main className="flex-1 overflow-y-auto p-4 md:p-6">
          <RiderShell>{children}</RiderShell>
        </main>
      </div>
    </div>
  );
}
