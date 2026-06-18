import { cookies } from "next/headers";
import { redirect } from "next/navigation";

/**
 * Landing page = a redirect.
 *
 * The cookie-driven branch is the smallest sufficient logic at this phase:
 * authenticated users land on their role's home page; everyone else lands
 * on /login. Real session resolution arrives in Phase F-2 (Authentication).
 */
export default async function RootPage() {
  const role = (await cookies()).get("rf_role")?.value;

  switch (role) {
    case "RIDER":
      redirect("/home");
    case "DRIVER":
      redirect("/dashboard");
    case "ADMIN":
      redirect("/overview");
    default:
      redirect("/login");
  }
}
