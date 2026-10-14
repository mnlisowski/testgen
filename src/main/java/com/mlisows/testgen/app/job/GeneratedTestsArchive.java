package com.mlisows.testgen.app.job;

import com.mlisows.testgen.usecase.generation.GeneratedTestFile;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class GeneratedTestsArchive {
    public byte[] create(List<GeneratedTestFile> tests) throws IOException {
        ByteArrayOutputStream archiveBytes = new ByteArrayOutputStream();
        try (ZipOutputStream archive = new ZipOutputStream(archiveBytes, StandardCharsets.UTF_8)) {
            for (GeneratedTestFile test : tests) {
                String entryName = test.relativePath().toString().replace('\\', '/');
                archive.putNextEntry(new ZipEntry(entryName));
                archive.write(test.content().getBytes(StandardCharsets.UTF_8));
                archive.closeEntry();
            }
        }
        return archiveBytes.toByteArray();
    }
}
