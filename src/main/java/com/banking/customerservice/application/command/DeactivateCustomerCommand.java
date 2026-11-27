package com.banking.customerservice.application.command;

import java.util.Objects;
import java.util.UUID;

/** Logical deletion conditioned on the revision the client last read. */
public record DeactivateCustomerCommand(UUID customerId, long expectedVersion) {
  /** Rejects a missing customer ID or a negative expected revision. */
  public DeactivateCustomerCommand {
    Objects.requireNonNull(customerId, "Customer ID is required.");
    if (expectedVersion < 0) {
      throw new IllegalArgumentException("Expected version must not be negative.");
    }
  }
}
