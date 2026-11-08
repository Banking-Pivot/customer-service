package com.banking.customer_service.infrastructure.adapter.out.mongodb;

import com.banking.customer_service.application.exception.DuplicateCustomerDocumentException;
import com.banking.customer_service.application.port.out.CustomerWritePort;
import com.banking.customer_service.domain.model.Customer;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Repository
public class CustomerMongoWriteAdapter implements CustomerWritePort {

    private final CustomerMongoRepository repository;
    private final ReactiveMongoTemplate mongoTemplate;
    private final CustomerMongoMapper mapper;

    public CustomerMongoWriteAdapter(
            CustomerMongoRepository repository,
            ReactiveMongoTemplate mongoTemplate,
            CustomerMongoMapper mapper
    ) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.mapper = mapper;
    }

    @Override
    public Mono<Customer> insert(Customer customer) {
        // insert (not save) so an existing _id is never overwritten; @Version starts at 0.
        return repository.insert(mapper.toDocument(customer))
                .map(mapper::toDomain)
                .onErrorMap(
                        DuplicateKeyException.class,
                        DuplicateCustomerDocumentException::new
                );
    }

    @Override
    public Mono<Customer> findById(UUID customerId) {
        return repository.findById(customerId.toString())
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Customer> update(Customer customer, long expectedVersion) {
        // Compare-and-set in a single document operation: the filter carries the
        // client's revision, so a concurrent writer makes this match nothing.
        // Type and identity document are never part of the update.
        var filter = query(
                where(CustomerDocument.ID).is(customer.getId().toString())
                        .and(CustomerDocument.VERSION).is(expectedVersion)
        );
        var changes = new Update()
                .set(CustomerDocument.NAME, customer.getName())
                .set(CustomerDocument.PROFILE, customer.getProfile())
                .set(CustomerDocument.STATUS, customer.getStatus())
                .set(CustomerDocument.UPDATED_AT, customer.getUpdatedAt())
                .inc(CustomerDocument.VERSION, 1);

        return mongoTemplate.findAndModify(
                        filter,
                        changes,
                        FindAndModifyOptions.options().returnNew(true),
                        CustomerDocument.class
                )
                .map(mapper::toDomain);
    }
}
