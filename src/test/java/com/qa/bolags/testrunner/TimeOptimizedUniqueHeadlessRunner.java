package com.qa.bolags.testrunner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.BeforeSuite;

/**
 * Runs the unique-steps feature in a single headless Chrome session.
 * Run with {@code mvn test -Ptimeopt-headless}.
 */
@CucumberOptions(
        features = {"src/test/resources/features/timeOptimizedUniqueStepsFlow.feature"},
        glue = {"com.qa.bolags.stepdefs"},
        tags = "@time-optimized",
        plugin = {"pretty",
                "com.qa.bolags.reporting.ExecutionReportPlugin",
                "html:target/HtmlReports/time-optimized-unique-steps-headless-report.html",
                "json:target/JSONReports/time-optimized-unique-steps-headless-report.json",
                "junit:target/JUnitReports/time-optimized-unique-steps-headless-report.xml"},
        monochrome = true
)
public class TimeOptimizedUniqueHeadlessRunner extends AbstractTestNGCucumberTests {

    @BeforeSuite(alwaysRun = true)
    public void enableHeadlessChrome() {
        System.setProperty("headless", "true");
        System.setProperty("chrome.headless", "true");
    }
}
