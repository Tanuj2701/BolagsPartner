package com.qa.bolags.testrunner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

/**
 * Accept-offer shareholder variant scenarios ({@code acceptOfferShareholderVariants.feature}).
 */
@CucumberOptions(
        features = {"src/test/resources/features/acceptOfferShareholderVariants.feature"},
        glue = {"com.qa.bolags.stepdefs"},
        plugin = {"pretty",
                "html:target/HtmlReports/accept-offer-shareholder-variants-report.html",
                "json:target/JSONReports/accept-offer-shareholder-variants-report.json",
                "junit:target/JUnitReports/accept-offer-shareholder-variants-report.xml"},
        monochrome = true
)
public class AcceptOfferShareholderVariantsRunner extends AbstractTestNGCucumberTests {
}
