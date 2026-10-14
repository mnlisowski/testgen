package com.mlisows.testgen.domain.generation;

public record GenerationSettings(long seed, int maxCandidates, int initialCandidates,
                                 int mutatedInitialCandidates, int maxValuesPerSlot) {
    public GenerationSettings {
        if (mutatedInitialCandidates < 0) throw new IllegalArgumentException("mutatedInitialCount must not be negative");
        if (initialCandidates < 0) throw new IllegalArgumentException("initialCandidateCount must not be negative");
        if (maxValuesPerSlot < 1) throw new IllegalArgumentException("maxValuesPerSlot must be positive");
        if (maxCandidates < 1) throw new IllegalArgumentException("maxCandidates must be positive");
    }

    public static GenerationSettings defaults() {
        return new GenerationSettings(0, 50, 10, 3, 10);
    }
}
