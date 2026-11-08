package com.banking.customer_service.application.handler.command;

import com.banking.customer_service.application.command.CreateCustomerCommand;
import com.banking.customer_service.application.model.CustomerView;
import com.banking.customer_service.application.port.in.CreateCustomerUseCase;
import com.banking.customer_service.application.port.out.CustomerWritePort;
import com.banking.customer_service.domain.model.Customer;
import com.banking.customer_service.domain.model.IdentityDocument;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

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
        return Mono.defer(() -> {
            // MongoDB stores milliseconds; truncating keeps the response equal to the stored value.
            Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);

            Customer customer = Customer.create(
                    UUID.randomUUID(),
                    command.type(),
                    command.profile(),
                    new IdentityDocument(command.documentType(), command.documentNumber()),
                    command.name(),
                    now
            );

            // The unique index decides identity collisions atomically; see the write adapter.
            return writePort.insert(customer);
        }).map(CustomerView::from);
    }
}
