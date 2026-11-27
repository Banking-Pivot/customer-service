package com.banking.customerservice.application.handler.query;

import com.banking.customerservice.application.exception.CustomerNotFoundException;
import com.banking.customerservice.application.model.CustomerView;
import com.banking.customerservice.application.port.in.GetCustomerUseCase;
import com.banking.customerservice.application.port.out.CustomerReadPort;
import com.banking.customerservice.application.query.GetCustomerQuery;
import java.util.Objects;
import reactor.core.publisher.Mono;

/** Reads one customer by ID from the read side, whether active or inactive. */
public class GetCustomerHandler implements GetCustomerUseCase {

  private final CustomerReadPort readPort;

  public GetCustomerHandler(CustomerReadPort readPort) {
    this.readPort = Objects.requireNonNull(readPort);
  }

  @Override
  public Mono<CustomerView> execute(GetCustomerQuery query) {
    return readPort
        .findById(query.customerId())
        .switchIfEmpty(Mono.error(() -> new CustomerNotFoundException(query.customerId())));
  }
}
