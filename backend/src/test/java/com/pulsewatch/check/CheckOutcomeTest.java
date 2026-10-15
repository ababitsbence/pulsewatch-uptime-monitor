package com.pulsewatch.check;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CheckOutcomeTest {

    @Test
    void upOutcomeHasNoError() {
        CheckOutcome outcome = CheckOutcome.up(42);

        assertThat(outcome.status()).isEqualTo(CheckStatus.UP);
        assertThat(outcome.responseTimeMs()).isEqualTo(42);
        assertThat(outcome.error()).isNull();
    }

    @Test
    void longErrorMessagesAreTruncated() {
        CheckOutcome outcome = CheckOutcome.down(1, "x".repeat(2000));

        assertThat(outcome.error()).hasSize(CheckOutcome.MAX_ERROR_LENGTH);
    }

    @Test
    void exceptionWithoutMessageFallsBackToClassName() {
        CheckOutcome outcome = CheckOutcome.down(1, new IllegalStateException());

        assertThat(outcome.error()).isEqualTo("IllegalStateException");
    }
}