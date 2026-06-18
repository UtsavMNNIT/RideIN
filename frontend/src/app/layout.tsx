import type { Metadata, Viewport } from "next";
import type { ReactNode } from "react";

import { AppProviders } from "@/ui/providers/AppProviders";
import { env } from "@/config/env";

import "@/ui/styles/globals.css";

export const metadata: Metadata = {
  title: {
    default:  env.NEXT_PUBLIC_APP_NAME,
    template: `%s — ${env.NEXT_PUBLIC_APP_NAME}`,
  },
  description: "Production-grade ride matching platform.",
  applicationName: env.NEXT_PUBLIC_APP_NAME,
  icons: { icon: "/favicon.ico" },
};

export const viewport: Viewport = {
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#ffffff" },
    { media: "(prefers-color-scheme: dark)",  color: "#0a0a0a" },
  ],
  width: "device-width",
  initialScale: 1,
};

/**
 * Root layout. Suppresses hydration warning on <html> because next-themes
 * adds the {@code class="dark"} attribute on first paint, which the server
 * markup cannot anticipate. Also suppresses on <body> to prevent warnings
 * from browser extensions that inject attributes.
 */
export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className="min-h-full bg-background text-foreground" suppressHydrationWarning>
        <AppProviders>{children}</AppProviders>
      </body>
    </html>
  );
}
