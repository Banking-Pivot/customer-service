package com.banking.customer_service.application.handler.query;

import com.banking.customer_service.application.model.CustomerPage;
import com.banking.customer_service.application.port.in.ListCustomersUseCase;
import com.banking.customer_service.application.port.out.CustomerReadPort;
import com.banking.customer_service.application.query.ListCustomersQuery;
import reactor.core.publisher.Mono;

import java.util.Objects;

public class ListCustomersHandler implements ListCustomersUseCase {

    private final CustomerReadPort readPort;

    public ListCustomersHandler(CustomerReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort);
    }

    @Override
    public Mono<CustomerPage> execute(ListCustomersQuery query) {
        // Items and count run concurrently; an offset past the last match yields no items.
        return Mono.zip(readPort.findPage(query).collectList(), readPort.count(query))
                .map(result -> CustomerPage.of(
                        result.getT1(),
                        query.page(),
                        query.size(),
                        result.getT2()
                ));
    }
}
