/**
 * Admin / operator types. There is NO backend for these yet — no metrics
 * endpoint exists, and the backend has no ADMIN role. The application hook
 * serves clearly-labeled placeholder data shaped to match a future
 * `GET /v1/admin/metrics`. When that lands, only the hook's queryFn changes.
 */

export type OperatorMetrics = {
  activeRides:      number;
  driversOnline:    number;
  totalRiders:      number;
  completionRate:   number; // 0..1
  avgDispatchSecs:  number;
};
