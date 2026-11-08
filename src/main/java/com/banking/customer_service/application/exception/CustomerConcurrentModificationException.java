package com.banking.customer_service.application.exception;

import java.util.UUID;

/**
 * The revision supplied by the client no longer matches the stored one,
 * either before the write or because another writer won the race.
 */
public class CustomerConcurrentModificationException extends RuntimeException {
    public CustomerConcurrentModificationException(UUID customerId) {
        super("Customer " + customerId + " changed. Read it again before retrying.");
    }
}
