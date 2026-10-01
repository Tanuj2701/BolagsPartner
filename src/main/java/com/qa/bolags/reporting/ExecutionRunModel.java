package com.qa.bolags.reporting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Path;

/**
 * In-memory model for one Cucumber execution (feature / scenario / step + HTTP errors).
 */
public final class ExecutionRunModel {

    public String browserMode = ReportPaths.browserMode();
    public String startedAt = "";
    public String finishedAt = "";
    public long durationMs;
    public String overallStatus = "UNKNOWN";
    public boolean dryRun;
    public int featuresTotal;
    public int featuresPassed;
    public int featuresFailed;
    public int featuresSkipped;
    public int featuresBlocked;
    public int featuresDryRun;
    public int scenariosTotal;
    public int scenariosPassed;
    public int scenariosFailed;
    public int scenariosBroken;
    public int scenariosSkipped;
    public int scenariosBlocked;
    public int scenariosDryRun;
    public int featuresPartial;
    public int stepsTotal;
    public int stepsPassed;
    public int stepsFailed;
    public int stepsSkipped;
    public int stepsUndefined;
    public int stepsBlocked;
    public int stepsDryRun;
    public int xhrTotal = -1;
    public int xhrSuccess = -1;
    public int xhr4xx;
    public int xhr5xx;
    public final List<FeatureResult> features = new ArrayList<>();
    public final List<FailureRecord> failures = new ArrayList<>();
    public final List<HttpErrorCapture.HttpError> httpErrors = new ArrayList<>();

    final Map<String, FeatureResult> featureByUri = new LinkedHashMap<>();
    public RunMetadata metadata = new RunMetadata();
    public double passRatePct = Double.NaN;
    public double releaseConfidenceIndex = Double.NaN;
    public String releaseConfidenceIndication = "AMBER";
    public final List<String> releaseRisks = new ArrayList<>();
    public Path runDirectory;
    ScenarioResult currentScenario;

    public static final class FeatureResult {
        public String name = "";
        public String uri = "";
        public String status = "PASSED";
        public long durationMs;
        public int scenariosPassed;
        public int scenariosFailed;
        public int scenariosSkipped;
        public int scenariosBlocked;
        public int scenariosDryRun;
        public final List<String> tags = new ArrayList<>();
        public final List<ScenarioResult> scenarios = new ArrayList<>();
    }

    public static final class ScenarioResult {
        public String name = "";
        public String featureName = "";
        public String uri = "";
        public String status = "PASSED";
        public long durationMs;
        public String screenshot = RunMetadata.NA;
        public final List<String> tags = new ArrayList<>();
        public final List<StepResult> steps = new ArrayList<>();
        long startedNanos;
    }

    public static final class StepResult {
        public String keyword = "";
        public String text = "";
        public String status = "PASSED";
        public long durationMs;
        public String error = "";
        public String failureCategory = RunMetadata.NA;
    }

    public static final class FailureRecord {
        public String level = "STEP";
        public String feature = "";
        public String scenario = "";
        public String step = "";
        public String message = "";
        public String category = "Unknown";
        public String exceptionType = RunMetadata.NA;
        public String executionMode = RunMetadata.NA;
        public String pageUrl = RunMetadata.NA;
        public String screenshot = RunMetadata.NA;
    }
}
