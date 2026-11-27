package com.banking.customerservice.application.port.in;

import com.banking.customerservice.application.model.CustomerView;
import com.banking.customerservice.application.query.GetCustomerQuery;
import reactor.core.publisher.Mono;

/** Looks up one customer by ID. */
public interface GetCustomerUseCase {
  /** Emits the customer, active or inactive, or fails with CustomerNotFoundException. */
  Mono<CustomerView> execute(GetCustomerQuery query);
}
