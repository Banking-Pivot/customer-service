package com.banking.customer_service.application.port.in;

import com.banking.customer_service.application.command.UpdateCustomerCommand;
import com.banking.customer_service.application.model.CustomerView;
import reactor.core.publisher.Mono;

public interface UpdateCustomerUseCase {
    /**
     * Emits the resulting customer. Fails with CustomerNotFoundException,
     * CustomerConcurrentModificationException, InactiveCustomerException or
     * InvalidCustomerException.
     */
    Mono<CustomerView> execute(UpdateCustomerCommand command);
}
