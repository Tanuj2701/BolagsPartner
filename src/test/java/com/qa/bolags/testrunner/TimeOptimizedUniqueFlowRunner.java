package com.qa.bolags.testrunner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

/**
 * Single-scenario unique-steps flow in one Chrome session.
 * <p>
 * {@code mvn test -Ptimeopt}
 * {@code mvn test -Ptimeopt -Dheadless=true}
 */
@CucumberOptions(
        features = {"src/test/resources/features/timeOptimizedUniqueStepsFlow.feature"},
        glue = {"com.qa.bolags.stepdefs"},
        tags = "@time-optimized",
        plugin = {"pretty",
                "com.qa.bolags.reporting.ExecutionReportPlugin",
                "html:target/HtmlReports/time-optimized-unique-steps-report.html",
                "json:target/JSONReports/time-optimized-unique-steps-report.json",
                "junit:target/JUnitReports/time-optimized-unique-steps-report.xml"},
        monochrome = true
)
public class TimeOptimizedUniqueFlowRunner extends AbstractTestNGCucumberTests {
}
