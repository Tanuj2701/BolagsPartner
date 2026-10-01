package com.qa.bolags.testrunner;

import org.testng.annotations.BeforeSuite;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

/**
 * Runs every feature under {@code src/test/resources/features} in Chrome headless mode.
 * <p>
 * Use this to validate flow status and API capture (saveInitial / sendOffer / order details)
 * faster than a headed run. Network logging stays enabled via existing browser hooks.
 * <pre>
 *   mvn test -Dtest=AllFeaturesHeadlessRunner
 *   mvn test -Pheadless
 *   mvn test -Dtest=AllFeaturesHeadlessRunner -Dcucumber.filter.tags="@smoke"
 * </pre>
 */
@CucumberOptions(
        features = {"src/test/resources/features"},
        glue = {"com.qa.bolags.stepdefs"},
        tags = "@regression",
        plugin = {"pretty",
                "com.qa.bolags.reporting.ExecutionReportPlugin",
                "html:target/HtmlReports/all-features-headless-report.html",
                "json:target/JSONReports/all-features-headless-report.json",
                "junit:target/JUnitReports/all-features-headless-report.xml"},
        monochrome = true
)
public class AllFeaturesHeadlessRunner extends AbstractTestNGCucumberTests {

    @BeforeSuite(alwaysRun = true)
    public void enableHeadlessChrome() {
        System.setProperty("headless", "true");
        System.setProperty("chrome.headless", "true");
    }
}
