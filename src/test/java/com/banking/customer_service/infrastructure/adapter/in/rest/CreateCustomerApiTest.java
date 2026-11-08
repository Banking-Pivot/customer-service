package com.banking.customer_service.infrastructure.adapter.in.rest;

import com.banking.customer_service.support.MongoIntegrationTest;
import com.banking.customer_service.support.SteppingClockConfiguration;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** POST /customers over HTTP against MongoDB. */
class CreateCustomerApiTest extends MongoIntegrationTest {

    @Test
    void createsActiveCustomerWithNormalizedIdentityLocationAndETag() {
        var result = webTestClient.post().uri("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"type":"PERSONAL","profile":"VIP","documentType":"  dni ",
                         "documentNumber":" 00a-12 b\\t","name":"  Ana Pérez  "}
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueEquals("ETag", "\"0\"")
                .expectBody()
                .jsonPath("$.type").isEqualTo("PERSONAL")
                .jsonPath("$.profile").isEqualTo("VIP")
                .jsonPath("$.documentType").isEqualTo("DNI")
                .jsonPath("$.documentNumber").isEqualTo("00A-12 B")
                .jsonPath("$.name").isEqualTo("Ana Pérez")
                .jsonPath("$.status").isEqualTo("ACTIVE")
                .jsonPath("$.createdAt").isNotEmpty()
                .returnResult();

        String body = new String(result.getResponseBodyContent());
        String id = com.jayway.jsonpath.JsonPath.read(body, "$.id");
        String createdAt = com.jayway.jsonpath.JsonPath.read(body, "$.createdAt");
        String updatedAt = com.jayway.jsonpath.JsonPath.read(body, "$.updatedAt");

        assertThat(UUID.fromString(id).toString()).isEqualTo(id);
        assertThat(result.getResponseHeaders().getLocation()).hasToString("/customers/" + id);
        assertThat(createdAt).isEqualTo(updatedAt).endsWith("Z");

        Document stored = stored(id);
        assertThat(stored).isNotNull();
        assertThat(stored.getString("documentType")).isEqualTo("DNI");
        assertThat(stored.getString("documentNumber")).isEqualTo("00A-12 B");
        assertThat(stored.getString("name")).isEqualTo("Ana Pérez");
        assertThat(stored.getString("status")).isEqualTo("ACTIVE");
        assertThat(stored.get("version", Number.class).longValue()).isZero();
        assertThat(stored.get("createdAt", Date.class).toInstant())
                .isAfterOrEqualTo(SteppingClockConfiguration.START);
    }

    @Test
    void rejectsIdentityThatIsDuplicatedAfterNormalization() {
        createCustomer("PERSONAL", "STANDARD", "DNI", "12345678", "Ana Perez");

        webTestClient.post().uri("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"type":"BUSINESS","profile":"SME","documentType":" dni",
                         "documentNumber":"12345678 ","name":"Otra Empresa"}
                        """)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("CUSTOMER_DOCUMENT_ALREADY_EXISTS")
                .jsonPath("$.path").isEqualTo("/customers")
                .jsonPath("$.timestamp").isNotEmpty();

        assertThat(storedCount()).isEqualTo(1);
    }

    @Test
    void leadingZerosMakeADifferentIdentity() {
        createCustomer("PERSONAL", "STANDARD", "DNI", "12345678", "Ana Perez");
        createCustomer("PERSONAL", "STANDARD", "DNI", "012345678", "Luis Rojas");

        assertThat(storedCount()).isEqualTo(2);
    }

    @Test
    void acceptsEveryProfileAllowedForEachType() {
        createCustomer("PERSONAL", "STANDARD", "DNI", "1", "A");
        createCustomer("PERSONAL", "VIP", "DNI", "2", "B");
        createCustomer("BUSINESS", "STANDARD", "RUC", "3", "C");
        createCustomer("BUSINESS", "SME", "RUC", "4", "D");

        assertThat(storedCount()).isEqualTo(4);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PERSONAL:SME", "BUSINESS:VIP"})
    void rejectsIncompatibleTypeAndProfileWith422(String combination) {
        String[] parts = combination.split(":");

        webTestClient.post().uri("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"type":"%s","profile":"%s","documentType":"DNI",
                         "documentNumber":"12345678","name":"Ana Perez"}
                        """.formatted(parts[0], parts[1]))
                .exchange()
                .expectStatus().isEqualTo(422)
                .expectBody()
                .jsonPath("$.code").isEqualTo("CUSTOMER_RULE_VIOLATION");

        assertThat(storedCount()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // missing name
            "{\"type\":\"PERSONAL\",\"profile\":\"STANDARD\",\"documentType\":\"DNI\",\"documentNumber\":\"1\"}",
            // blank name
            "{\"type\":\"PERSONAL\",\"profile\":\"STANDARD\",\"documentType\":\"DNI\",\"documentNumber\":\"1\",\"name\":\"   \"}",
            // unknown property (additionalProperties: false)
            "{\"type\":\"PERSONAL\",\"profile\":\"STANDARD\",\"documentType\":\"DNI\",\"documentNumber\":\"1\",\"name\":\"A\",\"status\":\"INACTIVE\"}",
            // unknown enum value
            "{\"type\":\"PERSONAL\",\"profile\":\"GOLD\",\"documentType\":\"DNI\",\"documentNumber\":\"1\",\"name\":\"A\"}",
            // document number longer than 32
            "{\"type\":\"PERSONAL\",\"profile\":\"STANDARD\",\"documentType\":\"DNI\",\"documentNumber\":\"123456789012345678901234567890123\",\"name\":\"A\"}",
            // malformed JSON
            "{\"type\":\"PERSONAL\",",
            // empty body
            ""
    })
    void rejectsMalformedOrInvalidInputWith400(String body) {
        webTestClient.post().uri("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_REQUEST");

        assertThat(storedCount()).isZero();
    }

    @Test
    void rejectsNonJsonContentTypeWith415() {
        webTestClient.post().uri("/customers")
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValue("type=PERSONAL")
                .exchange()
                .expectStatus().isEqualTo(415)
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo("UNSUPPORTED_MEDIA_TYPE");
    }
}
