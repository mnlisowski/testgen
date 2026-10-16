package com.mlisows.testgen.app.project;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Profile("api")
@RestController
public class ProjectController {
    private final ProjectRegistry projects;

    public ProjectController(ProjectRegistry projects) {
        this.projects = projects;
    }

    @GetMapping("/api/projects")
    public List<ProjectResponse> listProjects() {
        return projects.projectIds().stream()
                .map(ProjectResponse::new)
                .toList();
    }
}
