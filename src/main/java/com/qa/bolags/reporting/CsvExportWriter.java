package com.qa.bolags.reporting;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class CsvExportWriter {

    private CsvExportWriter() {
    }

    static void write(Path dir, ExecutionRunModel model) throws IOException {
        Files.createDirectories(dir);
        StringBuilder scenarios = new StringBuilder("feature,scenario,status,durationMs,tags\n");
        StringBuilder steps = new StringBuilder("feature,scenario,step,status,durationMs,category,error\n");
        StringBuilder failures = new StringBuilder(
                "level,feature,scenario,step,category,exception,mode,url,screenshot,message\n");
        StringBuilder apis = new StringBuilder("status,class,method,url,type,responseTimeMs,body,curlCommand\n");
        for (ExecutionRunModel.FeatureResult feature : model.features) {
            for (ExecutionRunModel.ScenarioResult scenario : feature.scenarios) {
                scenarios.append(csv(feature.name)).append(',').append(csv(scenario.name)).append(',')
                        .append(csv(scenario.status)).append(',').append(scenario.durationMs).append(',')
                        .append(csv(String.join(" ", scenario.tags))).append('\n');
                for (ExecutionRunModel.StepResult step : scenario.steps) {
                    steps.append(csv(feature.name)).append(',').append(csv(scenario.name)).append(',')
                            .append(csv(step.keyword + step.text)).append(',').append(csv(step.status)).append(',')
                            .append(step.durationMs).append(',').append(csv(step.failureCategory)).append(',')
                            .append(csv(step.error)).append('\n');
                }
            }
        }
        for (ExecutionRunModel.FailureRecord failure : model.failures) {
            failures.append(csv(failure.level)).append(',').append(csv(failure.feature)).append(',')
                    .append(csv(failure.scenario)).append(',').append(csv(failure.step)).append(',')
                    .append(csv(failure.category)).append(',').append(csv(failure.exceptionType)).append(',')
                    .append(csv(failure.executionMode)).append(',').append(csv(failure.pageUrl)).append(',')
                    .append(csv(failure.screenshot)).append(',').append(csv(failure.message)).append('\n');
        }
        for (HttpErrorCapture.HttpError error : model.httpErrors) {
            String cls = error.status >= 500 ? "5xx" : "4xx";
            apis.append(error.status).append(',').append(cls).append(',').append(csv(error.method)).append(',')
                    .append(csv(error.url)).append(',').append(csv(error.resourceType)).append(',')
                    .append(error.responseTimeMs).append(',').append(csv(error.body)).append(',')
                    .append(csv(error.curlCommand)).append('\n');
        }
        Files.write(dir.resolve("scenarios.csv"), scenarios.toString().getBytes(StandardCharsets.UTF_8));
        Files.write(dir.resolve("steps.csv"), steps.toString().getBytes(StandardCharsets.UTF_8));
        Files.write(dir.resolve("failures.csv"), failures.toString().getBytes(StandardCharsets.UTF_8));
        Files.write(dir.resolve("api-errors.csv"), apis.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String csv(String value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
