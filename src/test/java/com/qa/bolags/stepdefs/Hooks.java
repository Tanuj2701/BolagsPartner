package com.qa.bolags.stepdefs;

import com.qa.bolags.baseTest.BaseTest;
import com.qa.bolags.constants.AcceptOfferContext;
import com.qa.bolags.constants.LiquidationOrderIdContext;
import com.qa.bolags.constants.OfferSentDataContext;
import com.qa.bolags.utility.SaveInitialOrderCapture;
import com.qa.bolags.utility.SendOfferResponseCapture;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

/**
 * Central browser lifecycle so admin features do not inherit the liquidation landing URL.
 */
public class Hooks extends BaseTest {

    @Before(order = 0)
    public void startBrowserForScenario(Scenario scenario) {
        String uri = scenario.getUri().toString();
        if (uri.contains("liquidationflow") || uri.contains("e2eLiquidationToAdminOrder")) {
            LiquidationOrderIdContext.clear();
            AcceptOfferContext.clear();
            OfferSentDataContext.clear();
            SaveInitialOrderCapture.resetForNewBrowserSession();
            SendOfferResponseCapture.resetForNewBrowserSession();
            initializeDriverForLiquidationWithSaveInitialCapture();
        } else if (uri.contains("adminflow") || uri.contains("shiroResellerUsers")) {
            OfferSentDataContext.clear();
            initializeChromeWithoutDefaultNavigation();
        } else {
            if (BaseTest.driver == null) {
                initializeDriver();
            }
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
