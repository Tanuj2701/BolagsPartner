package com.qa.bolags.reporting;

import com.qa.bolags.constants.Constants;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Headed vs headless report folders under {@code target/reports}.
 */
public final class ReportPaths {

    public static final Path ROOT = Paths.get("target", "reports");
    public static final Path DASHBOARD = ROOT.resolve("dashboard");
    public static final Path RUNS_INDEX = DASHBOARD.resolve("runs.json");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS");

    private ReportPaths() {
    }

    public static String browserMode() {
        return Constants.isHeadless() ? "headless" : "headed";
    }

    public static Path newRunDirectory() {
        return ROOT.resolve(browserMode()).resolve(STAMP.format(LocalDateTime.now()));
    }
}
