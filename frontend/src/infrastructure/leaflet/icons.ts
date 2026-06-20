import L from "leaflet";
import iconRetinaUrl from "leaflet/dist/images/marker-icon-2x.png";
import iconUrl from "leaflet/dist/images/marker-icon.png";
import shadowUrl from "leaflet/dist/images/marker-shadow.png";

/**
 * Leaflet's default marker points at relative image paths that break under a
 * bundler. Re-point them at the assets bundled from leaflet/dist/images (Next
 * types these imports as StaticImageData, hence `.src`). Call once, client-side.
 */
let applied = false;

export function applyLeafletIconFix(): void {
  if (applied) return;
  applied = true;
  L.Icon.Default.mergeOptions({
    iconRetinaUrl: iconRetinaUrl.src,
    iconUrl:       iconUrl.src,
    shadowUrl:     shadowUrl.src,
  });
}
