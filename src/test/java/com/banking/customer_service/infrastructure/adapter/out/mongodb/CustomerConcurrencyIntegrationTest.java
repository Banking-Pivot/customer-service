package com.banking.customer_service.infrastructure.adapter.out.mongodb;

import com.banking.customer_service.application.port.out.CustomerWritePort;
import com.banking.customer_service.domain.model.Customer;
import com.banking.customer_service.support.MongoIntegrationTest;
import com.banking.customer_service.support.SteppingClockConfiguration;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Race conditions resolved by MongoDB itself: the unique identity index for
 * concurrent registrations and the version-conditioned findAndModify for
 * concurrent updates. Requests are fired concurrently through a non-blocking
 * client against the running server.
 */
class CustomerConcurrencyIntegrationTest extends MongoIntegrationTest {

    private static final Instant LATER = SteppingClockConfiguration.START.plus(Duration.ofDays(1));

    @LocalServerPort
    private int port;

    @Autowired
    private CustomerWritePort writePort;

    private WebClient client;

    @BeforeEach
    void createClient() {
        client = WebClient.create("http://localhost:" + port);
    }

    @Test
    void uniqueCompoundIndexExistsInMongo() {
        List<IndexInfo> indexes = mongoTemplate.indexOps(CustomerDocument.COLLECTION)
                .getIndexInfo().collectList().block();

        assertThat(indexes).anySatisfy(index -> {
            assertThat(index.getName()).isEqualTo("uk_customer_identity");
            assertThat(index.isUnique()).isTrue();
            assertThat(index.getIndexFields()).extracting(field -> field.getKey())
                    .containsExactly("documentType", "documentNumber");
        });
    }

    @Test
    void databaseRejectsDuplicateIdentityEvenOutsideTheApplication() {
        createPersonal("12345678");

        Document duplicate = new Document("_id", UUID.randomUUID().toString())
                .append("documentType", "DNI")
                .append("documentNumber", "12345678");

        assertThatThrownBy(() -> mongoTemplate.insert(duplicate, CustomerDocument.COLLECTION).block())
                .isInstanceOf(DuplicateKeyException.class);
    }

    @RepeatedTest(10)
    void simultaneousRegistrationsOfTheSameIdentityYieldOne201AndOne409() {
        String documentNumber = UUID.randomUUID().toString().substring(0, 12);
        String body = """
                {"type":"PERSONAL","profile":"STANDARD","documentType":"DNI",
                 "documentNumber":"%s","name":"Ana Perez"}
                """.formatted(documentNumber);

        List<Integer> statuses = Mono.zip(post(body), post(body))
                .map(pair -> List.of(pair.getT1(), pair.getT2()))
                .block(Duration.ofSeconds(10));

        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        assertThat(mongoTemplate.count(
                Query.query(Criteria.where("documentNumber").is(documentNumber.toUpperCase())),
                CustomerDocument.COLLECTION
        ).block()).isEqualTo(1);
    }

    @RepeatedTest(10)
    void simultaneousUpdatesWithTheSameETagYieldOne200AndOne412WithoutLostUpdate() {
        Created created = createPersonal(UUID.randomUUID().toString().substring(0, 12));

        List<Outcome> outcomes = Mono.zip(
                        put(created.id(), created.etag(), "Writer A"),
                        put(created.id(), created.etag(), "Writer B"))
                .map(pair -> List.of(pair.getT1(), pair.getT2()))
                .block(Duration.ofSeconds(10));

        assertThat(outcomes).extracting(Outcome::status).containsExactlyInAnyOrder(200, 412);
        String winner = outcomes.stream().filter(outcome -> outcome.status() == 200)
                .findFirst().orElseThrow().name();

        Document stored = stored(created.id());
        assertThat(stored.getString("name")).isEqualTo(winner);
        assertThat(stored.get("version", Number.class).longValue()).isEqualTo(1);
    }

    @Test
    void writeIsConditionedOnTheClientRevisionEvenAfterReReadingTheDocument() {
        Created created = createPersonal("12345678");
        long clientRevision = 0;

        // Another writer moves the customer to revision 1.
        Customer competitor = writePort.findById(UUID.fromString(created.id())).block();
        competitor.updateInformation("Competitor", competitor.getProfile(), LATER);
        assertThat(writePort.update(competitor, clientRevision).block().getVersion()).isEqualTo(1);

        // The handler re-reads (revision 1) but keeps the client's revision as condition.
        Customer reRead = writePort.findById(UUID.fromString(created.id())).block();
        reRead.updateInformation("Late Writer", reRead.getProfile(), LATER);

        StepVerifier.create(writePort.update(reRead, clientRevision))
                .verifyComplete();

        Document stored = stored(created.id());
        assertThat(stored.getString("name")).isEqualTo("Competitor");
        assertThat(stored.get("version", Number.class).longValue()).isEqualTo(1);
    }

    private Mono<Integer> post(String body) {
        return client.post().uri("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchangeToMono(response -> response.releaseBody()
                        .thenReturn(response.statusCode().value()));
    }

    private Mono<Outcome> put(String id, String etag, String name) {
        return client.put().uri("/customers/{id}", id)
                .header("If-Match", etag)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"" + name + "\",\"profile\":\"VIP\"}")
                .exchangeToMono(response -> response.releaseBody()
                        .thenReturn(new Outcome(response.statusCode().value(), name)));
    }

    private record Outcome(int status, String name) {
    }
}
