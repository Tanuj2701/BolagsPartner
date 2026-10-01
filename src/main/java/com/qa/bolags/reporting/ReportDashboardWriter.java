package com.qa.bolags.reporting;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

final class ReportDashboardWriter {

    private ReportDashboardWriter() {
    }

    static void update(Path runDir, ExecutionRunModel model, Path htmlReport, Path pdfReport,
                       Path qualityReport, Path exportDir) throws IOException {
        Files.createDirectories(ReportPaths.DASHBOARD);
        JSONArray runs = readRuns();
        JSONObject run = new JSONObject();
        run.put("id", runDir.getFileName().toString());
        run.put("browserMode", model.browserMode);
        run.put("status", model.overallStatus);
        run.put("dryRun", model.dryRun);
        run.put("startedAt", model.startedAt);
        run.put("finishedAt", model.finishedAt);
        run.put("durationMs", model.durationMs);
        run.put("passRate", finiteOrNull(model.passRatePct));
        run.put("featuresPassed", model.featuresPassed);
        run.put("featuresTotal", model.featuresTotal);
        run.put("featuresFailed", model.featuresFailed);
        run.put("featuresSkipped", model.featuresSkipped);
        run.put("featuresBlocked", model.featuresBlocked);
        run.put("featuresDryRun", model.featuresDryRun);
        run.put("featuresPartial", model.featuresPartial);
        run.put("scenariosPassed", model.scenariosPassed);
        run.put("scenariosTotal", model.scenariosTotal);
        run.put("scenariosFailed", model.scenariosFailed);
        run.put("scenariosBroken", model.scenariosBroken);
        run.put("scenariosSkipped", model.scenariosSkipped);
        run.put("scenariosBlocked", model.scenariosBlocked);
        run.put("scenariosDryRun", model.scenariosDryRun);
        run.put("stepsPassed", model.stepsPassed);
        run.put("stepsTotal", model.stepsTotal);
        run.put("stepsFailed", model.stepsFailed);
        run.put("stepsSkipped", model.stepsSkipped);
        run.put("stepsUndefined", model.stepsUndefined);
        run.put("stepsDryRun", model.stepsDryRun);
        run.put("failures", model.failures.size());
        run.put("httpErrors", model.httpErrors.size());
        run.put("xhrTotal", model.xhrTotal);
        run.put("xhr4xx", model.xhr4xx);
        run.put("xhr5xx", model.xhr5xx);
        run.put("releaseConfidenceIndex", finiteOrNull(model.releaseConfidenceIndex));
        run.put("releaseConfidenceIndication", model.releaseConfidenceIndication);
        run.put("releaseRisks", new JSONArray(model.releaseRisks));
        run.put("scenarioDigest", scenarioDigest(model));
        run.put("failureCategories", failureCategories(model));
        run.put("html", relativeToDashboard(htmlReport));
        run.put("pdf", relativeToDashboard(pdfReport));
        run.put("quality", relativeToDashboard(qualityReport));
        run.put("exports", relativeToDashboard(exportDir));
        run.put("scenariosCsv", relativeToDashboard(exportDir.resolve("scenarios.csv")));
        run.put("stepsCsv", relativeToDashboard(exportDir.resolve("steps.csv")));
        run.put("failuresCsv", relativeToDashboard(exportDir.resolve("failures.csv")));
        run.put("apiErrorsCsv", relativeToDashboard(exportDir.resolve("api-errors.csv")));
        run.put("json", relativeToDashboard(runDir.resolve("summary.json")));
        runs.put(run);
        Files.write(ReportPaths.RUNS_INDEX, runs.toString(2).getBytes(StandardCharsets.UTF_8));
        writeIndex(runs);
        Path latest = ReportPaths.ROOT.resolve(model.browserMode).resolve("latest");
        Files.createDirectories(latest.getParent());
        if (!model.dryRun) {
            copyIfPresent(htmlReport, latest.resolve("html").resolve("index.html"));
            copyIfPresent(qualityReport, latest.resolve("html").resolve("quality.html"));
            copyIfPresent(pdfReport, latest.resolve("pdf").resolve("execution-report.pdf"));
            copyIfPresent(runDir.resolve("summary.json"), latest.resolve("summary.json"));
        }
        if (!model.dryRun && Files.isDirectory(exportDir)) {
            try (java.util.stream.Stream<Path> files = Files.list(exportDir)) {
                files.forEach(file -> {
                    try {
                        copyIfPresent(file, latest.resolve("export").resolve(file.getFileName()));
                    } catch (IOException e) {
                        throw new java.io.UncheckedIOException(e);
                    }
                });
            } catch (java.io.UncheckedIOException e) {
                throw e.getCause();
            }
        }
        if (!model.dryRun) {
            copyDirectoryIfPresent(runDir.resolve("artifacts"), latest.resolve("artifacts"));
        }
    }

