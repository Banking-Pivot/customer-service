package com.banking.customerservice.application.port.in;

import com.banking.customerservice.application.model.CustomerPage;
import com.banking.customerservice.application.query.ListCustomersQuery;
import reactor.core.publisher.Mono;

/** Lists customers page by page, optionally filtered by type and status. */
public interface ListCustomersUseCase {
  /** Emits the requested page; it is empty when the offset is past the last match. */
  Mono<CustomerPage> execute(ListCustomersQuery query);
}
