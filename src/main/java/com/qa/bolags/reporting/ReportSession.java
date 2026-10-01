package com.qa.bolags.reporting;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Current execution report session (run folder is created at start so artifacts can be stored).
 */
public final class ReportSession {

    private static final Logger LOG = LoggerFactory.getLogger(ReportSession.class);
    private static final AtomicReference<Path> RUN_DIR = new AtomicReference<>();
    private static final AtomicReference<String> RUN_ID = new AtomicReference<>();
    private static final AtomicReference<String> EXECUTION_ID = new AtomicReference<>();
    private static final AtomicReference<String> LAST_SCREENSHOT = new AtomicReference<>(RunMetadata.NA);
    private static final AtomicReference<String> LAST_URL = new AtomicReference<>(RunMetadata.NA);
    private static final AtomicInteger SCREENSHOT_SEQUENCE = new AtomicInteger();

    private ReportSession() {
    }

    public static void begin(Path runDir) {
        RUN_DIR.set(runDir);
        RUN_ID.set(runDir.getFileName().toString());
        EXECUTION_ID.set("exec-" + UUID.randomUUID());
        LAST_SCREENSHOT.set(RunMetadata.NA);
        LAST_URL.set(RunMetadata.NA);
        SCREENSHOT_SEQUENCE.set(0);
    }

    public static Path runDir() {
        return RUN_DIR.get();
    }

    public static String runId() {
        return RUN_ID.get() == null ? "" : RUN_ID.get();
    }

    public static String executionId() {
        return EXECUTION_ID.get() == null ? "" : EXECUTION_ID.get();
    }

    public static Path artifactsDir() {
        Path dir = runDir();
        return dir == null ? null : dir.resolve("artifacts");
    }

    public static void setLastScreenshot(String relativeFromHtml) {
        LAST_SCREENSHOT.set(relativeFromHtml == null ? RunMetadata.NA : relativeFromHtml);
    }

    public static String lastScreenshot() {
        return LAST_SCREENSHOT.get() == null ? RunMetadata.NA : LAST_SCREENSHOT.get();
    }

    public static Path captureFailureScreenshot(WebDriver driver) {
        Path runDir = RUN_DIR.get();
        if (!(driver instanceof TakesScreenshot) || runDir == null) {
            return null;
        }
        try {
            Path artifacts = runDir.resolve("artifacts");
            Files.createDirectories(artifacts);
            String filename = "failure-" + SCREENSHOT_SEQUENCE.incrementAndGet() + ".png";
            Path destination = artifacts.resolve(filename);
            byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Files.write(destination, screenshot);
            LAST_SCREENSHOT.set("../artifacts/" + filename);
            return destination;
        } catch (IOException | RuntimeException e) {
            LOG.warn("Unable to persist failure screenshot: {}", e.getMessage());
            return null;
        }
    }

    public static void setLastUrl(String url) {
        LAST_URL.set(url == null || url.isEmpty() ? RunMetadata.NA : HttpErrorCapture.sanitizeUrl(url));
    }

    public static String lastUrl() {
        return LAST_URL.get() == null ? RunMetadata.NA : LAST_URL.get();
    }
}
