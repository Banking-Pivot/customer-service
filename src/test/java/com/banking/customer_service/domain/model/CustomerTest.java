package com.banking.customer_service.domain.model;

import com.banking.customer_service.domain.exception.InvalidCustomerException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerTest {

    private static final Instant T0 = Instant.parse("2026-11-05T15:00:00Z");
    private static final Instant T1 = T0.plusSeconds(60);
    private static final IdentityDocument DNI = new IdentityDocument("DNI", "12345678");

    @Test
    void newCustomerIsActiveAtRevisionZeroWithTrimmedName() {
        Customer customer = personal(CustomerProfile.STANDARD);

        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(customer.isActive()).isTrue();
        assertThat(customer.getVersion()).isZero();
        assertThat(customer.getName()).isEqualTo("Ana Perez");
        assertThat(customer.getCreatedAt()).isEqualTo(T0).isEqualTo(customer.getUpdatedAt());
    }

    @ParameterizedTest
    @CsvSource({
            "PERSONAL, STANDARD, true",
            "PERSONAL, VIP,      true",
            "PERSONAL, SME,      false",
            "BUSINESS, STANDARD, true",
            "BUSINESS, SME,      true",
            "BUSINESS, VIP,      false"
    })
    void profileMustBeCompatibleWithType(CustomerType type, CustomerProfile profile, boolean allowed) {
        if (allowed) {
            assertThat(Customer.create(UUID.randomUUID(), type, profile, DNI, "A", T0).getProfile())
                    .isEqualTo(profile);
        } else {
            assertThatThrownBy(() -> Customer.create(UUID.randomUUID(), type, profile, DNI, "A", T0))
                    .isInstanceOf(InvalidCustomerException.class);
        }
    }

    @Test
    void rejectsBlankOrOverlongName() {
        assertThatThrownBy(() -> Customer.create(UUID.randomUUID(), CustomerType.PERSONAL,
                CustomerProfile.STANDARD, DNI, "   ", T0))
                .isInstanceOf(InvalidCustomerException.class);
        assertThatThrownBy(() -> Customer.create(UUID.randomUUID(), CustomerType.PERSONAL,
                CustomerProfile.STANDARD, DNI, "x".repeat(121), T0))
                .isInstanceOf(InvalidCustomerException.class);
    }

    @Test
    void updateReplacesEditableFieldsAndTimestamp() {
        Customer customer = personal(CustomerProfile.STANDARD);

        boolean changed = customer.updateInformation(" Ana Gomez ", CustomerProfile.VIP, T1);

        assertThat(changed).isTrue();
        assertThat(customer.getName()).isEqualTo("Ana Gomez");
        assertThat(customer.getProfile()).isEqualTo(CustomerProfile.VIP);
        assertThat(customer.getUpdatedAt()).isEqualTo(T1);
        assertThat(customer.getCreatedAt()).isEqualTo(T0);
        assertThat(customer.getType()).isEqualTo(CustomerType.PERSONAL);
        assertThat(customer.getIdentityDocument()).isEqualTo(DNI);
    }

    @Test
    void updateWithIdenticalValuesChangesNothing() {
        Customer customer = personal(CustomerProfile.STANDARD);

        boolean changed = customer.updateInformation("  Ana Perez", CustomerProfile.STANDARD, T1);

        assertThat(changed).isFalse();
        assertThat(customer.getUpdatedAt()).isEqualTo(T0);
    }

    @Test
    void invalidUpdateLeavesStateUntouched() {
        Customer customer = personal(CustomerProfile.STANDARD);

        assertThatThrownBy(() -> customer.updateInformation("New Name", CustomerProfile.SME, T1))
                .isInstanceOf(InvalidCustomerException.class);

        assertThat(customer.getName()).isEqualTo("Ana Perez");
        assertThat(customer.getProfile()).isEqualTo(CustomerProfile.STANDARD);
        assertThat(customer.getUpdatedAt()).isEqualTo(T0);
    }

    @Test
    void inactiveCustomerCannotBeUpdated() {
        Customer customer = personal(CustomerProfile.STANDARD);
        customer.deactivate(T1);

        assertThatThrownBy(() -> customer.updateInformation("Other", CustomerProfile.VIP, T1))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deactivationIsIdempotent() {
        Customer customer = personal(CustomerProfile.VIP);

        assertThat(customer.deactivate(T1)).isTrue();
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.INACTIVE);
        assertThat(customer.getUpdatedAt()).isEqualTo(T1);

        assertThat(customer.deactivate(T1.plusSeconds(60))).isFalse();
        assertThat(customer.getUpdatedAt()).isEqualTo(T1);
        assertThat(customer.getIdentityDocument()).isEqualTo(DNI);
    }

    @Test
    void operationsCannotMoveTimeBackwards() {
        Customer customer = personal(CustomerProfile.STANDARD);

        assertThatThrownBy(() -> customer.deactivate(T0.minusSeconds(1)))
                .isInstanceOf(InvalidCustomerException.class);
    }

    @Test
    void restoreValidatesPersistedState() {
        assertThatThrownBy(() -> Customer.restore(UUID.randomUUID(), CustomerType.BUSINESS,
                CustomerProfile.VIP, DNI, "A", CustomerStatus.ACTIVE, T0, T0, 0))
                .isInstanceOf(InvalidCustomerException.class);
        assertThatThrownBy(() -> Customer.restore(UUID.randomUUID(), CustomerType.BUSINESS,
                CustomerProfile.SME, DNI, "A", CustomerStatus.ACTIVE, T1, T0, 0))
                .isInstanceOf(InvalidCustomerException.class);
        assertThatThrownBy(() -> Customer.restore(UUID.randomUUID(), CustomerType.BUSINESS,
                CustomerProfile.SME, DNI, "A", CustomerStatus.ACTIVE, T0, T0, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Customer personal(CustomerProfile profile) {
        return Customer.create(UUID.randomUUID(), CustomerType.PERSONAL, profile, DNI, "  Ana Perez ", T0);
    }
}
