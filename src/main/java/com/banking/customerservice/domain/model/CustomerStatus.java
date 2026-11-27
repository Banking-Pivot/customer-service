package com.banking.customerservice.domain.model;

/**
 * Lifecycle state of a customer. INACTIVE marks a logical deletion: the record stays but can no
 * longer be edited.
 */
public enum CustomerStatus {
  ACTIVE,
  INACTIVE
}
