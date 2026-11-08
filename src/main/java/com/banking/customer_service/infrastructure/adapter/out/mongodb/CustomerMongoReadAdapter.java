package com.banking.customer_service.infrastructure.adapter.out.mongodb;

import com.banking.customer_service.application.model.CustomerView;
import com.banking.customer_service.application.port.out.CustomerReadPort;
import com.banking.customer_service.application.query.ListCustomersQuery;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Read model over the same {@code customers} collection used by the write side.
 * Projects documents into views without rebuilding the aggregate.
 */
@Repository
public class CustomerMongoReadAdapter implements CustomerReadPort {

    private final CustomerMongoRepository repository;
    private final ReactiveMongoTemplate mongoTemplate;
    private final CustomerMongoMapper mapper;

    public CustomerMongoReadAdapter(
            CustomerMongoRepository repository,
            ReactiveMongoTemplate mongoTemplate,
            CustomerMongoMapper mapper
    ) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.mapper = mapper;
    }

    @Override
    public Mono<CustomerView> findById(UUID customerId) {
        return repository.findById(customerId.toString())
                .map(mapper::toView);
    }

    @Override
    public Flux<CustomerView> findPage(ListCustomersQuery query) {
        // _id holds the canonical lowercase UUID string, so ascending _id is
        // UUID ascending (unsigned 128-bit order) and uses the default _id index.
        Query pageQuery = filter(query)
                .with(Sort.by(Sort.Direction.ASC, CustomerDocument.ID))
                .skip(query.offset())
                .limit(query.size());

        return mongoTemplate.find(pageQuery, CustomerDocument.class)
                .map(mapper::toView);
    }

    @Override
    public Mono<Long> count(ListCustomersQuery query) {
        return mongoTemplate.count(filter(query), CustomerDocument.class);
    }

    private static Query filter(ListCustomersQuery query) {
        Query filter = new Query();
        if (query.type() != null) {
            filter.addCriteria(Criteria.where(CustomerDocument.TYPE).is(query.type()));
        }
        if (query.status() != null) {
            filter.addCriteria(Criteria.where(CustomerDocument.STATUS).is(query.status()));
        }
        return filter;
    }
}
