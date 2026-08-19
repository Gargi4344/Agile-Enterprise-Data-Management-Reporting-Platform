package com.aedmp.platform.dataset;

import org.junit.jupiter.api.Test;

import static com.aedmp.platform.dataset.DatasetVersionStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

class DatasetVersionStatusTest {

    @Test
    void uploadedCanOnlyMoveToValidating() {
        assertThat(UPLOADED.canTransitionTo(VALIDATING)).isTrue();
        assertThat(UPLOADED.canTransitionTo(READY)).isFalse();
        assertThat(UPLOADED.canTransitionTo(PROCESSING)).isFalse();
    }

    @Test
    void validatingCanResultInReadyOrRejected() {
        assertThat(VALIDATING.canTransitionTo(READY)).isTrue();
        assertThat(VALIDATING.canTransitionTo(REJECTED)).isTrue();
        assertThat(VALIDATING.canTransitionTo(PROCESSING)).isFalse();
    }

    @Test
    void rejectedCanOnlyReturnToValidating() {
        assertThat(REJECTED.canTransitionTo(VALIDATING)).isTrue();
        assertThat(REJECTED.canTransitionTo(READY)).isFalse();
    }

    @Test
    void readyCanMoveToProcessingOrBackToValidating() {
        assertThat(READY.canTransitionTo(PROCESSING)).isTrue();
        assertThat(READY.canTransitionTo(VALIDATING)).isTrue();
        assertThat(READY.canTransitionTo(COMPLETED)).isFalse();
    }

    @Test
    void processingResolvesToCompletedOrFailed() {
        assertThat(PROCESSING.canTransitionTo(COMPLETED)).isTrue();
        assertThat(PROCESSING.canTransitionTo(FAILED)).isTrue();
        assertThat(PROCESSING.canTransitionTo(READY)).isFalse();
    }

    @Test
    void failedCanOnlyBeRetriedIntoProcessing() {
        assertThat(FAILED.canTransitionTo(PROCESSING)).isTrue();
        assertThat(FAILED.canTransitionTo(COMPLETED)).isFalse();
    }

    @Test
    void completedIsTerminal() {
        for (DatasetVersionStatus target : DatasetVersionStatus.values()) {
            assertThat(COMPLETED.canTransitionTo(target)).isFalse();
        }
    }
}
