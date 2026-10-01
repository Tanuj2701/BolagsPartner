package com.qa.bolags.reporting;

import com.qa.bolags.baseTest.BaseTest;
import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.PickleStepTestStep;
import io.cucumber.plugin.event.Result;
import io.cucumber.plugin.event.Status;
import io.cucumber.plugin.event.TestCase;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestCaseStarted;
import io.cucumber.plugin.event.TestRunFinished;
import io.cucumber.plugin.event.TestRunStarted;
import io.cucumber.plugin.event.TestStep;
import io.cucumber.plugin.event.TestStepFinished;
import io.cucumber.plugin.event.TestStepStarted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Cucumber plugin: feature / scenario / step status, failures, 4xx/5xx APIs, HTML + PDF, dashboard.
 * Reports are split by headed vs headless under {@code target/reports}.
 */
public class ExecutionReportPlugin implements ConcurrentEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutionReportPlugin.class);
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ExecutionRunModel model = new ExecutionRunModel();
    private LocalDateTime startedAt;
    private Path runDir;

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestRunStarted.class, this::onRunStarted);
        publisher.registerHandlerFor(TestCaseStarted.class, this::onCaseStarted);
        publisher.registerHandlerFor(TestStepStarted.class, this::onStepStarted);
        publisher.registerHandlerFor(TestStepFinished.class, this::onStepFinished);
        publisher.registerHandlerFor(TestCaseFinished.class, this::onCaseFinished);
        publisher.registerHandlerFor(TestRunFinished.class, this::onRunFinished);
    }

    private void onRunStarted(TestRunStarted event) {
        HttpErrorCapture.reset();
        startedAt = LocalDateTime.now();
        model.browserMode = ReportPaths.browserMode();
        model.dryRun = Boolean.parseBoolean(System.getProperty("cucumber.execution.dry-run", "false"));
        model.startedAt = ISO.format(startedAt);
        runDir = ReportPaths.newRunDirectory();
        model.runDirectory = runDir;
        ReportSession.begin(runDir);
        model.metadata = RunMetadata.captureStart();
        model.metadata.date = LocalDate.now().toString();
        model.metadata.startTime = model.startedAt;
        model.metadata.testSuite = System.getProperty("suiteFile", RunMetadata.NA);
        model.metadata.executionMode = model.browserMode;
        LOG.info("Execution report started in {} mode", model.browserMode);
    }

    private void onCaseStarted(TestCaseStarted event) {
        TestCase testCase = event.getTestCase();
        String uri = testCase.getUri() == null ? "" : testCase.getUri().toString();
        ExecutionRunModel.FeatureResult feature = model.featureByUri.get(uri);
        if (feature == null) {
            feature = new ExecutionRunModel.FeatureResult();
            feature.uri = uri;
            feature.name = featureName(uri, testCase);
            model.featureByUri.put(uri, feature);
            model.features.add(feature);
        }
        ExecutionRunModel.ScenarioResult scenario = new ExecutionRunModel.ScenarioResult();
        scenario.name = testCase.getName();
        scenario.uri = uri;
        scenario.featureName = feature.name;
        scenario.startedNanos = System.nanoTime();
        java.util.List<String> tags = testCase.getTags();
        if (tags != null) {
            for (String tag : tags) {
                if (tag != null && !scenario.tags.contains(tag)) {
                    scenario.tags.add(tag);
                }
                if (tag != null && !feature.tags.contains(tag)) {
                    feature.tags.add(tag);
                }
            }
        }
        feature.scenarios.add(scenario);
        model.currentScenario = scenario;
    }

    private void onStepStarted(TestStepStarted event) {
        RunMetadata.refreshBrowser(model.metadata);
    }

    private void onStepFinished(TestStepFinished event) {
        HttpErrorCapture.drain(BaseTest.driver);
        TestStep testStep = event.getTestStep();
        if (!(testStep instanceof PickleStepTestStep)) {
            Result hookResult = event.getResult();
            if (hookResult != null && hookResult.getError() != null) {
                addFailure("HOOK", model.currentScenario, String.valueOf(testStep),
                        stack(hookResult.getError()), hookResult.getError(),
                        category(hookResult.getError(), stack(hookResult.getError())));
            }
            return;
        }
        PickleStepTestStep pickle = (PickleStepTestStep) testStep;
        ExecutionRunModel.StepResult step = new ExecutionRunModel.StepResult();
        step.keyword = pickle.getStep().getKeyword() == null ? "" : pickle.getStep().getKeyword();
        step.text = pickle.getStep().getText() == null ? "" : pickle.getStep().getText();
        Result result = event.getResult();
        step.status = model.dryRun ? "DRY_RUN"
            : result.getStatus() == null ? "UNKNOWN" : result.getStatus().name();
        step.durationMs = result.getDuration() == null ? 0 : result.getDuration().toMillis();
        if (result.getError() != null) {
            step.error = stack(result.getError());
        }
        if (isFailureStatus(step.status)) {
            step.failureCategory = category(result.getError(), step.error);
        }
        if (model.currentScenario != null) {
            model.currentScenario.steps.add(step);
        }
        tallyStep(result.getStatus());
        if (isFailureStatus(step.status)) {
            addFailure("STEP", model.currentScenario, (step.keyword + step.text).trim(),
                    step.error.isEmpty() ? step.status : step.error, result.getError(), step.failureCategory);
        }
    }

    private void onCaseFinished(TestCaseFinished event) {
        HttpErrorCapture.drain(BaseTest.driver);
        ExecutionRunModel.ScenarioResult scenario = model.currentScenario;
        if (scenario == null) {
            return;
        }
        scenario.durationMs = (System.nanoTime() - scenario.startedNanos) / 1_000_000L;
        Status status = event.getResult() == null ? Status.PASSED : event.getResult().getStatus();
        scenario.status = model.dryRun ? "DRY_RUN" : status.name();
        if (!model.dryRun && isFailureStatus(scenario.status)
                && RunMetadata.NA.equals(scenario.screenshot)
                && !RunMetadata.NA.equals(ReportSession.lastScreenshot())) {
            scenario.screenshot = ReportSession.lastScreenshot();
        }
        model.scenariosTotal++;
        if (model.dryRun) {
            model.scenariosDryRun++;
        } else if (status == Status.PASSED) {
            model.scenariosPassed++;
        } else if ("PENDING".equals(scenario.status) || "BLOCKED".equals(scenario.status)) {
            model.scenariosBlocked++;
        } else if (status == Status.SKIPPED) {
            model.scenariosSkipped++;
        } else {
            String message = event.getResult() == null ? scenario.status : stack(event.getResult().getError());
            String category = category(event.getResult() == null ? null : event.getResult().getError(), message);
            if (isBrokenCategory(category)) {
                model.scenariosBroken++;
            } else {
                model.scenariosFailed++;
            }
            if (event.getResult() != null && event.getResult().getError() != null
                    && !alreadyHasScenarioFailure(scenario.name)) {
                addFailure("SCENARIO", scenario, "", message, event.getResult().getError(), category);
            }
        }
        tallyFeatureScenarios(model.featureByUri.get(scenario.uri), scenario.status);
        model.currentScenario = null;
    }

    private void onRunFinished(TestRunFinished event) {
        HttpErrorCapture.drain(BaseTest.driver);
        model.httpErrors.addAll(HttpErrorCapture.snapshot());
        model.finishedAt = ISO.format(LocalDateTime.now());
        if (startedAt != null) {
            model.durationMs = ChronoUnit.MILLIS.between(startedAt, LocalDateTime.now());
        }
        for (ExecutionRunModel.FeatureResult feature : model.features) {
            boolean failed = false;
            boolean passed = false;
            boolean skipped = false;
            boolean blocked = false;
            boolean dryRun = false;
            long duration = 0;
            for (ExecutionRunModel.ScenarioResult scenario : feature.scenarios) {
                duration += scenario.durationMs;
                if ("PASSED".equals(scenario.status)) {
                    passed = true;
                } else if ("SKIPPED".equals(scenario.status)) {
                    skipped = true;
                } else if ("PENDING".equals(scenario.status) || "BLOCKED".equals(scenario.status)) {
                    blocked = true;
                } else if ("DRY_RUN".equals(scenario.status)) {
                    dryRun = true;
                } else {
                    failed = true;
                }
            }
            feature.durationMs = duration;
            feature.status = dryRun ? "DRY_RUN" : failed ? "FAILED" : blocked ? "BLOCKED"
                    : passed && skipped ? "PARTIAL" : skipped ? "SKIPPED" : "PASSED";
            model.featuresTotal++;
            if ("FAILED".equals(feature.status)) {
                model.featuresFailed++;
            } else if ("PARTIAL".equals(feature.status)) {
                model.featuresPartial++;
            } else if ("SKIPPED".equals(feature.status)) {
                model.featuresSkipped++;
            } else if ("BLOCKED".equals(feature.status)) {
                model.featuresBlocked++;
            } else if ("DRY_RUN".equals(feature.status)) {
                model.featuresDryRun++;
            } else {
                model.featuresPassed++;
            }
        }
        boolean runFailed = event.getResult() != null && event.getResult().getError() != null;
        model.xhrTotal = HttpErrorCapture.apiTotal();
        model.xhrSuccess = HttpErrorCapture.apiSuccess();
        model.xhr4xx = HttpErrorCapture.api4xx();
        model.xhr5xx = HttpErrorCapture.api5xx();
        model.passRatePct = model.dryRun ? Double.NaN : QualityIntelligence.pct(model.scenariosPassed,
            model.scenariosPassed + model.scenariosFailed + model.scenariosBroken);
        model.overallStatus = model.dryRun ? "DRY_RUN" : (model.featuresFailed > 0 || model.scenariosFailed > 0
            || model.scenariosBroken > 0 || runFailed) ? "FAILED"
            : model.scenariosBlocked > 0 && model.scenariosPassed == 0 ? "BLOCKED"
            : (model.scenariosSkipped > 0 || model.scenariosBlocked > 0) && model.scenariosPassed > 0
            ? "PARTIAL" : model.scenariosPassed == 0 && model.scenariosSkipped > 0
            ? "SKIPPED" : "PASSED";
        model.metadata.endTime = model.finishedAt;
        if (runFailed) {
            addFailure("RUN", null, "", stack(event.getResult().getError()),
                event.getResult().getError(), category(event.getResult().getError(), ""));
        }
        writeArtifacts();
    }

    private void writeArtifacts() {
        try {
            Path html = runDir.resolve("html").resolve("index.html");
            Path pdf = runDir.resolve("pdf").resolve("execution-report.pdf");
            JSONArray history = readHistory();
            QualityIntelligence.Snapshot quality = QualityIntelligence.analyse(model, history);
            if (model.dryRun) {
                quality.confidence.score = Double.NaN;
                quality.confidence.indication = "NOT EVALUATED";
                quality.confidence.statusLabel = "Cucumber dry run; browser behavior was not executed";
                quality.signOffIndication = "NOT EVALUATED";
                quality.confidence.primaryFactors.clear();
                quality.confidence.primaryFactors.add(
                        "Dry run validates step bindings only; execution outcomes are unavailable");
                quality.executiveNarrative = "Cucumber dry run validated feature-step bindings only. "
                    + "No browser behavior, pass rate, API health, or release confidence was evaluated.";
            }
            model.releaseConfidenceIndex = quality.confidence.score;
            model.releaseConfidenceIndication = quality.confidence.indication;
            model.releaseRisks.addAll(quality.confidence.primaryFactors);
            ExecutionReportJson.write(runDir.resolve("summary.json"), model);
            HtmlExecutionReportWriter.write(html, model);
            PdfExecutionReportWriter.write(pdf, model);
            QualityDashboardWriter.write(runDir.resolve("html").resolve("quality.html"), quality, history);
            CsvExportWriter.write(runDir.resolve("export"), model);
                ReportDashboardWriter.update(runDir, model, html, pdf,
                    runDir.resolve("html").resolve("quality.html"), runDir.resolve("export"));
            LOG.info("Execution reports written to {} (dashboard: {})",
                    runDir.toAbsolutePath(), ReportPaths.DASHBOARD.resolve("index.html").toAbsolutePath());
        } catch (Exception e) {
            LOG.error("Unable to write execution reports: {}", e.getMessage(), e);
        }
    }

    private void tallyStep(Status status) {
        model.stepsTotal++;
        if (model.dryRun) {
            model.stepsDryRun++;
            return;
        }
        if (status == Status.PASSED) {
            model.stepsPassed++;
        } else if (status == Status.SKIPPED) {
            model.stepsSkipped++;
        } else if (status == Status.UNDEFINED) {
            model.stepsUndefined++;
        } else if ("PENDING".equals(status.name()) || "BLOCKED".equals(status.name())) {
            model.stepsBlocked++;
        } else {
            model.stepsFailed++;
        }
    }

    private void tallyFeatureScenarios(ExecutionRunModel.FeatureResult feature, String status) {
        if (feature == null) {
            return;
        }
        if ("PASSED".equals(status)) {
            feature.scenariosPassed++;
        } else if ("SKIPPED".equals(status)) {
            feature.scenariosSkipped++;
        } else if ("PENDING".equals(status) || "BLOCKED".equals(status)) {
            feature.scenariosBlocked++;
        } else if ("DRY_RUN".equals(status)) {
            feature.scenariosDryRun++;
        } else {
            feature.scenariosFailed++;
        }
    }

    private void addFailure(String level, ExecutionRunModel.ScenarioResult scenario, String step,
                            String message, Throwable error, String category) {
        ExecutionRunModel.FailureRecord failure = new ExecutionRunModel.FailureRecord();
        failure.level = level;
        failure.feature = scenario == null ? "" : scenario.featureName;
        failure.scenario = scenario == null ? "" : scenario.name;
        failure.step = step;
        failure.message = message == null ? "" : message;
        failure.exceptionType = FailureClassifier.exceptionType(message);
        failure.category = category;
        failure.executionMode = model.browserMode;
        WebDriver driver = BaseTest.driver;
        if (driver != null) {
            try {
                failure.pageUrl = HttpErrorCapture.sanitizeUrl(driver.getCurrentUrl());
            } catch (Exception ignored) {
                failure.pageUrl = RunMetadata.NA;
            }
        }
        failure.screenshot = saveFailureScreenshot(driver);
                if (RunMetadata.NA.equals(failure.screenshot)) {
                    failure.screenshot = ReportSession.lastScreenshot();
                }
        if (scenario != null && RunMetadata.NA.equals(scenario.screenshot)) {
            scenario.screenshot = failure.screenshot;
        }
        model.failures.add(failure);
    }

    private String saveFailureScreenshot(WebDriver driver) {
        return ReportSession.captureFailureScreenshot(driver) == null
                ? RunMetadata.NA : ReportSession.lastScreenshot();
    }

    private String category(Throwable error, String message) {
        String text = message == null ? "" : message.toLowerCase();
        boolean explicitlyHttp = text.contains("http") || text.contains("status code")
                || text.contains("response code") || text.contains("4xx") || text.contains("5xx");
        boolean has4xx = false;
        boolean has5xx = false;
        if (explicitlyHttp) {
            for (HttpErrorCapture.HttpError httpError : HttpErrorCapture.snapshot()) {
                has4xx |= httpError.status >= 400 && httpError.status < 500;
                has5xx |= httpError.status >= 500;
            }
        }
        return FailureClassifier.classify(FailureClassifier.exceptionType(message), message, has5xx, has4xx);
    }

    private static boolean isFailureStatus(String status) {
        return "FAILED".equals(status) || "UNDEFINED".equals(status) || "AMBIGUOUS".equals(status)
                || "PENDING".equals(status);
    }

    private static boolean isBrokenCategory(String category) {
        return "Browser".equals(category) || "Environment".equals(category) || "Network".equals(category)
                || "Automation Framework".equals(category) || "Timeout".equals(category);
    }

    private JSONArray readHistory() {
        try {
            Path file = ReportPaths.RUNS_INDEX;
            if (Files.exists(file)) {
                return new JSONArray(new String(Files.readAllBytes(file), java.nio.charset.StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            LOG.warn("Unable to read report history: {}", e.getMessage());
        }
        return new JSONArray();
    }

    private boolean alreadyHasScenarioFailure(String scenarioName) {
        for (ExecutionRunModel.FailureRecord failure : model.failures) {
            if ("SCENARIO".equals(failure.level) && scenarioName.equals(failure.scenario)) {
                return true;
            }
        }
        return false;
    }

    private static String featureName(String uri, TestCase testCase) {
        if (uri.contains("/")) {
            return uri.substring(uri.lastIndexOf('/') + 1);
        }
        return testCase.getName();
    }

    private static String stack(Throwable error) {
        if (error == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(error).append('\n');
        StackTraceElement[] frames = error.getStackTrace();
        int limit = Math.min(frames.length, 12);
        for (int i = 0; i < limit; i++) {
            sb.append("  at ").append(frames[i]).append('\n');
        }
        return sb.toString();
    }
}
