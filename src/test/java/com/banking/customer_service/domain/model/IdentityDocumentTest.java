package com.banking.customer_service.domain.model;

import com.banking.customer_service.domain.exception.InvalidCustomerException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentityDocumentTest {

    @Test
    void stripsAndUppercasesPreservingInternalCharactersAndLeadingZeros() {
        IdentityDocument document = new IdentityDocument("  dni\t", " 00a-12 b ");

        assertThat(document.type()).isEqualTo("DNI");
        assertThat(document.number()).isEqualTo("00A-12 B");
    }

    @Test
    void uppercasingDoesNotDependOnTheServerLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));

            // Turkish locale would turn "i" into dotted "İ".
            assertThat(new IdentityDocument("ci", "i1").type()).isEqualTo("CI");
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void equalNormalizedValuesAreTheSameIdentity() {
        assertThat(new IdentityDocument(" dni", "abc"))
                .isEqualTo(new IdentityDocument("DNI ", "ABC"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t\n"})
    void rejectsBlankValues(String blank) {
        assertThatThrownBy(() -> new IdentityDocument("DNI", blank))
                .isInstanceOf(InvalidCustomerException.class);
        assertThatThrownBy(() -> new IdentityDocument(blank, "123"))
                .isInstanceOf(InvalidCustomerException.class);
    }

    @Test
    void rejectsNullAndOverlongValues() {
        assertThatThrownBy(() -> new IdentityDocument(null, "1"))
                .isInstanceOf(InvalidCustomerException.class);
        assertThatThrownBy(() -> new IdentityDocument("X".repeat(25), "1"))
                .isInstanceOf(InvalidCustomerException.class);
        assertThatThrownBy(() -> new IdentityDocument("DNI", "9".repeat(33)))
                .isInstanceOf(InvalidCustomerException.class);
    }

    @Test
    void lengthLimitAppliesAfterStripping() {
        assertThat(new IdentityDocument("DNI", "  " + "9".repeat(32) + "  ").number())
                .hasSize(32);
    }
}
