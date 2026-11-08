package com.banking.customer_service.application.exception;

import java.util.UUID;

/** Editable information of an inactive customer cannot be changed. */
public class InactiveCustomerException extends RuntimeException {
    public InactiveCustomerException(UUID customerId) {
        super("Customer " + customerId + " is inactive and cannot be updated.");
    }
}
