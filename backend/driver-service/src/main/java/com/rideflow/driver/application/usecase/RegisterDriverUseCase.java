package com.rideflow.driver.application.usecase;

import com.rideflow.driver.application.port.out.DriverRepository;
import com.rideflow.driver.application.port.out.PasswordHasher;
import com.rideflow.driver.domain.exception.DriverAlreadyExistsException;
import com.rideflow.driver.domain.model.Driver;
import com.rideflow.driver.domain.model.VehicleType;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registers a new driver: enforces email/phone uniqueness, hashes the password,
 * and persists the OFFLINE-by-default aggregate.
 *
 * <p>The pre-check on email/phone gives a clean 409 for the common case; the
 * unique indexes in {@code V1__init.sql} remain the authoritative guard against
 * the race between concurrent registrations (translated to 409 by the adapter).
 */
@Service
public class RegisterDriverUseCase {

    private final DriverRepository repository;
    private final PasswordHasher   passwordHasher;

    public RegisterDriverUseCase(DriverRepository repository, PasswordHasher passwordHasher) {
        this.repository     = repository;
        this.passwordHasher = passwordHasher;
    }

    @Transactional
    public Driver register(RegisterCommand cmd) {
        String email = cmd.email().trim().toLowerCase();
        String phone = cmd.phone().trim();

        if (repository.existsByEmail(email)) {
            throw new DriverAlreadyExistsException("email", email);
        }
        if (repository.existsByPhone(phone)) {
            throw new DriverAlreadyExistsException("phone", phone);
        }

        Driver driver = Driver.register(
                email,
                phone,
                cmd.fullName(),
                passwordHasher.hash(cmd.rawPassword()),
                cmd.vehicleType(),
                cmd.vehiclePlate());

        return repository.save(driver);
    }

    /**
     * Use-case input, decoupled from the transport DTO. Carries the raw password
     * only for the duration of the call — it is hashed before persistence and
     * never logged.
     */
    public record RegisterCommand(
            String      email,
            String      phone,
            String      fullName,
            String      rawPassword,
            VehicleType vehicleType,
            String      vehiclePlate) {}
}
