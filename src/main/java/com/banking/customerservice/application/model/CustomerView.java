package com.banking.customerservice.application.model;

import com.banking.customerservice.domain.model.Customer;
import com.banking.customerservice.domain.model.CustomerProfile;
import com.banking.customerservice.domain.model.CustomerStatus;
import com.banking.customerservice.domain.model.CustomerType;
import java.time.Instant;
import java.util.UUID;

/**
 * Read representation of a customer returned by every use case. {@code version} is the persisted
 * revision exposed to HTTP clients as ETag.
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
    long version) {
  /** Copies the state of the aggregate, flattening its identity document into type and number. */
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
        customer.getVersion());
  }
}
