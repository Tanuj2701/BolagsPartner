package com.qa.bolags.reporting;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class HtmlExecutionReportWriter {

    private HtmlExecutionReportWriter() {
    }

    static void write(Path file, ExecutionRunModel model) throws IOException {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        html.append("<title>Execution Report — ").append(esc(model.browserMode)).append("</title>");
        html.append(sharedCss());
        html.append("</head><body>");
        html.append("<header class=\"hero\"><div>");
        html.append("<p class=\"eyebrow\">BolagsPartner QA</p>");
        html.append("<h1>Execution report</h1>");
        html.append("<p class=\"meta\">Mode <strong>").append(esc(model.browserMode))
                .append("</strong> · ").append(esc(model.startedAt))
                .append(" → ").append(esc(model.finishedAt))
                .append(" · ").append(formatDuration(model.durationMs)).append("</p>");
        if (model.dryRun) {
            html.append("<p class=\"status-banner\">Cucumber dry run: bindings checked, application behavior not executed.</p>");
        }
        html.append("</div><span class=\"badge ").append(model.overallStatus.toLowerCase())
                .append("\">").append(esc(model.overallStatus)).append("</span></header>");

        html.append("<section class=\"kpis\">");
        kpi(html, "Features", model.featuresPassed, model.featuresFailed, model.featuresTotal);
        kpi(html, "Scenarios (TC)", model.scenariosPassed, model.scenariosFailed, model.scenariosTotal);
        kpi(html, "Steps", model.stepsPassed, model.stepsFailed, model.stepsTotal);
        html.append("<article class=\"kpi\"><h3>Overall pass rate</h3><p class=\"num\">")
            .append(Double.isNaN(model.passRatePct) ? "Data Not Available"
                : String.format(java.util.Locale.ROOT, "%.1f%%", model.passRatePct))
            .append("</p><p class=\"sub\">Skipped ").append(model.scenariosSkipped)
            .append(" · blocked ").append(model.scenariosBlocked).append(" · broken ")
                .append(model.scenariosBroken).append(" · dry-run ").append(model.scenariosDryRun)
                .append("</p></article>");
        html.append("<article class=\"kpi\"><h3>HTTP 4xx/5xx</h3><p class=\"num\">")
                .append(model.httpErrors.size()).append("</p></article>");
        html.append("<article class=\"kpi\"><h3>Release confidence</h3><p class=\"num\">")
            .append(Double.isNaN(model.releaseConfidenceIndex) ? "Data Not Available"
                : String.format(java.util.Locale.ROOT, "%.0f / 100", model.releaseConfidenceIndex))
            .append("</p><p class=\"sub\">").append(esc(model.releaseConfidenceIndication))
            .append("</p></article>");
        html.append("</section>");

        html.append("<section><h2>Release risks</h2>");
        if (model.releaseRisks.isEmpty()) {
            html.append("<p class=\"ok\">No release risks identified from captured evidence.</p>");
        } else {
            html.append("<ul>");
            for (String risk : model.releaseRisks) {
            html.append("<li>").append(esc(risk)).append("</li>");
            }
            html.append("</ul>");
        }
        html.append("</section>");

        html.append("<section><h2>Failures &amp; errors</h2>");
        if (model.failures.isEmpty()) {
            html.append("<p class=\"ok\">No assertion or step failures.</p>");
        } else {
            html.append("<table><thead><tr><th>Level</th><th>Category</th><th>Feature</th><th>Scenario</th><th>Step</th><th>Evidence</th><th>Error</th></tr></thead><tbody>");
            for (ExecutionRunModel.FailureRecord failure : model.failures) {
                html.append("<tr class=\"fail-row\"><td>").append(esc(failure.level))
                        .append("</td><td>").append(esc(failure.category))
                        .append("</td><td>").append(esc(failure.feature))
                        .append("</td><td>").append(esc(failure.scenario))
                        .append("</td><td>").append(esc(failure.step))
                        .append("</td><td>");
                if (!RunMetadata.NA.equals(failure.screenshot)) {
                    html.append("<a href=\"").append(esc(failure.screenshot)).append("\">Screenshot</a> · ");
                }
                html.append(esc(failure.executionMode)).append(" · ").append(esc(failure.pageUrl))
                        .append("</td><td><pre>").append(esc(failure.message)).append("</pre></td></tr>");
            }
            html.append("</tbody></table>");
        }
        html.append("</section>");

        html.append("<section><h2>API errors (4xx / 5xx)</h2>");
        if (model.httpErrors.isEmpty()) {
            html.append("<p class=\"ok\">No 4xx or 5xx responses captured.</p>");
        } else {
            html.append("<table><thead><tr><th>Status</th><th>Type</th><th>Method</th><th>URL</th><th>Duration</th><th>Body</th><th>cURL</th></tr></thead><tbody>");
            for (HttpErrorCapture.HttpError error : model.httpErrors) {
                html.append("<tr><td>").append(error.status).append("</td><td>")
                        .append(esc(error.resourceType)).append("</td><td>").append(esc(error.method))
                        .append("</td><td class=\"url\">").append(esc(error.url)).append("</td><td>")
                        .append(error.responseTimeMs < 0 ? "Data Not Available" : error.responseTimeMs + " ms")
                        .append("</td><td><pre>")
                        .append(esc(error.body)).append("</pre></td><td>");
                if (error.curlCommand.isEmpty()) {
                    html.append("Data Not Available");
                } else {
                    html.append("<details><summary>Show cURL</summary><pre class=\"curl\">")
                            .append(esc(error.curlCommand)).append("</pre></details>");
                }
                html.append("</td></tr>");
            }
            html.append("</tbody></table>");
        }
        html.append("</section>");

        html.append("<section><h2>Feature / scenario / step status</h2>");
        for (ExecutionRunModel.FeatureResult feature : model.features) {
            html.append("<article class=\"feature\"><h3>")
                    .append(esc(feature.name)).append(" <span class=\"badge ")
                    .append(feature.status.toLowerCase()).append("\">")
                    .append(esc(feature.status)).append("</span></h3>");
            html.append("<p class=\"meta\">").append(esc(feature.uri)).append(" · ")
                    .append(formatDuration(feature.durationMs)).append(" · ").append(feature.scenariosPassed)
                    .append(" passed / ").append(feature.scenariosFailed).append(" failed / ")
                    .append(feature.scenariosSkipped).append(" skipped / ").append(feature.scenariosBlocked)
                    .append(" blocked</p>");
            for (ExecutionRunModel.ScenarioResult scenario : feature.scenarios) {
                html.append("<div class=\"scenario\"><h4>")
                        .append(esc(scenario.name)).append(" <span class=\"badge ")
                        .append(scenario.status.toLowerCase()).append("\">")
                        .append(esc(scenario.status)).append("</span></h4><ol>");
                for (ExecutionRunModel.StepResult step : scenario.steps) {
                    html.append("<li class=\"").append(step.status.toLowerCase()).append("\"><span class=\"kw\">")
                            .append(esc(step.keyword)).append("</span> ")
                            .append(esc(step.text)).append(" <em>")
                            .append(esc(step.status)).append(" · ")
                            .append(formatDuration(step.durationMs)).append("</em>");
                    if (!step.error.isEmpty()) {
                        html.append("<pre>").append(esc(step.error)).append("</pre>");
                    }
                    html.append("</li>");
                }
                html.append("</ol></div>");
            }
            html.append("</article>");
        }
        html.append("</section>");
        html.append("<footer><a href=\"../../../dashboard/index.html\">← Report dashboard</a> · ")
            .append("<a href=\"quality.html\">Quality Intelligence &amp; Release Confidence</a></footer>");
        html.append("</body></html>");
        Files.createDirectories(file.getParent());
        Files.write(file, html.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void kpi(StringBuilder html, String label, int passed, int failed, int total) {
        html.append("<article class=\"kpi\"><h3>").append(esc(label))
                .append("</h3><p class=\"num\">").append(passed).append(" / ").append(total)
                .append("</p><p class=\"sub\">").append(failed).append(" failed</p></article>");
    }

    static String sharedCss() {
        return "<style>"
                + "body{font-family:Inter,Segoe UI,system-ui,sans-serif;margin:0;background:#0f172a;color:#e2e8f0;}"
                + ".hero,.kpis,section,footer{max-width:1200px;margin:0 auto;padding:24px;}"
                + ".hero{display:flex;justify-content:space-between;align-items:flex-start;gap:16px;}"
                + "h1,h2,h3,h4{margin:0 0 8px;} .eyebrow{letter-spacing:.12em;text-transform:uppercase;color:#94a3b8;font-size:12px;}"
                + ".meta{color:#94a3b8;} table{width:100%;border-collapse:collapse;background:#1e293b;}"
                + "th,td{border-bottom:1px solid #334155;padding:10px;text-align:left;vertical-align:top;font-size:14px;}"
                + ".badge{padding:4px 10px;border-radius:999px;font-size:12px;font-weight:600;}"
                + ".badge.passed{background:#14532d;color:#bbf7d0;} .badge.failed{background:#7f1d1d;color:#fecaca;}"
                + ".badge.skipped,.badge.undefined,.badge.pending{background:#78350f;color:#fde68a;}"
                + ".badge.dry_run{background:#475569;color:#e2e8f0;} .status-banner{padding:12px;border-left:4px solid #f59e0b;background:#422006;color:#fde68a;}"
                + ".kpis{display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:12px;}"
                + ".kpi{background:#1e293b;border:1px solid #334155;border-radius:12px;padding:16px;}"
                + ".num{font-size:28px;margin:0;font-weight:700;} .ok{color:#86efac;} .url{word-break:break-all;}"
                + "pre{white-space:pre-wrap;font-size:12px;color:#fecaca;}pre.curl{color:#bfdbfe;max-width:520px;}details summary{cursor:pointer;color:#93c5fd;} ol{padding-left:20px;}"
                + "li.failed{color:#fecaca;} li.skipped,li.undefined{color:#fde68a;} a{color:#93c5fd;}"
                + ".feature,.scenario{background:#1e293b;border:1px solid #334155;border-radius:12px;padding:16px;margin:12px 0;}"
                + "</style>";
    }

    static String esc(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    static String formatDuration(long ms) {
        long seconds = Math.max(0, ms) / 1000;
        return String.format("%d min %02d s", seconds / 60, seconds % 60);
    }
}
