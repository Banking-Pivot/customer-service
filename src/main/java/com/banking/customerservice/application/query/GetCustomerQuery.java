package com.banking.customerservice.application.query;

import java.util.Objects;
import java.util.UUID;

/** Request for one customer by ID. */
public record GetCustomerQuery(UUID customerId) {
  public GetCustomerQuery {
    Objects.requireNonNull(customerId, "Customer ID is required.");
  }
}
