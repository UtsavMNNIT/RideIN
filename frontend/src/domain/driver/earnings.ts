/**
 * Driver earnings types. There is NO backend endpoint for these yet (no
 * trip-history or payout API in driver-service / trip-service), so the
 * application hook serves clearly-labeled placeholder data shaped to match a
 * future `GET /v1/drivers/{id}/earnings` (+ trips) response. When that endpoint
 * lands, only the hook's queryFn changes — these types and the page stay.
 */

export type EarningTrip = {
  id:              string;
  completedAt:     string; // ISO
  vehicleType:     string;
  distanceMeters:  number;
  fareTotal:       number;
  currency:        string;
};

export type EarningsSummary = {
  currency:        string;
  totalEarnings:   number;
  ridesCompleted:  number;
  onlineHours:     number;
  avgFare:         number;
  trips:           EarningTrip[];
};
