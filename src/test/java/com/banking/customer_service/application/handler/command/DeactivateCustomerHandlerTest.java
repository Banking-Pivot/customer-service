package com.banking.customer_service.application.handler.command;

import com.banking.customer_service.application.command.DeactivateCustomerCommand;
import com.banking.customer_service.application.exception.CustomerConcurrentModificationException;
import com.banking.customer_service.application.exception.CustomerNotFoundException;
import com.banking.customer_service.domain.model.Customer;
import com.banking.customer_service.domain.model.CustomerProfile;
import com.banking.customer_service.domain.model.CustomerStatus;
import com.banking.customer_service.domain.model.CustomerType;
import com.banking.customer_service.domain.model.IdentityDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DeactivateCustomerHandlerTest {

    private static final Instant T0 = Instant.parse("2026-11-05T15:00:00Z");

    private final InMemoryCustomerWritePort port = new InMemoryCustomerWritePort();
    private final DeactivateCustomerHandler handler =
            new DeactivateCustomerHandler(port, Clock.fixed(T0.plusSeconds(60), ZoneOffset.UTC));
    private UUID id;

    @BeforeEach
    void storeCustomer() {
        Customer customer = Customer.create(UUID.randomUUID(), CustomerType.BUSINESS,
                CustomerProfile.SME, new IdentityDocument("RUC", "20123456789"), "Bodega", T0);
        port.save(customer);
        id = customer.getId();
    }

    @Test
    void deactivatesWithTheClientRevision() {
        StepVerifier.create(handler.execute(new DeactivateCustomerCommand(id, 0))).verifyComplete();

        assertThat(port.stored(id).getStatus()).isEqualTo(CustomerStatus.INACTIVE);
        assertThat(port.stored(id).getVersion()).isEqualTo(1);
        assertThat(port.updateConditions).containsExactly(0L);
    }

    @Test
    void alreadyInactiveWithMatchingRevisionCompletesWithoutWriting() {
        handler.execute(new DeactivateCustomerCommand(id, 0)).block();

        StepVerifier.create(handler.execute(new DeactivateCustomerCommand(id, 1))).verifyComplete();

        assertThat(port.stored(id).getVersion()).isEqualTo(1);
        assertThat(port.updateConditions).containsExactly(0L);
    }

    @Test
    void oldRevisionIsStaleEvenWhenAlreadyInactive() {
        handler.execute(new DeactivateCustomerCommand(id, 0)).block();

        StepVerifier.create(handler.execute(new DeactivateCustomerCommand(id, 0)))
                .expectError(CustomerConcurrentModificationException.class)
                .verify();
    }

    @Test
    void unknownCustomerIsNotFound() {
        StepVerifier.create(handler.execute(new DeactivateCustomerCommand(UUID.randomUUID(), 0)))
                .expectError(CustomerNotFoundException.class)
                .verify();
    }
}
