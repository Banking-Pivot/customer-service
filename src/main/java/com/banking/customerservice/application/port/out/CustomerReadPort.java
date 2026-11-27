package com.banking.customerservice.application.port.out;

import com.banking.customerservice.application.model.CustomerView;
import com.banking.customerservice.application.query.ListCustomersQuery;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Read side of the customer store. Projects stored data into views without loading the aggregate.
 */
public interface CustomerReadPort {

  /** Emits the customer view, or completes empty when it does not exist. */
  Mono<CustomerView> findById(UUID customerId);

  /** Emits one page of matching customers ordered by UUID ascending. */
  Flux<CustomerView> findPage(ListCustomersQuery query);

  /** Counts the customers matching the query filters. */
  Mono<Long> count(ListCustomersQuery query);
}
