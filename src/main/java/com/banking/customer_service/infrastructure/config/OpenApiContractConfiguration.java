package com.banking.customer_service.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

/**
 * Publishes the OpenAPI contract the code is generated from, unchanged, at
 * {@code /openapi/customer-api.yml}. Swagger UI points at this file
 * ({@code springdoc.swagger-ui.url}) instead of the spec springdoc rebuilds from
 * annotations, which drops the ETag and Location headers, the API info and
 * {@code additionalProperties: false}.
 */
@Configuration
public class OpenApiContractConfiguration {

    public static final String CONTRACT_PATH = "/openapi/customer-api.yml";

    @Bean
    public RouterFunction<ServerResponse> openApiContractRoute() {
        Resource contract = new ClassPathResource("openapi/customer-api.yml");

        return RouterFunctions.route(GET(CONTRACT_PATH), request -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_YAML)
                .bodyValue(contract));
    }
}
