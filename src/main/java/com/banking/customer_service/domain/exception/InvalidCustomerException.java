package com.banking.customer_service.domain.exception;

public class InvalidCustomerException extends RuntimeException {
  public InvalidCustomerException(String message) {
    super(message);
  }
}
