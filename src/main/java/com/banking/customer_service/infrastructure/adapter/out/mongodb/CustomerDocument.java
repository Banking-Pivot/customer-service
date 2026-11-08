package com.banking.customer_service.infrastructure.adapter.out.mongodb;

import com.banking.customer_service.domain.model.CustomerProfile;
import com.banking.customer_service.domain.model.CustomerStatus;
import com.banking.customer_service.domain.model.CustomerType;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Persistence shape of a customer in {@code customers_db.customers}.
 *
 * <p>The unique compound index over the normalized identity document is the
 * single source of truth for identity uniqueness; it also covers inactive
 * customers because deactivation never removes the record.
 * {@code version} is the optimistic-locking revision exposed as ETag.
 */
@Document(collection = CustomerDocument.COLLECTION)
@CompoundIndex(
        name = CustomerDocument.IDENTITY_INDEX,
        def = "{'documentType': 1, 'documentNumber': 1}",
        unique = true
)
public class CustomerDocument {

    public static final String COLLECTION = "customers";
    public static final String IDENTITY_INDEX = "uk_customer_identity";

    static final String ID = "id";
    static final String TYPE = "type";
    static final String PROFILE = "profile";
    static final String DOCUMENT_TYPE = "documentType";
    static final String DOCUMENT_NUMBER = "documentNumber";
    static final String NAME = "name";
    static final String STATUS = "status";
    static final String UPDATED_AT = "updatedAt";
    static final String VERSION = "version";

    @Id
    private String id;
    private CustomerType type;
    private CustomerProfile profile;
    private String documentType;
    private String documentNumber;
    private String name;
    private CustomerStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    @Version
    private Long version;

    public CustomerDocument(
            String id,
            CustomerType type,
            CustomerProfile profile,
            String documentType,
            String documentNumber,
            String name,
            CustomerStatus status,
            Instant createdAt,
            Instant updatedAt,
            Long version
    ) {
        this.id = id;
        this.type = type;
        this.profile = profile;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.name = name;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public CustomerType getType() {
        return type;
    }

    public CustomerProfile getProfile() {
        return profile;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public String getName() {
        return name;
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}
