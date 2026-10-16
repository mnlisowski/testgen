package com.mlisows.testgen.infrastructure.persistence.job;

import com.mlisows.testgen.domain.generation.GenerationSettings;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Generation settings stored as columns of the generation_jobs table. */
@Embeddable
public class GenerationSettingsEmbeddable {
    @Column(name = "seed", nullable = false)
    private long seed;

    @Column(name = "max_candidates", nullable = false)
    private int maxCandidates;

    @Column(name = "initial_candidates", nullable = false)
    private int initialCandidates;

    @Column(name = "mutated_initial_candidates", nullable = false)
    private int mutatedInitialCandidates;

    @Column(name = "max_values_per_slot", nullable = false)
    private int maxValuesPerSlot;

    protected GenerationSettingsEmbeddable() {
    }

    public GenerationSettingsEmbeddable(GenerationSettings settings) {
        this.seed = settings.seed();
        this.maxCandidates = settings.maxCandidates();
        this.initialCandidates = settings.initialCandidates();
        this.mutatedInitialCandidates = settings.mutatedInitialCandidates();
        this.maxValuesPerSlot = settings.maxValuesPerSlot();
    }

    public GenerationSettings toSettings() {
        return new GenerationSettings(seed, maxCandidates, initialCandidates, mutatedInitialCandidates,
                maxValuesPerSlot);
    }
}
