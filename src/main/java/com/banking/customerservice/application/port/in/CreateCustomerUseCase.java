package com.banking.customerservice.application.port.in;

import com.banking.customerservice.application.command.CreateCustomerCommand;
import com.banking.customerservice.application.model.CustomerView;
import reactor.core.publisher.Mono;

/** Registers a personal or business customer under a unique identity document. */
public interface CreateCustomerUseCase {
  /**
   * Emits the new customer, active and at revision 0. Fails with InvalidCustomerException or
   * DuplicateCustomerDocumentException.
   */
  Mono<CustomerView> execute(CreateCustomerCommand command);
}
