package com.qa.bolags.utility;

import com.qa.bolags.constants.LiquidationOrderIdContext;
import com.qa.bolags.pages.liquidationpage;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;

/**
 * Minimal liquidation flow to capture a {@code saveInitial} order id when none exists in context.
 */
public final class LiquidationOrderBootstrap {

    private LiquidationOrderBootstrap() {
    }

    public static void ensureSaveInitialOrderIdCaptured(WebDriver driver) {
        LiquidationOrderIdContext.tryRestoreFromPersistedFile();
        if (LiquidationOrderIdContext.getCapturedOrderIdOrNull() != null) {
            return;
        }

        String workflowOrderId = System.getProperty("workflow.orderId", "").trim();
        if (!workflowOrderId.isEmpty()) {
            LiquidationOrderIdContext.setCapturedOrderId(workflowOrderId);
            return;
        }

        if (!System.getProperty("adminOrderDetailUrl", "").trim().isEmpty()
                || !System.getProperty("adminOrderDetailPath", "").trim().isEmpty()) {
            return;
        }

        liquidationpage page = new liquidationpage(driver);
        page.logintoapplication();
        Assert.assertTrue(page.isOfferPageDisplayed(), "Offer page not visible during order bootstrap");
        page.searchCompany();
        page.enterFornamn("Bootstrap");
        page.enterEfternamn("Order");
        page.enterEpost("bootstrap.order@test.bolagspartner.se");
        page.agreeToTermsAndContinue();
        page.clickonGoOn();
        SaveInitialOrderCapture.pollAndStoreOrderIdFromChrome(driver, 15);
        page.uploadDocument("src/main/ABC.pdf");
        if (LiquidationOrderIdContext.getCapturedOrderIdOrNull() == null) {
            SaveInitialOrderCapture.pollAndStoreOrderIdFromChrome(driver, 30);
        }
        page.clickonGoOn();
        page.enterAddressDetails("Bootstrapgatan 1", "11122", "Stockholm", "0701234567", "0709876543",
                "Bootstrap AB", "Minimal order bootstrap for admin tests");
        page.userClickOnSave();
        Assert.assertTrue(page.isRequestReceivedPageDisplayed(), "Request received page not visible after bootstrap");
        if (LiquidationOrderIdContext.getCapturedOrderIdOrNull() == null) {
            SaveInitialOrderCapture.pollAndStoreOrderIdFromChrome(driver, 30);
        }
        Assert.assertNotNull(LiquidationOrderIdContext.getCapturedOrderIdOrNull(),
                "Could not capture saveInitial orderId during bootstrap");
    }
}
