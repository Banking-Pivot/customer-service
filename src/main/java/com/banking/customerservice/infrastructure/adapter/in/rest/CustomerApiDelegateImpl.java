package com.banking.customerservice.infrastructure.adapter.in.rest;

import com.banking.customerservice.application.command.DeactivateCustomerCommand;
import com.banking.customerservice.application.exception.CustomerConcurrentModificationException;
import com.banking.customerservice.application.model.CustomerView;
import com.banking.customerservice.application.port.in.CreateCustomerUseCase;
import com.banking.customerservice.application.port.in.DeactivateCustomerUseCase;
import com.banking.customerservice.application.port.in.GetCustomerUseCase;
import com.banking.customerservice.application.port.in.ListCustomersUseCase;
import com.banking.customerservice.application.port.in.UpdateCustomerUseCase;
import com.banking.customerservice.application.query.GetCustomerQuery;
import com.banking.customerservice.infrastructure.adapter.in.rest.generated.api.CustomersApiDelegate;
import com.banking.customerservice.infrastructure.adapter.in.rest.generated.model.CreateCustomerRequest;
import com.banking.customerservice.infrastructure.adapter.in.rest.generated.model.CustomerPageResponse;
import com.banking.customerservice.infrastructure.adapter.in.rest.generated.model.CustomerResponse;
import com.banking.customerservice.infrastructure.adapter.in.rest.generated.model.CustomerStatus;
import com.banking.customerservice.infrastructure.adapter.in.rest.generated.model.CustomerType;
import com.banking.customerservice.infrastructure.adapter.in.rest.generated.model.UpdateCustomerRequest;
import java.net.URI;
import java.util.OptionalLong;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Implements the contract-generated delegate. The generated {@code CustomersApiController} receives
 * HTTP requests, validates parameters and calls these methods; this class only translates and
 * chooses status codes.
 */
@Component
public class CustomerApiDelegateImpl implements CustomersApiDelegate {

  private final CreateCustomerUseCase createCustomerUseCase;
  private final GetCustomerUseCase getCustomerUseCase;
  private final ListCustomersUseCase listCustomersUseCase;
  private final UpdateCustomerUseCase updateCustomerUseCase;
  private final DeactivateCustomerUseCase deactivateCustomerUseCase;
  private final CustomerRestMapper mapper;

  /**
   * Receives the five customer use cases and the mapper between REST DTOs and application records.
   */
  public CustomerApiDelegateImpl(
      CreateCustomerUseCase createCustomerUseCase,
      GetCustomerUseCase getCustomerUseCase,
      ListCustomersUseCase listCustomersUseCase,
      UpdateCustomerUseCase updateCustomerUseCase,
      DeactivateCustomerUseCase deactivateCustomerUseCase,
      CustomerRestMapper mapper) {
    this.createCustomerUseCase = createCustomerUseCase;
    this.getCustomerUseCase = getCustomerUseCase;
    this.listCustomersUseCase = listCustomersUseCase;
    this.updateCustomerUseCase = updateCustomerUseCase;
    this.deactivateCustomerUseCase = deactivateCustomerUseCase;
    this.mapper = mapper;
  }

  @Override
  public Mono<ResponseEntity<CustomerResponse>> createCustomer(
      Mono<CreateCustomerRequest> request, ServerWebExchange exchange) {
    return request
        .map(mapper::toCommand)
        .flatMap(createCustomerUseCase::execute)
        .map(
            view ->
                ResponseEntity.created(URI.create("/api/v1/customers/" + view.id()))
                    .eTag(mapper.toEtag(view.version()))
                    .body(mapper.toResponse(view)));
  }

  @Override
  public Mono<ResponseEntity<CustomerResponse>> getCustomer(
      UUID customerId, ServerWebExchange exchange) {
    return getCustomerUseCase.execute(new GetCustomerQuery(customerId)).map(this::okWithEtag);
  }

  @Override
  public Mono<ResponseEntity<CustomerPageResponse>> listCustomers(
      Integer page,
      Integer size,
      CustomerType type,
      CustomerStatus status,
      ServerWebExchange exchange) {
    return Mono.fromSupplier(() -> mapper.toQuery(page, size, type, status))
        .flatMap(listCustomersUseCase::execute)
        .map(result -> ResponseEntity.ok(mapper.toPageResponse(result)));
  }

  @Override
  public Mono<ResponseEntity<CustomerResponse>> updateCustomer(
      UUID customerId,
      String ifMatch,
      Mono<UpdateCustomerRequest> request,
      ServerWebExchange exchange) {
    return request
        .flatMap(
            body -> {
              OptionalLong expectedVersion = mapper.toExpectedVersion(ifMatch);
              if (expectedVersion.isEmpty()) {
                return unmatchableRevision(customerId);
              }
              return updateCustomerUseCase.execute(
                  mapper.toCommand(customerId, expectedVersion.getAsLong(), body));
            })
        .map(this::okWithEtag);
  }

  @Override
  public Mono<ResponseEntity<Void>> deactivateCustomer(
      UUID customerId, String ifMatch, ServerWebExchange exchange) {
    ResponseEntity<Void> noContent = ResponseEntity.noContent().build();

    return Mono.defer(
            () -> {
              OptionalLong expectedVersion = mapper.toExpectedVersion(ifMatch);
              if (expectedVersion.isEmpty()) {
                return unmatchableRevision(customerId).then();
              }
              return deactivateCustomerUseCase.execute(
                  new DeactivateCustomerCommand(customerId, expectedVersion.getAsLong()));
            })
        .thenReturn(noContent);
  }

  private ResponseEntity<CustomerResponse> okWithEtag(CustomerView view) {
    return ResponseEntity.ok().eTag(mapper.toEtag(view.version())).body(mapper.toResponse(view));
  }

  /**
   * A well-formed tag that no revision can equal: keep 404 precedence for a missing customer,
   * otherwise the precondition fails (412).
   */
  private Mono<CustomerView> unmatchableRevision(UUID customerId) {
    return getCustomerUseCase
        .execute(new GetCustomerQuery(customerId))
        .then(Mono.error(() -> new CustomerConcurrentModificationException(customerId)));
  }
}
