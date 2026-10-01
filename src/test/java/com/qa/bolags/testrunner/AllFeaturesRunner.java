package com.qa.bolags.testrunner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

/**
 * Runs every feature under {@code src/test/resources/features} filtered by tags.
 * <p>
 * Default tag expression is {@code @regression} (applied on all feature files).
 * Override from Maven:
 * <pre>
 *   mvn test -Dtest=AllFeaturesRunner -Dcucumber.filter.tags="@smoke"
 *   mvn test -Dtest=AllFeaturesRunner -Dcucumber.filter.tags="@e2e and not @negative"
 *   mvn test -Dtest=AllFeaturesRunner -Dcucumber.filter.tags="@regression"
 * </pre>
 */
@CucumberOptions(
        features = {"src/test/resources/features"},
        glue = {"com.qa.bolags.stepdefs"},
        tags = "@regression",
        plugin = {"pretty",
                "com.qa.bolags.reporting.ExecutionReportPlugin",
                "html:target/HtmlReports/all-features-report.html",
                "json:target/JSONReports/all-features-report.json",
                "junit:target/JUnitReports/all-features-report.xml"},
        monochrome = true
)
public class AllFeaturesRunner extends AbstractTestNGCucumberTests {
}
