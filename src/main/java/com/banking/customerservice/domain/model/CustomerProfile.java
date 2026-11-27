package com.banking.customerservice.domain.model;

/**
 * Commercial segment of a customer. VIP applies only to personal customers and SME only to business
 * customers; STANDARD applies to both.
 */
public enum CustomerProfile {
  STANDARD,
  VIP,
  SME
}
