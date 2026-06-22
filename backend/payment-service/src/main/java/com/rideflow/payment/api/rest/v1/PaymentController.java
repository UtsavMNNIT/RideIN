package com.rideflow.payment.api.rest.v1;

import com.rideflow.payment.api.dto.request.AddPaymentMethodRequest;
import com.rideflow.payment.api.dto.response.PaymentMethodResponse;
import com.rideflow.payment.api.dto.response.PaymentResponse;
import com.rideflow.payment.application.usecase.GetPaymentUseCase;
import com.rideflow.payment.application.usecase.PaymentMethodService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Payment API.
 *
 * <pre>
 *   GET  /v1/payments/methods?userId=...   list mock saved cards
 *   POST /v1/payments/methods              add a mock saved card
 *   GET  /v1/payments/rides/{rideId}       fetch the settlement/receipt for a ride
 * </pre>
 *
 * <p>Settlement itself is event-driven (off {@code ride.completed}); it is not
 * created through this controller. In production the gateway authorizes the
 * caller's JWT; this service does not run its own servlet security filter.
 */
@RestController
@RequestMapping("/v1/payments")
public class PaymentController {

    private final PaymentMethodService methods;
    private final GetPaymentUseCase    getPayment;

    public PaymentController(PaymentMethodService methods, GetPaymentUseCase getPayment) {
        this.methods    = methods;
        this.getPayment = getPayment;
    }

    @GetMapping("/methods")
    public List<PaymentMethodResponse> listMethods(@RequestParam UUID userId) {
        return methods.list(userId).stream().map(PaymentMethodResponse::from).toList();
    }

    @PostMapping("/methods")
    public ResponseEntity<PaymentMethodResponse> addMethod(@Valid @RequestBody AddPaymentMethodRequest req) {
        var method = methods.add(req.userId(), req.brand(), req.last4(), req.isDefault());
        return ResponseEntity
                .created(URI.create("/v1/payments/methods/" + method.id()))
                .body(PaymentMethodResponse.from(method));
    }

    @GetMapping("/rides/{rideId}")
    public PaymentResponse byRide(@PathVariable UUID rideId) {
        return PaymentResponse.from(getPayment.byRideId(rideId));
    }
}
