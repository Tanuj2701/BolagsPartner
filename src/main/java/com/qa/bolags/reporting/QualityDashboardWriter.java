package com.qa.bolags.reporting;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Enterprise Automation Quality Intelligence & QA Release Sign-off dashboard.
 */
final class QualityDashboardWriter {

    private QualityDashboardWriter() {
    }

    static void write(Path file, QualityIntelligence.Snapshot snap, JSONArray history) throws IOException {
        ExecutionRunModel model = snap.model;
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang=\"en\" data-theme=\"dark\"><head><meta charset=\"UTF-8\">");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        html.append("<title>Automation Quality Intelligence — QA Release Sign-off</title>");
        html.append(css());
        html.append("</head><body>");
        html.append("<div class=\"app\"><aside>");
        html.append("<p class=\"brand\">BolagsPartner</p><h1>Quality Intelligence</h1>");
        html.append("<nav>");
        nav(html, "exec", "Executive Summary");
        nav(html, "confidence", "QA Release Confidence");
        nav(html, "overview", "Execution Overview");
        nav(html, "coverage", "Coverage");
        nav(html, "features", "Feature Results");
        nav(html, "scenarios", "Scenario / TC Results");
        nav(html, "steps", "Step Results");
        nav(html, "failures", "Failure Intelligence");
        nav(html, "rca", "Root Cause Analysis");
        nav(html, "api", "API Intelligence");
        nav(html, "modes", "Headed vs Headless");
        nav(html, "journeys", "Business Journeys");
        nav(html, "stability", "Test Stability");
        nav(html, "perf", "Performance");
        nav(html, "env", "Environment Health");
        nav(html, "data", "Test Data Health");
        nav(html, "defects", "Defect Traceability");
        nav(html, "artifacts", "Artifacts");
        nav(html, "history", "Historical Trends");
        nav(html, "compare", "Run Comparison");
        html.append("</nav><button id=\"themeBtn\" type=\"button\">Light / Dark</button>");
        html.append("</aside><main>");
        html.append("<header class=\"top\"><div><p class=\"crumb\">Automation Quality Intelligence / ")
                .append(esc(model.browserMode)).append("</p>");
        html.append("<h2>QA Release Sign-off Dashboard</h2>");
        StringBuilder append = html.append("<p class=\"muted\">Run ").append(esc(model.metadata.runId))
                .append(" · ").append(esc(model.startedAt)).append(" → ").append(esc(model.finishedAt))
                .append(" · ").append(HtmlExecutionReportWriter.formatDuration(model.durationMs))
                .append("</p></div>");
        html.append("<div class=\"actions\"><input id=\"q\" placeholder=\"Search dashboard…\">");
        html.append("<a class=\"btn\" href=\"../pdf/execution-report.pdf\">PDF</a>");
        html.append("<a class=\"btn\" href=\"../summary.json\">JSON</a>");
        html.append("<a class=\"btn\" href=\"../export/scenarios.csv\">CSV</a>");
        html.append("<button class=\"btn\" onclick=\"window.print()\">Print</button></div></header>");

        executive(html, snap, model);
        confidence(html, snap);
        overview(html, snap, model);
        coverage(html, snap, model);
        features(html, model);
        scenarios(html, model);
        steps(html, model);
        failures(html, snap, model);
        rca(html, snap);
        api(html, snap, model);
        modes(html, snap, history, model);
        journeys(html, snap);
        stability(html, snap);
        perf(html, snap, model);
        env(html, snap, model);
        data(html, snap, model);
        defects(html);
        artifacts(html, model);
        historySection(html, history, model);
        compare(html, snap, model);

