package com.banking.customerservice.domain.model.enums;

/**
 * Commercial segment of a customer. VIP applies only to personal customers and SME only to business
 * customers; STANDARD applies to both.
 */
public enum CustomerProfile {
  STANDARD,
  VIP,
  SME
}
