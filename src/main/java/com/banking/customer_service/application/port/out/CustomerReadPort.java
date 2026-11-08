package com.banking.customer_service.application.port.out;

import com.banking.customer_service.application.model.CustomerView;
import com.banking.customer_service.application.query.ListCustomersQuery;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Read side of the customer store. Projects stored data into views without
 * loading the aggregate.
 */
public interface CustomerReadPort {

    /** Emits the customer view, or completes empty when it does not exist. */
    Mono<CustomerView> findById(UUID customerId);

    /** Emits one page of matching customers ordered by UUID ascending. */
    Flux<CustomerView> findPage(ListCustomersQuery query);

    /** Counts the customers matching the query filters. */
    Mono<Long> count(ListCustomersQuery query);
}
