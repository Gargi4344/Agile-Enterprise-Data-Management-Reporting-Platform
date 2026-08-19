package com.aedmp.platform.dataset;

import java.util.Map;
import java.util.Set;

/**
 * Allowed transitions mirror docs/state-machine.md. Any transition not listed here is rejected
 * by DatasetVersionService#transition(...) so the version can never sit in an invalid state.
 */
public enum DatasetVersionStatus {
    UPLOADED,
    VALIDATING,
    READY,
    REJECTED,
    PROCESSING,
    COMPLETED,
    FAILED;

    private static final Map<DatasetVersionStatus, Set<DatasetVersionStatus>> ALLOWED = Map.of(
            UPLOADED, Set.of(VALIDATING),
            VALIDATING, Set.of(READY, REJECTED),
            READY, Set.of(PROCESSING, VALIDATING),
            REJECTED, Set.of(VALIDATING),
            PROCESSING, Set.of(COMPLETED, FAILED),
            FAILED, Set.of(PROCESSING),
            COMPLETED, Set.of()
    );

    public boolean canTransitionTo(DatasetVersionStatus target) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(target);
    }
}
