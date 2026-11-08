package com.banking.customer_service.application.exception;

public class DuplicateCustomerDocumentException extends RuntimeException {
    public DuplicateCustomerDocumentException(Throwable cause) {
        super("A customer with this identity document already exists.", cause);
    }
}
