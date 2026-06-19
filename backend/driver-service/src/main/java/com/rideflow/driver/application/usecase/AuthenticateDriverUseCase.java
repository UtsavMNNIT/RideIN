package com.rideflow.driver.application.usecase;

import com.rideflow.driver.application.port.out.DriverRepository;
import com.rideflow.driver.application.port.out.PasswordHasher;
import com.rideflow.driver.application.port.out.TokenIssuer;
import com.rideflow.driver.domain.exception.InvalidCredentialsException;
import com.rideflow.driver.domain.model.Driver;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authenticates a driver by email + password and issues an access token.
 *
 * <p>Both the unknown-email and wrong-password paths throw the same
 * {@link InvalidCredentialsException} so the API cannot be used to enumerate
 * registered accounts.
 */
@Service
public class AuthenticateDriverUseCase {

    private final DriverRepository repository;
    private final PasswordHasher   passwordHasher;
    private final TokenIssuer       tokenIssuer;

    public AuthenticateDriverUseCase(DriverRepository repository,
                                     PasswordHasher passwordHasher,
                                     TokenIssuer tokenIssuer) {
        this.repository     = repository;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer    = tokenIssuer;
    }

    @Transactional(readOnly = true)
    public Result login(String email, String rawPassword) {
        Driver driver = repository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordHasher.matches(rawPassword, driver.passwordHash())) {
            throw new InvalidCredentialsException();
        }

        return new Result(driver, tokenIssuer.issue(driver));
    }

    /** Authenticated driver plus the freshly issued access token. */
    public record Result(Driver driver, TokenIssuer.IssuedToken token) {}
}
