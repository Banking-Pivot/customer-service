package com.banking.customer_service.application.model;

import com.banking.customer_service.domain.model.Customer;
import com.banking.customer_service.domain.model.CustomerProfile;
import com.banking.customer_service.domain.model.CustomerStatus;
import com.banking.customer_service.domain.model.CustomerType;

import java.time.Instant;
import java.util.UUID;

/**
 * Read representation of a customer returned by every use case.
 * {@code version} is the persisted revision exposed to HTTP clients as ETag.
 */
public record CustomerView(
        UUID id,
        CustomerType type,
        CustomerProfile profile,
        String documentType,
        String documentNumber,
        String name,
        CustomerStatus status,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
    public static CustomerView from(Customer customer) {
        return new CustomerView(
                customer.getId(),
                customer.getType(),
                customer.getProfile(),
                customer.getIdentityDocument().type(),
                customer.getIdentityDocument().number(),
                customer.getName(),
                customer.getStatus(),
                customer.getCreatedAt(),
                customer.getUpdatedAt(),
                customer.getVersion()
        );
    }
}
