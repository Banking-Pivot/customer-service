package com.banking.customerservice.application.handler.command;

import com.banking.customerservice.application.command.CreateCustomerCommand;
import com.banking.customerservice.application.model.CustomerView;
import com.banking.customerservice.application.port.in.CreateCustomerUseCase;
import com.banking.customerservice.application.port.out.CustomerWritePort;
import com.banking.customerservice.domain.model.Customer;
import com.banking.customerservice.domain.model.IdentityDocument;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * Registers a customer under a new UUID. Uniqueness of the identity document is left to the store,
 * which rejects a duplicate atomically.
 */
public class CreateCustomerHandler implements CreateCustomerUseCase {

  private final CustomerWritePort writePort;
  private final Clock clock;

  public CreateCustomerHandler(CustomerWritePort writePort, Clock clock) {
    this.writePort = Objects.requireNonNull(writePort);
    this.clock = Objects.requireNonNull(clock);
  }

  @Override
  public Mono<CustomerView> execute(CreateCustomerCommand command) {
    // defer: domain validation errors become onError signals, not thrown exceptions.
    return Mono.defer(
            () -> {
              // MongoDB stores milliseconds; truncating keeps the response equal to the stored
              // value.
              Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);

              Customer customer =
                  Customer.create(
                      UUID.randomUUID(),
                      command.type(),
                      command.profile(),
                      new IdentityDocument(command.documentType(), command.documentNumber()),
                      command.name(),
                      now);

              // The unique index decides identity collisions atomically; see the write adapter.
              return writePort.insert(customer);
            })
        .map(CustomerView::from);
  }
}
