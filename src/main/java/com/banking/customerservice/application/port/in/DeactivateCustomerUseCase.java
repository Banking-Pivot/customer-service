package com.banking.customerservice.application.port.in;

import com.banking.customerservice.application.command.DeactivateCustomerCommand;
import reactor.core.publisher.Mono;

/** Logically deletes a customer; the record and its identity document stay reserved. */
public interface DeactivateCustomerUseCase {
  /**
   * Completes when the customer is inactive. Fails with CustomerNotFoundException or
   * CustomerConcurrentModificationException.
   */
  Mono<Void> execute(DeactivateCustomerCommand command);
}
