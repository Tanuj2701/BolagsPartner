package com.qa.bolags.reporting;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal PDF 1.4 writer (Helvetica) so execution summaries are available without extra libraries.
 */
final class PdfExecutionReportWriter {

    private PdfExecutionReportWriter() {
    }

    static void write(Path file, ExecutionRunModel model) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("BolagsPartner QA — Execution Report");
        lines.add("Mode: " + model.browserMode + "    Status: " + model.overallStatus);
        if (model.dryRun) {
            lines.add("DRY RUN: Cucumber bindings checked; application behavior was not executed.");
        }
        lines.add("Started: " + model.startedAt);
        lines.add("Finished: " + model.finishedAt);
        lines.add("Duration: " + HtmlExecutionReportWriter.formatDuration(model.durationMs));
        lines.add("Pass rate: " + (Double.isNaN(model.passRatePct) ? RunMetadata.NA
            : String.format(java.util.Locale.ROOT, "%.1f%%", model.passRatePct)));
        lines.add("Release confidence: " + (Double.isNaN(model.releaseConfidenceIndex) ? RunMetadata.NA
            : String.format(java.util.Locale.ROOT, "%.0f/100", model.releaseConfidenceIndex))
            + " (" + model.releaseConfidenceIndication + ")");
        lines.add("");
        lines.add("Feature: " + model.featuresPassed + "/" + model.featuresTotal
            + " passed, " + model.featuresFailed + " failed, " + model.featuresPartial
            + " partial, " + model.featuresSkipped + " skipped, " + model.featuresBlocked + " blocked");
        lines.add("Scenario (TC): " + model.scenariosPassed + "/" + model.scenariosTotal
            + " passed, " + model.scenariosFailed + " failed, " + model.scenariosBroken + " broken, "
                + model.scenariosSkipped + " skipped, " + model.scenariosBlocked + " blocked, "
                + model.scenariosDryRun + " dry-run");
        lines.add("Step: " + model.stepsPassed + "/" + model.stepsTotal
                + " passed, " + model.stepsFailed + " failed, " + model.stepsSkipped + " skipped, "
            + model.stepsUndefined + " undefined, " + model.stepsBlocked + " blocked");
        lines.add("HTTP 4xx/5xx: " + model.httpErrors.size());
        lines.add("XHR/Fetch total: " + (model.xhrTotal < 0 ? RunMetadata.NA : model.xhrTotal)
            + " (success " + (model.xhrSuccess < 0 ? RunMetadata.NA : model.xhrSuccess)
            + ", 4xx " + model.xhr4xx + ", 5xx " + model.xhr5xx + ")");
        lines.add("");
        lines.add("RELEASE RISKS");
        if (model.releaseRisks.isEmpty()) {
            lines.add("None identified from available evidence");
        } else {
            for (String risk : model.releaseRisks) {
            lines.add("- " + risk);
            }
        }
        lines.add("");
        lines.add("FAILURES");
        if (model.failures.isEmpty()) {
            lines.add("None");
        } else {
            for (ExecutionRunModel.FailureRecord failure : model.failures) {
                lines.add("- [" + failure.level + "] [" + failure.category + "] "
                    + failure.feature + " / " + failure.scenario);
                lines.add("  Step: " + failure.step);
                lines.add("  Mode: " + failure.executionMode + "  URL: " + failure.pageUrl);
                lines.add("  Screenshot: " + failure.screenshot);
                addWrapped(lines, failure.message, 90);
            }
        }
        lines.add("");
        lines.add("API ERRORS (4xx/5xx)");
        if (model.httpErrors.isEmpty()) {
            lines.add("None");
        } else {
            for (HttpErrorCapture.HttpError error : model.httpErrors) {
                lines.add("- " + error.status + " " + error.resourceType + " " + error.method + " " + error.url
                    + " (" + (error.responseTimeMs < 0 ? RunMetadata.NA : error.responseTimeMs + " ms") + ")");
                addWrapped(lines, error.body, 90);
                if (!error.curlCommand.isEmpty()) {
                    lines.add("  Reproduction (redacted cURL):");
                    addWrapped(lines, error.curlCommand, 86);
                }
            }
        }
        lines.add("");
        lines.add("FEATURES / SCENARIOS / STEPS");
        for (ExecutionRunModel.FeatureResult feature : model.features) {
            lines.add("Feature: " + feature.name + " [" + feature.status + "]");
            for (ExecutionRunModel.ScenarioResult scenario : feature.scenarios) {
                lines.add("  Scenario: " + scenario.name + " [" + scenario.status + "]");
                for (ExecutionRunModel.StepResult step : scenario.steps) {
                    lines.add("    " + step.keyword + step.text + " [" + step.status + "]");
                    if (!step.error.isEmpty()) {
                        addWrapped(lines, step.error, 86);
                    }
                }
            }
        }
        Files.createDirectories(file.getParent());
        Files.write(file, buildPdf(lines));
    }

    private static void addWrapped(List<String> lines, String text, int width) {
        if (text == null || text.isEmpty()) {
            return;
        }
        String cleaned = text.replace("\r", " ").replace("\t", " ");
        for (String raw : cleaned.split("\n")) {
            String remaining = raw;
            while (remaining.length() > width) {
                lines.add("    " + remaining.substring(0, width));
                remaining = remaining.substring(width);
            }
            if (!remaining.isEmpty()) {
                lines.add("    " + remaining);
            }
        }
    }

    private static byte[] buildPdf(List<String> lines) throws IOException {
        StringBuilder stream = new StringBuilder();
        int y = 800;
        List<String> pageStreams = new ArrayList<>();
        stream.append("BT /F1 11 Tf 50 ").append(y).append(" Td\n");
        for (String line : lines) {
            if (y < 60) {
                stream.append("ET\n");
                pageStreams.add(stream.toString());
                stream = new StringBuilder();
                y = 800;
                stream.append("BT /F1 11 Tf 50 ").append(y).append(" Td\n");
            }
            stream.append("(").append(pdfEscape(line)).append(") Tj\n0 -14 Td\n");
            y -= 14;
        }
        stream.append("ET\n");
        pageStreams.add(stream.toString());

        List<byte[]> content = new ArrayList<>();
        for (String s : pageStreams) {
            content.add(s.getBytes(StandardCharsets.ISO_8859_1));
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        write(out, "%PDF-1.4\n");
        offsets.add(0);
        // 1: catalog
        offsets.add(out.size());
        write(out, "1 0 obj<< /Type /Catalog /Pages 2 0 R >>endobj\n");
        // 2: pages
        offsets.add(out.size());
        StringBuilder kids = new StringBuilder("[");
        int fontObj = 3;
        int firstPageObj = 4;
        for (int i = 0; i < content.size(); i++) {
            kids.append(firstPageObj + i * 2).append(" 0 R ");
        }
        kids.append("]");
        write(out, "2 0 obj<< /Type /Pages /Kids " + kids + " /Count " + content.size() + " >>endobj\n");
        // 3: font
        offsets.add(out.size());
        write(out, "3 0 obj<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>endobj\n");

        for (int i = 0; i < content.size(); i++) {
            int pageObj = firstPageObj + i * 2;
            int streamObj = pageObj + 1;
            offsets.add(out.size());
            write(out, pageObj + " 0 obj<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 "
                    + fontObj + " 0 R >> >> /Contents " + streamObj + " 0 R >>endobj\n");
            offsets.add(out.size());
            byte[] body = content.get(i);
            write(out, streamObj + " 0 obj<< /Length " + body.length + " >>stream\n");
            out.write(body);
            write(out, "\nendstream\nendobj\n");
        }

        int xref = out.size();
        int objectCount = 3 + content.size() * 2;
        write(out, "xref\n0 " + (objectCount + 1) + "\n");
        write(out, "0000000000 65535 f \n");
        for (int i = 1; i < offsets.size(); i++) {
            write(out, String.format("%010d 00000 n \n", offsets.get(i)));
        }
        write(out, "trailer<< /Size " + (objectCount + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF");
        return out.toByteArray();
    }

    private static void write(ByteArrayOutputStream out, String s) throws IOException {
        out.write(s.getBytes(StandardCharsets.ISO_8859_1));
    }

    private static String pdfEscape(String text) {
        String safe = text == null ? "" : text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
        StringBuilder ascii = new StringBuilder();
        for (int i = 0; i < safe.length(); i++) {
            char c = safe.charAt(i);
            ascii.append(c < 32 || c > 126 ? '?' : c);
        }
        if (ascii.length() > 110) {
            return ascii.substring(0, 110);
        }
        return ascii.toString();
    }
}
