package com.qa.bolags.testrunner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

/**
 * Send-agreement shareholder variant scenarios ({@code sendAgreementShareholderVariants.feature}).
 */
@CucumberOptions(
        features = {"src/test/resources/features/sendAgreementShareholderVariants.feature"},
        glue = {"com.qa.bolags.stepdefs"},
        plugin = {"pretty",
                "html:target/HtmlReports/send-agreement-shareholder-variants-report.html",
                "json:target/JSONReports/send-agreement-shareholder-variants-report.json",
                "junit:target/JUnitReports/send-agreement-shareholder-variants-report.xml"},
        monochrome = true
)
public class SendAgreementShareholderVariantsRunner extends AbstractTestNGCucumberTests {
}
