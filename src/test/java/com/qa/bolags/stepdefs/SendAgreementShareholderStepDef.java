package com.qa.bolags.stepdefs;

import com.qa.bolags.baseTest.BaseTest;
import com.qa.bolags.constants.AcceptOfferContext;
import com.qa.bolags.constants.LiquidationOrderIdContext;
import com.qa.bolags.pages.AdminPage;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;

/**
 * Send-agreement shareholder variant steps — extends accept-offer shareholder flow
 * ({@code sendAgreementShareholderVariants.feature}) with admin Send Agreement
 * ({@code AcceptedByCustomer.tsx} → POST {@code /v1/companyLiquidationOrders/send-agreement}).
 */
public class SendAgreementShareholderStepDef extends BaseTest {

    private AdminPage adminPage;

    @Before(order = 1)
    public void initSendAgreementShareholderPages(Scenario scenario) {
        if (scenario.getUri().toString().contains("sendAgreementShareholderVariants")) {
            adminPage = new AdminPage(BaseTest.driver);
        }
    }

    /**
     * Admin returns to Manage order (LIQ_CLIENT_ACCEPT_COMPLETE), confirms shareholders updated,
     * selects required documents, and submits Send Agreement.
     */
    @When("admin sends agreement for shareholder variant order")
    public void adminSendsAgreementForShareholderVariantOrder() {
        String orderId = LiquidationOrderIdContext.getCapturedOrderIdOrNull();
        Assert.assertNotNull(orderId, "Liquidation order id must be in test context before send agreement");
        Assert.assertNotNull(
                AcceptOfferContext.getOrderIdOrNull(),
                "Accept-offer order id must be captured before send agreement");

        adminPage.openGenericOrderDetail(adminPage.getResolvedGenericOrderDetailUrl());
        adminPage.checkShareholdersUpdatedOnManageOrder();
        adminPage.openSendAgreementDocumentModal();
        adminPage.clickSendAgreementInDocumentModal();
    }

    @Then("send agreement shareholder variant flow should complete successfully")
    public void sendAgreementShareholderVariantFlowShouldCompleteSuccessfully() {
        Assert.assertTrue(
                adminPage.isAgreementSentSuccessfully(),
                "Expected Manage order Waiting for signature UI after Send agreement "
                        + "(order id: " + LiquidationOrderIdContext.getCapturedOrderIdOrNull() + ")");
    }
}
