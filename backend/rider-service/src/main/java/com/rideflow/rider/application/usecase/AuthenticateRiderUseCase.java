package com.rideflow.rider.application.usecase;

import com.rideflow.rider.application.port.out.PasswordHasher;
import com.rideflow.rider.application.port.out.RiderRepository;
import com.rideflow.rider.application.port.out.TokenIssuer;
import com.rideflow.rider.domain.exception.InvalidCredentialsException;
import com.rideflow.rider.domain.model.Rider;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authenticates a rider by email + password and issues an access token. Both
 * the unknown-email and wrong-password paths throw the same
 * {@link InvalidCredentialsException} (anti-enumeration).
 */
@Service
public class AuthenticateRiderUseCase {

    private final RiderRepository repository;
    private final PasswordHasher  passwordHasher;
    private final TokenIssuer     tokenIssuer;

    public AuthenticateRiderUseCase(RiderRepository repository,
                                    PasswordHasher passwordHasher,
                                    TokenIssuer tokenIssuer) {
        this.repository     = repository;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer    = tokenIssuer;
    }

    @Transactional(readOnly = true)
    public Result login(String email, String rawPassword) {
        Rider rider = repository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordHasher.matches(rawPassword, rider.passwordHash())) {
            throw new InvalidCredentialsException();
        }

        return new Result(rider, tokenIssuer.issue(rider));
    }

    public record Result(Rider rider, TokenIssuer.IssuedToken token) {}
}
