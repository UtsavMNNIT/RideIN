import type { ReactNode } from "react";

import { AppHeader } from "@/ui/components/common/AppHeader";
import { AppSidebar } from "@/ui/components/common/AppSidebar";

/**
 * Driver shell. Phase F-5 adds a location-heartbeat side-effect hook
 * (sends GPS to location-service every N seconds while available) and a
 * WS subscription that surfaces incoming ride offers as a dialog.
 */
export default function DriverLayout({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col">
      <AppHeader role="DRIVER" />
      <div className="flex flex-1">
        <AppSidebar role="DRIVER" />
        <main className="flex-1 overflow-y-auto p-4 md:p-6">{children}</main>
      </div>
    </div>
  );
}
