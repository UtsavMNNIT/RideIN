package com.rideflow.rider.application.usecase;

import com.rideflow.rider.application.usecase.AuthenticateRiderUseCase.Result;
import com.rideflow.rider.domain.exception.InvalidCredentialsException;
import com.rideflow.rider.domain.model.Rider;
import com.rideflow.rider.support.FakePasswordHasher;
import com.rideflow.rider.support.FakeTokenIssuer;
import com.rideflow.rider.support.InMemoryRiderRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticateRiderUseCaseTest {

    private InMemoryRiderRepository riders;
    private FakePasswordHasher hasher;
    private FakeTokenIssuer tokenIssuer;
    private AuthenticateRiderUseCase useCase;

    @BeforeEach
    void setUp() {
        riders = new InMemoryRiderRepository();
        hasher = new FakePasswordHasher();
        tokenIssuer = new FakeTokenIssuer();
        useCase = new AuthenticateRiderUseCase(riders, hasher, tokenIssuer);
        // Register a rider whose passwordHash matches the fake hasher.
        riders.seed(Rider.register("ann@example.com", "111", "Ann", hasher.hash("secret")));
    }

    @Test
    void login_validCredentials_issuesToken() {
        Result result = useCase.login("  ANN@example.com ", "secret");

        assertThat(result.rider().email()).isEqualTo("ann@example.com");
        assertThat(result.token().token()).isEqualTo("token-for-" + result.rider().id());
        assertThat(tokenIssuer.lastIssuedFor).isEqualTo(result.rider());
    }

    @Test
    void login_unknownEmail_throwsInvalidCredentials() {
        assertThatThrownBy(() -> useCase.login("nobody@example.com", "secret"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        assertThatThrownBy(() -> useCase.login("ann@example.com", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
