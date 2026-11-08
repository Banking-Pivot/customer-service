package com.banking.customer_service.application.port.in;

import com.banking.customer_service.application.command.CreateCustomerCommand;
import com.banking.customer_service.application.model.CustomerView;
import reactor.core.publisher.Mono;

public interface CreateCustomerUseCase {
    Mono<CustomerView> execute(CreateCustomerCommand command);
}
