package com.qa.bolags.utility;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Default upload files shipped with the suite ({@code src/main/ABC.pdf} and review-wizard fixtures).
 */
public final class UploadFixtures {

    private UploadFixtures() {
    }

    public static String defaultPdf() {
        Path path = Paths.get(System.getProperty("user.dir"), "src", "main", "ABC.pdf");
        if (!Files.exists(path)) {
            throw new RuntimeException("Default upload fixture not found at " + path);
        }
        return path.toAbsolutePath().toString();
    }

    /**
     * Multi-page PDF/DOC fixtures for Review Wizards document viewer (each &gt; 10 pages).
     */
    public static List<String> reviewWizardMultiPageFiles() {
        Path dir = Paths.get(System.getProperty("user.dir"), "src", "main", "testdata");
        String[] names = {
                "review-wizard-12p-a.pdf",
                "review-wizard-12p-b.pdf",
                "review-wizard-12p.doc"
        };
        List<String> files = new ArrayList<String>();
        for (int i = 0; i < names.length; i++) {
            Path path = dir.resolve(names[i]);
            if (Files.exists(path)) {
                files.add(path.toAbsolutePath().toString());
            }
        }
        if (files.isEmpty()) {
            files.add(defaultPdf());
        }
        return files;
    }
}
