package com.rideflow.rider.application.usecase;

import com.rideflow.rider.application.port.out.RideRepository;
import com.rideflow.rider.application.port.out.RiderRepository;
import com.rideflow.rider.domain.exception.RiderNotFoundException;
import com.rideflow.rider.domain.model.Ride;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Returns a rider's ride history, newest-first and paginated. Validates that
 * the rider exists so an unknown id yields 404 rather than a misleading empty
 * list.
 */
@Service
public class GetRideHistoryUseCase {

    private final RiderRepository riderRepository;
    private final RideRepository  rideRepository;

    public GetRideHistoryUseCase(RiderRepository riderRepository, RideRepository rideRepository) {
        this.riderRepository = riderRepository;
        this.rideRepository  = rideRepository;
    }

    @Transactional(readOnly = true)
    public List<Ride> history(UUID riderId, int page, int size) {
        if (!riderRepository.existsById(riderId)) {
            throw new RiderNotFoundException(riderId);
        }
        return rideRepository.findByRider(riderId, page, size);
    }
}
