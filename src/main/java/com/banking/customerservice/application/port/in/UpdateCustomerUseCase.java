package com.banking.customerservice.application.port.in;

import com.banking.customerservice.application.command.UpdateCustomerCommand;
import com.banking.customerservice.application.model.CustomerView;
import reactor.core.publisher.Mono;

/** Replaces the name and profile of an active customer. */
public interface UpdateCustomerUseCase {
  /**
   * Emits the resulting customer. Fails with CustomerNotFoundException,
   * CustomerConcurrentModificationException, InactiveCustomerException or InvalidCustomerException.
   */
  Mono<CustomerView> execute(UpdateCustomerCommand command);
}
