/**
 * Demo-grade user store kept in localStorage. This stands in for a real
 * backend user table until Phase F-2 wires the credentials form to the
 * gateway's /auth endpoints. Passwords are stored in plaintext ON PURPOSE —
 * this is a throwaway demo store, never a production pattern.
 */

import type { Role } from "@/ui/components/common/RoleBadge";

export type DemoUser = {
  userId: string;
  name: string;
  email: string;
  phone: string;
  password: string;
  role: Role;
};

const STORAGE_KEY = "rf_demo_users";

// A couple of ready-made accounts so login works out of the box without
// having to register first.
const SEED: DemoUser[] = [
  {
    userId: "seed-rider",
    name: "Demo Rider",
    email: "rider@cabpro.test",
    phone: "+10000000001",
    password: "password123",
    role: "RIDER",
  },
  {
    userId: "seed-driver",
    name: "Demo Driver",
    email: "driver@cabpro.test",
    phone: "+10000000002",
    password: "password123",
    role: "DRIVER",
  },
];

function read(): DemoUser[] {
  if (typeof window === "undefined") return [];
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(SEED));
      return [...SEED];
    }
    return JSON.parse(raw) as DemoUser[];
  } catch {
    return [...SEED];
  }
}

function write(users: DemoUser[]) {
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(users));
}

export function findUserByEmail(email: string): DemoUser | undefined {
  const needle = email.trim().toLowerCase();
  return read().find((u) => u.email.toLowerCase() === needle);
}

export function findUserByPhone(phone: string): DemoUser | undefined {
  const needle = phone.replace(/\s/g, "");
  return read().find((u) => u.phone.replace(/\s/g, "") === needle);
}

export type CredentialResult =
  | { status: "ok"; user: DemoUser }
  | { status: "no-user" }
  | { status: "bad-password" }
  | { status: "wrong-role"; user: DemoUser };

/** Check an email/password pair, optionally requiring a specific role. */
export function verifyCredentials(
  email: string,
  password: string,
  role?: Role,
): CredentialResult {
  const user = findUserByEmail(email);
  if (!user) return { status: "no-user" };
  if (user.password !== password) return { status: "bad-password" };
  if (role && user.role !== role) return { status: "wrong-role", user };
  return { status: "ok", user };
}

/** Create a new demo user. Returns null if the email is already taken. */
export function createUser(
  input: Omit<DemoUser, "userId">,
): DemoUser | null {
  const users = read();
  if (users.some((u) => u.email.toLowerCase() === input.email.trim().toLowerCase())) {
    return null;
  }
  const user: DemoUser = { ...input, userId: crypto.randomUUID() };
  users.push(user);
  write(users);
  return user;
}

/** Overwrite a user's password (used by the forgot-password flow). */
export function resetPassword(email: string, newPassword: string): boolean {
  const users = read();
  const needle = email.trim().toLowerCase();
  const existing = users.find((u) => u.email.toLowerCase() === needle);
  if (!existing) return false;
  existing.password = newPassword;
  write(users);
  return true;
}
