"use client";

import { ThemeProvider as NextThemesProvider } from "next-themes";
import type { ComponentProps, ReactNode } from "react";

type Props = ComponentProps<typeof NextThemesProvider> & { children: ReactNode };

/**
 * Thin wrapper over next-themes. `class` strategy plays nicely with Shadcn's
 * `dark:` Tailwind variants. `disableTransitionOnChange` prevents the brief
 * flash of unstyled colours when the user flips the theme.
 */
export function ThemeProvider({ children, ...props }: Props) {
  return (
    <NextThemesProvider
      attribute="class"
      defaultTheme="system"
      enableSystem
      disableTransitionOnChange
      {...props}
    >
      {children}
    </NextThemesProvider>
  );
}
