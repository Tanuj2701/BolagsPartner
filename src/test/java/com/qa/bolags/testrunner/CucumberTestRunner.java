package com.qa.bolags.testrunner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
    features = "src/test/resources/features/liquidationflow.feature",
    glue = {"com.qa.bolags.stepdefs"},
    plugin = {"pretty", "html:target/HtmlReports/report.html",
            "json:target/JSONReports/report.json",
            "junit:target/JUnitReports/report.xml"},
    monochrome = true
)
public class CucumberTestRunner extends AbstractTestNGCucumberTests {
}

