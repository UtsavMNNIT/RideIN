"use client";

import { useRouter } from "next/navigation";
import { LogOut } from "lucide-react";

import { signOut } from "@/lib/auth/session";
import { Button } from "@/ui/components/ui/button";

/**
 * Minimal sign-out control rendered in the header. Clears the demo session
 * (cookies + localStorage token/profile) and returns to the login screen.
 */
export function UserMenu() {
  const router = useRouter();

  const onSignOut = () => {
    signOut();
    router.push("/login");
  };

  return (
    <Button
      variant="ghost"
      size="sm"
      onClick={onSignOut}
      aria-label="Sign out"
      className="text-muted-foreground"
    >
      <LogOut className="h-4 w-4" />
      <span className="hidden sm:inline">Sign out</span>
    </Button>
  );
}
