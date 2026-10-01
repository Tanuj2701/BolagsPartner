package com.qa.bolags.testrunner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
        features = {"src/test/resources/features/adminLoginValidation.feature"},
        glue = {"com.qa.bolags.stepdefs"},
        plugin = {"pretty",
                "com.qa.bolags.reporting.ExecutionReportPlugin",
                "html:target/HtmlReports/admin-login-validation-report.html",
                "json:target/JSONReports/admin-login-validation-report.json",
                "junit:target/JUnitReports/admin-login-validation-report.xml"},
        monochrome = true
)
public class AdminLoginValidationRunner extends AbstractTestNGCucumberTests {
}
