package com.banking.customerservice.application.command;

import com.banking.customerservice.domain.model.CustomerProfile;
import com.banking.customerservice.domain.model.CustomerType;

/**
 * Data to register a new customer. The identity document arrives as the client typed it; the domain
 * normalizes it.
 */
public record CreateCustomerCommand(
    CustomerType type,
    CustomerProfile profile,
    String documentType,
    String documentNumber,
    String name) {}
