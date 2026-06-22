/**
 * Payment types — mirror the payment-service API DTOs
 * (backend/payment-service/.../api/dto). Settlement is event-driven off
 * `ride.completed`; the frontend only reads payments and manages mock cards.
 */

export type PaymentStatus =
  | "PENDING"
  | "AUTHORIZED"
  | "CAPTURED"
  | "SETTLED"
  | "FAILED"
  | "CANCELLED";

/** A settlement / receipt for one ride (GET /v1/payments/rides/{rideId}). */
export type Payment = {
  paymentId:        string;
  rideId:           string;
  riderId:          string;
  driverId:         string | null;
  amount:           number;
  currency:         string;
  status:           PaymentStatus;
  paymentMethodId:  string | null;
  failureReason:    string | null;
  createdAt:        string;        // ISO
  settledAt:        string | null; // ISO, once SETTLED
};

/** A mock saved card (token never leaves the server). */
export type PaymentMethod = {
  id:        string;
  userId:    string;
  brand:     string;
  last4:     string;
  isDefault: boolean;
  createdAt: string; // ISO
};

/** Body for POST /v1/payments/methods. */
export type AddPaymentMethodInput = {
  userId:    string;
  brand:     string;
  last4:     string;
  isDefault: boolean;
};

const TERMINAL: ReadonlySet<PaymentStatus> = new Set(["SETTLED", "FAILED", "CANCELLED"]);

export function isTerminalPaymentStatus(status: PaymentStatus): boolean {
  return TERMINAL.has(status);
}