        html.append("</main></div>");
        html.append(js());
        html.append("</body></html>");
        Files.createDirectories(file.getParent());
        Files.write(file, html.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void nav(StringBuilder html, String id, String label) {
        html.append("<a href=\"#").append(id).append("\">").append(label).append("</a>");
    }

    private static void executive(StringBuilder html, QualityIntelligence.Snapshot snap, ExecutionRunModel model) {
        html.append("<section id=\"exec\"><h3>Executive Summary</h3>");
        html.append("<div class=\"client-box\"><h4>Release quality summary</h4>");
        html.append("<div class=\"kpis\">");
        kpi(html, "Scenarios executed", snap.scenariosExecuted + " / " + model.scenariosTotal, "blue");
        kpi(html, "Pass rate", QualityIntelligence.fmtPct(snap.passRatePct), tone(snap.passRatePct, 90, 70));
        kpi(html, "Coverage (executed suite)", QualityIntelligence.fmtPct(snap.functionalCoveragePct), "blue");
        kpi(html, "Critical flows passed", journeyPassLabel(snap), toneJourney(snap));
        kpi(html, "API health", QualityIntelligence.fmtPct(snap.apiSuccessPct), tone(snap.apiSuccessPct, 95, 80));
        kpi(html, "Open critical risks", String.valueOf(snap.confidence.primaryFactors.size()),
                snap.confidence.primaryFactors.isEmpty() ? "green" : "red");
        kpi(html, "Potentially flaky", String.valueOf(snap.potentiallyFlaky.size()),
                snap.potentiallyFlaky.isEmpty() ? "green" : "amber");
        kpi(html, "QA Release Confidence", fmtScore(snap.confidence.score), snap.confidence.indication.toLowerCase());
        html.append("</div><p class=\"status-banner ").append(snap.confidence.indication.toLowerCase())
                .append("\">").append(esc(snap.confidence.indication)).append(" — ")
                .append(esc(snap.confidence.statusLabel)).append("</p>");
        html.append("<p>").append(esc(snap.executiveNarrative)).append("</p>");
        html.append("<h5>Top risks</h5><ol>");
        if (snap.confidence.primaryFactors.isEmpty()) {
            html.append("<li>None identified from available evidence.</li>");
        } else {
            int max = Math.min(3, snap.confidence.primaryFactors.size());
            for (int i = 0; i < max; i++) {
                html.append("<li>").append(esc(snap.confidence.primaryFactors.get(i))).append("</li>");
            }
        }
        html.append("</ol></div></section>");
    }

    private static void confidence(StringBuilder html, QualityIntelligence.Snapshot snap) {
        html.append("<section id=\"confidence\"><h3>QA Release Confidence</h3>");
        html.append("<div class=\"score-hero ").append(snap.confidence.indication.toLowerCase()).append("\">");
        html.append("<p>QA RELEASE CONFIDENCE INDEX</p><p class=\"big\">")
                .append(fmtScore(snap.confidence.score)).append("</p>");
        html.append("<p>").append(esc(snap.confidence.statusLabel)).append("</p>");
        html.append("<p class=\"muted\">Not an approval or rejection. Final decision remains with the QA/release owner.</p></div>");
        html.append("<table><thead><tr><th>Factor</th><th>Weight</th><th>Evidence score</th></tr></thead><tbody>");
        factorRow(html, "Pass Rate", snap.confidence.weightPass, snap.confidence.passScore);
        factorRow(html, "Critical Flow Health", snap.confidence.weightFlow, snap.confidence.flowScore);
        factorRow(html, "Functional / regression coverage of executed suite",
                snap.confidence.weightCoverage, snap.confidence.coverageScore);
        factorRow(html, "Critical / high failure impact", snap.confidence.weightCritical, snap.confidence.criticalScore);
        factorRow(html, "API Health", snap.confidence.weightApi, snap.confidence.apiScore);
        factorRow(html, "Stability / flakiness", snap.confidence.weightStability, snap.confidence.stabilityScore);
        factorRow(html, "Environment Health", snap.confidence.weightEnv, snap.confidence.envScore);
        factorRow(html, "Performance", snap.confidence.weightPerf, snap.confidence.perfScore);
        html.append("</tbody></table>");
        if (!snap.confidence.unavailableFactors.isEmpty()) {
            html.append("<p class=\"muted\">Excluded from weighted score (data not available): ")
                    .append(esc(String.join(", ", snap.confidence.unavailableFactors))).append("</p>");
        }
        html.append("<h4>Primary factors</h4><ul>");
        if (snap.confidence.primaryFactors.isEmpty()) {
            html.append("<li>None identified from available evidence.</li>");
        } else {
            for (String f : snap.confidence.primaryFactors) {
                html.append("<li>").append(esc(f)).append("</li>");
            }
        }
        html.append("</ul>");
        html.append("<h4>QA Release Sign-off Summary</h4><pre class=\"narrative\">")
                .append(esc(snap.executiveNarrative)).append("</pre></section>");
    }

    private static void overview(StringBuilder html, QualityIntelligence.Snapshot snap, ExecutionRunModel model) {
        html.append("<section id=\"overview\"><h3>Execution Overview</h3><div class=\"meta-grid\">");
        meta(html, "Execution ID", model.metadata.executionId);
        meta(html, "Run ID", model.metadata.runId);
        meta(html, "Mode", model.browserMode);
        meta(html, "Environment", model.metadata.environment);
        meta(html, "Browser", model.metadata.browser);
        meta(html, "Browser version", model.metadata.browserVersion);
        meta(html, "OS", model.metadata.operatingSystem);
        meta(html, "Branch", model.metadata.branch);
        meta(html, "Commit", model.metadata.commitId);
        meta(html, "Build", model.metadata.buildNumber);
        meta(html, "Framework", model.metadata.frameworkVersion);
        meta(html, "Suite", model.metadata.testSuite);
        meta(html, "Workers", model.metadata.parallelWorkers);
        meta(html, "Test data id", model.metadata.testDataIdentifier);
        html.append("</div><div class=\"kpis\">");
        kpi(html, "Features", String.valueOf(model.featuresTotal), "purple");
        kpi(html, "Scenarios", String.valueOf(model.scenariosTotal), "purple");
        kpi(html, "Steps", String.valueOf(model.stepsTotal), "purple");
        kpi(html, "Passed", String.valueOf(model.scenariosPassed), "green");
        kpi(html, "Failed", String.valueOf(model.scenariosFailed), "red");
        kpi(html, "Skipped", String.valueOf(model.scenariosSkipped), "amber");
        kpi(html, "Broken/Error", String.valueOf(model.scenariosBroken + model.stepsUndefined), "red");
        kpi(html, "Blocked", snap.blockedNote, "blue");
        kpi(html, "Duration", HtmlExecutionReportWriter.formatDuration(model.durationMs), "purple");
        kpi(html, "Avg scenario", dur(snap.avgScenarioMs), "blue");
        html.append("</div></section>");
    }

    private static void coverage(StringBuilder html, QualityIntelligence.Snapshot snap, ExecutionRunModel model) {
        html.append("<section id=\"coverage\"><h3>Coverage</h3>");
        html.append("<p class=\"muted\">Values reflect this execution only. Product-wide requirement mapping is not claimed.</p>");
        html.append("<div class=\"kpis\">");
        kpi(html, "Feature coverage (executed suite)", QualityIntelligence.fmtPct(snap.featurePassRatePct), "blue");
        kpi(html, "Scenario coverage (executed)", QualityIntelligence.fmtPct(snap.functionalCoveragePct), "blue");
        kpi(html, "Step pass rate", QualityIntelligence.fmtPct(snap.stepPassRatePct), "blue");
        kpi(html, "Regression (tagged/e2e in this run)", QualityIntelligence.fmtPct(snap.regressionCoveragePct), "blue");
        kpi(html, "Requirement coverage", snap.requirementCoverageNote, "blue");
        kpi(html, "Automation coverage (product)", snap.automationCoverageNote, "blue");
        html.append("</div><table><thead><tr><th>Level</th><th>Total</th><th>Passed</th><th>Failed</th><th>Skipped / other</th></tr></thead><tbody>");
        html.append("<tr><td>Features</td><td>").append(model.featuresTotal).append("</td><td>")
                .append(model.featuresPassed).append("</td><td>").append(model.featuresFailed)
                .append("</td><td>partial ").append(model.featuresPartial).append("</td></tr>");
        html.append("<tr><td>Scenarios</td><td>").append(model.scenariosTotal).append("</td><td>")
                .append(model.scenariosPassed).append("</td><td>").append(model.scenariosFailed)
                .append("</td><td>").append(model.scenariosSkipped).append("</td></tr>");
        html.append("<tr><td>Steps</td><td>").append(model.stepsTotal).append("</td><td>")
                .append(model.stepsPassed).append("</td><td>").append(model.stepsFailed)
                .append("</td><td>").append(model.stepsSkipped).append(" skipped / ")
                .append(model.stepsUndefined).append(" undefined</td></tr>");
        html.append("</tbody></table>");
        if (snap.suiteBuckets.isEmpty()) {
            html.append("<p>Suite category coverage: ").append(QualityIntelligence.MAPPING_NA).append("</p>");
        } else {
            html.append("<h4>Coverage by suite tag (this run)</h4><table><thead><tr><th>Category</th><th>Total</th><th>Passed</th><th>Failed</th><th>Pass %</th></tr></thead><tbody>");
            for (Map.Entry<String, QualityIntelligence.SuiteBucket> e : snap.suiteBuckets.entrySet()) {
                QualityIntelligence.SuiteBucket b = e.getValue();
                if (b.total == 0) {
                    continue;
                }
                html.append("<tr><td>").append(esc(b.name)).append("</td><td>").append(b.total)
                        .append("</td><td>").append(b.passed).append("</td><td>").append(b.failed)
                        .append("</td><td>").append(QualityIntelligence.fmtPct(QualityIntelligence.pct(b.passed, b.total)))
                        .append("</td></tr>");
            }
            html.append("</tbody></table>");
        }
        html.append("</section>");
    }

    private static void features(StringBuilder html, ExecutionRunModel model) {
        html.append("<section id=\"features\"><h3>Feature Results</h3>");
        html.append(toolbar());
        html.append("<table class=\"grid\"><thead><tr><th>Feature</th><th>Status</th><th>Scenarios</th><th>Duration</th></tr></thead><tbody>");
        for (ExecutionRunModel.FeatureResult feature : model.features) {
            html.append("<tr><td>").append(esc(feature.name)).append("</td><td>")
                    .append(badge(feature.status)).append("</td><td>")
                    .append(feature.scenarios.size()).append("</td><td>")
                    .append(HtmlExecutionReportWriter.formatDuration(feature.durationMs)).append("</td></tr>");
        }
        html.append("</tbody></table></section>");
    }

    private static void scenarios(StringBuilder html, ExecutionRunModel model) {
        html.append("<section id=\"scenarios\"><h3>Scenario / Test Case Results</h3>");
        html.append(toolbar());
        html.append("<table class=\"grid\"><thead><tr><th>Scenario</th><th>Feature</th><th>Status</th><th>Duration</th><th>Tags</th><th>Evidence</th></tr></thead><tbody>");
        for (ExecutionRunModel.FeatureResult feature : model.features) {
            for (ExecutionRunModel.ScenarioResult scenario : feature.scenarios) {
                html.append("<tr><td>").append(esc(scenario.name)).append("</td><td>")
                        .append(esc(feature.name)).append("</td><td>").append(badge(scenario.status))
                        .append("</td><td>").append(HtmlExecutionReportWriter.formatDuration(scenario.durationMs))
                        .append("</td><td>").append(esc(String.join(" ", scenario.tags)))
                        .append("</td><td>").append(artifactLink(scenario.screenshot)).append("</td></tr>");
            }
        }
        html.append("</tbody></table></section>");
    }

    private static void steps(StringBuilder html, ExecutionRunModel model) {
        html.append("<section id=\"steps\"><h3>Step Results</h3>");
        html.append(toolbar());
        html.append("<table class=\"grid\" id=\"stepTable\"><thead><tr><th>Step</th><th>Scenario</th><th>Status</th><th>Duration</th><th>Category</th><th>Error</th></tr></thead><tbody>");
        int i = 0;
        for (ExecutionRunModel.FeatureResult feature : model.features) {
            for (ExecutionRunModel.ScenarioResult scenario : feature.scenarios) {
                for (ExecutionRunModel.StepResult step : scenario.steps) {
                    html.append("<tr class=\"page-row\" data-page=\"").append(i / 40)
                            .append("\"><td>").append(esc(step.keyword + step.text)).append("</td><td>")
                            .append(esc(scenario.name)).append("</td><td>").append(badge(step.status))
                            .append("</td><td>").append(step.durationMs).append(" ms</td><td>")
                            .append(esc(step.failureCategory)).append("</td><td><pre>")
                            .append(esc(trim(step.error, 400))).append("</pre></td></tr>");
                    i++;
                }
            }
        }
        html.append("</tbody></table><div class=\"pager\" id=\"stepPager\"></div></section>");
    }

    private static void failures(StringBuilder html, QualityIntelligence.Snapshot snap, ExecutionRunModel model) {
        html.append("<section id=\"failures\"><h3>Failure Intelligence</h3>");
        if (model.failures.isEmpty()) {
            html.append("<p class=\"ok\">No assertion or step failures.</p></section>");
            return;
        }
        html.append("<table class=\"grid\"><thead><tr><th>Category</th><th>Feature</th><th>Scenario</th><th>Step</th><th>Exception</th><th>Mode</th><th>URL</th><th>Screenshot</th><th>Message</th></tr></thead><tbody>");
        for (ExecutionRunModel.FailureRecord failure : model.failures) {
            html.append("<tr><td>").append(esc(failure.category)).append("</td><td>")
                    .append(esc(failure.feature)).append("</td><td>").append(esc(failure.scenario))
                    .append("</td><td>").append(esc(failure.step)).append("</td><td>")
                    .append(esc(failure.exceptionType)).append("</td><td>").append(esc(failure.executionMode))
                    .append("</td><td class=\"url\">").append(esc(failure.pageUrl)).append("</td><td>")
                    .append(artifactLink(failure.screenshot)).append("</td><td><pre>")
                    .append(esc(trim(failure.message, 500))).append("</pre></td></tr>");
        }
        html.append("</tbody></table></section>");
    }

    private static void rca(StringBuilder html, QualityIntelligence.Snapshot snap) {
        html.append("<section id=\"rca\"><h3>Root Cause Analysis</h3><h4>Top issues requiring attention</h4>");
        if (snap.failureSignatures.isEmpty()) {
            html.append("<p class=\"ok\">No grouped failures.</p></section>");
            return;
        }
        html.append("<table><thead><tr><th>Signature</th><th>Occurrences</th><th>Tests</th><th>Features</th><th>Category</th><th>Common step</th><th>Mode</th></tr></thead><tbody>");
        for (QualityIntelligence.FailureSignature sig : snap.failureSignatures) {
            html.append("<tr><td>").append(esc(trim(sig.signature, 140))).append("</td><td>")
                    .append(sig.occurrences).append("</td><td>").append(sig.affectedTests.size())
                    .append("</td><td>").append(esc(String.join(", ", sig.affectedFeatures)))
                    .append("</td><td>").append(esc(sig.category)).append("</td><td>")
                    .append(esc(sig.commonStep)).append("</td><td>").append(esc(sig.mode)).append("</td></tr>");
        }
        html.append("</tbody></table></section>");
    }

    private static void api(StringBuilder html, QualityIntelligence.Snapshot snap, ExecutionRunModel model) {
        html.append("<section id=\"api\"><h3>API Intelligence</h3><div class=\"kpis\">");
        kpi(html, "API success % (XHR/Fetch/Document)", QualityIntelligence.fmtPct(snap.apiSuccessPct),
                tone(snap.apiSuccessPct, 95, 80));
        kpi(html, "XHR total", model.xhrTotal < 0 ? RunMetadata.NA : String.valueOf(model.xhrTotal), "blue");
        kpi(html, "4xx (client/contract)", String.valueOf(snap.http4xx), snap.http4xx == 0 ? "green" : "amber");
        kpi(html, "5xx (server/service)", String.valueOf(snap.http5xx), snap.http5xx == 0 ? "green" : "red");
        html.append("</div>");
        if (model.httpErrors.isEmpty()) {
            html.append("<p class=\"ok\">No 4xx or 5xx responses captured.</p></section>");
            return;
        }
        html.append("<p class=\"muted\">4xx = client/request/contract-side. 5xx = server/service-side. Secrets are masked.</p>");
        html.append("<table class=\"grid\"><thead><tr><th>Status</th><th>Class</th><th>Method</th><th>URL</th><th>Type</th><th>Time</th><th>Body</th><th>cURL</th></tr></thead><tbody>");
        for (HttpErrorCapture.HttpError error : model.httpErrors) {
            String cls = error.status >= 500 ? "5xx server" : "4xx client";
            html.append("<tr><td>").append(error.status).append("</td><td>").append(cls)
                    .append("</td><td>").append(esc(error.method)).append("</td><td class=\"url\">")
                    .append(esc(error.url)).append("</td><td>").append(esc(error.resourceType))
                    .append("</td><td>").append(error.responseTimeMs < 0 ? RunMetadata.NA : error.responseTimeMs + " ms")
                    .append("</td><td><pre>").append(esc(trim(error.body, 400))).append("</pre></td><td>");
            if (error.curlCommand.isEmpty()) {
                html.append(RunMetadata.NA);
            } else {
                html.append("<details><summary>Show cURL</summary><pre>")
                        .append(esc(error.curlCommand)).append("</pre></details>");
            }
            html.append("</td></tr>");
        }
        html.append("</tbody></table></section>");
    }

    private static void modes(StringBuilder html, QualityIntelligence.Snapshot snap, JSONArray history,
                              ExecutionRunModel model) {
        html.append("<section id=\"modes\"><h3>Headed vs Headless</h3>");
        JSONObject headed = lastMode(history, "headed");
        JSONObject headless = lastMode(history, "headless");
        html.append("<p class=\"muted\">Comparison uses the latest stored run of each mode. Missing mode = Data Not Available.</p>");
        html.append("<table><thead><tr><th>Metric</th><th>Headed</th><th>Headless</th><th>This run (").append(esc(model.browserMode)).append(")</th></tr></thead><tbody>");
        modeRow(html, "Pass %", pctOf(headed), pctOf(headless), QualityIntelligence.fmtPct(snap.passRatePct));
        modeRow(html, "Failures", num(headed, "failures"), num(headless, "failures"), String.valueOf(model.failures.size()));
        modeRow(html, "HTTP errors", num(headed, "httpErrors"), num(headless, "httpErrors"), String.valueOf(model.httpErrors.size()));
        modeRow(html, "Duration", durOf(headed), durOf(headless), HtmlExecutionReportWriter.formatDuration(model.durationMs));
        html.append("</tbody></table>");
        html.append("<h4>Mode-specific failure analysis</h4>");
        html.append("<p>This execution mode: <strong>").append(esc(model.browserMode)).append("</strong>. ");
        html.append("PASS in both / FAIL in both requires matching scenario names in both mode histories. ");
        if (headed == null || headless == null) {
            html.append(RunMetadata.NA).append(" for cross-mode overlap until both modes have been stored.</p>");
        } else {
            html.append("Latest headed run ").append(esc(headed.optString("id"))).append(" vs latest headless run ")
                    .append(esc(headless.optString("id"))).append(".</p>");
            compareModeScenarios(html, headed, headless);
        }
        html.append("</section>");
    }

    private static void journeys(StringBuilder html, QualityIntelligence.Snapshot snap) {
        html.append("<section id=\"journeys\"><h3>Business Journeys</h3>");
        html.append("<p class=\"muted\">Derived from matching executed step text. Journeys with no matching steps are Not Executed — not assumed passed.</p>");
        html.append("<ol class=\"flow\">");
        for (QualityIntelligence.Journey j : snap.journeys) {
            html.append("<li><strong>").append(esc(j.name)).append("</strong> ").append(badge(j.status));
            html.append(" · executed ").append(j.testsExecuted).append(" · passed ").append(j.testsPassed)
                    .append(" · failed ").append(j.testsFailed);
            if (j.criticalFailure) {
                html.append(" · critical failure");
            }
            if (j.apiFailure) {
                html.append(" · API failure");
            }
            html.append("</li>");
        }
        html.append("</ol></section>");
    }

    private static void stability(StringBuilder html, QualityIntelligence.Snapshot snap) {
        html.append("<section id=\"stability\"><h3>Test Stability</h3>");
        html.append("<div class=\"kpis\">");
        kpi(html, "Stability %", QualityIntelligence.fmtPct(snap.stabilityPct), "blue");
        kpi(html, "Flaky %", QualityIntelligence.fmtPct(snap.flakyPct), "amber");
        html.append("</div>");
        if (snap.potentiallyFlaky.isEmpty()) {
            html.append("<p>No potentially flaky tests from stored history. ")
                    .append(esc(snap.flakyLabel)).append(" requires pass and fail across runs.</p></section>");
            return;
        }
        html.append("<h4>").append(esc(snap.flakyLabel)).append("</h4>");
        html.append("<table><thead><tr><th>Test case</th><th>Pass</th><th>Fail</th><th>Stability %</th></tr></thead><tbody>");
        for (QualityIntelligence.FlakyTest ft : snap.potentiallyFlaky) {
            html.append("<tr><td>").append(esc(ft.name)).append("</td><td>").append(ft.passCount)
                    .append("</td><td>").append(ft.failCount).append("</td><td>")
                    .append(QualityIntelligence.fmtPct(ft.stabilityPct)).append("</td></tr>");
        }
        html.append("</tbody></table></section>");
    }

    private static void perf(StringBuilder html, QualityIntelligence.Snapshot snap, ExecutionRunModel model) {
        html.append("<section id=\"perf\"><h3>Performance</h3>");
        html.append("<p>Total duration ").append(HtmlExecutionReportWriter.formatDuration(model.durationMs)).append("</p>");
        html.append("<h4>Slowest scenarios</h4><table><thead><tr><th>Name</th><th>Duration</th><th>Status</th></tr></thead><tbody>");
        for (QualityIntelligence.NamedDuration n : snap.slowScenarios) {
            html.append("<tr><td>").append(esc(n.name)).append("</td><td>")
                    .append(HtmlExecutionReportWriter.formatDuration(n.durationMs)).append("</td><td>")
                    .append(badge(n.status)).append("</td></tr>");
        }
        html.append("</tbody></table><h4>Slowest steps</h4><table><thead><tr><th>Step</th><th>ms</th><th>Status</th></tr></thead><tbody>");
        for (QualityIntelligence.NamedDuration n : snap.slowSteps) {
            html.append("<tr><td>").append(esc(n.name)).append("</td><td>").append(n.durationMs)
                    .append("</td><td>").append(badge(n.status)).append("</td></tr>");
        }
        html.append("</tbody></table></section>");
    }

    private static void env(StringBuilder html, QualityIntelligence.Snapshot snap, ExecutionRunModel model) {
        html.append("<section id=\"env\"><h3>Environment Health</h3>");
        html.append("<p class=\"status-banner ").append(snap.environmentHealth.toLowerCase()).append("\">")
                .append(esc(snap.environmentHealth)).append("</p>");
        html.append("<p>Based on captured HTTP 5xx during this run, not inferred application defects. Environment: ")
                .append(esc(model.metadata.environment)).append(".</p></section>");
    }

    private static void data(StringBuilder html, QualityIntelligence.Snapshot snap, ExecutionRunModel model) {
        html.append("<section id=\"data\"><h3>Test Data Health</h3>");
        html.append("<p class=\"status-banner ").append(snap.testDataHealth.toLowerCase()).append("\">")
                .append(esc(snap.testDataHealth)).append("</p>");
        int n = 0;
        for (ExecutionRunModel.FailureRecord f : model.failures) {
            if ("Test Data".equals(f.category)) {
                n++;
            }
        }
        html.append("<p>Failures classified as Test Data from error evidence: ").append(n)
                .append(". These are separated from application assertion failures.</p></section>");
    }

    private static void defects(StringBuilder html) {
        html.append("<section id=\"defects\"><h3>Defect Traceability</h3>");
        html.append("<p>").append(RunMetadata.NA)
                .append(" — Jira/defect integration is not configured in this framework.</p></section>");
    }

    private static void artifacts(StringBuilder html, ExecutionRunModel model) {
        html.append("<section id=\"artifacts\"><h3>Artifacts</h3><ul>");
        html.append("<li>Detailed HTML report (this folder)</li>");
        html.append("<li>PDF executive report: <a href=\"../pdf/execution-report.pdf\">execution-report.pdf</a></li>");
        html.append("<li>JSON: <a href=\"../summary.json\">summary.json</a></li>");
        html.append("<li>CSV: <a href=\"../export/scenarios.csv\">scenarios.csv</a>, ");
        html.append("<a href=\"../export/failures.csv\">failures.csv</a>, ");
        html.append("<a href=\"../export/api-errors.csv\">api-errors.csv</a></li>");
        html.append("<li>Video: Artifact Not Available</li>");
        html.append("<li>Browser trace file: Artifact Not Available</li>");
        html.append("<li>Screenshots: ");
        boolean any = false;
        for (ExecutionRunModel.FailureRecord f : model.failures) {
            if (f.screenshot != null && f.screenshot.endsWith(".png")) {
                html.append(artifactLink(f.screenshot)).append(" ");
                any = true;
            }
        }
        if (!any) {
            html.append("Artifact Not Available");
        }
        html.append("</li></ul></section>");
    }

    private static void historySection(StringBuilder html, JSONArray history, ExecutionRunModel model) {
        html.append("<section id=\"history\"><h3>Historical Trends</h3>");
        if (history == null || history.length() == 0) {
            html.append("<p>").append(RunMetadata.NA).append(" — no prior runs stored.</p></section>");
            return;
        }
        html.append("<div class=\"bars\">");
        int from = Math.max(0, history.length() - 12);
        for (int i = from; i < history.length(); i++) {
            JSONObject run = history.optJSONObject(i);
            if (run == null) {
                continue;
            }
            double pr = run.optDouble("passRate", 0);
            html.append("<div class=\"bar\" title=\"").append(esc(run.optString("id"))).append(" ")
                    .append(pr).append("%\"><span style=\"height:")
                    .append(Math.max(4, (int) pr)).append("%\"></span></div>");
        }
        html.append("</div><table><thead><tr><th>Run</th><th>Mode</th><th>Status</th><th>Pass %</th><th>Failures</th><th>HTTP</th><th>Duration</th></tr></thead><tbody>");
        for (int i = history.length() - 1; i >= 0; i--) {
            JSONObject run = history.optJSONObject(i);
            if (run == null) {
                continue;
            }
            html.append("<tr><td>").append(esc(run.optString("id"))).append("</td><td>")
                    .append(esc(run.optString("browserMode"))).append("</td><td>")
                    .append(badge(run.optString("status"))).append("</td><td>")
                    .append(run.has("passRate") ? run.optDouble("passRate") + "%" : RunMetadata.NA)
                    .append("</td><td>").append(run.optInt("failures")).append("</td><td>")
                    .append(run.optInt("httpErrors")).append("</td><td>")
                    .append(HtmlExecutionReportWriter.formatDuration(run.optLong("durationMs")))
                    .append("</td></tr>");
        }
        html.append("</tbody></table></section>");
    }

    private static void compare(StringBuilder html, QualityIntelligence.Snapshot snap, ExecutionRunModel model) {
        html.append("<section id=\"compare\"><h3>Run Comparison</h3>");
        if (Double.isNaN(snap.previousPassRate) && snap.previousDurationMs < 0) {
            html.append("<p>").append(RunMetadata.NA).append(" — no previous run to compare.</p></section>");
            return;
        }
        html.append("<table><thead><tr><th>Metric</th><th>Previous</th><th>Current</th><th>Change</th></tr></thead><tbody>");
        html.append("<tr><td>Pass rate</td><td>").append(QualityIntelligence.fmtPct(snap.previousPassRate))
                .append("</td><td>").append(QualityIntelligence.fmtPct(snap.passRatePct)).append("</td><td>");
        if (!Double.isNaN(snap.previousPassRate) && !Double.isNaN(snap.passRatePct)) {
            html.append(String.format(Locale.US, "%+.1f pp", snap.passRatePct - snap.previousPassRate));
        } else {
            html.append(RunMetadata.NA);
        }
        html.append("</td></tr><tr><td>Duration</td><td>")
                .append(snap.previousDurationMs < 0 ? RunMetadata.NA
                        : HtmlExecutionReportWriter.formatDuration(snap.previousDurationMs))
                .append("</td><td>").append(HtmlExecutionReportWriter.formatDuration(model.durationMs))
                .append("</td><td>");
        if (snap.previousDurationMs > 0) {
            html.append(model.durationMs - snap.previousDurationMs).append(" ms");
        } else {
            html.append(RunMetadata.NA);
        }
        html.append("</td></tr><tr><td>Failures</td><td>")
                .append(snap.previousFailures < 0 ? RunMetadata.NA : snap.previousFailures)
                .append("</td><td>").append(model.failures.size()).append("</td><td>")
                .append(snap.previousFailures < 0 ? RunMetadata.NA
                        : String.valueOf(model.failures.size() - snap.previousFailures))
                .append("</td></tr><tr><td>HTTP errors</td><td>")
                .append(snap.previousHttpErrors < 0 ? RunMetadata.NA : snap.previousHttpErrors)
                .append("</td><td>").append(model.httpErrors.size()).append("</td><td>")
                .append(snap.previousHttpErrors < 0 ? RunMetadata.NA
                        : String.valueOf(model.httpErrors.size() - snap.previousHttpErrors))
                .append("</td></tr></tbody></table>");
        html.append("<h4>Scenario failure changes</h4>");
        if (snap.newFailures.isEmpty() && snap.fixedFailures.isEmpty() && snap.persistentFailures.isEmpty()) {
            html.append("<p class=\"muted\">").append(RunMetadata.NA)
                    .append(" — no comparable scenario failures found, or no prior scenario digest exists.</p>");
        } else {
            failureDeltaList(html, "New", snap.newFailures);
            failureDeltaList(html, "Fixed", snap.fixedFailures);
            failureDeltaList(html, "Persistent", snap.persistentFailures);
        }
        html.append("</section>");
    }

    private static void failureDeltaList(StringBuilder html, String label, List<String> items) {
        html.append("<p><strong>").append(label).append(" ( ").append(items.size()).append(" )</strong></p>");
        if (items.isEmpty()) {
            html.append("<p class=\"muted\">None</p>");
            return;
        }
        html.append("<ul>");
        for (String item : items) {
            html.append("<li>").append(esc(item)).append("</li>");
        }
        html.append("</ul>");
    }

    private static void compareModeScenarios(StringBuilder html, JSONObject headed, JSONObject headless) {
        JSONArray h = headed.optJSONArray("scenarioDigest");
        JSONArray l = headless.optJSONArray("scenarioDigest");
        if (h == null || l == null) {
            html.append("<p>Scenario digest ").append(RunMetadata.NA).append(" on one or both modes.</p>");
            return;
        }
        Map<String, String> headedStatuses = scenarioStatuses(h);
        Map<String, String> headlessStatuses = scenarioStatuses(l);
        Set<String> names = new LinkedHashSet<>();
        names.addAll(headedStatuses.keySet());
        names.addAll(headlessStatuses.keySet());
        html.append("<table><thead><tr><th>Feature / scenario</th><th>Headed</th><th>Headless</th><th>Comparison</th></tr></thead><tbody>");
        for (String name : names) {
            String headedStatus = headedStatuses.get(name);
            String headlessStatus = headlessStatuses.get(name);
            String comparison;
            if (headedStatus == null || headlessStatus == null) {
                comparison = "Not executed in both modes";
            } else if (headedStatus.equals(headlessStatus)) {
                comparison = "Same status";
            } else {
                comparison = "Mode difference";
            }
            html.append("<tr><td>").append(esc(name)).append("</td><td>")
                    .append(esc(headedStatus == null ? "Not executed" : headedStatus)).append("</td><td>")
                    .append(esc(headlessStatus == null ? "Not executed" : headlessStatus)).append("</td><td>")
                    .append(esc(comparison)).append("</td></tr>");
        }
        html.append("</tbody></table>");
    }

    private static Map<String, String> scenarioStatuses(JSONArray digest) {
        Map<String, String> statuses = new LinkedHashMap<>();
        for (int i = 0; i < digest.length(); i++) {
            JSONObject scenario = digest.optJSONObject(i);
            if (scenario == null) {
                continue;
            }
            String name = scenario.optString("feature", "") + " / " + scenario.optString("name", "");
            statuses.put(name, scenario.optString("status", "UNKNOWN"));
        }
        return statuses;
    }

    private static JSONObject lastMode(JSONArray history, String mode) {
        if (history == null) {
            return null;
        }
        for (int i = history.length() - 1; i >= 0; i--) {
            JSONObject run = history.optJSONObject(i);
            if (run != null && !run.optBoolean("dryRun") && mode.equals(run.optString("browserMode"))) {
                return run;
            }
        }
        return null;
    }

    private static void modeRow(StringBuilder html, String metric, String a, String b, String current) {
        html.append("<tr><td>").append(esc(metric)).append("</td><td>").append(esc(a)).append("</td><td>")
                .append(esc(b)).append("</td><td>").append(esc(current)).append("</td></tr>");
    }

    private static String pctOf(JSONObject run) {
        if (run == null || !run.has("passRate")) {
            return RunMetadata.NA;
        }
        return run.optDouble("passRate") + "%";
    }

    private static String num(JSONObject run, String key) {
        if (run == null || !run.has(key)) {
            return RunMetadata.NA;
        }
        return String.valueOf(run.optInt(key));
    }

    private static String durOf(JSONObject run) {
        if (run == null || !run.has("durationMs")) {
            return RunMetadata.NA;
        }
        return HtmlExecutionReportWriter.formatDuration(run.optLong("durationMs"));
    }

    private static void factorRow(StringBuilder html, String name, double weight, double score) {
        html.append("<tr><td>").append(esc(name)).append("</td><td>").append((int) weight)
                .append("%</td><td>").append(Double.isNaN(score) ? RunMetadata.NA
                        : String.format(Locale.US, "%.1f", score)).append("</td></tr>");
    }

    private static void kpi(StringBuilder html, String label, String value, String tone) {
        html.append("<article class=\"kpi ").append(tone).append("\"><h4>").append(esc(label))
                .append("</h4><p>").append(esc(value)).append("</p></article>");
    }

    private static void meta(StringBuilder html, String k, String v) {
        html.append("<div><span>").append(esc(k)).append("</span><strong>")
                .append(esc(RunMetadata.display(v))).append("</strong></div>");
    }

    private static String toolbar() {
        return "<div class=\"tools\"><input class=\"filter\" placeholder=\"Filter table…\"></div>";
    }

    private static String badge(String status) {
        String s = status == null ? "UNKNOWN" : status;
        return "<span class=\"badge " + s.toLowerCase() + "\">" + HtmlExecutionReportWriter.esc(s) + "</span>";
    }

    private static String artifactLink(String path) {
        if (path == null || path.isEmpty() || path.equals(RunMetadata.NA)
                || path.equals("Artifact Not Available")) {
            return "Artifact Not Available";
        }
        return "<a href=\"" + HtmlExecutionReportWriter.esc(path) + "\">screenshot</a>";
    }

    private static String tone(double pct, double green, double amber) {
        if (Double.isNaN(pct)) {
            return "blue";
        }
        if (pct >= green) {
            return "green";
        }
        if (pct >= amber) {
            return "amber";
        }
        return "red";
    }

    private static String toneJourney(QualityIntelligence.Snapshot snap) {
        for (QualityIntelligence.Journey j : snap.journeys) {
            if (j.criticalFailure && !"Post-INK2 lifecycle".equals(j.name)) {
                return "red";
            }
        }
        return "green";
    }

    private static String journeyPassLabel(QualityIntelligence.Snapshot snap) {
        int ex = 0;
        int pass = 0;
        for (QualityIntelligence.Journey j : snap.journeys) {
            if ("NOT_EXECUTED".equals(j.status)) {
                continue;
            }
            ex++;
            if ("PASSED".equals(j.status)) {
                pass++;
            }
        }
        return pass + " / " + ex;
    }

    private static String fmtScore(double score) {
        return Double.isNaN(score) ? RunMetadata.NA : String.format(Locale.US, "%.0f / 100", score);
    }

    private static String dur(long ms) {
        return ms < 0 ? RunMetadata.NA : HtmlExecutionReportWriter.formatDuration(ms);
    }

    private static String trim(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private static String esc(String s) {
        return HtmlExecutionReportWriter.esc(s);
    }

    private static String css() {
        return "<style>"
                + ":root{--bg:#0b1220;--panel:#121a2b;--line:#243049;--text:#e8eefc;--muted:#93a0b8;"
                + "--green:#16a34a;--red:#dc2626;--amber:#d97706;--blue:#2563eb;--purple:#7c3aed;}"
                + "[data-theme=light]{--bg:#f5f7fb;--panel:#fff;--line:#d7deea;--text:#0f172a;--muted:#475569;}"
                + "html,body{margin:0;background:var(--bg);color:var(--text);font-family:Inter,Segoe UI,sans-serif;}"
                + ".app{display:grid;grid-template-columns:260px 1fr;min-height:100vh;}"
                + "aside{border-right:1px solid var(--line);padding:20px;position:sticky;top:0;height:100vh;overflow:auto;}"
                + "aside a{display:block;color:var(--muted);text-decoration:none;padding:6px 0;font-size:13px;}"
                + "aside a:hover{color:var(--text);} .brand{letter-spacing:.14em;text-transform:uppercase;font-size:11px;color:var(--muted);}"
                + "main{padding:24px 32px 80px;} section{margin:28px 0;background:var(--panel);border:1px solid var(--line);border-radius:16px;padding:20px;}"
                + ".top{display:flex;justify-content:space-between;gap:16px;align-items:flex-start;}"
                + ".kpis{display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:12px;}"
                + ".kpi{border:1px solid var(--line);border-radius:12px;padding:12px;} .kpi h4{margin:0;font-size:12px;color:var(--muted);}"
                + ".kpi p{margin:8px 0 0;font-size:22px;font-weight:700;} .kpi.green{border-color:var(--green);} .kpi.red{border-color:var(--red);}"
                + ".kpi.amber{border-color:var(--amber);} .kpi.blue{border-color:var(--blue);} .kpi.purple{border-color:var(--purple);}"
                + "table{width:100%;border-collapse:collapse;} th,td{border-bottom:1px solid var(--line);padding:8px;text-align:left;vertical-align:top;font-size:13px;}"
                + "thead th{position:sticky;top:0;background:var(--panel);} .badge{padding:2px 8px;border-radius:999px;font-size:11px;}"
                + ".badge.passed,.badge.green{background:#14532d;color:#bbf7d0;} .badge.failed,.badge.red{background:#7f1d1d;color:#fecaca;}"
                + ".badge.skipped,.badge.amber,.badge.partial,.badge.not_executed{background:#78350f;color:#fde68a;}"
                + ".status-banner{padding:10px 12px;border-radius:10px;} .status-banner.green{background:#14532d;} .status-banner.red{background:#7f1d1d;} .status-banner.amber{background:#78350f;}"
                + ".muted{color:var(--muted);} .url{word-break:break-all;} pre{white-space:pre-wrap;font-size:11px;}"
                + ".meta-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:8px;margin-bottom:16px;}"
                + ".meta-grid span{display:block;color:var(--muted);font-size:11px;} .score-hero{text-align:center;padding:20px;border-radius:16px;}"
                + ".big{font-size:48px;margin:8px;} .btn{color:var(--text);border:1px solid var(--line);padding:6px 10px;border-radius:8px;background:transparent;text-decoration:none;margin-left:6px;}"
                + "#q,.filter{background:transparent;border:1px solid var(--line);color:var(--text);padding:8px;border-radius:8px;width:220px;}"
                + ".bars{display:flex;gap:6px;align-items:flex-end;height:80px;margin:12px 0;} .bar{flex:1;background:var(--line);height:100%;display:flex;align-items:flex-end;}"
                + ".bar span{display:block;width:100%;background:var(--blue);} .ok{color:#86efac;} .narrative{white-space:pre-wrap;}"
                + "@media print{aside,.actions,#themeBtn{display:none;} .app{display:block;} section{break-inside:avoid;}}"
                + "</style>";
    }

    private static String js() {
        return "<script>"
                + "document.getElementById('themeBtn').onclick=function(){var r=document.documentElement;"
                + "r.setAttribute('data-theme', r.getAttribute('data-theme')==='dark'?'light':'dark');};"
                + "document.getElementById('q').addEventListener('input',function(e){var q=e.target.value.toLowerCase();"
                + "document.querySelectorAll('section').forEach(function(s){s.style.display=!q||s.innerText.toLowerCase().indexOf(q)>=0?'block':'none';});});"
                + "document.querySelectorAll('.filter').forEach(function(inp){inp.addEventListener('input',function(){var q=inp.value.toLowerCase();"
                + "var table=inp.closest('section').querySelector('table'); if(!table)return;"
                + "table.querySelectorAll('tbody tr').forEach(function(tr){tr.style.display=!q||tr.innerText.toLowerCase().indexOf(q)>=0?'':'none';});});});"
                + "var rows=[].slice.call(document.querySelectorAll('#stepTable .page-row'));"
                + "var page=0; function render(){rows.forEach(function(r){r.style.display=r.getAttribute('data-page')==String(page)?'':'none';});"
                + "var p=document.getElementById('stepPager'); if(!p)return; var max=0; rows.forEach(function(r){max=Math.max(max,+r.getAttribute('data-page'));});"
                + "p.innerHTML='Page '+(page+1)+' / '+(max+1)+' <button type=button id=prevP>Prev</button> <button type=button id=nextP>Next</button>';"
                + "var a=document.getElementById('prevP'); var b=document.getElementById('nextP');"
                + "if(a)a.onclick=function(){page=Math.max(0,page-1);render();}; if(b)b.onclick=function(){page=Math.min(max,page+1);render();};}"
                + "if(rows.length)render();"
                + "</script>";
    }
}
