"use client";

import { useEffect, useMemo, useState } from "react";
import Image from "next/image";
import { Car, Crown, Users } from "lucide-react";

import type { VehicleType } from "@/domain/ride/types";
import { cn } from "@/lib/utils/cn";

/**
 * A concrete vehicle a rider can pick within a class. The backend only knows
 * the three classes (STANDARD / XL / PREMIUM), so the specific vehicle is a
 * UI-only refinement — picking any sub-option requests its parent class.
 */
type SubOption = {
  id: string;
  label: string;
  /** Image served from /public/images. */
  img: string;
  /** Heading the option is grouped under (e.g. "Two wheeler"). */
  group: string;
};

const CLASSES: {
  value: VehicleType;
  label: string;
  hint: string;
  Icon: typeof Car;
  subs: SubOption[];
}[] = [
  {
    value: "STANDARD",
    label: "Standard",
    hint: "Everyday",
    Icon: Car,
    subs: [
      { id: "bike",   label: "Bike",          img: "/images/bike.jpg",   group: "Two wheeler" },
      { id: "scooty", label: "Scooty",        img: "/images/scooty.jpg", group: "Two wheeler" },
      { id: "auto",   label: "Auto rickshaw", img: "/images/auto.jpg",   group: "Three wheeler" },
      { id: "swift",  label: "Swift",         img: "/images/swift.jpg",  group: "Four wheeler" },
    ],
  },
  {
    value: "XL",
    label: "XL",
    hint: "Up to 6",
    Icon: Users,
    subs: [
      { id: "suv", label: "SUV", img: "/images/xl-suv.jpg", group: "Six seater" },
    ],
  },
  {
    value: "PREMIUM",
    label: "Premium",
    hint: "Top-rated",
    Icon: Crown,
    subs: [
      { id: "thar", label: "Mahindra Thar", img: "/images/thar.jpg", group: "Premium SUV" },
    ],
  },
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
  const active = useMemo(
    () => CLASSES.find((c) => c.value === value) ?? CLASSES[0]!,
    [value],
  );

  // The specific vehicle within the active class. Reset to the first option
  // whenever the class changes so the highlight always points at something.
  const [subId, setSubId] = useState(active.subs[0]!.id);
  useEffect(() => {
    setSubId(active.subs[0]!.id);
  }, [active]);

  // Group the active class's sub-options by their `group` heading, preserving
  // declaration order.
  const groups = useMemo(() => {
    const ordered: { heading: string; options: SubOption[] }[] = [];
    for (const sub of active.subs) {
      let bucket = ordered.find((g) => g.heading === sub.group);
      if (!bucket) {
        bucket = { heading: sub.group, options: [] };
        ordered.push(bucket);
      }
      bucket.options.push(sub);
    }
    return ordered;
  }, [active]);

  return (
    <div className="space-y-3">
      <div className="grid grid-cols-3 gap-2">
        {CLASSES.map((c) => {
          const isActive = c.value === value;
          return (
            <button
              key={c.value}
              type="button"
              disabled={disabled}
              onClick={() => onChange(c.value)}
              aria-pressed={isActive}
              className={cn(
                "flex flex-col items-center gap-1 rounded-md border p-3 text-center transition-colors",
                isActive
                  ? "border-primary bg-primary/5 text-foreground"
                  : "border-input hover:bg-accent hover:text-accent-foreground",
                disabled && "pointer-events-none opacity-50",
              )}
            >
              <c.Icon className="h-5 w-5" />
              <span className="text-sm font-medium">{c.label}</span>
              <span className="text-xs text-muted-foreground">{c.hint}</span>
            </button>
          );
        })}
      </div>

      <div className={cn("space-y-3", disabled && "pointer-events-none opacity-50")}>
        {groups.map((g) => (
          <div key={g.heading} className="space-y-1.5">
            <p className="text-xs font-medium text-muted-foreground">{g.heading}</p>
            <div className="grid grid-cols-4 gap-2">
              {g.options.map((sub) => {
                const isSelected = sub.id === subId;
                return (
                  <button
                    key={sub.id}
                    type="button"
                    disabled={disabled}
                    onClick={() => {
                      setSubId(sub.id);
                      // Picking a vehicle keeps the class in sync with the
                      // group it lives in.
                      onChange(active.value);
                    }}
                    aria-pressed={isSelected}
                    title={sub.label}
                    className={cn(
                      "flex flex-col items-center gap-1 rounded-md border p-2 text-center transition-colors",
                      isSelected
                        ? "border-primary bg-primary/5"
                        : "border-input hover:bg-accent",
                    )}
                  >
                    <span className="relative h-12 w-full overflow-hidden rounded">
                      <Image
                        src={sub.img}
                        alt={sub.label}
                        fill
                        sizes="80px"
                        className="object-contain"
                      />
                    </span>
                    <span className="text-xs font-medium leading-tight">{sub.label}</span>
                  </button>
                );
              })}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
