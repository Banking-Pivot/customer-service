package com.banking.customer_service.application.command;

import com.banking.customer_service.domain.model.CustomerProfile;

import java.util.Objects;
import java.util.UUID;

/**
 * Full replacement of the editable fields, conditioned on the revision the
 * client last read.
 */
public record UpdateCustomerCommand(
        UUID customerId,
        String name,
        CustomerProfile profile,
        long expectedVersion
) {
    public UpdateCustomerCommand {
        Objects.requireNonNull(customerId, "Customer ID is required.");
        if (expectedVersion < 0) {
            throw new IllegalArgumentException("Expected version must not be negative.");
        }
    }
}
