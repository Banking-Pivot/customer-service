package com.banking.customerservice.application.command;

import com.banking.customerservice.domain.model.enums.CustomerProfile;
import java.util.Objects;
import java.util.UUID;

/** Full replacement of the editable fields, conditioned on the revision the client last read. */
public record UpdateCustomerCommand(
    UUID customerId, String name, CustomerProfile profile, long expectedVersion) {
  /**
   * Rejects a missing customer ID or a negative expected revision. Name and profile are validated
   * by the aggregate.
   */
  public UpdateCustomerCommand {
    Objects.requireNonNull(customerId, "Customer ID is required.");
    if (expectedVersion < 0) {
      throw new IllegalArgumentException("Expected version must not be negative.");
    }
  }
}
