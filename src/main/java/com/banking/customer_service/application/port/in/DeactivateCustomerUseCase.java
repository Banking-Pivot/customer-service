package com.banking.customer_service.application.port.in;

import com.banking.customer_service.application.command.DeactivateCustomerCommand;
import reactor.core.publisher.Mono;

public interface DeactivateCustomerUseCase {
    /**
     * Completes when the customer is inactive. Fails with CustomerNotFoundException
     * or CustomerConcurrentModificationException.
     */
    Mono<Void> execute(DeactivateCustomerCommand command);
}
