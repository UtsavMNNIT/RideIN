package com.rideflow.pricing.api.rest.v1;

import com.rideflow.pricing.api.dto.response.RateCardResponse;
import com.rideflow.pricing.application.port.out.RateCardProvider;
import com.rideflow.pricing.domain.model.VehicleType;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only rate-card API — lets clients show "rates from ₹X" and lets ops
 * verify the live cards.
 *
 * <pre>
 *   GET /v1/rate-cards                  all configured rate cards
 *   GET /v1/rate-cards/{vehicleType}    one card
 * </pre>
 *
 * <p>Writes (retuning cards) are intentionally not exposed here yet — they're an
 * ops-controlled migration today. An admin write path is a natural future
 * addition behind gateway authz.
 */
@RestController
@RequestMapping("/v1/rate-cards")
public class RateCardController {

    private final RateCardProvider rateCards;

    public RateCardController(RateCardProvider rateCards) {
        this.rateCards = rateCards;
    }

    @GetMapping
    public List<RateCardResponse> all() {
        return rateCards.all().stream().map(RateCardResponse::from).toList();
    }

    @GetMapping("/{vehicleType}")
    public RateCardResponse one(@PathVariable VehicleType vehicleType) {
        return RateCardResponse.from(rateCards.cardFor(vehicleType));
    }
}
