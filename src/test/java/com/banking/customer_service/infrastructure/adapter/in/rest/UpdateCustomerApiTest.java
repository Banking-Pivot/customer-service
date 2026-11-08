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

/** PUT /customers/{customerId} over HTTP against MongoDB. */
class UpdateCustomerApiTest extends MongoIntegrationTest {

    private static final String VALID_BODY = "{\"name\":\"Ana Perez Gomez\",\"profile\":\"VIP\"}";

    @Test
    void replacesNameAndProfileAndReturnsTheNewETag() {
        Created created = createPersonal("12345678");
        Document before = stored(created.id());

        put(created.id(), "\"0\"", "{\"name\":\"  Ana Perez Gomez \",\"profile\":\"VIP\"}")
                .expectStatus().isOk()
                .expectHeader().valueEquals("ETag", "\"1\"")
                .expectBody()
                .jsonPath("$.name").isEqualTo("Ana Perez Gomez")
                .jsonPath("$.profile").isEqualTo("VIP")
                .jsonPath("$.type").isEqualTo("PERSONAL")
                .jsonPath("$.documentType").isEqualTo("DNI")
                .jsonPath("$.documentNumber").isEqualTo("12345678")
                .jsonPath("$.status").isEqualTo("ACTIVE");

        Document after = stored(created.id());
        assertThat(after.get("version", Number.class).longValue()).isEqualTo(1);
        assertThat(after.getString("name")).isEqualTo("Ana Perez Gomez");
        assertThat(after.getString("profile")).isEqualTo("VIP");
        assertThat(after.get("createdAt", Date.class)).isEqualTo(before.get("createdAt", Date.class));
        assertThat(after.get("updatedAt", Date.class)).isAfter(before.get("updatedAt", Date.class));
        assertThat(after.getString("documentNumber")).isEqualTo("12345678");
    }

    @Test
    void identicalValuesWithMatchingRevisionKeepTheRevision() {
        Created created = createPersonal("12345678");
        Document before = stored(created.id());

        put(created.id(), created.etag(), "{\"name\":\" Ana Perez \",\"profile\":\"STANDARD\"}")
                .expectStatus().isOk()
                .expectHeader().valueEquals("ETag", "\"0\"")
                .expectBody()
                .jsonPath("$.name").isEqualTo("Ana Perez");

        assertThat(stored(created.id())).isEqualTo(before);
    }

    @Test
    void staleETagReturns412AndDoesNotOverwrite() {
        Created created = createPersonal("12345678");
        put(created.id(), "\"0\"", "{\"name\":\"First Writer\",\"profile\":\"VIP\"}")
                .expectStatus().isOk();

        put(created.id(), "\"0\"", "{\"name\":\"Second Writer\",\"profile\":\"STANDARD\"}")
                .expectStatus().isEqualTo(412)
                .expectBody()
                .jsonPath("$.code").isEqualTo("CUSTOMER_VERSION_CONFLICT");

        Document after = stored(created.id());
        assertThat(after.getString("name")).isEqualTo("First Writer");
        assertThat(after.get("version", Number.class).longValue()).isEqualTo(1);
    }

    @Test
    void missingIfMatchReturns428() {
        Created created = createPersonal("12345678");

        webTestClient.put().uri("/customers/{id}", created.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(VALID_BODY)
                .exchange()
                .expectStatus().isEqualTo(428)
                .expectBody()
                .jsonPath("$.code").isEqualTo("IF_MATCH_REQUIRED");

        assertThat(stored(created.id()).get("version", Number.class).longValue()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"W/\"0\"", "*", "0", "\"0\", \"1\"", "\"abc\"", "\"\"", "\"99999999999999999999\""})
    void malformedIfMatchReturns400(String ifMatch) {
        Created created = createPersonal("12345678");

        put(created.id(), ifMatch, VALID_BODY)
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_IF_MATCH");
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"00\"", "\"9999999999999999999\""})
    void wellFormedTagThatCanNeverMatchReturns412(String ifMatch) {
        Created created = createPersonal("12345678");

        put(created.id(), ifMatch, VALID_BODY)
                .expectStatus().isEqualTo(412)
                .expectBody()
                .jsonPath("$.code").isEqualTo("CUSTOMER_VERSION_CONFLICT");
    }

    @Test
    void inactiveCustomerReturns409() {
        Created created = createPersonal("12345678");
        deactivate(created.id(), "\"0\"");

        put(created.id(), "\"1\"", VALID_BODY)
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("CUSTOMER_INACTIVE");
    }

    @Test
    void inactiveCustomerWithStaleETagReturns412First() {
        Created created = createPersonal("12345678");
        deactivate(created.id(), "\"0\"");

        put(created.id(), "\"0\"", VALID_BODY)
                .expectStatus().isEqualTo(412);
    }

    @Test
    void incompatibleProfileReturns422AndKeepsTheRecord() {
        Created created = createPersonal("12345678");
        Document before = stored(created.id());

        put(created.id(), "\"0\"", "{\"name\":\"Ana\",\"profile\":\"SME\"}")
                .expectStatus().isEqualTo(422)
                .expectBody()
                .jsonPath("$.code").isEqualTo("CUSTOMER_RULE_VIOLATION");

        assertThat(stored(created.id())).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"name\":\"Ana\"}",
            "{\"profile\":\"VIP\"}",
            "{\"name\":\"   \",\"profile\":\"VIP\"}",
            "{\"name\":\"Ana\",\"profile\":\"VIP\",\"type\":\"BUSINESS\"}",
            "{\"name\":\"Ana\",\"profile\":\"VIP\",\"documentNumber\":\"999\"}"
    })
    void missingOrImmutableFieldsReturn400(String body) {
        Created created = createPersonal("12345678");

        put(created.id(), "\"0\"", body)
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_REQUEST");
    }

    @Test
    void unknownCustomerReturns404() {
        put(UUID.randomUUID().toString(), "\"0\"", VALID_BODY)
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("CUSTOMER_NOT_FOUND");
    }

    @Test
    void unknownCustomerWithUnmatchableTagStillReturns404() {
        put(UUID.randomUUID().toString(), "\"00\"", VALID_BODY)
                .expectStatus().isNotFound();
    }

    @Test
    void nonJsonContentTypeReturns415() {
        Created created = createPersonal("12345678");

        webTestClient.put().uri("/customers/{id}", created.id())
                .header("If-Match", "\"0\"")
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValue("name=Ana")
                .exchange()
                .expectStatus().isEqualTo(415)
                .expectBody()
                .jsonPath("$.code").isEqualTo("UNSUPPORTED_MEDIA_TYPE");
    }

    private WebTestClient.ResponseSpec put(String id, String ifMatch, String body) {
        return webTestClient.put().uri("/customers/{id}", id)
                .header("If-Match", ifMatch)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();
    }

    private void deactivate(String id, String ifMatch) {
        webTestClient.delete().uri("/customers/{id}", id)
                .header("If-Match", ifMatch)
                .exchange()
                .expectStatus().isNoContent();
    }
}
