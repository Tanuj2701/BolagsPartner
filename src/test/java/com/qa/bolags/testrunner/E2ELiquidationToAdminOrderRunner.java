package com.qa.bolags.testrunner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

/**
 * TestNG entry for the liquidation → admin end-to-end feature (see {@code testng.xml} test {@code E2E_Liquidation_To_Admin_Order}).
 */
@CucumberOptions(
        features = {"src/test/resources/features/e2eLiquidationToAdminOrder.feature"},
        glue = {"com.qa.bolags.stepdefs"},
        plugin = {"pretty",
                "com.qa.bolags.reporting.ExecutionReportPlugin",
                "html:target/HtmlReports/e2e-liquidation-admin-report.html",
                "json:target/JSONReports/e2e-liquidation-admin-report.json",
                "junit:target/JUnitReports/e2e-liquidation-admin-report.xml"},
        monochrome = true
)
public class E2ELiquidationToAdminOrderRunner extends AbstractTestNGCucumberTests {
}
