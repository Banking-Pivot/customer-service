package com.banking.customer_service.application.port.in;

import com.banking.customer_service.application.model.CustomerPage;
import com.banking.customer_service.application.query.ListCustomersQuery;
import reactor.core.publisher.Mono;

public interface ListCustomersUseCase {
    Mono<CustomerPage> execute(ListCustomersQuery query);
}
