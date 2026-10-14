package com.mlisows.testgen.app.job;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
public class GenerationJobController {
    private final GenerationJobService generationJobService;

    public GenerationJobController(GenerationJobService generationJobService) {
        this.generationJobService = generationJobService;
    }

    @PostMapping("/api/generations")
    public ResponseEntity<JobResponse> submit(@Valid @RequestBody SubmitGenerationRequest request) {
        JobResponse response = JobResponse.from(generationJobService.submit(request));
        URI statusLocation = URI.create("/api/jobs/" + response.id());
        return ResponseEntity.accepted().location(statusLocation).body(response);
    }

    @GetMapping("/api/jobs/{jobId}")
    public JobResponse find(@PathVariable UUID jobId) {
        return JobResponse.from(generationJobService.find(jobId));
    }
}
