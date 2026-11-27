package com.banking.customerservice.application.handler.query;

import com.banking.customerservice.application.model.CustomerPage;
import com.banking.customerservice.application.port.in.ListCustomersUseCase;
import com.banking.customerservice.application.port.out.CustomerReadPort;
import com.banking.customerservice.application.query.ListCustomersQuery;
import java.util.Objects;
import reactor.core.publisher.Mono;

/**
 * Reads one page of customers, optionally filtered by type and status, together with the total
 * number of matches.
 */
public class ListCustomersHandler implements ListCustomersUseCase {

  private final CustomerReadPort readPort;

  public ListCustomersHandler(CustomerReadPort readPort) {
    this.readPort = Objects.requireNonNull(readPort);
  }

  @Override
  public Mono<CustomerPage> execute(ListCustomersQuery query) {
    // Items and count run concurrently; an offset past the last match yields no items.
    return Mono.zip(readPort.findPage(query).collectList(), readPort.count(query))
        .map(result -> CustomerPage.of(result.getT1(), query.page(), query.size(), result.getT2()));
  }
}
