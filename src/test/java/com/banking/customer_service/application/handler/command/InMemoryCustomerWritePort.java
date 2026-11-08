package com.banking.customer_service.application.handler.command;

import com.banking.customer_service.application.port.out.CustomerWritePort;
import com.banking.customer_service.domain.model.Customer;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Write port that mimics the MongoDB adapter: loads return copies and
 * {@link #update} is a compare-and-set on the stored revision.
 */
final class InMemoryCustomerWritePort implements CustomerWritePort {

    private final Map<UUID, Customer> store = new ConcurrentHashMap<>();
    final List<Long> updateConditions = new ArrayList<>();
    private Runnable afterLoad = () -> { };

    void save(Customer customer) {
        store.put(customer.getId(), copy(customer, customer.getVersion()));
    }

    Customer stored(UUID id) {
        return store.get(id);
    }

    /** Simulates another writer acting between the handler's read and write. */
    void afterNextLoad(Runnable action) {
        this.afterLoad = action;
    }

    @Override
    public Mono<Customer> insert(Customer customer) {
        save(customer);
        return Mono.just(copy(customer, customer.getVersion()));
    }

    @Override
    public Mono<Customer> findById(UUID customerId) {
        return Mono.fromSupplier(() -> {
            Customer current = store.get(customerId);
            Customer loaded = current == null ? null : copy(current, current.getVersion());
            Runnable action = afterLoad;
            afterLoad = () -> { };
            action.run();
            return loaded;
        });
    }

    @Override
    public synchronized Mono<Customer> update(Customer customer, long expectedVersion) {
        updateConditions.add(expectedVersion);
        Customer current = store.get(customer.getId());
        if (current == null || current.getVersion() != expectedVersion) {
            return Mono.empty();
        }
        Customer saved = copy(customer, expectedVersion + 1);
        store.put(saved.getId(), saved);
        return Mono.just(copy(saved, saved.getVersion()));
    }

    private static Customer copy(Customer customer, long version) {
        return Customer.restore(
                customer.getId(),
                customer.getType(),
                customer.getProfile(),
                customer.getIdentityDocument(),
                customer.getName(),
                customer.getStatus(),
                customer.getCreatedAt(),
                customer.getUpdatedAt(),
                version
        );
    }
}
