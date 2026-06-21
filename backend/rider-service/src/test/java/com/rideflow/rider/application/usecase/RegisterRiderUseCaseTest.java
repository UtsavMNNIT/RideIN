package com.rideflow.rider.application.usecase;

import com.rideflow.rider.application.usecase.RegisterRiderUseCase.RegisterCommand;
import com.rideflow.rider.domain.exception.RiderAlreadyExistsException;
import com.rideflow.rider.domain.model.Rider;
import com.rideflow.rider.support.FakePasswordHasher;
import com.rideflow.rider.support.InMemoryRiderRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegisterRiderUseCaseTest {

    private InMemoryRiderRepository riders;
    private FakePasswordHasher hasher;
    private RegisterRiderUseCase useCase;

    @BeforeEach
    void setUp() {
        riders = new InMemoryRiderRepository();
        hasher = new FakePasswordHasher();
        useCase = new RegisterRiderUseCase(riders, hasher);
    }

    @Test
    void register_normalisesEmail_hashesPassword_andPersists() {
        Rider rider = useCase.register(new RegisterCommand(
                "  ANN@Example.COM ", " +91-99 ", "Ann Rider", "secret"));

        assertThat(rider.id()).isNotNull();
        assertThat(rider.email()).isEqualTo("ann@example.com");
        assertThat(rider.phone()).isEqualTo("+91-99");
        assertThat(rider.passwordHash()).isEqualTo("hashed:secret");
        assertThat(riders.findById(rider.id())).contains(rider);
    }

    @Test
    void register_duplicateEmail_throws() {
        useCase.register(new RegisterCommand("ann@example.com", "111", "Ann", "p1"));

        assertThatThrownBy(() -> useCase.register(
                new RegisterCommand("ANN@example.com", "222", "Ann2", "p2")))
                .isInstanceOf(RiderAlreadyExistsException.class);
    }

    @Test
    void register_duplicatePhone_throws() {
        useCase.register(new RegisterCommand("ann@example.com", "555", "Ann", "p1"));

        assertThatThrownBy(() -> useCase.register(
                new RegisterCommand("bob@example.com", "555", "Bob", "p2")))
                .isInstanceOf(RiderAlreadyExistsException.class);
    }
}
