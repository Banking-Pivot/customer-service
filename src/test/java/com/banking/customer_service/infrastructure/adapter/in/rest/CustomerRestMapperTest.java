package com.banking.customer_service.infrastructure.adapter.in.rest;

import com.banking.customer_service.application.model.CustomerPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ServerWebInputException;

import java.util.List;
import java.util.OptionalLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerRestMapperTest {

    private final CustomerRestMapper mapper = new CustomerRestMapper();

    @Test
    void etagIsTheQuotedRevision() {
        assertThat(mapper.toETag(0)).isEqualTo("\"0\"");
        assertThat(mapper.toExpectedVersion("\"42\"")).isEqualTo(OptionalLong.of(42));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"00\"", "\"042\"", "\"9223372036854775808\""})
    void wellFormedTagsThatNoRevisionCanEqualAreUnmatchable(String tag) {
        assertThat(mapper.toExpectedVersion(tag)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"*", "W/\"1\"", "1", "\"1\",\"2\"", "\"-1\""})
    void malformedTagsAreRejected(String tag) {
        assertThatThrownBy(() -> mapper.toExpectedVersion(tag))
                .isInstanceOf(ServerWebInputException.class);
    }

    @Test
    void totalPagesIsTheCeilingAndZeroWhenEmpty() {
        assertThat(CustomerPage.of(List.of(), 0, 20, 0).totalPages()).isZero();
        assertThat(CustomerPage.of(List.of(), 0, 2, 4).totalPages()).isEqualTo(2);
        assertThat(CustomerPage.of(List.of(), 0, 2, 5).totalPages()).isEqualTo(3);
    }
}
