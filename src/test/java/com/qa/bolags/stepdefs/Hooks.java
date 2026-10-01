package com.qa.bolags.stepdefs;

import com.qa.bolags.baseTest.BaseTest;
import com.qa.bolags.pages.AdminOrderWorkflowPage;
import com.qa.bolags.constants.AcceptOfferContext;
import com.qa.bolags.constants.LiquidationOrderIdContext;
import com.qa.bolags.constants.OfferSentDataContext;
import com.qa.bolags.utility.SaveInitialOrderCapture;
import com.qa.bolags.utility.SendOfferResponseCapture;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Central browser lifecycle so admin features do not inherit the liquidation landing URL.
 */
public class Hooks extends BaseTest {

    @Before(order = 0)
    public void startBrowserForScenario(Scenario scenario) {
        String uri = scenario.getUri().toString();
        if (uri.contains("liquidationflow") || uri.contains("liquidationFormValidation")
                || uri.contains("e2eLiquidationToAdminOrder") || uri.contains("declineOfferFlow")
                || uri.contains("clientDocumentChecklistFlow") || uri.contains("adminDocumentReviewWizard")
                || uri.contains("adminPaymentWorkflow") || uri.contains("adminBoardChangeFusionFlow")
                || uri.contains("adminBolagsverketSubmissionFlow") || uri.contains("adminFinalReportCompletionFlow")
                || uri.contains("adminflow") || uri.contains("timeOptimizedUniqueStepsFlow")) {
            LiquidationOrderIdContext.clear();
            com.qa.bolags.constants.OrderOrganizationNumberContext.clear();
            AcceptOfferContext.clear();
            OfferSentDataContext.clear();
            AdminOrderWorkflowPage.resetWorkflowSessionState();
            com.qa.bolags.constants.ClientDocumentTokenContext.clear();
            SaveInitialOrderCapture.resetForNewBrowserSession();
            SendOfferResponseCapture.resetForNewBrowserSession();
            com.qa.bolags.utility.OrderDetailsCapture.resetForNewBrowserSession();
            initializeDriverForLiquidationWithSaveInitialCapture();
        } else if (uri.contains("shiroResellerUsers") || uri.contains("adminLoginValidation")
                || uri.contains("adminOrderListSmoke")) {
            OfferSentDataContext.clear();
            initializeChromeWithoutDefaultNavigation();
        } else {
            if (BaseTest.driver == null) {
                initializeDriver();
            }
        }
    }

    @After(order = 50)
    public void captureFailureArtifacts(Scenario scenario) {
        com.qa.bolags.reporting.HttpErrorCapture.drain(BaseTest.driver);
        if (scenario == null || !scenario.isFailed() || BaseTest.driver == null) {
            return;
        }
        try {
            Path screenshot = com.qa.bolags.reporting.ReportSession.captureFailureScreenshot(BaseTest.driver);
            byte[] png = screenshot == null
                    ? ((TakesScreenshot) BaseTest.driver).getScreenshotAs(OutputType.BYTES)
                    : Files.readAllBytes(screenshot);
            scenario.attach(png, "image/png", "failure-screenshot");
            log.info("Attached failure screenshot to scenario '{}'", scenario.getName());
        } catch (Exception e) {
            log.warn("Unable to attach failure screenshot: {}", e.getMessage());
        }
    }

    @After(order = 100)
    public void stopBrowser() {
        if (BaseTest.driver != null) {
            try {
                BaseTest.driver.quit();
                log.info("Browser closed after scenario.");
            } catch (Exception e) {
                log.warn("Error while closing browser: {}", e.getMessage());
            } finally {
                BaseTest.driver = null;
            }
        }
    }
}
