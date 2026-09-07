package com.qa.bolags.testrunner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
        features = {
                "src/test/resources/features/adminLoginValidation.feature",
                "src/test/resources/features/adminOrderListSmoke.feature"
        },
        glue = {"com.qa.bolags.stepdefs"},
        plugin = {"pretty",
                "html:target/HtmlReports/admin-smoke-report.html",
                "json:target/JSONReports/admin-smoke-report.json",
                "junit:target/JUnitReports/admin-smoke-report.xml"},
        monochrome = true
)
public class AdminSmokeTestRunner extends AbstractTestNGCucumberTests {
}
