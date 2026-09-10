package com.qa.bolags.testrunner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

/**
 * TestNG entry for Bolagsverket company update + send offer flow.
 */
@CucumberOptions(
        features = {"src/test/resources/features/adminBolagsverketCompanyUpdateFlow.feature"},
        glue = {"com.qa.bolags.stepdefs"},
        plugin = {"pretty",
                "html:target/HtmlReports/admin-bolagsverket-company-update-report.html",
                "json:target/JSONReports/admin-bolagsverket-company-update-report.json",
                "junit:target/JUnitReports/admin-bolagsverket-company-update-report.xml"},
        monochrome = true
)
public class AdminBolagsverketCompanyUpdateRunner extends AbstractTestNGCucumberTests {
}
