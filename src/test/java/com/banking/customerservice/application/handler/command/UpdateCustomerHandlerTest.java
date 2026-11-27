package com.banking.customerservice.application.handler.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.banking.customerservice.application.command.UpdateCustomerCommand;
import com.banking.customerservice.application.exception.CustomerConcurrentModificationException;
import com.banking.customerservice.application.exception.CustomerNotFoundException;
import com.banking.customerservice.application.exception.InactiveCustomerException;
import com.banking.customerservice.domain.exception.InvalidCustomerException;
import com.banking.customerservice.domain.model.Customer;
import com.banking.customerservice.domain.model.enums.CustomerProfile;
import com.banking.customerservice.domain.model.enums.CustomerType;
import com.banking.customerservice.domain.model.vo.IdentityDocument;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class UpdateCustomerHandlerTest {

  private static final Instant T0 = Instant.parse("2026-11-05T15:00:00Z");
  private static final Instant T1 = T0.plusSeconds(60);

  private final InMemoryCustomerWritePort port = new InMemoryCustomerWritePort();
  private final UpdateCustomerHandler handler =
      new UpdateCustomerHandler(port, Clock.fixed(T1, ZoneOffset.UTC));
  private UUID id;

  @BeforeEach
  void storeCustomer() {
    Customer customer =
        Customer.create(
            UUID.randomUUID(),
            CustomerType.PERSONAL,
            CustomerProfile.STANDARD,
            new IdentityDocument("DNI", "1"),
            "Ana",
            T0);
    port.save(customer);
    id = customer.getId();
  }

  @Test
  void writesWithTheClientRevisionAndReturnsTheNextOne() {
    StepVerifier.create(
            handler.execute(new UpdateCustomerCommand(id, "Ana Gomez", CustomerProfile.VIP, 0)))
        .assertNext(
            view -> {
              assertThat(view.version()).isEqualTo(1);
              assertThat(view.name()).isEqualTo("Ana Gomez");
              assertThat(view.updatedAt()).isEqualTo(T1);
            })
        .verifyComplete();

    assertThat(port.updateConditions).containsExactly(0L);
  }

  @Test
  void identicalValuesSkipTheWriteAndKeepTheRevision() {
    StepVerifier.create(
            handler.execute(new UpdateCustomerCommand(id, " Ana ", CustomerProfile.STANDARD, 0)))
        .assertNext(view -> assertThat(view.version()).isZero())
        .verifyComplete();

    assertThat(port.updateConditions).isEmpty();
  }

  @Test
  void staleRevisionFailsBeforeWriting() {
    StepVerifier.create(handler.execute(new UpdateCustomerCommand(id, "X", CustomerProfile.VIP, 7)))
        .expectError(CustomerConcurrentModificationException.class)
        .verify();

    assertThat(port.updateConditions).isEmpty();
  }

  @Test
  void writerThatWinsBetweenReadAndWriteMakesThisOneFail() {
    port.afterNextLoad(
        () -> {
          Customer competitor = port.stored(id);
          competitor.updateInformation("Competitor", CustomerProfile.VIP, T1);
          port.update(competitor, 0).block();
        });

    StepVerifier.create(
            handler.execute(new UpdateCustomerCommand(id, "Late", CustomerProfile.STANDARD, 0)))
        .expectError(CustomerConcurrentModificationException.class)
        .verify();

    assertThat(port.updateConditions).containsExactly(0L, 0L);
    assertThat(port.stored(id).getName()).isEqualTo("Competitor");
  }

  @Test
  void inactiveCustomerIsAConflict() {
    Customer inactive = port.stored(id);
    inactive.deactivate(T0);
    port.save(inactive);

    StepVerifier.create(handler.execute(new UpdateCustomerCommand(id, "X", CustomerProfile.VIP, 0)))
        .expectError(InactiveCustomerException.class)
        .verify();
  }

  @Test
  void incompatibleProfileIsABusinessRuleViolation() {
    StepVerifier.create(handler.execute(new UpdateCustomerCommand(id, "X", CustomerProfile.SME, 0)))
        .expectError(InvalidCustomerException.class)
        .verify();
  }

  @Test
  void unknownCustomerIsNotFound() {
    StepVerifier.create(
            handler.execute(
                new UpdateCustomerCommand(UUID.randomUUID(), "X", CustomerProfile.VIP, 0)))
        .expectError(CustomerNotFoundException.class)
        .verify();
  }
}
