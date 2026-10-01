package com.qa.bolags.testrunner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
        // Default suite (after E2E test in testng.xml): shiro and other non–liquidation-admin E2E features.
        features = {"src/test/resources/features/shiroResellerUsers.feature"},
    glue = {"com.qa.bolags.stepdefs"},
    plugin = {"pretty",
                "com.qa.bolags.reporting.ExecutionReportPlugin", "html:target/HtmlReports/report.html",
            "json:target/JSONReports/report.json",
            "junit:target/JUnitReports/report.xml"},
    monochrome = true
)
public class CucumberTestRunner extends AbstractTestNGCucumberTests {
}

