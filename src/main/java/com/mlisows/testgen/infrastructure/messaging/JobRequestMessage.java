package com.mlisows.testgen.infrastructure.messaging;

import java.util.Objects;
import java.util.UUID;

/**
 * Content of a message on the job requests topic, sent as JSON.
 *
 * @param schemaVersion version of this format; a consumer can reject versions it does not know
 * @param eventId       identifies this publication; publishing the same job again creates a new event id
 * @param jobId         identifies the stored job to execute
 */
public record JobRequestMessage(int schemaVersion, UUID eventId, UUID jobId) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public JobRequestMessage {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(jobId, "jobId");
    }

    public static JobRequestMessage forJob(UUID jobId) {
        return new JobRequestMessage(CURRENT_SCHEMA_VERSION, UUID.randomUUID(), jobId);
    }
}
