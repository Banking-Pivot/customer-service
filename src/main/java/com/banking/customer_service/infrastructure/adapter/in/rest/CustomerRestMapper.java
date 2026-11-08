package com.banking.customer_service.infrastructure.adapter.in.rest;

import com.banking.customer_service.application.command.CreateCustomerCommand;
import com.banking.customer_service.application.command.UpdateCustomerCommand;
import com.banking.customer_service.application.model.CustomerPage;
import com.banking.customer_service.application.model.CustomerView;
import com.banking.customer_service.application.query.ListCustomersQuery;
import com.banking.customer_service.domain.model.CustomerProfile;
import com.banking.customer_service.domain.model.CustomerStatus;
import com.banking.customer_service.domain.model.CustomerType;
import com.banking.customer_service.infrastructure.adapter.in.rest.generated.model.CreateCustomerRequest;
import com.banking.customer_service.infrastructure.adapter.in.rest.generated.model.CustomerPageResponse;
import com.banking.customer_service.infrastructure.adapter.in.rest.generated.model.CustomerResponse;
import com.banking.customer_service.infrastructure.adapter.in.rest.generated.model.UpdateCustomerRequest;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebInputException;

import java.time.ZoneOffset;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Translates between generated REST DTOs and application records, and between
 * revisions and strong ETags ({@code "3"}). Generated enums and domain enums
 * share names, so conversion goes through {@code name()}.
 */
@Component
public class CustomerRestMapper {

    private static final Pattern STRONG_NUMERIC_ETAG = Pattern.compile("^\"([0-9]+)\"$");

    public CreateCustomerCommand toCommand(CreateCustomerRequest request) {
        return new CreateCustomerCommand(
                CustomerType.valueOf(request.getType().name()),
                CustomerProfile.valueOf(request.getProfile().name()),
                request.getDocumentType(),
                request.getDocumentNumber(),
                request.getName()
        );
    }

    public UpdateCustomerCommand toCommand(
            UUID customerId,
            long expectedVersion,
            UpdateCustomerRequest request
    ) {
        return new UpdateCustomerCommand(
                customerId,
                request.getName(),
                CustomerProfile.valueOf(request.getProfile().name()),
                expectedVersion
        );
    }

    public ListCustomersQuery toQuery(
            Integer page,
            Integer size,
            com.banking.customer_service.infrastructure.adapter.in.rest.generated.model.CustomerType type,
            com.banking.customer_service.infrastructure.adapter.in.rest.generated.model.CustomerStatus status
    ) {
        return new ListCustomersQuery(
                page == null ? ListCustomersQuery.DEFAULT_PAGE : page,
                size == null ? ListCustomersQuery.DEFAULT_SIZE : size,
                type == null ? null : CustomerType.valueOf(type.name()),
                status == null ? null : CustomerStatus.valueOf(status.name())
        );
    }

    public CustomerResponse toResponse(CustomerView view) {
        return new CustomerResponse()
                .id(view.id())
                .type(com.banking.customer_service.infrastructure.adapter.in.rest
                        .generated.model.CustomerType.valueOf(view.type().name()))
                .profile(com.banking.customer_service.infrastructure.adapter.in.rest
                        .generated.model.CustomerProfile.valueOf(view.profile().name()))
                .documentType(view.documentType())
                .documentNumber(view.documentNumber())
                .name(view.name())
                .status(com.banking.customer_service.infrastructure.adapter.in.rest
                        .generated.model.CustomerStatus.valueOf(view.status().name()))
                .createdAt(view.createdAt().atOffset(ZoneOffset.UTC))
                .updatedAt(view.updatedAt().atOffset(ZoneOffset.UTC));
    }

    public CustomerPageResponse toPageResponse(CustomerPage page) {
        return new CustomerPageResponse()
                .items(page.items().stream().map(this::toResponse).toList())
                .page(page.page())
                .size(page.size())
                .totalElements(page.totalElements())
                .totalPages(page.totalPages());
    }

    /** Strong entity tag for a revision, quoted as the contract requires. */
    public String toETag(long version) {
        return "\"" + version + "\"";
    }

    /**
     * Reads the revision from If-Match.
     *
     * @return the revision, or empty when the tag is well formed but can never
     * match one this API issued (leading zeros such as {@code "03"}, or a value
     * beyond the long range). Strong comparison is character by character, so
     * {@code "03"} does not match {@code "3"}: the caller must answer 412, not 400.
     * @throws ServerWebInputException when the header is not one quoted number.
     * The generated controller already validates the pattern; this is a guard.
     */
    public OptionalLong toExpectedVersion(String ifMatch) {
        var matcher = ifMatch == null ? null : STRONG_NUMERIC_ETAG.matcher(ifMatch);
        if (matcher == null || !matcher.matches()) {
            throw new ServerWebInputException("Malformed If-Match header.");
        }

        String digits = matcher.group(1);
        try {
            long version = Long.parseLong(digits);
            return toETag(version).equals(ifMatch) ? OptionalLong.of(version) : OptionalLong.empty();
        } catch (NumberFormatException beyondLongRange) {
            return OptionalLong.empty();
        }
    }
}
