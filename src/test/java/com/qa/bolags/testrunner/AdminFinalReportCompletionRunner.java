package com.qa.bolags.testrunner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
        features = {"src/test/resources/features/adminFinalReportCompletionFlow.feature"},
        glue = {"com.qa.bolags.stepdefs"},
        plugin = {"pretty",
                "com.qa.bolags.reporting.ExecutionReportPlugin",
                "html:target/HtmlReports/admin-final-report-completion-report.html",
                "json:target/JSONReports/admin-final-report-completion-report.json",
                "junit:target/JUnitReports/admin-final-report-completion-report.xml"},
        monochrome = true
)
public class AdminFinalReportCompletionRunner extends AbstractTestNGCucumberTests {
}
