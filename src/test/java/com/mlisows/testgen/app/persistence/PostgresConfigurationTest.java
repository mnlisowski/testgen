package com.mlisows.testgen.app.persistence;

import com.mlisows.testgen.app.project.ConfiguredProjectsInitializer;
import com.mlisows.testgen.app.project.ProjectRegistry;
import com.mlisows.testgen.infrastructure.persistence.project.ProjectEntity;
import com.mlisows.testgen.infrastructure.persistence.project.ProjectRepository;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.transaction.annotation.Transactional;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("postgres")
@Testcontainers
class PostgresConfigurationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Flyway flyway;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectRegistry projectRegistry;

    @Autowired
    private ConfiguredProjectsInitializer projectsInitializer;

    @Test
    void shouldApplyMigrationAndInitializeHibernate() {
        assertTrue(entityManagerFactory.isOpen());
        assertEquals("3", flyway.info().current().getVersion().getVersion());
        assertEquals((long) projectRegistry.projectIds().size(),
                jdbcTemplate.queryForObject("SELECT count(*) FROM projects", Long.class));
        assertEquals(3L, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success = true", Long.class));

        flyway.migrate();

        assertEquals(3L, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success = true", Long.class));
    }

    @Test
    void shouldRegisterConfiguredProjectsDuringStartup() {
        List<String> storedIds = projectRepository.findAll().stream()
                .map(ProjectEntity::getId).sorted().toList();
        assertEquals(projectRegistry.projectIds(), storedIds);
        ProjectEntity project = projectRepository.findById("fit-bench").orElseThrow();
        assertEquals("fit-bench", project.getName());
    }

    @Test
    @Transactional
    void shouldPreserveExistingProjectsAndNamesWhenInitializationRunsAgain() {
        projectRepository.saveAndFlush(new ProjectEntity("archived-project", "Previous project"));
        jdbcTemplate.update("UPDATE projects SET name = ? WHERE id = ?", "Fit benchmark", "fit-bench");
        long previousCount = projectRepository.count();

        projectsInitializer.run(new DefaultApplicationArguments());
        projectsInitializer.run(new DefaultApplicationArguments());
        projectRepository.flush();

        assertEquals(previousCount, projectRepository.count());
        assertEquals("Fit benchmark", projectRepository.findById("fit-bench").orElseThrow().getName());
        assertEquals("Previous project", projectRepository.findById("archived-project").orElseThrow().getName());
        assertTrue(projectRegistry.findProjectRoot("archived-project").isEmpty());
    }

    @Test
    @Transactional
    void shouldInsertConfiguredProjectMissingFromDatabase() {
        projectRepository.deleteById("fit-bench");
        projectRepository.flush();

        projectsInitializer.run(new DefaultApplicationArguments());
        projectRepository.flush();

        assertEquals("fit-bench", jdbcTemplate.queryForObject(
                "SELECT name FROM projects WHERE id = ?", String.class, "fit-bench"));
    }
}
