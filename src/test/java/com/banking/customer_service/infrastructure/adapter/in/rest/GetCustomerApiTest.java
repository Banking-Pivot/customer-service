package com.banking.customer_service.infrastructure.adapter.in.rest;

import com.banking.customer_service.support.MongoIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

/** GET /customers/{customerId} over HTTP against MongoDB. */
class GetCustomerApiTest extends MongoIntegrationTest {

    @Test
    void returnsActiveCustomerWithETag() {
        Created created = createCustomer("BUSINESS", "SME", "RUC", "20123456789", "Bodega SAC");

        webTestClient.get().uri("/customers/{id}", created.id())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("ETag", "\"0\"")
                .expectBody()
                .jsonPath("$.id").isEqualTo(created.id())
                .jsonPath("$.type").isEqualTo("BUSINESS")
                .jsonPath("$.profile").isEqualTo("SME")
                .jsonPath("$.documentType").isEqualTo("RUC")
                .jsonPath("$.documentNumber").isEqualTo("20123456789")
                .jsonPath("$.name").isEqualTo("Bodega SAC")
                .jsonPath("$.status").isEqualTo("ACTIVE");
    }

    @Test
    void returnsInactiveCustomerWithItsCurrentETag() {
        Created created = createPersonal("12345678");
        webTestClient.delete().uri("/customers/{id}", created.id())
                .header("If-Match", created.etag())
                .exchange()
                .expectStatus().isNoContent();

        webTestClient.get().uri("/customers/{id}", created.id())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("ETag", "\"1\"")
                .expectBody()
                .jsonPath("$.status").isEqualTo("INACTIVE");
    }

    @Test
    void returns404ForAValidUuidThatDoesNotExist() {
        webTestClient.get().uri("/customers/{id}", UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("CUSTOMER_NOT_FOUND");
    }

    @Test
    void returns400ForAMalformedUuid() {
        webTestClient.get().uri("/customers/not-a-uuid")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_REQUEST");
    }
}
