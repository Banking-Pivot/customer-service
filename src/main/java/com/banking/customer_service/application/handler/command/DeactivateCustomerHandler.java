package com.banking.customer_service.application.handler.command;

import com.banking.customer_service.application.command.DeactivateCustomerCommand;
import com.banking.customer_service.application.exception.CustomerConcurrentModificationException;
import com.banking.customer_service.application.exception.CustomerNotFoundException;
import com.banking.customer_service.application.port.in.DeactivateCustomerUseCase;
import com.banking.customer_service.application.port.out.CustomerWritePort;
import com.banking.customer_service.domain.model.Customer;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Logical deletion: ACTIVE becomes INACTIVE, the record and its identity
 * document stay reserved. Repeating it with the current revision is a no-op;
 * repeating it with an old revision is a stale precondition (412).
 */
public class DeactivateCustomerHandler implements DeactivateCustomerUseCase {

    private final CustomerWritePort writePort;
    private final Clock clock;

    public DeactivateCustomerHandler(CustomerWritePort writePort, Clock clock) {
        this.writePort = Objects.requireNonNull(writePort);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Mono<Void> execute(DeactivateCustomerCommand command) {
        return writePort.findById(command.customerId())
                .switchIfEmpty(Mono.error(() -> new CustomerNotFoundException(command.customerId())))
                .flatMap(customer -> apply(customer, command));
    }

    private Mono<Void> apply(Customer customer, DeactivateCustomerCommand command) {
        if (customer.getVersion() != command.expectedVersion()) {
            return Mono.error(new CustomerConcurrentModificationException(customer.getId()));
        }

        boolean changed = customer.deactivate(clock.instant().truncatedTo(ChronoUnit.MILLIS));

        if (!changed) {
            return Mono.empty();
        }

        return writePort.update(customer, command.expectedVersion())
                .switchIfEmpty(Mono.error(
                        () -> new CustomerConcurrentModificationException(customer.getId())
                ))
                .then();
    }
}
