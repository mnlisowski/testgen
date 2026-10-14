package com.mlisows.testgen.usecase.ports;

import com.mlisows.testgen.usecase.generation.GenerationRequest;
import com.mlisows.testgen.usecase.generation.GenerationResult;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

public interface GenerationRunner {
    GenerationResult run(UUID jobId, GenerationRequest request)
            throws IOException, InterruptedException, TimeoutException;
}
