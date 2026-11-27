package com.banking.customerservice.application.exception;

import java.util.UUID;

/** No customer exists with the requested ID. */
public class CustomerNotFoundException extends RuntimeException {
  public CustomerNotFoundException(UUID customerId) {
    super("Customer " + customerId + " does not exist.");
  }
}
