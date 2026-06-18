import { z } from "zod";

/**
 * Typed env access. Reads only NEXT_PUBLIC_* values (anything else is unavailable
 * client-side anyway). Validating at import time means a missing var fails fast
 * with a clear error in the dev console instead of an opaque "undefined" deep
 * inside a fetch call.
 *
 * Do not re-export `process.env` directly anywhere else in the app — go through
 * this module so a future env rename is a one-line refactor.
 */
const schema = z.object({
  NEXT_PUBLIC_API_BASE_URL: z.string().url(),
  NEXT_PUBLIC_WS_BASE_URL:  z.string().url(),
  NEXT_PUBLIC_MAP_PROVIDER: z.enum(["osm", "mapbox"]).default("osm"),
  NEXT_PUBLIC_MAPBOX_TOKEN: z.string().optional(),
  NEXT_PUBLIC_APP_NAME:     z.string().default("RideFlow"),
  NEXT_PUBLIC_APP_VERSION:  z.string().default("0.0.0"),
});

const parsed = schema.safeParse({
  NEXT_PUBLIC_API_BASE_URL: process.env.NEXT_PUBLIC_API_BASE_URL,
  NEXT_PUBLIC_WS_BASE_URL:  process.env.NEXT_PUBLIC_WS_BASE_URL,
  NEXT_PUBLIC_MAP_PROVIDER: process.env.NEXT_PUBLIC_MAP_PROVIDER,
  NEXT_PUBLIC_MAPBOX_TOKEN: process.env.NEXT_PUBLIC_MAPBOX_TOKEN,
  NEXT_PUBLIC_APP_NAME:     process.env.NEXT_PUBLIC_APP_NAME,
  NEXT_PUBLIC_APP_VERSION:  process.env.NEXT_PUBLIC_APP_VERSION,
});

if (!parsed.success) {
  // Surface every missing/invalid var in one go — easier to fix than one at a time.
  // eslint-disable-next-line no-console
  console.error("Invalid environment configuration", parsed.error.flatten().fieldErrors);
  throw new Error("Invalid environment — see console for missing keys");
}

export const env = parsed.data;
export type Env = typeof env;
