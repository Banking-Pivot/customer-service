package com.banking.customer_service.application.handler.command;

import com.banking.customer_service.application.command.UpdateCustomerCommand;
import com.banking.customer_service.application.exception.CustomerConcurrentModificationException;
import com.banking.customer_service.application.exception.CustomerNotFoundException;
import com.banking.customer_service.application.exception.InactiveCustomerException;
import com.banking.customer_service.application.model.CustomerView;
import com.banking.customer_service.application.port.in.UpdateCustomerUseCase;
import com.banking.customer_service.application.port.out.CustomerWritePort;
import com.banking.customer_service.domain.model.Customer;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Replaces name and profile of an active customer.
 *
 * <p>Evaluation order: 404 missing, 412 stale revision, 409 inactive,
 * 422 incompatible profile. The precondition is checked before the state so a
 * client holding an old revision always re-reads first.
 */
public class UpdateCustomerHandler implements UpdateCustomerUseCase {

    private final CustomerWritePort writePort;
    private final Clock clock;

    public UpdateCustomerHandler(CustomerWritePort writePort, Clock clock) {
        this.writePort = Objects.requireNonNull(writePort);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Mono<CustomerView> execute(UpdateCustomerCommand command) {
        return writePort.findById(command.customerId())
                .switchIfEmpty(Mono.error(() -> new CustomerNotFoundException(command.customerId())))
                .flatMap(customer -> apply(customer, command))
                .map(CustomerView::from);
    }

    private Mono<Customer> apply(Customer customer, UpdateCustomerCommand command) {
        if (customer.getVersion() != command.expectedVersion()) {
            return Mono.error(new CustomerConcurrentModificationException(customer.getId()));
        }
        if (!customer.isActive()) {
            return Mono.error(new InactiveCustomerException(customer.getId()));
        }

        boolean changed = customer.updateInformation(
                command.name(),
                command.profile(),
                clock.instant().truncatedTo(ChronoUnit.MILLIS)
        );

        if (!changed) {
            // Same revision, same values: keep the revision and return the current state.
            return Mono.just(customer);
        }

        // The client's revision, not the re-read one, is the write condition.
        return writePort.update(customer, command.expectedVersion())
                .switchIfEmpty(Mono.error(
                        () -> new CustomerConcurrentModificationException(customer.getId())
                ));
    }
}
