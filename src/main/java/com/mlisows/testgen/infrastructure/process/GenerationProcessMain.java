package com.mlisows.testgen.infrastructure.process;

import com.mlisows.testgen.infrastructure.TestgenUseCases;
import com.mlisows.testgen.usecase.generation.GenerationRequest;
import com.mlisows.testgen.usecase.generation.GenerationResult;

import java.nio.file.Path;

/** Machine-facing entry point. Existing human-facing CLI remains unchanged. */
public final class GenerationProcessMain {
    private GenerationProcessMain() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Expected request.json and result.json paths");
        }
        var protocol = new GenerationProcessProtocol();
        GenerationRequest generationRequest = protocol.readRequest(Path.of(args[0]));
        GenerationResult generationResult = TestgenUseCases.generation().generate(generationRequest);
        protocol.writeResult(Path.of(args[1]), generationResult);
    }
}
