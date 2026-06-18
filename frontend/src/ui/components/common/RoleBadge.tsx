import { cn } from "@/lib/utils/cn";

export type Role = "RIDER" | "DRIVER" | "ADMIN";

const styles: Record<Role, string> = {
  RIDER:  "bg-blue-100 text-blue-900   dark:bg-blue-950   dark:text-blue-100",
  DRIVER: "bg-green-100 text-green-900 dark:bg-green-950  dark:text-green-100",
  ADMIN:  "bg-purple-100 text-purple-900 dark:bg-purple-950 dark:text-purple-100",
};

/** Tiny visual cue used in headers/sidebars so the operator knows which app they're in. */
export function RoleBadge({ role, className }: { role: Role; className?: string }) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium",
        styles[role],
        className,
      )}
    >
      {role}
    </span>
  );
}
