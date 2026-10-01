package com.qa.bolags.stepdefs;

import com.qa.bolags.baseTest.BaseTest;
import com.qa.bolags.constants.AcceptOfferContext;
import com.qa.bolags.pages.AcceptOfferPage;
import com.qa.bolags.pages.AdminPage;
import com.qa.bolags.utility.SendOfferResponseCapture;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;

/**
 * Admin portal steps (see {@code adminflow.feature}).
 */
public class adminStepDefe extends BaseTest {

    private AdminPage adminPage;
    private AcceptOfferPage acceptOfferPage;

    @Before(order = 1)
    public void initAdminPage(Scenario scenario) {
        String uri = scenario.getUri().toString();
        if (uri.contains("adminflow") || uri.contains("e2eLiquidationToAdminOrder")
                || uri.contains("adminLoginValidation") || uri.contains("adminOrderListSmoke")
                || uri.contains("declineOfferFlow") || uri.contains("clientDocumentChecklistFlow")
                || uri.contains("adminDocumentReviewWizard") || uri.contains("adminPaymentWorkflow")
                || uri.contains("adminBoardChangeFusionFlow") || uri.contains("adminBolagsverketSubmissionFlow")
                || uri.contains("adminFinalReportCompletionFlow")
                || uri.contains("timeOptimizedUniqueStepsFlow")) {
            adminPage = new AdminPage(BaseTest.driver);
        }
        if (uri.contains("e2eLiquidationToAdminOrder") || uri.contains("clientDocumentChecklistFlow")
                || uri.contains("timeOptimizedUniqueStepsFlow")) {
            acceptOfferPage = new AcceptOfferPage(BaseTest.driver);
        }
    }

    @Given("User opens admin login page")
    public void userOpensAdminLoginPage() {
        adminPage.openAdminPortal();
    }

    @Then("User clicks on LOGGA IN link from top navigation")
    public void userClicksOnLoggaInLinkFromTopNavigation() {
        adminPage.clickLoggaInFromTopNavigation();
    }

    @Then("User logs in with valid admin credentials")
    public void userLogsInWithValidAdminCredentials() {
        adminPage.enterAdminCredentials();
    }

    @Then("User Click on Login Button")
    public void userClickOnLoginButton() {
        adminPage.clickLoginButton();
    }

    @Then("User should land on admin dashboard")
    public void userShouldLandOnAdminDashboard() {
        Assert.assertTrue(adminPage.isDashboardVisible(), "Admin dashboard is not visible");
    }

    @When("User clicks on the first order in the orders list")
    public void userClicksOnTheFirstOrderInTheOrdersList() {
        adminPage.clickFirstOrderInList();
    }

    @Then("User should see the order detail page")
    public void userShouldSeeTheOrderDetailPage() {
        Assert.assertTrue(adminPage.isRequestDetailsVisible(), "Order detail page is not visible");
    }

    @Then("user enter the data for offer sent")
    public void userEnterTheDataForOfferSent() {
        adminPage.enterOfferSentAccountingData();
    }

    @When("user clicks send quote")
    public void userClicksSendQuote() {
        adminPage.clickSendQuoteOnManageOrder();
        SendOfferResponseCapture.pollAndStoreFromSendOfferChrome(BaseTest.driver, 45);
    }

    @When("user sends the offer quote")
    public void userSendsTheOfferQuote() {
        adminPage.clickSendQuoteOnManageOrder();
        SendOfferResponseCapture.pollAndStoreFromSendOfferChrome(BaseTest.driver, 45);
    }

    @Then("user Accept the offer")
    public void userAcceptTheOffer() {
        if (AcceptOfferContext.getMd5TokenOrNull() == null) {
            SendOfferResponseCapture.pollAndStoreFromSendOfferChrome(BaseTest.driver, 15);
        }
        Assert.assertNotNull(
                AcceptOfferContext.getMd5TokenOrNull(),
                "Expected PUT .../companyLiquidationOrders/{id}/sendOffer JSON to contain md5Token. "
                        + "Cumulative network request map size was: "
                        + SendOfferResponseCapture.debugCumulativeRequestMapSize());
        Assert.assertNotNull(
                AcceptOfferContext.getOrderIdOrNull(),
                "Expected PUT .../companyLiquidationOrders/{id}/sendOffer JSON to contain orderId.");
        adminPage.openAcceptOfferFromCapturedSendOfferResponse();
        Assert.assertTrue(adminPage.isAcceptOfferPageDisplayed(), "Accept offer page is not visible");
    }

    @And("user add the shareholder")
    public void userAddTheShareholder() {
        acceptOfferPage.addDefaultSwedishPersonShareholder();
    }

    @And("user select the Signing method")
    public void userSelectTheSigningMethod() {
        acceptOfferPage.selectPaperSignatureAndComplete();
    }

    @And("user checks the shareholders updated checkbox on manage order")
    public void userChecksTheShareholdersUpdatedCheckboxOnManageOrder() {
        adminPage.checkShareholdersUpdatedOnManageOrder();
    }

    @And("admin adds a former representative on the order if the form is shown")
    public void adminAddsAFormerRepresentativeOnTheOrderIfTheFormIsShown() {
        adminPage.addFormerRepresentativeIfFormShown();
    }

    @When("user opens the send agreement document modal")
    public void userOpensTheSendAgreementDocumentModal() {
        adminPage.openSendAgreementDocumentModal();
    }

    @And("user clicks send agreement in the document modal")
    public void userClicksSendAgreementInTheDocumentModal() {
        adminPage.clickSendAgreementInDocumentModal();
    }

    @When("user sends the agreement for the accepted order")
    public void userSendsTheAgreementForTheAcceptedOrder() {
        adminPage.sendAgreementForAcceptedOrder();
    }

    @Then("the agreement should be sent successfully")
    public void theAgreementShouldBeSentSuccessfully() {
        Assert.assertTrue(
                adminPage.isAgreementSentSuccessfully(),
                "Expected Manage order to show Waiting for signature UI after Send agreement "
                        + "(order id: " + AcceptOfferContext.getOrderIdOrNull() + ")");
    }

    /* Legacy / alternate wording kept for older feature files */
    @Given("Admin login with a valid credentials")
    public void adminLoginWithAValidCredentials() {
        adminPage.openAdminPortal();
        adminPage.loginWithConfiguredCredentials();
    }

    @Then("Admin should land on Dashboard page")
    public void adminShouldLandOnDashboardPage() {
        Assert.assertTrue(adminPage.isDashboardVisible(), "Admin dashboard is not visible");
    }

    @Then("Admin click on Requests")
    public void adminClickOnRequests() {
        adminPage.openRequestsMenu();
    }

    @Then("Admin click on Liquidation Requests")
    public void adminClickOnLiquidationRequests() {
        adminPage.openLiquidationRequests();
    }

    @Then("Admin search with company name")
    public void adminSearchWithCompanyName() {
        adminPage.searchForCompany(System.getProperty("admin.companyName", ""));
    }

    @Then("Admin click on View Details")
    public void adminClickOnViewDetails() {
        adminPage.openFirstMatchingRequest();
    }

    @Then("Admin verify the details and Approve the request")
    public void adminVerifyTheDetailsAndApproveTheRequest() {
        adminPage.approveCurrentRequest();
    }
}
