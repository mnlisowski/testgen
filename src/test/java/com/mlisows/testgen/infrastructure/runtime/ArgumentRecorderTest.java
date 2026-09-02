package com.mlisows.testgen.infrastructure.runtime;

import com.mlisows.testgen.domain.ObservedProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArgumentRecorderTest {
    private static final String PROFILE_DIR_PROPERTY = "testgen.profile.dir";

    @TempDir
    Path tempDir;

    private String previousProfileDir;

    @BeforeEach
    void rememberProfileDir() {
        previousProfileDir = System.getProperty(PROFILE_DIR_PROPERTY);
    }

    @AfterEach
    void resetRecorder() {
        ArgumentRecorder.reset();

        if (previousProfileDir == null) {
            System.clearProperty(PROFILE_DIR_PROPERTY);
        } else {
            System.setProperty(PROFILE_DIR_PROPERTY, previousProfileDir);
        }
    }

    @Test
    void shouldRecordArgumentHintsAndInvocation() {
        ArgumentRecorder.record(
                "sample.Order.<init>",
                new String[]{"total", "status"},
                new String[]{"int", "String"},
                new Object[]{620, "PAID"}
        );

        assertEquals(List.of(
                "hint|sample.Order.<init>|total|int|620|runtime-observed",
                "hint|sample.Order.<init>|status|String|\"PAID\"|runtime-observed",
                "invocation|sample.Order.<init>|int|620|String|\"PAID\""
        ), ArgumentRecorder.snapshotLines());
    }

    @Test
    void shouldWriteProfileReadableByTextObservedProfileReader() {
        Path profilePath = tempDir.resolve("observed-profile.txt");

        ArgumentRecorder.record(
                "sample.Order.<init>",
                new String[]{"total", "status"},
                new String[]{"int", "String"},
                new Object[]{620, "PAID"}
        );
        ArgumentRecorder.writeTo(profilePath);

        ObservedProfile profile = new TextObservedProfileReader().read(profilePath);

        assertEquals(2, profile.getArgumentValueHints().size());
        assertEquals(1, profile.getObservedInvocations().size());
        assertEquals("\"PAID\"", profile.getArgumentValueHints().get(1).getValue());
        assertEquals("\"PAID\"", profile.getObservedInvocations().get(0).getArguments().get(1).getValue());
    }

    @Test
    void shouldAppendProfileLinesWhenProfileDirectoryIsConfigured() throws Exception {
        Path profileDir = tempDir.resolve("profile-output");
        Path profilePath = profileDir.resolve("observed-profile.txt");
        System.setProperty(PROFILE_DIR_PROPERTY, profileDir.toString());

        ArgumentRecorder.record(
                "sample.Order.<init>",
                new String[]{"total", "status"},
                new String[]{"int", "String"},
                new Object[]{620, "PAID"}
        );

        assertEquals(List.of(
                "hint|sample.Order.<init>|total|int|620|runtime-observed",
                "hint|sample.Order.<init>|status|String|\"PAID\"|runtime-observed",
                "invocation|sample.Order.<init>|int|620|String|\"PAID\""
        ), Files.readAllLines(profilePath));
    }

    @Test
    void shouldSkipInvocationWhenArgumentCannotBeReplayed() {
        ArgumentRecorder.record(
                "sample.DiscountService.calculate",
                new String[]{"order"},
                new String[]{"Order"},
                new Object[]{new Object()}
        );

        assertEquals(List.of(), ArgumentRecorder.snapshotLines());
    }

    @Test
    void shouldEscapeStringValuesAsJavaLiterals() {
        ArgumentRecorder.record(
                "sample.MessageService.normalize",
                new String[]{"message"},
                new String[]{"String"},
                new Object[]{"A\"B\\C"}
        );

        assertEquals(List.of(
                "hint|sample.MessageService.normalize|message|String|\"A\\\"B\\\\C\"|runtime-observed",
                "invocation|sample.MessageService.normalize|String|\"A\\\"B\\\\C\""
        ), ArgumentRecorder.snapshotLines());
    }
}
