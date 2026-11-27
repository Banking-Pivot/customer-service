package com.banking.customerservice.infrastructure.adapter.out.mongodb;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import com.banking.customerservice.application.exception.DuplicateCustomerDocumentException;
import com.banking.customerservice.application.port.out.CustomerWritePort;
import com.banking.customerservice.domain.model.Customer;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Write side over the {@code customers} collection. Inserts rely on the unique identity index to
 * reject duplicates, and updates are a compare-and-set on the revision.
 */
@Repository
public class CustomerMongoWriteAdapter implements CustomerWritePort {

  private final CustomerMongoRepository repository;
  private final ReactiveMongoTemplate mongoTemplate;
  private final CustomerMongoMapper mapper;

  /**
   * Receives the repository for inserts and lookups by ID and the template for the conditional
   * update.
   */
  public CustomerMongoWriteAdapter(
      CustomerMongoRepository repository,
      ReactiveMongoTemplate mongoTemplate,
      CustomerMongoMapper mapper) {
    this.repository = repository;
    this.mongoTemplate = mongoTemplate;
    this.mapper = mapper;
  }

  @Override
  public Mono<Customer> insert(Customer customer) {
    // insert (not save) so an existing _id is never overwritten; @Version starts at 0.
    return repository
        .insert(mapper.toDocument(customer))
        .map(mapper::toDomain)
        .onErrorMap(DuplicateKeyException.class, DuplicateCustomerDocumentException::new);
  }

  @Override
  public Mono<Customer> findById(UUID customerId) {
    return repository.findById(customerId.toString()).map(mapper::toDomain);
  }

  @Override
  public Mono<Customer> update(Customer customer, long expectedVersion) {
    // Compare-and-set in a single document operation: the filter carries the
    // client's revision, so a concurrent writer makes this match nothing.
    // Type and identity document are never part of the update.
    var filter =
        query(
            where(CustomerDocument.FIELD_ID)
                .is(customer.getId().toString())
                .and(CustomerDocument.FIELD_VERSION)
                .is(expectedVersion));
    var changes =
        new Update()
            .set(CustomerDocument.FIELD_NAME, customer.getName())
            .set(CustomerDocument.FIELD_PROFILE, customer.getProfile())
            .set(CustomerDocument.FIELD_STATUS, customer.getStatus())
            .set(CustomerDocument.FIELD_UPDATED_AT, customer.getUpdatedAt())
            .inc(CustomerDocument.FIELD_VERSION, 1);

    return mongoTemplate
        .findAndModify(
            filter, changes, FindAndModifyOptions.options().returnNew(true), CustomerDocument.class)
        .map(mapper::toDomain);
  }
}
