package com.rideflow.rider.application.usecase;

import com.rideflow.rider.application.port.out.PasswordHasher;
import com.rideflow.rider.application.port.out.RiderRepository;
import com.rideflow.rider.domain.exception.RiderAlreadyExistsException;
import com.rideflow.rider.domain.model.Rider;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registers a new rider: enforces email/phone uniqueness, hashes the password,
 * and persists the aggregate. The unique indexes remain the authoritative guard
 * against the concurrent-registration race (translated to 409 by the adapter).
 */
@Service
public class RegisterRiderUseCase {

    private final RiderRepository repository;
    private final PasswordHasher  passwordHasher;

    public RegisterRiderUseCase(RiderRepository repository, PasswordHasher passwordHasher) {
        this.repository     = repository;
        this.passwordHasher = passwordHasher;
    }

    @Transactional
    public Rider register(RegisterCommand cmd) {
        String email = cmd.email().trim().toLowerCase();
        String phone = cmd.phone().trim();

        if (repository.existsByEmail(email)) {
            throw new RiderAlreadyExistsException("email", email);
        }
        if (repository.existsByPhone(phone)) {
            throw new RiderAlreadyExistsException("phone", phone);
        }

        Rider rider = Rider.register(email, phone, cmd.fullName(), passwordHasher.hash(cmd.rawPassword()));
        return repository.save(rider);
    }

    public record RegisterCommand(String email, String phone, String fullName, String rawPassword) {}
}
