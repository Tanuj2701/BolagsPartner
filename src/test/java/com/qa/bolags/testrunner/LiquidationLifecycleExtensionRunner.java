package com.qa.bolags.testrunner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
        features = {
                "src/test/resources/features/declineOfferFlow.feature",
                "src/test/resources/features/clientDocumentChecklistFlow.feature",
                "src/test/resources/features/adminDocumentReviewWizard.feature",
                "src/test/resources/features/adminPaymentWorkflow.feature",
                "src/test/resources/features/adminBoardChangeFusionFlow.feature",
                "src/test/resources/features/adminBolagsverketSubmissionFlow.feature",
                "src/test/resources/features/adminFinalReportCompletionFlow.feature"
        },
        glue = {"com.qa.bolags.stepdefs"},
        plugin = {"pretty",
                "html:target/HtmlReports/lifecycle-extension-report.html",
                "json:target/JSONReports/lifecycle-extension-report.json",
                "junit:target/JUnitReports/lifecycle-extension-report.xml"},
        monochrome = true
)
public class LiquidationLifecycleExtensionRunner extends AbstractTestNGCucumberTests {
}
