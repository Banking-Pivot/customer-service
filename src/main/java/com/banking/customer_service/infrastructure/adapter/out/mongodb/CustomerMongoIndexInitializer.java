package com.banking.customer_service.infrastructure.adapter.out.mongodb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.core.index.ReactiveIndexOperations;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

/**
 * Creates the indexes declared on {@link CustomerDocument} and verifies that the
 * unique identity index exists before the HTTP server starts.
 *
 * <p>Runs in {@link SmartInitializingSingleton}, which completes before the web
 * server begins accepting requests, so no request can reach MongoDB without the
 * index. Blocking here happens once at startup, never on the HTTP path. Startup
 * fails if the index cannot be created, for example because stored data already
 * contains duplicated identity documents or a conflicting index exists.
 */
@Slf4j
@Component
public class CustomerMongoIndexInitializer implements SmartInitializingSingleton {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final List<String> IDENTITY_FIELDS = List.of(
            CustomerDocument.DOCUMENT_TYPE,
            CustomerDocument.DOCUMENT_NUMBER
    );

    private final ReactiveMongoTemplate mongoTemplate;

    public CustomerMongoIndexInitializer(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void afterSingletonsInstantiated() {
        ReactiveIndexOperations indexOps = mongoTemplate.indexOps(CustomerDocument.class);
        IndexResolver resolver = IndexResolver.create(
                mongoTemplate.getConverter().getMappingContext()
        );

        Boolean identityIndexPresent = Flux
                .fromIterable(resolver.resolveIndexFor(CustomerDocument.class))
                .concatMap(indexOps::createIndex)
                .thenMany(indexOps.getIndexInfo())
                .any(index -> index.isUnique() && index.isIndexForFields(IDENTITY_FIELDS))
                .block(TIMEOUT);

        if (!Boolean.TRUE.equals(identityIndexPresent)) {
            throw new IllegalStateException(
                    "Unique index on customers(documentType, documentNumber) is missing."
            );
        }

        log.info(
                "Verified unique index {} on {}({}).",
                CustomerDocument.IDENTITY_INDEX,
                CustomerDocument.COLLECTION,
                String.join(", ", IDENTITY_FIELDS)
        );
    }
}
