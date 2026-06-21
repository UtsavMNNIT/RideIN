package com.rideflow.driver.application.usecase;

import com.rideflow.driver.domain.exception.InvalidCredentialsException;
import com.rideflow.driver.domain.model.Driver;
import com.rideflow.driver.domain.model.VehicleType;
import com.rideflow.driver.support.FakePasswordHasher;
import com.rideflow.driver.support.FakeTokenIssuer;
import com.rideflow.driver.support.InMemoryDriverRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticateDriverUseCaseTest {

    private InMemoryDriverRepository repo;
    private AuthenticateDriverUseCase useCase;
    private Driver registered;

    @BeforeEach
    void setUp() {
        repo = new InMemoryDriverRepository();
        FakePasswordHasher hasher = new FakePasswordHasher();
        useCase = new AuthenticateDriverUseCase(repo, hasher, new FakeTokenIssuer());
        registered = Driver.register("driver@rideflow.test", "+911234567890", "Jane Doe",
                hasher.hash("correct-password"), VehicleType.STANDARD, "KA01AB1234");
        repo.seed(registered);
    }

    @Test
    void login_validCredentials_returnsDriverAndToken() {
        AuthenticateDriverUseCase.Result result = useCase.login("Driver@RideFlow.Test", "correct-password");
        assertThat(result.driver().id()).isEqualTo(registered.id());
        assertThat(result.token().token()).isEqualTo("token-for-" + registered.id());
        assertThat(result.token().tokenType()).isEqualTo("Bearer");
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        assertThatThrownBy(() -> useCase.login("driver@rideflow.test", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_unknownEmail_throwsInvalidCredentials() {
        assertThatThrownBy(() -> useCase.login("nobody@rideflow.test", "correct-password"))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
