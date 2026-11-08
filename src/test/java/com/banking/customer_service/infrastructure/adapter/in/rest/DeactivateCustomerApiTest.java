package com.banking.customer_service.infrastructure.adapter.in.rest;

import com.banking.customer_service.support.MongoIntegrationTest;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** DELETE /customers/{customerId} (logical deletion) over HTTP against MongoDB. */
class DeactivateCustomerApiTest extends MongoIntegrationTest {

    @Test
    void deactivatesAndKeepsTheRecord() {
        Created created = createPersonal("12345678");

        delete(created.id(), created.etag())
                .expectStatus().isNoContent()
                .expectBody().isEmpty();

        Document stored = stored(created.id());
        assertThat(stored).isNotNull();
        assertThat(stored.getString("status")).isEqualTo("INACTIVE");
        assertThat(stored.getString("documentType")).isEqualTo("DNI");
        assertThat(stored.getString("documentNumber")).isEqualTo("12345678");
        assertThat(stored.get("version", Number.class).longValue()).isEqualTo(1);
        assertThat(stored.get("updatedAt", Date.class)).isAfter(stored.get("createdAt", Date.class));
    }

    @Test
    void repeatingWithTheCurrentETagReturns204WithoutChanges() {
        Created created = createPersonal("12345678");
        delete(created.id(), "\"0\"").expectStatus().isNoContent();
        Document afterFirst = stored(created.id());

        delete(created.id(), "\"1\"").expectStatus().isNoContent();

        assertThat(stored(created.id())).isEqualTo(afterFirst);
    }

    @Test
    void repeatingWithTheOldETagReturns412() {
        Created created = createPersonal("12345678");
        delete(created.id(), "\"0\"").expectStatus().isNoContent();

        delete(created.id(), "\"0\"")
                .expectStatus().isEqualTo(412)
                .expectBody()
                .jsonPath("$.code").isEqualTo("CUSTOMER_VERSION_CONFLICT");
    }

    @Test
    void staleETagOnActiveCustomerReturns412AndKeepsItActive() {
        Created created = createPersonal("12345678");
        webTestClient.put().uri("/customers/{id}", created.id())
                .header("If-Match", "\"0\"")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"Ana\",\"profile\":\"VIP\"}")
                .exchange()
                .expectStatus().isOk();

        delete(created.id(), "\"0\"").expectStatus().isEqualTo(412);

        assertThat(stored(created.id()).getString("status")).isEqualTo("ACTIVE");
    }

    @Test
    void missingIfMatchReturns428() {
        Created created = createPersonal("12345678");

        webTestClient.delete().uri("/customers/{id}", created.id())
                .exchange()
                .expectStatus().isEqualTo(428)
                .expectBody()
                .jsonPath("$.code").isEqualTo("IF_MATCH_REQUIRED");

        assertThat(stored(created.id()).getString("status")).isEqualTo("ACTIVE");
    }

    @ParameterizedTest
    @ValueSource(strings = {"W/\"0\"", "*", "0"})
    void malformedIfMatchReturns400(String ifMatch) {
        Created created = createPersonal("12345678");

        delete(created.id(), ifMatch)
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_IF_MATCH");
    }

    @Test
    void unknownCustomerReturns404() {
        delete(UUID.randomUUID().toString(), "\"0\"")
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("CUSTOMER_NOT_FOUND");
    }

    @Test
    void identityDocumentStaysReservedAfterDeactivation() {
        Created created = createPersonal("12345678");
        delete(created.id(), created.etag()).expectStatus().isNoContent();

        webTestClient.post().uri("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"type":"PERSONAL","profile":"STANDARD","documentType":"dni",
                         "documentNumber":"12345678","name":"Otra Persona"}
                        """)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("CUSTOMER_DOCUMENT_ALREADY_EXISTS");

        assertThat(storedCount()).isEqualTo(1);
    }

    private WebTestClient.ResponseSpec delete(String id, String ifMatch) {
        return webTestClient.delete().uri("/customers/{id}", id)
                .header("If-Match", ifMatch)
                .exchange();
    }
}
