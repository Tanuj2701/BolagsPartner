package com.qa.bolags.stepdefs;

import com.qa.bolags.baseTest.BaseTest;
import com.qa.bolags.constants.AcceptOfferContext;
import com.qa.bolags.constants.ClientDocumentTokenContext;
import com.qa.bolags.constants.LiquidationOrderIdContext;
import com.qa.bolags.constants.OfferSentDataContext;
import com.qa.bolags.pages.AcceptOfferPage;
import com.qa.bolags.pages.AdminPage;
import com.qa.bolags.pages.liquidationpage;
import com.qa.bolags.utility.OrderDetailsCapture;
import com.qa.bolags.utility.SaveInitialOrderCapture;
import com.qa.bolags.utility.SendOfferResponseCapture;
import com.qa.bolags.utility.TestUtil;
import io.cucumber.java.en.Given;
import org.testng.Assert;

/**
 * Shared E2E setup through agreement-sent state for lifecycle extension features.
 */
public class LifecycleSetupStepDef extends BaseTest {

    @Given("the liquidation order is prepared through agreement sent state")
    public void theLiquidationOrderIsPreparedThroughAgreementSentState() {
        String skip = System.getProperty("skipLifecycleSetup", "false");
        if ("true".equalsIgnoreCase(skip)) {
            log.info("Skipping lifecycle setup — using -Dworkflow.orderId / stored context");
            Assert.assertNotNull(LiquidationOrderIdContext.getCapturedOrderIdOrNull(),
                    "Set -Dworkflow.orderId or run without skipLifecycleSetup");
            return;
        }

        liquidationpage liqPage = new liquidationpage(BaseTest.driver);
        AdminPage adminPage = new AdminPage(BaseTest.driver);
        AcceptOfferPage acceptPage = new AcceptOfferPage(BaseTest.driver);

        liqPage.logintoapplication();
        Assert.assertTrue(liqPage.isOfferPageDisplayed(), "Offer page not visible");
        liqPage.searchCompany();
        liqPage.enterFornamn("Tanuj");
        liqPage.enterEfternamn("Rasane");
        liqPage.enterEpost("tanuj.lifecycle@test.bolagspartner.se");
        liqPage.agreeToTermsAndContinue();
        liqPage.clickonGoOn();
        liqPage.uploadDocument("src/main/ABC.pdf");
        SaveInitialOrderCapture.pollAndStoreOrderIdFromChrome(BaseTest.driver, 30);
        if (LiquidationOrderIdContext.getCapturedOrderIdOrNull() == null) {
            SaveInitialOrderCapture.pollAndStoreOrderIdFromChrome(BaseTest.driver, 30);
        }
        liqPage.clickonGoOn();
        liqPage.enterAddressDetails("Testgatan 1", "11122", "Stockholm", "0701234567", "0709876543",
                "Lifecycle Test AB", "Automation lifecycle setup");
        liqPage.userClickOnSave();
        Assert.assertTrue(liqPage.isRequestReceivedPageDisplayed(), "Request received page not visible");
        Assert.assertNotNull(LiquidationOrderIdContext.getCapturedOrderIdOrNull(), "saveInitial orderId not captured");
        TestUtil.waitForSpecifiedTime(3);

        adminPage.loginToAdminPortal();
        adminPage.openGenericOrderDetail(adminPage.getResolvedGenericOrderDetailUrl());
        Assert.assertTrue(adminPage.isRequestDetailsVisible(), "Admin order detail not visible");
        adminPage.enterOfferSentAccountingData();
        adminPage.clickSendQuoteOnManageOrder();
        SendOfferResponseCapture.pollAndStoreFromSendOfferChrome(BaseTest.driver, 60);
        Assert.assertNotNull(AcceptOfferContext.getMd5TokenOrNull(), "sendOffer md5Token not captured");

        adminPage.openAcceptOfferFromCapturedSendOfferResponse();
        Assert.assertTrue(adminPage.isAcceptOfferPageDisplayed(), "Accept offer page not visible");
        OrderDetailsCapture.pollAndStoreUploadDocumentToken(BaseTest.driver, 45);
        acceptPage.addDefaultSwedishPersonShareholder();
        acceptPage.selectPaperSignatureAndComplete();
        if (ClientDocumentTokenContext.getUploadDocumentTokenOrNull() == null) {
            OrderDetailsCapture.pollAndStoreUploadDocumentToken(BaseTest.driver, 30);
        }

        adminPage.openGenericOrderDetail(adminPage.getResolvedGenericOrderDetailUrl());
        adminPage.checkShareholdersUpdatedOnManageOrder();
        adminPage.openSendAgreementDocumentModal();
        adminPage.clickSendAgreementInDocumentModal();
        Assert.assertTrue(adminPage.isAgreementSentSuccessfully(), "Agreement not sent successfully");

        OfferSentDataContext.clear();
        log.info("Lifecycle setup complete — orderId={}", LiquidationOrderIdContext.getCapturedOrderIdOrNull());
    }
}
