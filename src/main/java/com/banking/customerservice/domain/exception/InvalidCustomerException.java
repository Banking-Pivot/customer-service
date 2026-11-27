package com.banking.customerservice.domain.exception;

/**
 * A customer value breaks a domain rule: an invalid name or identity document, a profile that does
 * not fit the customer type, or timestamps out of order.
 */
public class InvalidCustomerException extends RuntimeException {
  public InvalidCustomerException(String message) {
    super(message);
  }
}
