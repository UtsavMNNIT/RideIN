package com.rideflow.driver.application.usecase;

import com.rideflow.driver.domain.exception.DriverAlreadyExistsException;
import com.rideflow.driver.domain.model.Driver;
import com.rideflow.driver.domain.model.DriverAvailability;
import com.rideflow.driver.domain.model.VehicleType;
import com.rideflow.driver.support.FakePasswordHasher;
import com.rideflow.driver.support.InMemoryDriverRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegisterDriverUseCaseTest {

    private InMemoryDriverRepository repo;
    private RegisterDriverUseCase useCase;

    @BeforeEach
    void setUp() {
        repo = new InMemoryDriverRepository();
        useCase = new RegisterDriverUseCase(repo, new FakePasswordHasher());
    }

    private RegisterDriverUseCase.RegisterCommand cmd(String email, String phone) {
        return new RegisterDriverUseCase.RegisterCommand(
                email, phone, "Jane Doe", "s3cret", VehicleType.STANDARD, "KA01AB1234");
    }

    @Test
    void register_happyPath_persistsHashedOfflineDriver() {
        Driver d = useCase.register(cmd("Jane@RideFlow.Test", "+919999999999"));

        assertThat(d.availability()).isEqualTo(DriverAvailability.OFFLINE);
        assertThat(d.email()).isEqualTo("jane@rideflow.test"); // normalized
        assertThat(d.passwordHash()).isEqualTo("hashed:s3cret"); // hashed, not raw
        assertThat(repo.findById(d.id())).isPresent();
    }

    @Test
    void register_duplicateEmail_throws() {
        useCase.register(cmd("dup@rideflow.test", "+910000000001"));
        assertThatThrownBy(() -> useCase.register(cmd("DUP@rideflow.test", "+910000000002")))
                .isInstanceOf(DriverAlreadyExistsException.class)
                .hasMessageContaining("email");
    }

    @Test
    void register_duplicatePhone_throws() {
        useCase.register(cmd("a@rideflow.test", "+910000000003"));
        assertThatThrownBy(() -> useCase.register(cmd("b@rideflow.test", "+910000000003")))
                .isInstanceOf(DriverAlreadyExistsException.class)
                .hasMessageContaining("phone");
    }
}
