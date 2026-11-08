package com.banking.customer_service.infrastructure.adapter.out.mongodb;

import com.banking.customer_service.application.model.CustomerView;
import com.banking.customer_service.domain.model.Customer;
import com.banking.customer_service.domain.model.IdentityDocument;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CustomerMongoMapper {

    public CustomerDocument toDocument(Customer customer) {
        return new CustomerDocument(
                customer.getId().toString(),
                customer.getType(),
                customer.getProfile(),
                customer.getIdentityDocument().type(),
                customer.getIdentityDocument().number(),
                customer.getName(),
                customer.getStatus(),
                customer.getCreatedAt(),
                customer.getUpdatedAt(),
                customer.getVersion()
        );
    }

    /** Write side: rebuilds the aggregate so its rules apply to the next change. */
    public Customer toDomain(CustomerDocument document) {
        return Customer.restore(
                UUID.fromString(document.getId()),
                document.getType(),
                document.getProfile(),
                new IdentityDocument(document.getDocumentType(), document.getDocumentNumber()),
                document.getName(),
                document.getStatus(),
                document.getCreatedAt(),
                document.getUpdatedAt(),
                document.getVersion()
        );
    }

    /** Read side: projects stored data straight into the view, without the aggregate. */
    public CustomerView toView(CustomerDocument document) {
        return new CustomerView(
                UUID.fromString(document.getId()),
                document.getType(),
                document.getProfile(),
                document.getDocumentType(),
                document.getDocumentNumber(),
                document.getName(),
                document.getStatus(),
                document.getCreatedAt(),
                document.getUpdatedAt(),
                document.getVersion()
        );
    }
}
