package com.banking.customer_service.support;

import com.banking.customer_service.infrastructure.adapter.in.rest.generated.model.CustomerResponse;
import com.banking.customer_service.infrastructure.adapter.out.mongodb.CustomerDocument;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.UUID;

/**
 * Boots the whole service on a random port against a real MongoDB in Docker.
 *
 * <p>The application keeps its real {@code spring.config.import}. A stub Config Server
 * serves where {@code customers_db} is (the Spring Boot 4 {@code spring.mongodb.*}
 * properties) and a real Vault holds the user and password the service logs in with, which
 * the Config Server never serves. {@code CONFIG_SERVER_URL}, {@code VAULT_URI} and
 * {@code VAULT_APPROLE_FILE} point the client at them. All start once, before any Spring
 * context, and are shared by every subclass.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import(SteppingClockConfiguration.class)
public abstract class MongoIntegrationTest {

    protected static final String DATABASE = "customers_db";
    protected static final String USERNAME = "customers";
    private static final String PASSWORD = UUID.randomUUID().toString();
    protected static final MongoDBContainer MONGO =
            new MongoDBContainer(DockerImageName.parse("mongo:8.0"));
    protected static final StubConfigServer CONFIG_SERVER;
    protected static final TestVault VAULT;

    static {
        MONGO.start();
        createServiceUser();
        // Read by application.yaml through VAULT_URI and VAULT_APPROLE_FILE.
        VAULT = TestVault.start("customer-service", Map.of(
                "spring.mongodb.username", USERNAME,
                "spring.mongodb.password", PASSWORD));
        CONFIG_SERVER = StubConfigServer.start(Map.of(
                "spring.mongodb.host", MONGO.getHost(),
                "spring.mongodb.port", String.valueOf(MONGO.getMappedPort(27017)),
                "spring.mongodb.database", DATABASE,
                "spring.mongodb.authentication-database", DATABASE));
        // Read by application.yaml: configserver:${CONFIG_SERVER_URL:http://localhost:8888}
        System.setProperty("CONFIG_SERVER_URL", CONFIG_SERVER.url());
    }

    /**
     * The user the service logs in as, created like infra/mongo/create-customer-user.js
     * does. The container does not enforce authorization, but it does check the password.
     */
    private static void createServiceUser() {
        try {
            ExecResult result = MONGO.execInContainer("mongosh", "--quiet", DATABASE, "--eval",
                    "db.createUser({user: '%s', pwd: '%s', roles: [{role: 'readWrite', db: '%s'}]})"
                            .formatted(USERNAME, PASSWORD, DATABASE));
            if (result.getExitCode() != 0) {
                throw new IllegalStateException("Could not create the MongoDB user: " + result.getStderr());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    @Autowired
    protected WebTestClient webTestClient;

    @Autowired
    protected ReactiveMongoTemplate mongoTemplate;

    @BeforeEach
    void removeCustomers() {
        // remove, not drop: dropping the collection would also drop the unique index.
        mongoTemplate.remove(new Query(), CustomerDocument.COLLECTION).block();
    }

    /** ID and ETag of a customer created through HTTP. */
    protected record Created(String id, String etag) {
    }

    protected Created createCustomer(
            String type,
            String profile,
            String documentType,
            String documentNumber,
            String name
    ) {
        EntityExchangeResult<CustomerResponse> result = webTestClient.post()
                .uri("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "type", type,
                        "profile", profile,
                        "documentType", documentType,
                        "documentNumber", documentNumber,
                        "name", name
                ))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(CustomerResponse.class)
                .returnResult();

        return new Created(
                result.getResponseBody().getId().toString(),
                result.getResponseHeaders().getETag()
        );
    }

    protected Created createPersonal(String documentNumber) {
        return createCustomer("PERSONAL", "STANDARD", "DNI", documentNumber, "Ana Perez");
    }

    /** Raw stored document, read straight from MongoDB. */
    protected Document stored(String id) {
        return mongoTemplate.findById(id, Document.class, CustomerDocument.COLLECTION).block();
    }

    protected long storedCount() {
        return mongoTemplate.count(new Query(), CustomerDocument.COLLECTION).block();
    }
}
