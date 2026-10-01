package com.qa.bolags.reporting;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class ExecutionReportJson {

    private ExecutionReportJson() {
    }

    static void write(Path file, ExecutionRunModel model) throws IOException {
        JSONObject root = new JSONObject();
        root.put("browserMode", model.browserMode);
        root.put("startedAt", model.startedAt);
        root.put("finishedAt", model.finishedAt);
        root.put("durationMs", model.durationMs);
        root.put("overallStatus", model.overallStatus);
        root.put("dryRun", model.dryRun);
        root.put("passRatePct", finiteOrNull(model.passRatePct));
        root.put("releaseConfidenceIndex", finiteOrNull(model.releaseConfidenceIndex));
        root.put("releaseConfidenceIndication", model.releaseConfidenceIndication);
        root.put("releaseRisks", new JSONArray(model.releaseRisks));
        root.put("metadata", metadataJson(model.metadata));
        JSONObject api = new JSONObject();
        api.put("total", model.xhrTotal);
        api.put("success", model.xhrSuccess);
        api.put("4xx", model.xhr4xx);
        api.put("5xx", model.xhr5xx);
        root.put("api", api);
        JSONObject counts = new JSONObject();
        JSONObject featureCounts = counts(model.featuresTotal, model.featuresPassed, model.featuresFailed,
            model.featuresSkipped);
        featureCounts.put("partial", model.featuresPartial);
        featureCounts.put("blocked", model.featuresBlocked);
        featureCounts.put("dryRun", model.featuresDryRun);
        counts.put("features", featureCounts);
        JSONObject scenarioCounts = counts(model.scenariosTotal, model.scenariosPassed, model.scenariosFailed,
            model.scenariosSkipped);
        scenarioCounts.put("broken", model.scenariosBroken);
        scenarioCounts.put("blocked", model.scenariosBlocked);
        scenarioCounts.put("dryRun", model.scenariosDryRun);
        counts.put("scenarios", scenarioCounts);
        counts.put("steps", stepCounts(model));
        root.put("counts", counts);
        root.put("features", featuresJson(model.features));
        root.put("failures", failuresJson(model.failures));
        root.put("httpErrors", httpJson(model.httpErrors));
        Files.createDirectories(file.getParent());
        Files.write(file, root.toString(2).getBytes(StandardCharsets.UTF_8));
    }

    private static Object finiteOrNull(double value) {
        return Double.isNaN(value) || Double.isInfinite(value) ? JSONObject.NULL : value;
    }

    private static JSONObject counts(int total, int passed, int failed, int skipped) {
        JSONObject o = new JSONObject();
        o.put("total", total);
        o.put("passed", passed);
        o.put("failed", failed);
        o.put("skipped", skipped);
        return o;
    }

    private static JSONObject stepCounts(ExecutionRunModel model) {
        JSONObject o = counts(model.stepsTotal, model.stepsPassed, model.stepsFailed, model.stepsSkipped);
        o.put("undefined", model.stepsUndefined);
        o.put("blocked", model.stepsBlocked);
        o.put("dryRun", model.stepsDryRun);
        return o;
    }

    private static JSONObject metadataJson(RunMetadata metadata) {
        JSONObject json = new JSONObject();
        json.put("executionId", metadata.executionId);
        json.put("runId", metadata.runId);
        json.put("date", metadata.date);
        json.put("startTime", metadata.startTime);
        json.put("endTime", metadata.endTime);
        json.put("environment", metadata.environment);
        json.put("browser", metadata.browser);
        json.put("browserVersion", metadata.browserVersion);
        json.put("operatingSystem", metadata.operatingSystem);
        json.put("branch", metadata.branch);
        json.put("commitId", metadata.commitId);
        json.put("buildNumber", metadata.buildNumber);
        json.put("frameworkVersion", metadata.frameworkVersion);
        json.put("testSuite", metadata.testSuite);
        json.put("parallelWorkers", metadata.parallelWorkers);
        json.put("testDataIdentifier", metadata.testDataIdentifier);
        json.put("executionMode", metadata.executionMode);
        json.put("timeZone", metadata.timeZone);
        return json;
    }

    private static JSONArray featuresJson(List<ExecutionRunModel.FeatureResult> features) {
        JSONArray arr = new JSONArray();
        for (ExecutionRunModel.FeatureResult feature : features) {
            JSONObject f = new JSONObject();
            f.put("name", feature.name);
            f.put("uri", feature.uri);
            f.put("status", feature.status);
            f.put("durationMs", feature.durationMs);
            f.put("tags", new JSONArray(feature.tags));
            JSONArray scenarios = new JSONArray();
            for (ExecutionRunModel.ScenarioResult scenario : feature.scenarios) {
                JSONObject s = new JSONObject();
                s.put("name", scenario.name);
                s.put("status", scenario.status);
                s.put("durationMs", scenario.durationMs);
                s.put("tags", new JSONArray(scenario.tags));
                s.put("screenshot", scenario.screenshot);
                JSONArray steps = new JSONArray();
                for (ExecutionRunModel.StepResult step : scenario.steps) {
                    JSONObject st = new JSONObject();
                    st.put("keyword", step.keyword);
                    st.put("text", step.text);
                    st.put("status", step.status);
                    st.put("durationMs", step.durationMs);
                    st.put("failureCategory", step.failureCategory);
                    if (!step.error.isEmpty()) {
                        st.put("error", step.error);
                    }
                    steps.put(st);
                }
                s.put("steps", steps);
                scenarios.put(s);
            }
            f.put("scenarios", scenarios);
            arr.put(f);
        }
        return arr;
    }

    private static JSONArray failuresJson(List<ExecutionRunModel.FailureRecord> failures) {
        JSONArray arr = new JSONArray();
        for (ExecutionRunModel.FailureRecord failure : failures) {
            JSONObject o = new JSONObject();
            o.put("level", failure.level);
            o.put("feature", failure.feature);
            o.put("scenario", failure.scenario);
            o.put("step", failure.step);
            o.put("message", failure.message);
            o.put("category", failure.category);
            o.put("exceptionType", failure.exceptionType);
            o.put("executionMode", failure.executionMode);
            o.put("pageUrl", failure.pageUrl);
            o.put("screenshot", failure.screenshot);
            arr.put(o);
        }
        return arr;
    }

    private static JSONArray httpJson(List<HttpErrorCapture.HttpError> errors) {
        JSONArray arr = new JSONArray();
        for (HttpErrorCapture.HttpError error : errors) {
            JSONObject o = new JSONObject();
            o.put("method", error.method);
            o.put("url", error.url);
            o.put("status", error.status);
            o.put("body", error.body);
            o.put("resourceType", error.resourceType);
            o.put("responseTimeMs", error.responseTimeMs);
            o.put("curlCommand", error.curlCommand);
            arr.put(o);
        }
        return arr;
    }
}
