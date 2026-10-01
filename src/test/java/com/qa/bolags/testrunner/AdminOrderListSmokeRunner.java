package com.qa.bolags.testrunner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
        features = {"src/test/resources/features/adminOrderListSmoke.feature"},
        glue = {"com.qa.bolags.stepdefs"},
        plugin = {"pretty",
                "com.qa.bolags.reporting.ExecutionReportPlugin",
                "html:target/HtmlReports/admin-order-list-smoke-report.html",
                "json:target/JSONReports/admin-order-list-smoke-report.json",
                "junit:target/JUnitReports/admin-order-list-smoke-report.xml"},
        monochrome = true
)
public class AdminOrderListSmokeRunner extends AbstractTestNGCucumberTests {
}
