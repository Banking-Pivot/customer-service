package com.banking.customer_service.application.port.out;

import com.banking.customer_service.domain.model.Customer;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Write side of the customer store. Loads aggregates and persists their changes.
 */
public interface CustomerWritePort {

    /**
     * Inserts a new customer and emits it with its persisted revision.
     * Fails with {@code DuplicateCustomerDocumentException} when the normalized
     * identity document already exists, including on inactive customers.
     */
    Mono<Customer> insert(Customer customer);

    /** Loads the aggregate, or completes empty when the customer does not exist. */
    Mono<Customer> findById(UUID customerId);

    /**
     * Persists name, profile, status and updatedAt only if the stored revision
     * still equals {@code expectedVersion}, incrementing it in the same atomic
     * operation. Emits the saved customer with its new revision, or completes
     * empty when the stored revision no longer matches.
     *
     * @param expectedVersion revision the client sent in If-Match; it is the write
     *                        condition even if the aggregate was re-read in between.
     */
    Mono<Customer> update(Customer customer, long expectedVersion);
}
