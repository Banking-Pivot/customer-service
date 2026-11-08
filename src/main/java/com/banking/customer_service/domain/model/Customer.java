package com.banking.customer_service.domain.model;

import com.banking.customer_service.domain.exception.InvalidCustomerException;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Customer aggregate.
 *
 * <p>Type and identity document are immutable. Only name and profile can be
 * edited, and only while the customer is active. Deactivation is logical:
 * the record and its identity document are kept.
 *
 * <p>{@code version} is the persisted revision the aggregate was loaded at.
 * The domain never increments it; the persistence adapter does so atomically
 * when it writes a change.
 */
@Getter
public final class Customer {

    public static final int MAX_NAME_LENGTH = 120;

    private final UUID id;
    private final CustomerType type;
    private final IdentityDocument identityDocument;
    private final Instant createdAt;
    private final long version;

    private CustomerProfile profile;
    private String name;
    private CustomerStatus status;
    private Instant updatedAt;

    private Customer(
            UUID id,
            CustomerType type,
            CustomerProfile profile,
            IdentityDocument identityDocument,
            String name,
            CustomerStatus status,
            Instant createdAt,
            Instant updatedAt,
            long version
    ) {
        this.id = Objects.requireNonNull(id, "Customer ID is required.");
        this.type = Objects.requireNonNull(type, "Customer type is required.");
        this.identityDocument = Objects.requireNonNull(
                identityDocument, "Identity document is required."
        );
        this.status = Objects.requireNonNull(status, "Customer status is required.");
        this.createdAt = Objects.requireNonNull(
                createdAt, "Creation timestamp is required."
        );
        this.updatedAt = Objects.requireNonNull(
                updatedAt, "Update timestamp is required."
        );

        if (updatedAt.isBefore(createdAt)) {
            throw new InvalidCustomerException(
                    "Update timestamp cannot precede creation timestamp."
            );
        }

        if (version < 0) {
            throw new IllegalArgumentException("Version must not be negative.");
        }

        validateProfile(profile);

        this.profile = profile;
        this.name = normalizeName(name);
        this.version = version;
    }

    /**
     * Registers a new customer. New customers are always ACTIVE and start at
     * revision 0 with equal creation and update timestamps.
     */
    public static Customer create(
            UUID id,
            CustomerType type,
            CustomerProfile profile,
            IdentityDocument identityDocument,
            String name,
            Instant now
    ) {
        return new Customer(
                id,
                type,
                profile,
                identityDocument,
                name,
                CustomerStatus.ACTIVE,
                now,
                now,
                0L
        );
    }

    /**
     * Rebuilds a customer from persisted state, validating the same invariants.
     */
    public static Customer restore(
            UUID id,
            CustomerType type,
            CustomerProfile profile,
            IdentityDocument identityDocument,
            String name,
            CustomerStatus status,
            Instant createdAt,
            Instant updatedAt,
            long version
    ) {
        return new Customer(
                id,
                type,
                profile,
                identityDocument,
                name,
                status,
                createdAt,
                updatedAt,
                version
        );
    }

    public boolean isActive() {
        return status == CustomerStatus.ACTIVE;
    }

    /**
     * Replaces the editable information of an active customer.
     *
     * @return {@code true} if name or profile changed; {@code false} if the
     * requested values were already in place, in which case nothing changes.
     * @throws IllegalStateException if the customer is inactive; callers must
     * check {@link #isActive()} first and report the conflict themselves.
     */
    public boolean updateInformation(
            String newName,
            CustomerProfile newProfile,
            Instant now
    ) {
        if (!isActive()) {
            throw new IllegalStateException("Inactive customers cannot be updated.");
        }

        // Validate every value before modifying the current state.
        String normalizedName = normalizeName(newName);
        validateProfile(newProfile);

        if (name.equals(normalizedName) && profile == newProfile) {
            return false;
        }

        validateTimestamp(now);

        this.name = normalizedName;
        this.profile = newProfile;
        this.updatedAt = now;
        return true;
    }

    /**
     * Deactivates the customer while preserving identity and history.
     *
     * @return {@code true} if the customer was active; {@code false} if it was
     * already inactive, in which case nothing changes.
     */
    public boolean deactivate(Instant now) {
        if (!isActive()) {
            return false;
        }

        validateTimestamp(now);

        this.status = CustomerStatus.INACTIVE;
        this.updatedAt = now;
        return true;
    }

    private void validateProfile(CustomerProfile candidate) {
        if (candidate == null) {
            throw new InvalidCustomerException("Customer profile is required.");
        }

        boolean compatible = switch (type) {
            case PERSONAL -> candidate == CustomerProfile.STANDARD
                    || candidate == CustomerProfile.VIP;
            case BUSINESS -> candidate == CustomerProfile.STANDARD
                    || candidate == CustomerProfile.SME;
        };

        if (!compatible) {
            throw new InvalidCustomerException(
                    "Customer profile " + candidate
                            + " is incompatible with customer type " + type + "."
            );
        }
    }

    private void validateTimestamp(Instant timestamp) {
        if (timestamp == null || timestamp.isBefore(updatedAt)) {
            throw new InvalidCustomerException(
                    "Operation timestamp cannot precede the last update."
            );
        }
    }

    private static String normalizeName(String value) {
        String normalized = value == null ? null : value.strip();

        if (normalized == null || normalized.isEmpty()
                || normalized.length() > MAX_NAME_LENGTH) {
            throw new InvalidCustomerException("Name is invalid.");
        }
        return normalized;
    }
}
