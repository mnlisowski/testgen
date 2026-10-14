package com.mlisows.testgen.app.job;

import com.mlisows.testgen.domain.generation.GenerationSettings;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SubmitGenerationRequest(
        @NotBlank String projectId,
        Long seed,
        @Min(1) Integer maxCandidates,
        @Min(0) Integer initialCandidates,
        @Min(0) Integer mutatedInitialCandidates,
        @Min(1) Integer maxValuesPerSlot
) {
    public GenerationSettings resolveSettings(GenerationSettings defaults) {
        return new GenerationSettings(
                seed == null ? defaults.seed() : seed,
                maxCandidates == null ? defaults.maxCandidates() : maxCandidates,
                initialCandidates == null ? defaults.initialCandidates() : initialCandidates,
                mutatedInitialCandidates == null ? defaults.mutatedInitialCandidates() : mutatedInitialCandidates,
                maxValuesPerSlot == null ? defaults.maxValuesPerSlot() : maxValuesPerSlot
        );
    }
}
