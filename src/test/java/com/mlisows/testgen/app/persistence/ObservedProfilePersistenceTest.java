package com.mlisows.testgen.app.persistence;

import com.mlisows.testgen.domain.profile.ObservedProfile;
import com.mlisows.testgen.infrastructure.persistence.profile.JpaObservedProfileStore;
import com.mlisows.testgen.infrastructure.runtime.TextObservedProfileReader;
import com.mlisows.testgen.usecase.ports.ObservedProfileStore;
import com.mlisows.testgen.usecase.profile.StoredObservedProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class ObservedProfilePersistenceTest {
    private static final String PROFILE_TEXT = """
            hint|sample.fit.PricingEngine.price(int)|amount|int|120|runtime-observed
            hint|sample.fit.PricingEngine.price(int)|amount|int|99|runtime-observed
            invocation|sample.fit.PricingEngine.price(int)|int|120
            """;

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    private ObservedProfileStore profileStore;

    @Autowired
    private TextObservedProfileReader profileReader;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldUseDatabaseStoreWithPostgresProfile() {
        assertInstanceOf(JpaObservedProfileStore.class, profileStore);
    }

    @Test
    void shouldStoreOriginalTextAndReadParsedProfileBack() {
        ObservedProfile parsedProfile = profileReader.parse(PROFILE_TEXT);

        StoredObservedProfile createdProfile = profileStore.create("fit-bench", PROFILE_TEXT, parsedProfile);
        StoredObservedProfile loadedProfile = profileStore.findById(createdProfile.id()).orElseThrow();

        assertEquals(createdProfile.id(), loadedProfile.id());
        assertEquals("fit-bench", loadedProfile.projectId());
        assertEquals(createdProfile.createdAt(), loadedProfile.createdAt());
        assertEquals(2, loadedProfile.profile().getArgumentValueHints().size());
        assertEquals(1, loadedProfile.profile().getObservedInvocations().size());

        Map<String, Object> storedRow = jdbcTemplate.queryForMap(
                "SELECT project_id, content FROM observed_profiles WHERE id = ?",
                createdProfile.id());
        assertEquals("fit-bench", storedRow.get("project_id"));
        assertEquals(PROFILE_TEXT, storedRow.get("content"));
    }

    @Test
    void shouldReturnEmptyResultForUnknownProfile() {
        assertTrue(profileStore.findById(UUID.randomUUID()).isEmpty());
    }

    @Test
    void shouldRejectProfileForProjectMissingFromDatabase() {
        ObservedProfile parsedProfile = profileReader.parse(PROFILE_TEXT);

        // The foreign key on project_id rejects the row; the service normally checks the project earlier.
        assertThrows(DataIntegrityViolationException.class,
                () -> profileStore.create("missing-project", PROFILE_TEXT, parsedProfile));
    }
}
