package com.qa.bolags.reporting;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Evidence-based quality KPIs and QA release confidence. Missing inputs stay unavailable
 * and are excluded from the weighted score (weights re-normalized).
 */
public final class QualityIntelligence {

    public static final String NA = RunMetadata.NA;
    public static final String MAPPING_NA = "Coverage mapping not available";

    private QualityIntelligence() {
    }

    public static Snapshot analyse(ExecutionRunModel model, JSONArray history) {
        Snapshot snap = new Snapshot();
        snap.model = model;
        int executedScenarios = model.scenariosPassed + model.scenariosFailed + model.scenariosBroken;
        snap.scenariosExecuted = executedScenarios;
        snap.passRatePct = pct(model.scenariosPassed, executedScenarios);
        snap.failRatePct = pct(model.scenariosFailed + model.scenariosBroken, executedScenarios);
        snap.skipRatePct = pct(model.scenariosSkipped, model.scenariosTotal);
        snap.stepPassRatePct = pct(model.stepsPassed, model.stepsTotal);
        snap.featurePassRatePct = pct(model.featuresPassed, model.featuresTotal);
        snap.scenarioPassRatePct = snap.passRatePct;
        snap.avgScenarioMs = executedScenarios <= 0 ? -1
                : model.durationMs / Math.max(1, executedScenarios);
        snap.avgStepMs = model.stepsTotal <= 0 ? -1 : model.durationMs / Math.max(1, model.stepsTotal);
        snap.functionalCoveragePct = pct(executedScenarios, model.scenariosTotal);
        snap.automationCoverageNote = MAPPING_NA;
        snap.requirementCoverageNote = MAPPING_NA;
        snap.regressionCoveragePct = regressionPass(model);
        snap.apiSuccessPct = model.xhrTotal > 0 ? pct(model.xhrSuccess, model.xhrTotal) : Double.NaN;
        snap.http4xx = model.xhr4xx;
        snap.http5xx = model.xhr5xx;
        snap.criticalFailures = countCategory(model, "API") + count5xxFailures(model);
        snap.highFailures = model.failures.size();
        snap.openDefectsNote = NA;
        snap.blockedNote = "Cucumber does not emit a Blocked status";
        snap.journeys = journeys(model);
        snap.suiteBuckets = suiteBuckets(model);
        snap.failureSignatures = groupFailures(model);
        snap.slowScenarios = slowScenarios(model, 8);
        snap.slowSteps = slowSteps(model, 8);
        snap.environmentHealth = environmentHealth(model);
        snap.testDataHealth = testDataHealth(model);
        applyHistory(snap, history, model);
        snap.confidence = confidence(snap, model);
        snap.signOffIndication = snap.confidence.indication;
        snap.executiveNarrative = narrative(snap, model);
        return snap;
    }

    public static double pct(int part, int whole) {
        if (whole <= 0) {
            return Double.NaN;
        }
        return Math.round(part * 1000.0 / whole) / 10.0;
    }

    public static String fmtPct(double value) {
        return Double.isNaN(value) ? NA : String.format(Locale.US, "%.1f%%", value);
    }

    public static String fmtNum(int value, boolean available) {
        return available ? String.valueOf(value) : NA;
    }

    private static double regressionPass(ExecutionRunModel model) {
        int total = 0;
        int passed = 0;
        for (ExecutionRunModel.FeatureResult feature : model.features) {
            boolean regression = containsTag(feature.tags, "regression") || containsTag(feature.tags, "e2e")
                    || feature.name.toLowerCase().contains("timeopt");
            for (ExecutionRunModel.ScenarioResult scenario : feature.scenarios) {
                boolean scReg = regression || containsTag(scenario.tags, "regression")
                        || containsTag(scenario.tags, "e2e") || containsTag(scenario.tags, "time-optimized");
                if (!scReg) {
                    continue;
                }
                total++;
                if ("PASSED".equals(scenario.status)) {
                    passed++;
                }
            }
        }
        return pct(passed, total);
    }

