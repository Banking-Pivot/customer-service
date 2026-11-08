package com.banking.customer_service.application.handler.query;

import com.banking.customer_service.application.exception.CustomerNotFoundException;
import com.banking.customer_service.application.model.CustomerView;
import com.banking.customer_service.application.port.in.GetCustomerUseCase;
import com.banking.customer_service.application.port.out.CustomerReadPort;
import com.banking.customer_service.application.query.GetCustomerQuery;
import reactor.core.publisher.Mono;

import java.util.Objects;

public class GetCustomerHandler implements GetCustomerUseCase {

    private final CustomerReadPort readPort;

    public GetCustomerHandler(CustomerReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort);
    }

    @Override
    public Mono<CustomerView> execute(GetCustomerQuery query) {
        return readPort.findById(query.customerId())
                .switchIfEmpty(Mono.error(() -> new CustomerNotFoundException(query.customerId())));
    }
}
