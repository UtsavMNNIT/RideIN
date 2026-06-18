import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

/**
 * Conditionally compose Tailwind class names and resolve conflicts.
 * Used by Shadcn primitives and everywhere a className-merge is needed.
 */
export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs));
}
