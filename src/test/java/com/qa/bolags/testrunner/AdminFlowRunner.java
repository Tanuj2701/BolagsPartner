package com.qa.bolags.testrunner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
        features = {"src/test/resources/features/adminflow.feature"},
        glue = {"com.qa.bolags.stepdefs"},
        plugin = {"pretty",
                "com.qa.bolags.reporting.ExecutionReportPlugin",
                "html:target/HtmlReports/adminflow-report.html",
                "json:target/JSONReports/adminflow-report.json",
                "junit:target/JUnitReports/adminflow-report.xml"},
        monochrome = true
)
public class AdminFlowRunner extends AbstractTestNGCucumberTests {
}
