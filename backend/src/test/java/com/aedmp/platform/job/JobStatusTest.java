package com.aedmp.platform.job;

import org.junit.jupiter.api.Test;

import static com.aedmp.platform.job.JobStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

class JobStatusTest {

    @Test
    void pendingMovesOnlyToRunning() {
        assertThat(PENDING.canTransitionTo(RUNNING)).isTrue();
        assertThat(PENDING.canTransitionTo(COMPLETED)).isFalse();
    }

    @Test
    void runningResolvesToCompletedOrFailed() {
        assertThat(RUNNING.canTransitionTo(COMPLETED)).isTrue();
        assertThat(RUNNING.canTransitionTo(FAILED)).isTrue();
        assertThat(RUNNING.canTransitionTo(PENDING)).isFalse();
    }

    @Test
    void failedCanOnlyMoveToRetrying() {
        assertThat(FAILED.canTransitionTo(RETRYING)).isTrue();
        assertThat(FAILED.canTransitionTo(RUNNING)).isFalse();
        assertThat(FAILED.canTransitionTo(COMPLETED)).isFalse();
    }

    @Test
    void retryingReturnsToRunning() {
        assertThat(RETRYING.canTransitionTo(RUNNING)).isTrue();
    }

    @Test
    void completedIsTerminal() {
        for (JobStatus target : JobStatus.values()) {
            assertThat(COMPLETED.canTransitionTo(target)).isFalse();
        }
    }
}
