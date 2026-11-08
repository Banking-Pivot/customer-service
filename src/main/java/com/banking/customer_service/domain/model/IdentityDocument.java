package com.banking.customer_service.domain.model;

import com.banking.customer_service.domain.exception.InvalidCustomerException;

import java.util.Locale;

/**
 * Identity document of a customer, always held in its normalized form.
 *
 * <p>Normalization removes surrounding whitespace and converts letters to
 * uppercase with {@link Locale#ROOT}, so the result does not depend on the
 * server locale. Internal characters and leading zeros are preserved.
 * Two documents are the same identity when their normalized values are equal.
 */
public record IdentityDocument(String type, String number) {

    public static final int MAX_TYPE_LENGTH = 24;
    public static final int MAX_NUMBER_LENGTH = 32;

    public IdentityDocument {
        type = normalize(type, MAX_TYPE_LENGTH, "Document type");
        number = normalize(number, MAX_NUMBER_LENGTH, "Document number");
    }

    private static String normalize(String value, int maxLength, String field) {
        if (value == null) {
            throw new InvalidCustomerException(field + " is required.");
        }

        String normalized = value.strip().toUpperCase(Locale.ROOT);

        if (normalized.isEmpty() || normalized.length() > maxLength) {
            throw new InvalidCustomerException(field + " is invalid.");
        }
        return normalized;
    }
}
