"use client";

import { Car, Crown, Users } from "lucide-react";

import type { VehicleType } from "@/domain/ride/types";
import { cn } from "@/lib/utils/cn";

const OPTIONS: { value: VehicleType; label: string; hint: string; Icon: typeof Car }[] = [
  { value: "STANDARD", label: "Standard", hint: "Everyday", Icon: Car },
  { value: "XL",       label: "XL",       hint: "Up to 6",  Icon: Users },
  { value: "PREMIUM",  label: "Premium",  hint: "Top-rated", Icon: Crown },
];

export function VehiclePicker({
  value,
  onChange,
  disabled,
}: {
  value: VehicleType;
  onChange: (v: VehicleType) => void;
  disabled?: boolean;
}) {
  return (
    <div className="grid grid-cols-3 gap-2">
      {OPTIONS.map((o) => {
        const active = o.value === value;
        return (
          <button
            key={o.value}
            type="button"
            disabled={disabled}
            onClick={() => onChange(o.value)}
            aria-pressed={active}
            className={cn(
              "flex flex-col items-center gap-1 rounded-md border p-3 text-center transition-colors",
              active
                ? "border-primary bg-primary/5 text-foreground"
                : "border-input hover:bg-accent hover:text-accent-foreground",
              disabled && "pointer-events-none opacity-50",
            )}
          >
            <o.Icon className="h-5 w-5" />
            <span className="text-sm font-medium">{o.label}</span>
            <span className="text-xs text-muted-foreground">{o.hint}</span>
          </button>
        );
      })}
    </div>
  );
}
