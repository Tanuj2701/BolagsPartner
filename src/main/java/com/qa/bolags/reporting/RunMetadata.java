package com.qa.bolags.reporting;

import com.qa.bolags.baseTest.BaseTest;
import com.qa.bolags.constants.Constants;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.Locale;

/**
 * Execution metadata captured from the runtime. Missing values stay "Data Not Available".
 */
public final class RunMetadata {

    public static final String NA = "Data Not Available";

    public String executionId = NA;
    public String runId = NA;
    public String date = NA;
    public String startTime = NA;
    public String endTime = NA;
    public String environment = "QA";
    public String browser = NA;
    public String browserVersion = NA;
    public String operatingSystem = NA;
    public String branch = NA;
    public String commitId = NA;
    public String buildNumber = NA;
    public String frameworkVersion = "1.0-SNAPSHOT";
    public String testSuite = NA;
    public String parallelWorkers = "1";
    public String testDataIdentifier = NA;
    public String executionMode = ReportPaths.browserMode();
    public String timeZone = ZoneId.systemDefault().getId();

    public static RunMetadata captureStart() {
        RunMetadata meta = new RunMetadata();
        meta.executionId = emptyToNa(ReportSession.executionId());
        meta.runId = emptyToNa(ReportSession.runId());
        meta.executionMode = ReportPaths.browserMode();
        meta.environment = emptyToNa(System.getProperty("qa.environment", "QA"));
        meta.operatingSystem = emptyToNa(System.getProperty("os.name") + " " + System.getProperty("os.version"));
        meta.buildNumber = firstNonBlank(System.getenv("BUILD_NUMBER"), System.getenv("GITHUB_RUN_NUMBER"),
                System.getenv("BUILD_BUILDNUMBER"), NA);
        meta.frameworkVersion = emptyToNa(System.getProperty("project.version", "1.0-SNAPSHOT"));
        meta.parallelWorkers = emptyToNa(System.getProperty("dataproviderthreadcount", "1"));
        meta.testDataIdentifier = emptyToNa(System.getProperty("testdata.id", NA));
        meta.branch = git("rev-parse", "--abbrev-ref", "HEAD");
        meta.commitId = git("rev-parse", "--short", "HEAD");
        refreshBrowser(meta);
        return meta;
    }

    public static void refreshBrowser(RunMetadata meta) {
        if (meta == null) {
            return;
        }
        meta.executionMode = Constants.isHeadless() ? "headless" : "headed";
        WebDriver driver = BaseTest.driver;
        if (!(driver instanceof RemoteWebDriver)) {
            return;
        }
        try {
            Capabilities caps = ((RemoteWebDriver) driver).getCapabilities();
            if (caps.getBrowserName() != null && !caps.getBrowserName().isEmpty()) {
                meta.browser = caps.getBrowserName();
            }
            if (caps.getBrowserVersion() != null && !caps.getBrowserVersion().isEmpty()) {
                meta.browserVersion = caps.getBrowserVersion();
            }
        } catch (Exception ignored) {
            // keep previous / NA
        }
    }

    private static String git(String... args) {
        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.command(new String[]{"git"});
            for (int i = 0; i < args.length; i++) {
                pb.command().add(args[i]);
            }
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                process.waitFor();
                if (line == null || line.trim().isEmpty() || line.contains("fatal")) {
                    return NA;
                }
                return line.trim();
            }
        } catch (Exception e) {
            return NA;
        }
    }

    private static String firstNonBlank(String... values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null && !values[i].trim().isEmpty()) {
                return values[i].trim();
            }
        }
        return NA;
    }

    private static String emptyToNa(String value) {
        if (value == null || value.trim().isEmpty()) {
            return NA;
        }
        return value.trim();
    }

    public static String modeLabel() {
        return Constants.isHeadless() ? "headless" : "headed";
    }

    public static String display(String value) {
        return value == null || value.trim().isEmpty() ? NA : value;
    }
}