    private static boolean containsTag(List<String> tags, String needle) {
        if (tags == null) {
            return false;
        }
        for (String tag : tags) {
            if (tag != null && tag.replace("@", "").equalsIgnoreCase(needle)) {
                return true;
            }
        }
        return false;
    }

    private static int countCategory(ExecutionRunModel model, String category) {
        int n = 0;
        for (ExecutionRunModel.FailureRecord failure : model.failures) {
            if (category.equalsIgnoreCase(failure.category)) {
                n++;
            }
        }
        return n;
    }

    private static int count5xxFailures(ExecutionRunModel model) {
        int n = 0;
        for (HttpErrorCapture.HttpError error : model.httpErrors) {
            if (error.status >= 500) {
                n++;
            }
        }
        return n;
    }

    private static List<Journey> journeys(ExecutionRunModel model) {
        String[][] defs = {
                {"Liquidation placement", "login", "offer page", "Fornamn", "Request Received"},
                {"Admin offer & agreement", "admin login", "send quote", "Accept the offer", "agreement"},
                {"Client document checklist", "checklist", "uploads one document"},
                {"Document review wizard", "Review Wizards", "approves the first document", "review wizard"},
                {"Post-INK2 lifecycle", "board change", "Bolagsverket", "liquidation order should be marked"},
                {"Admin order list", "generic order list"},
                {"Shiro & Reseller users", "Shiro user", "reseller"}
        };
        List<Journey> list = new ArrayList<>();
        List<ExecutionRunModel.StepResult> all = allSteps(model);
        for (String[] def : defs) {
            Journey j = new Journey();
            j.name = def[0];
            int matched = 0;
            int passed = 0;
            int failed = 0;
            int skipped = 0;
            boolean apiFail = false;
            for (int i = 1; i < def.length; i++) {
                String needle = def[i].toLowerCase();
                for (ExecutionRunModel.StepResult step : all) {
                    if (step.text.toLowerCase().contains(needle)) {
                        matched++;
                        if ("PASSED".equals(step.status)) {
                            passed++;
                        } else if ("FAILED".equals(step.status)) {
                            failed++;
                        } else if ("SKIPPED".equals(step.status)) {
                            skipped++;
                        }
                    }
                }
            }
            j.testsExecuted = matched;
            j.testsPassed = passed;
            j.testsFailed = failed;
            j.skipped = skipped;
            if (matched == 0) {
                j.status = "NOT_EXECUTED";
            } else if (failed > 0) {
                j.status = "FAILED";
            } else if (skipped > 0 && passed > 0) {
                j.status = "PARTIAL";
            } else if (passed > 0) {
                j.status = "PASSED";
            } else {
                j.status = "SKIPPED";
            }
            j.criticalFailure = failed > 0;
            j.apiFailure = apiFail;
            list.add(j);
        }
        snapJourneysApi(list, model);
        return list;
    }

    private static void snapJourneysApi(List<Journey> list, ExecutionRunModel model) {
        boolean api = false;
        for (HttpErrorCapture.HttpError error : model.httpErrors) {
            if (error.status >= 500 && error.url.contains("genericOrder")) {
                api = true;
            }
        }
        for (Journey j : list) {
            if ("Client document checklist".equals(j.name) || "Admin offer & agreement".equals(j.name)) {
                j.apiFailure = api && j.testsFailed > 0;
            }
        }
    }

    private static List<ExecutionRunModel.StepResult> allSteps(ExecutionRunModel model) {
        List<ExecutionRunModel.StepResult> all = new ArrayList<>();
        for (ExecutionRunModel.FeatureResult feature : model.features) {
            for (ExecutionRunModel.ScenarioResult scenario : feature.scenarios) {
                all.addAll(scenario.steps);
            }
        }
        return all;
    }

