package com.banking.customer_service.infrastructure.config;

import com.banking.customer_service.support.MongoIntegrationTest;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The client imports where its MongoDB is from Config Server and the user and password from
 * Vault, both through spring.config.import, and logs in to customers_db with them.
 */
class ConfigServerClientIntegrationTest extends MongoIntegrationTest {

    @Autowired
    private Environment environment;

    @Test
    void importsSpringBoot4MongoPropertiesFromConfigServer() {
        assertThat(environment.getProperty("spring.application.name")).isEqualTo("customer-service");
        assertThat(CONFIG_SERVER.requestedPaths()).contains("/customer-service/default");
        assertThat(environment.getProperty("spring.mongodb.host")).isEqualTo(MONGO.getHost());
        assertThat(environment.getProperty("spring.mongodb.port", Integer.class)).isEqualTo(MONGO.getMappedPort(27017));
        assertThat(sourceOf("spring.mongodb.database")).contains("configserver");
    }

    /** The user and password come from Vault; the Config Server never serves them. */
    @Test
    void logsInWithTheCredentialsFromVault() {
        assertThat(sourceOf("spring.mongodb.username")).contains("secret/customer-service");
        assertThat(sourceOf("spring.mongodb.password")).contains("secret/customer-service");
        Document status = mongoTemplate.executeCommand("{connectionStatus: 1}").block();
        assertThat(status.get("authInfo", Document.class).getList("authenticatedUsers", Document.class))
                .extracting(user -> user.getString("user"))
                .containsExactly(USERNAME);
    }

    @Test
    void connectsToCustomersDatabase() {
        assertThat(mongoTemplate.getMongoDatabase().block().getName()).isEqualTo(DATABASE);
    }

    /** Name of the first (winning) property source that defines {@code key}. */
    private String sourceOf(String key) {
        return ((ConfigurableEnvironment) environment).getPropertySources().stream()
                // Boot's view over all the other sources, not a source of its own.
                .filter(source -> !"configurationProperties".equals(source.getName()))
                .filter(source -> source.containsProperty(key))
                .map(PropertySource::getName)
                .findFirst()
                .orElse("none");
    }
}
