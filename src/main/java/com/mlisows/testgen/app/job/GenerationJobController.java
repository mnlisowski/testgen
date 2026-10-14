package com.mlisows.testgen.app.job;

import com.mlisows.testgen.usecase.generation.GenerationResult;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
public class GenerationJobController {
    private final GenerationJobService generationJobService;

    private final GeneratedTestsArchive generatedTestsArchive;

    public GenerationJobController(GenerationJobService generationJobService,
                                   GeneratedTestsArchive generatedTestsArchive) {
        this.generationJobService = generationJobService;
        this.generatedTestsArchive = generatedTestsArchive;
    }

    @PostMapping("/api/generations")
    public ResponseEntity<JobResponse> submit(@Valid @RequestBody SubmitGenerationRequest request) {
        JobResponse response = JobResponse.from(generationJobService.submit(request));
        URI statusLocation = URI.create("/api/jobs/" + response.id());
        return ResponseEntity.accepted().location(statusLocation).body(response);
    }

    @GetMapping("/api/jobs/{jobId}/report")
    public ResponseEntity<String> downloadReport(@PathVariable UUID jobId) {
        String reportText = generationJobService.findResult(jobId).reportText();
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("report-" + jobId + ".txt")
                .build();
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(reportText);
    }

    @GetMapping("/api/jobs/{jobId}/tests")
    public ResponseEntity<byte[]> downloadTests(@PathVariable UUID jobId) throws IOException {
        GenerationResult result = generationJobService.findResult(jobId);
        byte[] archive = generatedTestsArchive.create(result.tests());
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("tests-" + jobId + ".zip")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(archive);
    }

    @GetMapping("/api/jobs/{jobId}")
    public JobResponse find(@PathVariable UUID jobId) {
        return JobResponse.from(generationJobService.find(jobId));
    }
}