    private static Object finiteOrNull(double value) {
        return Double.isNaN(value) || Double.isInfinite(value) ? JSONObject.NULL : value;
    }

    private static void copyDirectoryIfPresent(Path source, Path destination) throws IOException {
        if (!Files.isDirectory(source)) {
            return;
        }
        try (java.util.stream.Stream<Path> files = Files.walk(source)) {
            files.forEach(file -> {
                try {
                    Path relative = source.relativize(file);
                    if (Files.isDirectory(file)) {
                        Files.createDirectories(destination.resolve(relative));
                    } else {
                        copyIfPresent(file, destination.resolve(relative));
                    }
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            });
        } catch (java.io.UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private static JSONArray scenarioDigest(ExecutionRunModel model) {
        JSONArray digest = new JSONArray();
        for (ExecutionRunModel.FeatureResult feature : model.features) {
            for (ExecutionRunModel.ScenarioResult scenario : feature.scenarios) {
                JSONObject item = new JSONObject();
                item.put("feature", feature.name);
                item.put("name", scenario.name);
                item.put("status", scenario.status);
                digest.put(item);
            }
        }
        return digest;
    }

    private static JSONObject failureCategories(ExecutionRunModel model) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ExecutionRunModel.FailureRecord failure : model.failures) {
            counts.put(failure.category, counts.getOrDefault(failure.category, 0) + 1);
        }
        return new JSONObject(counts);
    }

    private static void copyIfPresent(Path source, Path dest) throws IOException {
        if (source != null && Files.exists(source)) {
            Files.createDirectories(dest.getParent());
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static JSONArray readRuns() {
        try {
            if (Files.exists(ReportPaths.RUNS_INDEX)) {
                return new JSONArray(new String(Files.readAllBytes(ReportPaths.RUNS_INDEX), StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {
            // rebuild dashboard from this run only
        }
        return new JSONArray();
    }

    private static String relativeToDashboard(Path file) {
        Path dash = ReportPaths.DASHBOARD.toAbsolutePath().normalize();
        Path abs = file.toAbsolutePath().normalize();
        return dash.relativize(abs).toString().replace('\\', '/');
    }

    private static void writeIndex(JSONArray runs) throws IOException {
        int headed = 0;
        int headless = 0;
        int passed = 0;
        for (int i = 0; i < runs.length(); i++) {
            JSONObject run = runs.getJSONObject(i);
            if ("headless".equals(run.optString("browserMode"))) {
                headless++;
            } else {
                headed++;
            }
            if ("PASSED".equals(run.optString("status"))) {
                passed++;
            }
        }
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        html.append("<title>BolagsPartner QA — Report Dashboard</title>");
        html.append(HtmlExecutionReportWriter.sharedCss());
        html.append("<style>.filters{display:flex;gap:8px;flex-wrap:wrap}.filters button{background:#334155;color:#e2e8f0;border:0;padding:8px 14px;border-radius:6px;cursor:pointer;}");
        html.append(".filters button.active{background:#2563eb}tr.hidden{display:none}.mode-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:12px}.mode-card{background:#1e293b;border:1px solid #334155;border-top:4px solid #38bdf8;border-radius:8px;padding:18px}.mode-card.headless{border-top-color:#f59e0b}.toolbar{display:flex;gap:12px;align-items:center;flex-wrap:wrap}.toolbar input,.toolbar select{padding:9px;min-width:160px;border-radius:6px;border:1px solid #475569;background:#0f172a;color:#e2e8f0}.cleanup{display:flex;gap:10px;align-items:center;flex-wrap:wrap;padding:14px;background:#1e293b;border:1px solid #475569;border-radius:8px}.cleanup button{background:#b91c1c;color:#fff;border:0;border-radius:6px;padding:9px 14px;cursor:pointer}.cleanup button:disabled{opacity:.55;cursor:wait}.cleanup-status{min-width:180px;color:#bae6fd}</style>");
        html.append("</head><body>");
        html.append("<header class=\"hero\"><div><p class=\"eyebrow\">Industry dashboard</p>");
        html.append("<h1>Automation reports</h1>");
        html.append("<p class=\"meta\">Headed and headless executions · feature / scenario / step status · HTTP 4xx/5xx</p>");
        html.append("</div></header>");
        html.append("<section class=\"kpis\">");
        html.append("<article class=\"kpi\"><h3>Runs</h3><p id=\"dashboardRunCount\" class=\"num\">").append(runs.length()).append("</p></article>");
        html.append("<article class=\"kpi\"><h3>Passed</h3><p id=\"dashboardPassedCount\" class=\"num\">").append(passed).append("</p></article>");
        html.append("<article class=\"kpi\"><h3>Headed</h3><p id=\"dashboardHeadedCount\" class=\"num\">").append(headed).append("</p></article>");
        html.append("<article class=\"kpi\"><h3>Headless</h3><p id=\"dashboardHeadlessCount\" class=\"num\">").append(headless).append("</p></article>");
        html.append("</section>");
        html.append("<section id=\"modeSnapshots\"><h2>Latest mode snapshots</h2><div class=\"mode-grid\">");
        modeCard(html, lastMode(runs, "headed"), "Normal / Headed", "headed");
        modeCard(html, lastMode(runs, "headless"), "Headless", "headless");
        html.append("</div></section><section id=\"combinedComparison\"><h2>Combined latest comparison</h2>");
        combinedSummary(html, lastMode(runs, "headed"), lastMode(runs, "headless"));
        html.append("</section>");
        html.append("<section><div class=\"toolbar\"><div class=\"filters\">");
        html.append("<button class=\"active\" data-mode=\"all\">All</button>");
        html.append("<button data-mode=\"headed\">Normal browser</button>");
        html.append("<button data-mode=\"headless\">Headless</button></div><input id=\"runSearch\" type=\"search\" placeholder=\"Search run, status, mode\"></div>");
        html.append("<div class=\"cleanup\"><label for=\"retentionDays\">Remove reports older than</label>");
        html.append("<select id=\"retentionDays\"><option value=\"2\" selected>2 days</option><option value=\"7\">7 days</option>");
        html.append("<option value=\"14\">14 days</option><option value=\"30\">30 days</option><option value=\"custom\">Custom…</option></select>");
        html.append("<input id=\"customRetentionDays\" type=\"number\" min=\"1\" max=\"3650\" value=\"2\" aria-label=\"Custom retention days\" hidden>");
        html.append("<button id=\"clearOldReports\" type=\"button\">Clear old reports</button>");
        html.append("<span id=\"cleanupStatus\" class=\"cleanup-status\" role=\"status\"></span></div>");
        html.append("<p class=\"muted\">Cleanup asks you to select the target/reports folder and confirm before deleting dated headed/headless runs.</p>");
        html.append("<table><thead><tr><th>Run</th><th>Mode</th><th>Status</th><th>Features</th><th>Scenarios</th><th>Steps</th>");
        html.append("<th>Failures</th><th>HTTP errors</th><th>Duration</th><th>Artifacts</th></tr></thead><tbody>");
        for (int i = runs.length() - 1; i >= 0; i--) {
            JSONObject run = runs.getJSONObject(i);
            html.append("<tr data-run-id=\"").append(HtmlExecutionReportWriter.esc(run.optString("id")))
                    .append("\" data-mode=\"").append(HtmlExecutionReportWriter.esc(run.optString("browserMode")))
                    .append("\"><td>").append(HtmlExecutionReportWriter.esc(run.optString("id")))
                    .append("</td><td>").append(HtmlExecutionReportWriter.esc(run.optString("browserMode")))
                    .append("</td><td><span class=\"badge ").append(run.optString("status").toLowerCase())
                    .append("\">").append(HtmlExecutionReportWriter.esc(run.optString("status")))
                    .append("</span></td><td>").append(run.optInt("featuresPassed")).append("/")
                    .append(run.optInt("featuresTotal")).append("</td><td>")
                    .append(run.optInt("scenariosPassed")).append("/").append(run.optInt("scenariosTotal"))
                    .append("</td><td>").append(run.optInt("stepsPassed")).append("/")
                    .append(run.optInt("stepsTotal")).append("</td><td>").append(run.optInt("failures"))
                    .append("</td><td>").append(run.optInt("httpErrors")).append("</td><td>")
                    .append(HtmlExecutionReportWriter.formatDuration(run.optLong("durationMs")))
                    .append("</td><td><a href=\"").append(HtmlExecutionReportWriter.esc(run.optString("html")))
                    .append("\">HTML</a> · <a href=\"").append(HtmlExecutionReportWriter.esc(run.optString("quality")))
                    .append("\">Quality</a> · <a href=\"").append(HtmlExecutionReportWriter.esc(run.optString("pdf")))
                    .append("\">PDF</a> · <a href=\"").append(HtmlExecutionReportWriter.esc(run.optString("json")))
                    .append("\">JSON</a> · <a href=\"").append(HtmlExecutionReportWriter.esc(run.optString("scenariosCsv")))
                    .append("\">Scenarios CSV</a> · <a href=\"").append(HtmlExecutionReportWriter.esc(run.optString("failuresCsv")))
                    .append("\">Failures CSV</a> · <a href=\"").append(HtmlExecutionReportWriter.esc(run.optString("apiErrorsCsv")))
                    .append("\">API CSV</a></td></tr>");
        }
        html.append("</tbody></table></section>");
        html.append("<footer>Latest reports: <a href=\"../headed/latest/html/index.html\">headed HTML</a> · ");
        html.append("<a href=\"../headed/latest/html/quality.html\">headed quality</a> · ");
        html.append("<a href=\"../headless/latest/html/index.html\">headless HTML</a> · ");
        html.append("<a href=\"../headless/latest/html/quality.html\">headless quality</a></footer>");
        html.append("<script>document.querySelectorAll('.filters button').forEach(function(btn){");
        html.append("btn.addEventListener('click',function(){document.querySelectorAll('.filters button').forEach(function(b){b.classList.remove('active');});");
        html.append("btn.classList.add('active');filterRows();});});var search=document.getElementById('runSearch');search.addEventListener('input',filterRows);function filterRows(){var mode=document.querySelector('.filters button.active').getAttribute('data-mode');var q=search.value.toLowerCase();document.querySelectorAll('tbody tr').forEach(function(row){var modeOk=mode==='all'||row.getAttribute('data-mode')===mode;var textOk=row.textContent.toLowerCase().indexOf(q)>=0;row.classList.toggle('hidden',!modeOk||!textOk);});}</script>");
        html.append(cleanupScript());
        html.append("</body></html>");
        Files.write(ReportPaths.DASHBOARD.resolve("index.html"), html.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String cleanupScript() {
        return "<script>"
                + "const retention=document.getElementById('retentionDays');"
                + "const customDays=document.getElementById('customRetentionDays');"
                + "const cleanupButton=document.getElementById('clearOldReports');"
                + "const cleanupStatus=document.getElementById('cleanupStatus');"
                + "retention.addEventListener('change',()=>{customDays.hidden=retention.value!=='custom';});"
                + "function runDate(id){const m=/^(\\d{4})-(\\d{2})-(\\d{2})_(\\d{2})-(\\d{2})-(\\d{2})(?:-\\d+)?$/.exec(id||'');"
                + "return m?new Date(+m[1],+m[2]-1,+m[3],+m[4],+m[5],+m[6]):null;}"
                + "cleanupButton.addEventListener('click',async()=>{"
                + "if(!window.showDirectoryPicker){cleanupStatus.textContent='Use a recent Chrome/Edge browser to grant folder access.';return;}"
                + "let days=retention.value==='custom'?Number(customDays.value):Number(retention.value);"
                + "if(!Number.isInteger(days)||days<1||days>3650){cleanupStatus.textContent='Choose between 1 and 3650 days.';return;}"
                + "cleanupButton.disabled=true;cleanupStatus.textContent='Select target/reports…';"
                + "try{const root=await window.showDirectoryPicker({mode:'readwrite'});"
                + "const dashboard=await root.getDirectoryHandle('dashboard');"
                + "const runsFile=await dashboard.getFileHandle('runs.json');"
                + "const runs=JSON.parse(await (await runsFile.getFile()).text());"
                + "const cutoff=Date.now()-days*86400000;const removeIds=new Set();let folderCount=0;"
                + "for(const mode of ['headed','headless']){let modeDir;try{modeDir=await root.getDirectoryHandle(mode);}catch(e){continue;}"
                + "for await(const entry of modeDir.values()){if(entry.kind!=='directory')continue;const date=runDate(entry.name);"
                + "if(date&&date.getTime()<cutoff){removeIds.add(entry.name);folderCount++;}}"
                + "try{const latest=await modeDir.getDirectoryHandle('latest');const summaryHandle=await latest.getFileHandle('summary.json');"
                + "const summary=JSON.parse(await (await summaryHandle.getFile()).text());const started=new Date((summary.startedAt||'').replace(' ','T'));"
                + "if(!isNaN(started)&&started.getTime()<cutoff){for await(const latestEntry of latest.values())await latest.removeEntry(latestEntry.name,{recursive:true});}}catch(e){}}"
                + "const historyCount=runs.filter(r=>{const d=runDate(r.id);return d&&d.getTime()<cutoff;}).length;"
                + "if(folderCount===0&&historyCount===0){cleanupStatus.textContent='No dated runs older than '+days+' days.';return;}"
                + "if(!confirm('Permanently delete '+folderCount+' run folder(s) and remove '+historyCount+' history row(s) older than '+days+' days?')){cleanupStatus.textContent='Cleanup cancelled.';return;}"
                + "for(const mode of ['headed','headless']){let modeDir;try{modeDir=await root.getDirectoryHandle(mode,{create:false});}catch(e){continue;}"
                + "for(const id of removeIds){try{await modeDir.removeEntry(id,{recursive:true});}catch(e){}}}"
                + "const kept=runs.filter(r=>!removeIds.has(r.id)&&!(runDate(r.id)&&runDate(r.id).getTime()<cutoff));"
                + "const writable=await runsFile.createWritable();await writable.write(JSON.stringify(kept,null,2));await writable.close();"
                + "document.querySelectorAll('tbody tr[data-run-id]').forEach(row=>{if(removeIds.has(row.dataset.runId)||(!runDate(row.dataset.runId)?false:runDate(row.dataset.runId).getTime()<cutoff))row.remove();});"
                + "document.getElementById('dashboardRunCount').textContent=kept.length;"
                + "document.getElementById('dashboardPassedCount').textContent=kept.filter(r=>r.status==='PASSED').length;"
                + "document.getElementById('dashboardHeadedCount').textContent=kept.filter(r=>r.browserMode==='headed').length;"
                + "document.getElementById('dashboardHeadlessCount').textContent=kept.filter(r=>r.browserMode==='headless').length;"
                + "for(const card of document.querySelectorAll('.mode-card')){const mode=card.classList.contains('headless')?'headless':'headed';"
                + "if(!kept.some(r=>r.browserMode===mode&&!r.dryRun)){const title=card.querySelector('h3').textContent;card.innerHTML='<h3>'+title+'</h3><p>No retained execution for this mode.</p>';}}"
                + "if(!kept.some(r=>r.browserMode==='headed'&&!r.dryRun)||!kept.some(r=>r.browserMode==='headless'&&!r.dryRun))"
                + "document.getElementById('combinedComparison').innerHTML='<h2>Combined latest comparison</h2><p>Comparison unavailable until both modes have retained runs.</p>';"
                + "document.querySelectorAll('tbody tr').forEach(row=>row.classList.remove('hidden'));document.getElementById('runSearch').value='';"
                + "const indexHandle=await dashboard.getFileHandle('index.html',{create:true});const indexWritable=await indexHandle.createWritable();"
                + "await indexWritable.write('<!DOCTYPE html>\\n'+document.documentElement.outerHTML);await indexWritable.close();"
                + "cleanupStatus.textContent='Removed '+folderCount+' run folder(s). Dashboard history saved.';"
                + "}catch(e){cleanupStatus.textContent=e.name==='AbortError'?'Folder selection cancelled.':'Cleanup failed: '+e.message;}"
                + "finally{cleanupButton.disabled=false;}});</script>";
    }

    private static JSONObject lastMode(JSONArray runs, String mode) {
        for (int i = runs.length() - 1; i >= 0; i--) {
            JSONObject run = runs.optJSONObject(i);
            if (run != null && !run.optBoolean("dryRun") && mode.equals(run.optString("browserMode"))) {
                return run;
            }
        }
        return null;
    }

    private static void modeCard(StringBuilder html, JSONObject run, String label, String cssClass) {
        html.append("<article class=\"mode-card ").append(cssClass).append("\"><h3>").append(label).append("</h3>");
        if (run == null) {
            html.append("<p>No execution recorded.</p></article>");
            return;
        }
        html.append("<p>").append(HtmlExecutionReportWriter.esc(run.optString("id"))).append(" · ")
                .append(HtmlExecutionReportWriter.esc(run.optString("status"))).append(" · Pass rate ")
                .append(run.has("passRate") ? run.optDouble("passRate") + "%" : "Data Not Available")
                .append("</p><p>Scenarios ").append(run.optInt("scenariosPassed")).append("/")
                .append(run.optInt("scenariosTotal")).append(" · failures ").append(run.optInt("failures"))
                .append(" · HTTP errors ").append(run.optInt("httpErrors")).append(" · RCI ")
                .append(run.has("releaseConfidenceIndex") ? run.opt("releaseConfidenceIndex") : "Data Not Available")
                .append("</p><p><a href=\"").append(HtmlExecutionReportWriter.esc(run.optString("quality")))
                .append("\">Quality dashboard</a> · <a href=\"")
                .append(HtmlExecutionReportWriter.esc(run.optString("pdf"))).append("\">PDF</a></p></article>");
    }

    private static void combinedSummary(StringBuilder html, JSONObject headed, JSONObject headless) {
        if (headed == null || headless == null) {
            html.append("<p>Combined comparison is available after both headed and headless runs have completed.</p>");
            return;
        }
        double headedRate = headed.optDouble("passRate", Double.NaN);
        double headlessRate = headless.optDouble("passRate", Double.NaN);
        html.append("<p>This compares the latest run in each mode. Counts are not merged across suites that may differ.</p>");
        html.append("<table><thead><tr><th>Metric</th><th>Headed</th><th>Headless</th><th>Difference</th></tr></thead><tbody>");
        comparisonRow(html, "Pass rate", pct(headed, "passRate"), pct(headless, "passRate"),
                Double.isNaN(headedRate) || Double.isNaN(headlessRate) ? "Data Not Available"
                        : String.format(java.util.Locale.ROOT, "%+.1f pp", headlessRate - headedRate));
        comparisonRow(html, "Failed scenarios", String.valueOf(headed.optInt("scenariosFailed")),
                String.valueOf(headless.optInt("scenariosFailed")),
                signed(headless.optInt("scenariosFailed") - headed.optInt("scenariosFailed")));
        comparisonRow(html, "HTTP errors", String.valueOf(headed.optInt("httpErrors")),
                String.valueOf(headless.optInt("httpErrors")),
                signed(headless.optInt("httpErrors") - headed.optInt("httpErrors")));
        comparisonRow(html, "Duration", HtmlExecutionReportWriter.formatDuration(headed.optLong("durationMs")),
                HtmlExecutionReportWriter.formatDuration(headless.optLong("durationMs")),
                signed(headless.optLong("durationMs") - headed.optLong("durationMs")) + " ms");
        html.append("</tbody></table>");
    }

    private static void comparisonRow(StringBuilder html, String metric, String headed,
                                      String headless, String difference) {
        html.append("<tr><td>").append(metric).append("</td><td>").append(headed).append("</td><td>")
                .append(headless).append("</td><td>").append(difference).append("</td></tr>");
    }

    private static String pct(JSONObject run, String key) {
        return run.has(key) ? run.optDouble(key) + "%" : "Data Not Available";
    }

    private static String signed(long value) {
        return (value > 0 ? "+" : "") + value;
    }
}
