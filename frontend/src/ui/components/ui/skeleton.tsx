import { cn } from "@/lib/utils/cn";

/**
 * Loading placeholder. Use to reserve layout while React Query is fetching —
 * prevents layout shift and gives the UI a deliberate, calm feel.
 */
export function Skeleton({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn("animate-pulse rounded-md bg-muted", className)}
      {...props}
    />
  );
}
