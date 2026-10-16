package com.mlisows.testgen.app.config;

import com.mlisows.testgen.domain.generation.GenerationSettings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;
import java.util.Map;

@Validated
@ConfigurationProperties(prefix = "testgen", ignoreUnknownFields = false)
public record TestgenProperties(
        @NotEmpty Map<@Pattern(regexp = "[a-z0-9][a-z0-9-]*") String, @NotNull @Valid Project> projects,
        @NotBlank String workRoot,
        @NotNull @Valid Defaults defaults,
        @DefaultValue @Valid Runner runner,
        @DefaultValue("10") @Min(1) int queueCapacity,
        @DefaultValue @Valid Messaging messaging
) {
    public Path workRootPath() {
        return Path.of(workRoot).toAbsolutePath().normalize();
    }

    public record Runner(
            @DefaultValue("120") @Min(1) long timeoutSeconds,
            String classpath
    ) {
    }

    public record Project(@NotBlank String path) {
    }

    /**
     * Kafka topic for job requests. The number of partitions limits how many workers
     * of one consumer group can process requests at the same time.
     */
    public record Messaging(
            @DefaultValue("testgen.jobs.requests") @NotBlank String jobRequestsTopic,
            @DefaultValue("3") @Min(1) int jobRequestsPartitions,
            @DefaultValue("10") @Min(1) long publishTimeoutSeconds
    ) {
    }

    public record Defaults(
            @NotNull Long seed,
            @NotNull @Min(1) Integer maxCandidates,
            @NotNull @Min(0) Integer initialCandidates,
            @NotNull @Min(0) Integer mutatedInitialCandidates,
            @NotNull @Min(1) Integer maxValuesPerSlot
    ) {
        public GenerationSettings toGenerationSettings() {
            return new GenerationSettings(seed, maxCandidates, initialCandidates,
                    mutatedInitialCandidates, maxValuesPerSlot);
        }
    }
}
