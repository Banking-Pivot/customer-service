package com.banking.customer_service.infrastructure.adapter.out.mongodb;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface CustomerMongoRepository
        extends ReactiveMongoRepository<CustomerDocument, String> {
}