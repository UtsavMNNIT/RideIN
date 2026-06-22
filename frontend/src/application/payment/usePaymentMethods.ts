"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import type { AddPaymentMethodInput, PaymentMethod } from "@/domain/payment/types";
import { api } from "@/lib/api/client";

import { addPaymentMethodPath, paymentMethodsPath } from "./endpoints";

export const paymentMethodsQueryKey = (userId: string | undefined) =>
  ["payment-methods", userId] as const;

/** List the rider's saved (mock) cards. */
export function usePaymentMethods(userId: string | undefined) {
  return useQuery({
    queryKey: paymentMethodsQueryKey(userId),
    queryFn: () => api.get<PaymentMethod[]>(paymentMethodsPath(userId!)),
    enabled: Boolean(userId),
    staleTime: 30_000,
  });
}

/** Add a mock card, then refresh the list. */
export function useAddPaymentMethod(userId: string | undefined) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: AddPaymentMethodInput) =>
      api.post<PaymentMethod>(addPaymentMethodPath(), input),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: paymentMethodsQueryKey(userId) });
    },
  });
}