    private static Map<String, SuiteBucket> suiteBuckets(ExecutionRunModel model) {
        String[] keys = {"smoke", "sanity", "regression", "e2e", "admin", "api", "ui", "time-optimized"};
        Map<String, SuiteBucket> map = new LinkedHashMap<>();
        for (String key : keys) {
            map.put(key, new SuiteBucket(key));
        }
        int anyTag = 0;
        for (ExecutionRunModel.FeatureResult feature : model.features) {
            for (ExecutionRunModel.ScenarioResult scenario : feature.scenarios) {
                List<String> tags = new ArrayList<>();
                tags.addAll(feature.tags);
                tags.addAll(scenario.tags);
                boolean matched = false;
                for (String tag : tags) {
                    String t = tag == null ? "" : tag.replace("@", "").toLowerCase();
                    SuiteBucket bucket = map.get(t);
                    if (bucket == null && t.contains("critical")) {
                        bucket = map.computeIfAbsent("critical-business-flow", SuiteBucket::new);
                    }
                    if (bucket != null) {
                        matched = true;
                        anyTag++;
                        bucket.total++;
                        if ("PASSED".equals(scenario.status)) {
                            bucket.passed++;
                        } else if ("FAILED".equals(scenario.status)) {
                            bucket.failed++;
                        }
                    }
                }
                if (!matched && "timeOptimizedUniqueStepsFlow.feature".equals(feature.name)) {
                    SuiteBucket e2e = map.get("e2e");
                    e2e.total++;
                    anyTag++;
                    if ("PASSED".equals(scenario.status)) {
                        e2e.passed++;
                    } else if ("FAILED".equals(scenario.status)) {
                        e2e.failed++;
                    }
                }
            }
        }
        if (anyTag == 0) {
            map.clear();
        }
        return map;
    }

    private static List<FailureSignature> groupFailures(ExecutionRunModel model) {
        Map<String, FailureSignature> map = new LinkedHashMap<>();
        for (ExecutionRunModel.FailureRecord failure : model.failures) {
            String sig = failure.exceptionType + " | " + firstLine(failure.message);
            FailureSignature item = map.get(sig);
            if (item == null) {
                item = new FailureSignature();
                item.signature = sig;
                item.exceptionType = failure.exceptionType;
                item.category = failure.category;
                item.commonStep = failure.step;
                item.mode = failure.executionMode;
                map.put(sig, item);
            }
            item.occurrences++;
            if (!item.affectedTests.contains(failure.scenario)) {
                item.affectedTests.add(failure.scenario);
            }
            if (!item.affectedFeatures.contains(failure.feature)) {
                item.affectedFeatures.add(failure.feature);
            }
        }
        return new ArrayList<>(map.values());
    }

