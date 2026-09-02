package com.mlisows.testgen.cli;

import com.mlisows.testgen.domain.ArgumentValueHint;
import com.mlisows.testgen.domain.ObservedProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldPrintGenerationReportForMavenProject() {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));

            Main.main(new String[]{
                    "src/test/resources/sample-maven-project",
                    tempDir.resolve("work").toString()
            });
        } finally {
            System.setOut(originalOut);
        }

        String report = output.toString(StandardCharsets.UTF_8);

        assertTrue(report.contains("Test generation report"));
        assertTrue(report.contains("Classes analyzed: 4"));
        assertTrue(report.contains("Supported branch kinds: IF, FOR, WHILE, SWITCH"));
        assertTrue(report.contains("Methods analyzed: 8"));
        assertTrue(report.contains("Detected goals: 15"));
        assertTrue(report.contains("Covered goals: 12"));
        assertTrue(report.contains("Failed to execute: 0"));
        assertTrue(report.contains("Unsupported Java constructs"));
    }

    @Test
    void shouldUseObservedProfileHints() {
        ObservedProfile observedProfile = new ObservedProfile(
                List.of(new ArgumentValueHint(
                        "sample.maven.Order.<init>",
                        "status",
                        "String",
                        "\"PAID\"",
                        "runtime-observed"
                )),
                List.of()
        );

        String report = Main.generateReport(
                Path.of("src/test/resources/sample-maven-project"),
                tempDir.resolve("work-with-profile"),
                observedProfile
        ).toText();

        assertTrue(report.contains("Covered goals: 13"));
    }

    @Test
    void shouldGenerateObservedProfileForMavenProject() throws Exception {
        Path outputProfile = tempDir.resolve("observed-profile.txt");

        Main.generateObservedProfile(
                Path.of("src/test/resources/sample-maven-project"),
                tempDir.resolve("profile-work"),
                "sample.maven.DemoApplication",
                new String[]{},
                outputProfile
        );

        String profile = Files.readString(outputProfile);

        assertTrue(profile.contains("hint|sample.maven.DiscountService.<init>|vipThreshold|int|500|runtime-observed"));
        assertTrue(profile.contains("hint|sample.maven.Customer.<init>|segment|String|\"PREMIUM\"|runtime-observed"));
        assertTrue(profile.contains("hint|sample.maven.Customer.<init>|type|CustomerType|sample.maven.CustomerType.VIP|runtime-observed"));
        assertTrue(profile.contains("hint|sample.maven.Order.<init>|total|int|620|runtime-observed"));
        assertTrue(profile.contains("hint|sample.maven.Order.<init>|status|String|\"PAID\"|runtime-observed"));
        assertTrue(profile.contains("hint|sample.maven.DiscountService.calculate|couponCode|String|\"BLACK_FRIDAY\"|runtime-observed"));
        assertTrue(profile.contains("hint|sample.maven.DiscountService.shippingFee|amount|int|99|runtime-observed"));
    }

}
