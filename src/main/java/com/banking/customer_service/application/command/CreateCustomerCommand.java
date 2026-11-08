package com.banking.customer_service.application.command;

import com.banking.customer_service.domain.model.CustomerProfile;
import com.banking.customer_service.domain.model.CustomerType;

public record CreateCustomerCommand(
    CustomerType type,
    CustomerProfile profile,
    String documentType,
    String documentNumber,
    String name
) {
}
