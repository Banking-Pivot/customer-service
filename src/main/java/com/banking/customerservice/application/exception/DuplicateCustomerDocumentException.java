package com.banking.customerservice.application.exception;

/** Another customer, active or inactive, already holds the same normalized identity document. */
public class DuplicateCustomerDocumentException extends RuntimeException {
  public DuplicateCustomerDocumentException(Throwable cause) {
    super("A customer with this identity document already exists.", cause);
  }
}
