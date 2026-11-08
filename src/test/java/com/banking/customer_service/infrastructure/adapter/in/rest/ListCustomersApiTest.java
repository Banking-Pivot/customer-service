package com.banking.customer_service.infrastructure.adapter.in.rest;

import com.banking.customer_service.infrastructure.adapter.in.rest.generated.model.CustomerPageResponse;
import com.banking.customer_service.infrastructure.adapter.in.rest.generated.model.CustomerResponse;
import com.banking.customer_service.support.MongoIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/** GET /customers over HTTP against MongoDB. */
class ListCustomersApiTest extends MongoIntegrationTest {

    @Test
    void generatedPageItemsAreCustomerResponse() throws NoSuchMethodException {
        var itemsType = (ParameterizedType) CustomerPageResponse.class
                .getMethod("getItems").getGenericReturnType();

        assertThat(itemsType.getRawType()).isEqualTo(List.class);
        assertThat(itemsType.getActualTypeArguments()[0].getTypeName())
                .endsWith(CustomerResponse.class.getName());
    }

    @Test
    void usesDefaultsAndOrdersByUuidAscending() {
        List<String> ids = IntStream.range(0, 3)
                .mapToObj(i -> createPersonal("1000" + i).id())
                .toList();

        CustomerPageResponse page = list("/customers");

        assertThat(page.getPage()).isZero();
        assertThat(page.getSize()).isEqualTo(20);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(1);
        assertThat(page.getItems()).allSatisfy(item -> assertThat(item).isInstanceOf(CustomerResponse.class));
        assertThat(idsOf(page)).containsExactlyElementsOf(ids.stream().sorted().toList());
    }

    @Test
    void walksPagesInStableOrderAndReturnsEmptyItemsPastTheLastPage() {
        List<String> ids = IntStream.range(0, 5)
                .mapToObj(i -> createPersonal("2000" + i).id())
                .toList();

        List<String> walked = new ArrayList<>();
        for (int page = 0; page < 3; page++) {
            CustomerPageResponse response = list("/customers?page=" + page + "&size=2");
            assertThat(response.getTotalElements()).isEqualTo(5);
            assertThat(response.getTotalPages()).isEqualTo(3);
            walked.addAll(idsOf(response));
        }

        assertThat(walked).containsExactlyElementsOf(ids.stream().sorted().toList());

        CustomerPageResponse pastTheEnd = list("/customers?page=3&size=2");
        assertThat(pastTheEnd.getItems()).isEmpty();
        assertThat(pastTheEnd.getPage()).isEqualTo(3);
        assertThat(pastTheEnd.getTotalElements()).isEqualTo(5);
        assertThat(pastTheEnd.getTotalPages()).isEqualTo(3);
    }

    @Test
    void emptyCollectionHasZeroPages() {
        CustomerPageResponse page = list("/customers");

        assertThat(page.getItems()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getTotalPages()).isZero();
    }

    @Test
    void filtersByTypeAndStatus() {
        String personalActive = createCustomer("PERSONAL", "VIP", "DNI", "1", "A").id();
        Created personalInactive = createCustomer("PERSONAL", "STANDARD", "DNI", "2", "B");
        String business = createCustomer("BUSINESS", "SME", "RUC", "3", "C").id();
        webTestClient.delete().uri("/customers/{id}", personalInactive.id())
                .header("If-Match", personalInactive.etag())
                .exchange()
                .expectStatus().isNoContent();

        assertThat(idsOf(list("/customers?type=BUSINESS"))).containsExactly(business);
        assertThat(idsOf(list("/customers?status=INACTIVE"))).containsExactly(personalInactive.id());
        assertThat(idsOf(list("/customers?type=PERSONAL&status=ACTIVE"))).containsExactly(personalActive);
        assertThat(list("/customers?type=PERSONAL").getTotalElements()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "size=0", "size=101", "page=-1", "page=10001", "page=abc", "type=GOLD", "status=active"
    })
    void rejectsInvalidParametersWith400(String parameter) {
        webTestClient.get().uri("/customers?" + parameter)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_REQUEST");
    }

    @Test
    void acceptsTheMaximumPageSize() {
        CustomerPageResponse page = list("/customers?size=100&page=10000");

        assertThat(page.getSize()).isEqualTo(100);
        assertThat(page.getItems()).isEmpty();
    }

    private CustomerPageResponse list(String uri) {
        return webTestClient.get().uri(uri)
                .exchange()
                .expectStatus().isOk()
                .expectBody(CustomerPageResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private static List<String> idsOf(CustomerPageResponse page) {
        return page.getItems().stream().map(CustomerResponse::getId).map(UUID::toString).toList();
    }
}
