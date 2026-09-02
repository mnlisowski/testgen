package com.mlisows.testgen.infrastructure.runtime;

import com.mlisows.testgen.domain.ObservedProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TextObservedProfileReaderTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldReadArgumentHintsAndObservedInvocations() throws Exception {
        Path profilePath = tempDir.resolve("observed-profile.txt");

        Files.writeString(profilePath, """
                  # observed runtime input profile
                  hint|sample.Order.<init>|total|int|620|runtime-observed
                  hint|sample.Order.<init>|status|String|"PAID"|runtime-observed
                  invocation|sample.Order.<init>|int|620|String|"PAID"
                  """);

        ObservedProfile profile = new TextObservedProfileReader().read(profilePath);

        assertEquals(2, profile.getArgumentValueHints().size());
        assertEquals("sample.Order.<init>.total", profile.getArgumentValueHints().get(0).slotId());
        assertEquals("620", profile.getArgumentValueHints().get(0).getValue());

        assertEquals(1, profile.getObservedInvocations().size());
        assertEquals("sample.Order.<init>", profile.getObservedInvocations().get(0).getOwnerId());
        assertEquals(2, profile.getObservedInvocations().get(0).getArguments().size());
        assertEquals("620", profile.getObservedInvocations().get(0).getArguments().get(0).getValue());
        assertEquals("\"PAID\"", profile.getObservedInvocations().get(0).getArguments().get(1).getValue());
    }

    @Test
    void shouldRejectInvalidRecord() throws Exception {
        Path profilePath = tempDir.resolve("observed-profile.txt");

        Files.writeString(profilePath, "unknown|sample.Order.<init>\n");

        assertThrows(
                IllegalArgumentException.class,
                () -> new TextObservedProfileReader().read(profilePath)
        );
    }
}
