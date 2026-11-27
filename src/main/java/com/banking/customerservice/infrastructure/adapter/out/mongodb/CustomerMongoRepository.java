package com.banking.customerservice.infrastructure.adapter.out.mongodb;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

/** Spring Data repository of customer documents, keyed by the UUID stored as a string. */
public interface CustomerMongoRepository
    extends ReactiveMongoRepository<CustomerDocument, String> {}
