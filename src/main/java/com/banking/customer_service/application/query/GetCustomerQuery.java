package com.banking.customer_service.application.query;

import java.util.Objects;
import java.util.UUID;

public record GetCustomerQuery(UUID customerId) {
    public GetCustomerQuery {
        Objects.requireNonNull(customerId, "Customer ID is required.");
    }
}
