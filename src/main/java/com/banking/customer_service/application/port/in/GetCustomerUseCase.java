package com.banking.customer_service.application.port.in;

import com.banking.customer_service.application.model.CustomerView;
import com.banking.customer_service.application.query.GetCustomerQuery;
import reactor.core.publisher.Mono;

public interface GetCustomerUseCase {
    /** Emits the customer, active or inactive, or fails with CustomerNotFoundException. */
    Mono<CustomerView> execute(GetCustomerQuery query);
}