    private static String firstLine(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }
        String line = message.split("\\R", 2)[0];
        return line.length() > 160 ? line.substring(0, 160) : line;
    }

    private static List<NamedDuration> slowScenarios(ExecutionRunModel model, int limit) {
        List<NamedDuration> list = new ArrayList<>();
        for (ExecutionRunModel.FeatureResult feature : model.features) {
            for (ExecutionRunModel.ScenarioResult scenario : feature.scenarios) {
                list.add(new NamedDuration(scenario.name, scenario.durationMs, scenario.status));
            }
        }
        list.sort((a, b) -> Long.compare(b.durationMs, a.durationMs));
        if (list.size() > limit) {
            return list.subList(0, limit);
        }
        return list;
    }

    private static List<NamedDuration> slowSteps(ExecutionRunModel model, int limit) {
        List<NamedDuration> list = new ArrayList<>();
        for (ExecutionRunModel.FeatureResult feature : model.features) {
            for (ExecutionRunModel.ScenarioResult scenario : feature.scenarios) {
                for (ExecutionRunModel.StepResult step : scenario.steps) {
                    list.add(new NamedDuration((step.keyword + step.text).trim(), step.durationMs, step.status));
                }
            }
        }
        list.sort((a, b) -> Long.compare(b.durationMs, a.durationMs));
        if (list.size() > limit) {
            return list.subList(0, limit);
        }
        return list;
    }

    private static String environmentHealth(ExecutionRunModel model) {
        int server = 0;
        for (HttpErrorCapture.HttpError error : model.httpErrors) {
            if (error.status >= 500) {
                server++;
            }
        }
        if (server >= 2) {
            return "RED";
        }
        if (server == 1) {
            return "AMBER";
        }
        if (model.xhrTotal > 0) {
            return "GREEN";
        }
        return "AMBER";
    }

    private static String testDataHealth(ExecutionRunModel model) {
        int n = countCategory(model, "Test Data");
        if (n > 0) {
            return "RED";
        }
        return "GREEN";
    }

    private static void applyHistory(Snapshot snap, JSONArray history, ExecutionRunModel model) {
        snap.history = history == null ? new JSONArray() : history;
        if (history == null || history.length() < 1) {
            snap.stabilityPct = Double.NaN;
            snap.flakyPct = Double.NaN;
            snap.previousPassRate = Double.NaN;
            return;
        }
        JSONObject previous = null;
        for (int i = history.length() - 1; i >= 0; i--) {
            JSONObject run = history.optJSONObject(i);
            if (run == null) {
                continue;
            }
            if (run.optBoolean("dryRun")) {
                continue;
            }
            if (model.metadata != null && model.metadata.runId.equals(run.optString("id"))) {
                continue;
            }
            previous = run;
            break;
        }
        if (previous != null) {
            snap.previousPassRate = previous.optDouble("passRate", Double.NaN);
            snap.previousDurationMs = previous.optLong("durationMs", -1);
            snap.previousFailures = previous.optInt("failures", -1);
            snap.previousHttpErrors = previous.optInt("httpErrors", -1);
            compareScenarioFailures(snap, previous, model);
        } else {
            snap.previousPassRate = Double.NaN;
        }
        Map<String, int[]> byName = new LinkedHashMap<>();
        for (int i = 0; i < history.length(); i++) {
            JSONObject run = history.optJSONObject(i);
            if (run == null) {
                continue;
            }
            JSONArray scenarios = run.optJSONArray("scenarioDigest");
            if (scenarios == null) {
                continue;
            }
            for (int s = 0; s < scenarios.length(); s++) {
                JSONObject sc = scenarios.optJSONObject(s);
                if (sc == null) {
                    continue;
                }
                String name = sc.optString("name", "");
                int[] counts = byName.computeIfAbsent(name, k -> new int[2]);
                if ("PASSED".equals(sc.optString("status"))) {
                    counts[0]++;
                } else if ("FAILED".equals(sc.optString("status"))) {
                    counts[1]++;
                }
            }
        }
        int flaky = 0;
        int withHistory = 0;
        for (Map.Entry<String, int[]> e : byName.entrySet()) {
            int pass = e.getValue()[0];
            int fail = e.getValue()[1];
            if (pass + fail < 2) {
                continue;
            }
            withHistory++;
            if (pass > 0 && fail > 0) {
                flaky++;
                FlakyTest ft = new FlakyTest();
                ft.name = e.getKey();
                ft.passCount = pass;
                ft.failCount = fail;
                ft.stabilityPct = pct(pass, pass + fail);
                snap.potentiallyFlaky.add(ft);
            }
        }
        snap.stabilityPct = withHistory == 0 ? Double.NaN : pct(withHistory - flaky, withHistory);
        snap.flakyPct = withHistory == 0 ? Double.NaN : pct(flaky, withHistory);
        if (withHistory < 2) {
            snap.flakyLabel = "Potentially Flaky (limited history)";
        } else {
            snap.flakyLabel = "Potentially Flaky";
        }
    }

    private static void compareScenarioFailures(Snapshot snap, JSONObject previous, ExecutionRunModel model) {
        JSONArray previousDigest = previous.optJSONArray("scenarioDigest");
        if (previousDigest == null) {
            return;
        }
        Map<String, String> previousStatuses = new LinkedHashMap<>();
        for (int i = 0; i < previousDigest.length(); i++) {
            JSONObject scenario = previousDigest.optJSONObject(i);
            if (scenario != null) {
                previousStatuses.put(scenario.optString("feature", "") + " / " + scenario.optString("name", ""),
                        scenario.optString("status", "UNKNOWN"));
            }
        }
        Map<String, String> currentStatuses = new LinkedHashMap<>();
        for (ExecutionRunModel.FeatureResult feature : model.features) {
            for (ExecutionRunModel.ScenarioResult scenario : feature.scenarios) {
                currentStatuses.put(feature.name + " / " + scenario.name, scenario.status);
            }
        }
        for (Map.Entry<String, String> entry : currentStatuses.entrySet()) {
            String current = entry.getValue();
            String old = previousStatuses.get(entry.getKey());
            if (isFailedStatus(current)) {
                if (old == null || !isFailedStatus(old)) {
                    snap.newFailures.add(entry.getKey());
                } else {
                    snap.persistentFailures.add(entry.getKey());
                }
            } else if (old != null && isFailedStatus(old) && "PASSED".equals(current)) {
                snap.fixedFailures.add(entry.getKey());
            }
        }
    }

    private static boolean isFailedStatus(String status) {
        return "FAILED".equals(status) || "BROKEN".equals(status)
                || "UNDEFINED".equals(status) || "AMBIGUOUS".equals(status);
    }

    private static Confidence confidence(Snapshot snap, ExecutionRunModel model) {
        Confidence c = new Confidence();
        c.weightPass = weight("qa.confidence.weight.passRate", 25);
        c.weightFlow = weight("qa.confidence.weight.criticalFlow", 20);
        c.weightCoverage = weight("qa.confidence.weight.coverage", 15);
        c.weightCritical = weight("qa.confidence.weight.criticalFailures", 15);
        c.weightApi = weight("qa.confidence.weight.api", 10);
        c.weightStability = weight("qa.confidence.weight.stability", 5);
        c.weightEnv = weight("qa.confidence.weight.environment", 5);
        c.weightPerf = weight("qa.confidence.weight.performance", 5);

        c.passScore = scoreOrNa(snap.passRatePct);
        c.flowScore = journeyScore(snap.journeys);
        c.coverageScore = scoreOrNa(snap.functionalCoveragePct);
        c.criticalScore = criticalScore(snap, model);
        c.apiScore = scoreOrNa(snap.apiSuccessPct);
        c.stabilityScore = scoreOrNa(snap.stabilityPct);
        c.envScore = envScore(snap.environmentHealth);
        c.perfScore = perfScore(model.durationMs, snap.previousDurationMs);

        double[] scores = {c.passScore, c.flowScore, c.coverageScore, c.criticalScore,
                c.apiScore, c.stabilityScore, c.envScore, c.perfScore};
        double[] weights = {c.weightPass, c.weightFlow, c.weightCoverage, c.weightCritical,
                c.weightApi, c.weightStability, c.weightEnv, c.weightPerf};
        double wsum = 0;
        double acc = 0;
        for (int i = 0; i < scores.length; i++) {
            if (Double.isNaN(scores[i])) {
                c.unavailableFactors.add(factorName(i));
                continue;
            }
            wsum += weights[i];
            acc += scores[i] * weights[i];
        }
        c.score = wsum <= 0 ? Double.NaN : Math.round(acc / wsum);
        applyGates(c, snap, model);
        return c;
    }

    private static String factorName(int i) {
        String[] names = {"Pass Rate", "Critical Flow Health", "Regression Coverage",
                "Critical Failure Impact", "API Health", "Stability", "Environment Health", "Performance"};
        return names[i];
    }

    private static double weight(String key, double fallback) {
        try {
            return Double.parseDouble(System.getProperty(key, String.valueOf(fallback)));
        } catch (Exception e) {
            return fallback;
        }
    }

    private static double scoreOrNa(double pct) {
        return Double.isNaN(pct) ? Double.NaN : pct;
    }

    private static double journeyScore(List<Journey> journeys) {
        int executed = 0;
        int passed = 0;
        for (Journey j : journeys) {
            if ("NOT_EXECUTED".equals(j.status)) {
                continue;
            }
            executed++;
            if ("PASSED".equals(j.status)) {
                passed++;
            }
        }
        return pct(passed, executed);
    }

    private static double criticalScore(Snapshot snap, ExecutionRunModel model) {
        if (snap.criticalFailures <= 0 && model.scenariosFailed == 0) {
            return 100;
        }
        if (snap.criticalFailures >= 3 || model.xhr5xx >= 2) {
            return 20;
        }
        if (model.scenariosFailed > 0) {
            return 45;
        }
        return 70;
    }

    private static double envScore(String health) {
        if ("GREEN".equals(health)) {
            return 100;
        }
        if ("AMBER".equals(health)) {
            return 60;
        }
        if ("RED".equals(health)) {
            return 25;
        }
        return Double.NaN;
    }

    private static double perfScore(long durationMs, long previousMs) {
        if (durationMs <= 0) {
            return Double.NaN;
        }
        if (previousMs <= 0) {
            return 80;
        }
        if (durationMs <= previousMs * 1.2) {
            return 90;
        }
        if (durationMs <= previousMs * 1.6) {
            return 60;
        }
        return 35;
    }

    private static void applyGates(Confidence c, Snapshot snap, ExecutionRunModel model) {
        List<String> reasons = new ArrayList<>();
        if (model.xhr5xx > 0) {
            reasons.add(model.xhr5xx + " API 5xx response(s) during execution");
        }
        if (model.scenariosFailed > 0) {
            reasons.add(model.scenariosFailed + " failed scenario(s)");
        }
        int criticalJourneys = 0;
        for (Journey j : snap.journeys) {
            if (j.criticalFailure && !"Post-INK2 lifecycle".equals(j.name)) {
                criticalJourneys++;
            }
        }
        if (criticalJourneys > 0) {
            reasons.add(criticalJourneys + " critical business journey(s) failed");
        }
        if (!snap.potentiallyFlaky.isEmpty()) {
            reasons.add(snap.potentiallyFlaky.size() + " potentially flaky test(s) from history");
        }
        if ("RED".equals(snap.environmentHealth)) {
            reasons.add("Environment health RED (server-side HTTP 5xx)");
        }
        if (countCategory(model, "Test Data") > 0) {
            reasons.add("Test data / token capture failure");
        }
        c.primaryFactors.addAll(reasons);
        if (criticalJourneys > 0 || model.xhr5xx >= 2 || "RED".equals(snap.environmentHealth)) {
            c.indication = "RED";
            c.statusLabel = "Significant unresolved risks identified";
        } else if (!reasons.isEmpty() || (!Double.isNaN(c.score) && c.score < 80)) {
            c.indication = "AMBER";
            c.statusLabel = "Sign-off requires review of identified risks";
        } else {
            c.indication = "GREEN";
            c.statusLabel = "Evidence supports sign-off discussion";
        }
        if (!Double.isNaN(c.score) && c.score >= 90 && "RED".equals(c.indication)) {
            // gates must not hide critical risk
            c.statusLabel = "Score offset by gating rules — significant unresolved risks identified";
        }
    }

    private static String narrative(Snapshot snap, ExecutionRunModel model) {
        String pass = fmtPct(snap.passRatePct);
        String conf = Double.isNaN(snap.confidence.score) ? NA
                : String.format(Locale.US, "%.0f/100", snap.confidence.score);
        int journeysExecuted = 0;
        int journeysPassed = 0;
        for (Journey j : snap.journeys) {
            if (!"NOT_EXECUTED".equals(j.status)) {
                journeysExecuted++;
            }
            if ("PASSED".equals(j.status)) {
                journeysPassed++;
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Execution completed with ").append(pass).append(" pass rate. ");
        sb.append(snap.scenariosExecuted).append("/").append(model.scenariosTotal)
                .append(" scenarios executed. ");
        sb.append(journeysPassed).append("/").append(journeysExecuted)
                .append(" observed business journeys passed. ");
        sb.append(model.scenariosFailed).append(" failed scenario(s). ");
        sb.append(model.httpErrors.size()).append(" HTTP 4xx/5xx response(s) captured. ");
        sb.append(snap.potentiallyFlaky.size()).append(" potentially flaky test(s) from history. ");
        sb.append("Overall release confidence: ").append(conf).append(". ");
        if (snap.confidence.primaryFactors.isEmpty()) {
            sb.append("Primary risks requiring QA review: none identified from available evidence.");
        } else {
            sb.append("Primary risks requiring QA review: ");
            sb.append(String.join("; ", snap.confidence.primaryFactors)).append(".");
        }
        return sb.toString();
    }

    public static final class Snapshot {
        public ExecutionRunModel model;
        public int scenariosExecuted;
        public double passRatePct;
        public double failRatePct;
        public double skipRatePct;
        public double stepPassRatePct;
        public double featurePassRatePct;
        public double scenarioPassRatePct;
        public long avgScenarioMs = -1;
        public long avgStepMs = -1;
        public double functionalCoveragePct;
        public String automationCoverageNote = MAPPING_NA;
        public String requirementCoverageNote = MAPPING_NA;
        public double regressionCoveragePct;
        public double apiSuccessPct = Double.NaN;
        public int http4xx;
        public int http5xx;
        public int criticalFailures;
        public int highFailures;
        public String openDefectsNote = NA;
        public String blockedNote;
        public double stabilityPct = Double.NaN;
        public double flakyPct = Double.NaN;
        public String flakyLabel = "Potentially Flaky";
        public double previousPassRate = Double.NaN;
        public long previousDurationMs = -1;
        public int previousFailures = -1;
        public int previousHttpErrors = -1;
        public List<String> newFailures = new ArrayList<>();
        public List<String> fixedFailures = new ArrayList<>();
        public List<String> persistentFailures = new ArrayList<>();
        public List<Journey> journeys = new ArrayList<>();
        public Map<String, SuiteBucket> suiteBuckets = new LinkedHashMap<>();
        public List<FailureSignature> failureSignatures = new ArrayList<>();
        public List<NamedDuration> slowScenarios = new ArrayList<>();
        public List<NamedDuration> slowSteps = new ArrayList<>();
        public List<FlakyTest> potentiallyFlaky = new ArrayList<>();
        public String environmentHealth = "AMBER";
        public String testDataHealth = "GREEN";
        public Confidence confidence = new Confidence();
        public String signOffIndication = "AMBER";
        public String executiveNarrative = "";
        public JSONArray history = new JSONArray();
    }

    public static final class Journey {
        public String name;
        public String status;
        public int testsExecuted;
        public int testsPassed;
        public int testsFailed;
        public int skipped;
        public boolean criticalFailure;
        public boolean apiFailure;
    }

    public static final class SuiteBucket {
        public final String name;
        public int total;
        public int passed;
        public int failed;

        SuiteBucket(String name) {
            this.name = name;
        }
    }

    public static final class FailureSignature {
        public String signature;
        public String exceptionType;
        public String category;
        public String commonStep;
        public String mode;
        public int occurrences;
        public final List<String> affectedTests = new ArrayList<>();
        public final List<String> affectedFeatures = new ArrayList<>();
    }

    public static final class NamedDuration {
        public final String name;
        public final long durationMs;
        public final String status;

        NamedDuration(String name, long durationMs, String status) {
            this.name = name;
            this.durationMs = durationMs;
            this.status = status;
        }
    }

    public static final class FlakyTest {
        public String name;
        public int passCount;
        public int failCount;
        public double stabilityPct;
    }

    public static final class Confidence {
        public double score = Double.NaN;
        public String indication = "AMBER";
        public String statusLabel = "";
        public double passScore = Double.NaN;
        public double flowScore = Double.NaN;
        public double coverageScore = Double.NaN;
        public double criticalScore = Double.NaN;
        public double apiScore = Double.NaN;
        public double stabilityScore = Double.NaN;
        public double envScore = Double.NaN;
        public double perfScore = Double.NaN;
        public double weightPass;
        public double weightFlow;
        public double weightCoverage;
        public double weightCritical;
        public double weightApi;
        public double weightStability;
        public double weightEnv;
        public double weightPerf;
        public final List<String> primaryFactors = new ArrayList<>();
        public final List<String> unavailableFactors = new ArrayList<>();
    }
}
