package com.qa.bolags.stepdefs;

import com.qa.bolags.baseTest.BaseTest;
import com.qa.bolags.constants.AcceptOfferContext;
import com.qa.bolags.pages.AdminPage;
import com.qa.bolags.utility.SendOfferResponseCapture;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;

/**
 * Steps for {@code adminBolagsverketCompanyUpdateFlow.feature} — sync company from Bolagsverket before send offer.
 */
public class AdminBolagsverketUpdateStepDef extends BaseTest {

    private AdminPage adminPage;

    @Before(order = 1)
    public void initAdminBolagsverketPage(Scenario scenario) {
        String uri = scenario.getUri().toString();
        if (uri.contains("adminBolagsverketCompanyUpdateFlow")) {
            adminPage = new AdminPage(BaseTest.driver);
        }
    }

    @When("admin syncs company from Bolagsverket if required on order detail")
    public void adminSyncsCompanyFromBolagsverketIfRequiredOnOrderDetail() {
        adminPage.updateCompanyFromBolagsverketOnOrderDetail();
    }

    @When("admin opens Bolagsverket update modal from order warning if required")
    public void adminOpensBolagsverketUpdateModalFromOrderWarningIfRequired() {
        if (adminPage.isBolagsverketCompanyUpdateRequired()) {
            adminPage.openBolagsverketUpdateModalFromWarning();
            adminPage.assertBolagsverketUpdateModalDisplayed();
        }
    }

    @And("admin confirms Bolagsverket company update in modal if required")
    public void adminConfirmsBolagsverketCompanyUpdateInModalIfRequired() {
        if (adminPage.isBolagsverketUpdateModalOpen()
                || adminPage.isBolagsverketCompanyUpdateRequired()) {
            adminPage.confirmBolagsverketCompanyUpdateInModal();
            adminPage.openGenericOrderDetail(adminPage.getResolvedGenericOrderDetailUrl());
        }
    }

    @Then("the Bolagsverket company update should be applied successfully")
    public void theBolagsverketCompanyUpdateShouldBeAppliedSuccessfully() {
        if (adminPage.isBolagsverketCompanyUpdateRequired()) {
            adminPage.updateCompanyFromBolagsverketOnOrderDetail();
        }
        adminPage.assertBolagsverketCompanyUpdateApplied();
    }

    @When("user sends offer quote with Bolagsverket sync if required")
    public void userSendsOfferQuoteWithBolagsverketSyncIfRequired() {
        adminPage.sendOfferWithBolagsverketSyncIfRequired();
        SendOfferResponseCapture.pollAndStoreFromSendOfferChrome(BaseTest.driver, 60);
    }

    @Then("send offer should complete successfully with captured token")
    public void sendOfferShouldCompleteSuccessfullyWithCapturedToken() {
        Assert.assertNotNull(
                AcceptOfferContext.getMd5TokenOrNull(),
                "Expected PUT .../companyLiquidationOrders/{id}/sendOffer JSON to contain md5Token. "
                        + "Cumulative network request map size was: "
                        + SendOfferResponseCapture.debugCumulativeRequestMapSize());
        Assert.assertNotNull(
                AcceptOfferContext.getOrderIdOrNull(),
                "Expected PUT .../companyLiquidationOrders/{id}/sendOffer JSON to contain orderId.");
    }
}
