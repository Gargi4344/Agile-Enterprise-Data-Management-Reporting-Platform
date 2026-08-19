package com.aedmp.platform.job;

import java.util.Map;
import java.util.Set;

public enum JobStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    RETRYING;

    private static final Map<JobStatus, Set<JobStatus>> ALLOWED = Map.of(
            PENDING, Set.of(RUNNING),
            RUNNING, Set.of(COMPLETED, FAILED),
            FAILED, Set.of(RETRYING),
            RETRYING, Set.of(RUNNING),
            COMPLETED, Set.of()
    );

    public boolean canTransitionTo(JobStatus target) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(target);
    }
}
