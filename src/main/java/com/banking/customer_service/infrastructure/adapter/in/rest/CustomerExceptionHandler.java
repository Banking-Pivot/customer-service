package com.banking.customer_service.infrastructure.adapter.in.rest;

import com.banking.customer_service.application.exception.CustomerConcurrentModificationException;
import com.banking.customer_service.application.exception.CustomerNotFoundException;
import com.banking.customer_service.application.exception.DuplicateCustomerDocumentException;
import com.banking.customer_service.application.exception.InactiveCustomerException;
import com.banking.customer_service.domain.exception.InvalidCustomerException;
import com.banking.customer_service.infrastructure.adapter.in.rest.generated.model.ErrorResponse;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.MissingRequestValueException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.stream.Collectors;

/**
 * Maps every failure to the contract's ErrorResponse with a stable code.
 *
 * <p>Also receives errors raised before the delegate runs (415 from request
 * mapping, 428 from a missing If-Match, 400 from parameter validation),
 * because WebFlux routes dispatch errors to {@code @ExceptionHandler} methods.
 * Messages never echo request values, to keep identity data out of responses.
 */
@Slf4j
@RestControllerAdvice
public class CustomerExceptionHandler {

    private final Clock clock;

    public CustomerExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            CustomerNotFoundException exception,
            ServerWebExchange exchange
    ) {
        return error(HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND",
                "The customer does not exist.", exchange);
    }

    @ExceptionHandler(DuplicateCustomerDocumentException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(
            DuplicateCustomerDocumentException exception,
            ServerWebExchange exchange
    ) {
        return error(HttpStatus.CONFLICT, "CUSTOMER_DOCUMENT_ALREADY_EXISTS",
                "A customer with this identity document already exists.", exchange);
    }

    @ExceptionHandler(InactiveCustomerException.class)
    public ResponseEntity<ErrorResponse> handleInactive(
            InactiveCustomerException exception,
            ServerWebExchange exchange
    ) {
        return error(HttpStatus.CONFLICT, "CUSTOMER_INACTIVE",
                "Inactive customers cannot be updated.", exchange);
    }

    @ExceptionHandler(CustomerConcurrentModificationException.class)
    public ResponseEntity<ErrorResponse> handleVersionConflict(
            CustomerConcurrentModificationException exception,
            ServerWebExchange exchange
    ) {
        return error(HttpStatus.PRECONDITION_FAILED, "CUSTOMER_VERSION_CONFLICT",
                "The customer changed. Read it again before retrying.", exchange);
    }

    @ExceptionHandler(InvalidCustomerException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(
            InvalidCustomerException exception,
            ServerWebExchange exchange
    ) {
        // Domain messages name fields and enum values only, never the submitted text.
        return error(HttpStatus.UNPROCESSABLE_CONTENT, "CUSTOMER_RULE_VIOLATION",
                exception.getMessage(), exchange);
    }

    @ExceptionHandler(MissingRequestValueException.class)
    public ResponseEntity<ErrorResponse> handleMissingValue(
            MissingRequestValueException exception,
            ServerWebExchange exchange
    ) {
        if (HttpHeaders.IF_MATCH.equalsIgnoreCase(exception.getName())) {
            return error(HttpStatus.PRECONDITION_REQUIRED, "IF_MATCH_REQUIRED",
                    "If-Match header is required. Send the ETag from your last read.", exchange);
        }
        return invalidRequest("The request is missing a required value.", exchange);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBody(
            WebExchangeBindException exception,
            ServerWebExchange exchange
    ) {
        String fields = exception.getFieldErrors().stream()
                .map(FieldError::getField)
                .distinct()
                .sorted()
                .collect(Collectors.joining(", "));
        return invalidRequest("Invalid or missing fields: " + fields + ".", exchange);
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInput(
            ServerWebInputException exception,
            ServerWebExchange exchange
    ) {
        return invalidRequest(
                "The request is malformed or contains invalid, unknown or missing fields.",
                exchange
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            ServerWebExchange exchange
    ) {
        boolean ifMatch = exception.getConstraintViolations().stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().endsWith(".ifMatch"));
        return ifMatch ? invalidIfMatch(exchange) : invalidRequest(
                "The request contains invalid parameters.", exchange);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(
            HandlerMethodValidationException exception,
            ServerWebExchange exchange
    ) {
        if (exception.isForReturnValue()) {
            return unexpected(exception, exchange);
        }
        boolean ifMatch = exception.getParameterValidationResults().stream()
                .anyMatch(result -> isIfMatchHeader(result.getMethodParameter()));
        return ifMatch ? invalidIfMatch(exchange) : invalidRequest(
                "The request contains invalid parameters.", exchange);
    }

    @ExceptionHandler(UnsupportedMediaTypeStatusException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(
            UnsupportedMediaTypeStatusException exception,
            ServerWebExchange exchange
    ) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                "Request body must use application/json.", exchange);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleHttpError(
            ResponseStatusException exception,
            ServerWebExchange exchange
    ) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        if (status == null || status.is5xxServerError()) {
            return unexpected(exception, exchange);
        }
        return error(status, status.name(), status.getReasonPhrase() + ".", exchange);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception exception,
            ServerWebExchange exchange
    ) {
        return unexpected(exception, exchange);
    }

    private ResponseEntity<ErrorResponse> unexpected(Exception exception, ServerWebExchange exchange) {
        // Avoid logging payloads or database messages containing identity data.
        log.error(
                "Unhandled customer request failure. requestId={}, exceptionType={}",
                exchange.getRequest().getId(),
                exception.getClass().getName()
        );
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "The request could not be completed.", exchange);
    }

    private ResponseEntity<ErrorResponse> invalidIfMatch(ServerWebExchange exchange) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_IF_MATCH",
                "If-Match must be exactly one quoted numeric ETag, for example \"3\".", exchange);
    }

    private ResponseEntity<ErrorResponse> invalidRequest(String message, ServerWebExchange exchange) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message, exchange);
    }

    private static boolean isIfMatchHeader(MethodParameter parameter) {
        RequestHeader header = parameter.getParameterAnnotation(RequestHeader.class);
        return header != null && HttpHeaders.IF_MATCH.equalsIgnoreCase(
                header.value().isEmpty() ? header.name() : header.value()
        );
    }

    private ResponseEntity<ErrorResponse> error(
            HttpStatus status,
            String code,
            String message,
            ServerWebExchange exchange
    ) {
        ErrorResponse response = new ErrorResponse()
                .code(code)
                .message(message)
                .timestamp(OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC))
                .path(exchange.getRequest().getPath().value())
                .traceId(exchange.getRequest().getId());

        return ResponseEntity.status(status).body(response);
    }
}
