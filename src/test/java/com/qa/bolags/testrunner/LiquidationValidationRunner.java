package com.qa.bolags.testrunner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
        features = {"src/test/resources/features/liquidationFormValidation.feature"},
        glue = {"com.qa.bolags.stepdefs"},
        plugin = {"pretty",
                "com.qa.bolags.reporting.ExecutionReportPlugin",
                "html:target/HtmlReports/liquidation-validation-report.html",
                "json:target/JSONReports/liquidation-validation-report.json",
                "junit:target/JUnitReports/liquidation-validation-report.xml"},
        monochrome = true
)
public class LiquidationValidationRunner extends AbstractTestNGCucumberTests {
}
