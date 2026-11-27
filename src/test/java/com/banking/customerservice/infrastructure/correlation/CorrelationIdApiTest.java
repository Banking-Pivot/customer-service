package com.banking.customerservice.infrastructure.correlation;

import static org.assertj.core.api.Assertions.assertThat;

import com.banking.customerservice.support.MongoIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;

/** The correlation identifier of a request over HTTP: response header and error body. */
class CorrelationIdApiTest extends MongoIntegrationTest {

  private static final String RECEIVED = "order-2026.11_20-A";

  @Test
  void theIdentifierReceivedGoesBackInTheHeaderAndInTheErrorBody() {
    webTestClient
        .get()
        .uri("/api/v1/customers/{id}", UUID.randomUUID())
        .header(CorrelationId.HEADER, RECEIVED)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectHeader()
        .valueEquals(CorrelationId.HEADER, RECEIVED)
        .expectBody()
        .jsonPath("$.traceId")
        .isEqualTo(RECEIVED);
  }

  @Test
  void theIdentifierReceivedGoesBackWhenTheRequestSucceeds() {
    headed(RECEIVED)
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .valueEquals(CorrelationId.HEADER, RECEIVED);
  }

  @Test
  void aRequestWithoutIdentifierGetsANewOne() {
    String first = generatedFor(webTestClient.get().uri("/api/v1/customers"));
    String second = generatedFor(webTestClient.get().uri("/api/v1/customers"));

    assertThat(UUID.fromString(first)).isNotEqualTo(UUID.fromString(second));
  }

  @Test
  void anIdentifierThatCouldBreakALogLineIsReplaced() {
    String tooLong = "x".repeat(65);

    assertThat(generatedFor(headed(tooLong))).isNotEqualTo(tooLong);
    assertThat(generatedFor(headed("two words"))).isNotEqualTo("two words");
    assertThat(generatedFor(headed("{\"injected\":true}"))).doesNotContain("injected");
  }

  private WebTestClient.RequestHeadersSpec<?> headed(String identifier) {
    return webTestClient.get().uri("/api/v1/customers").header(CorrelationId.HEADER, identifier);
  }

  private static String generatedFor(WebTestClient.RequestHeadersSpec<?> request) {
    String identifier =
        request
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(Void.class)
            .getResponseHeaders()
            .getFirst(CorrelationId.HEADER);
    assertThat(identifier).matches("[0-9a-f-]{36}");
    return identifier;
  }
}
