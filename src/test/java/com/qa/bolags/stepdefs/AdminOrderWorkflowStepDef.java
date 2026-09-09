package com.qa.bolags.stepdefs;

import com.qa.bolags.baseTest.BaseTest;
import com.qa.bolags.pages.AdminOrderWorkflowPage;
import com.qa.bolags.pages.AdminPage;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps for admin post-agreement lifecycle features (review, payment, board change, BV, completion).
 */
public class AdminOrderWorkflowStepDef extends BaseTest {

    private AdminPage adminPage;
    private AdminOrderWorkflowPage workflowPage;

    @Before(order = 1)
    public void initWorkflowPages(Scenario scenario) {
        String uri = scenario.getUri().toString();
        if (uri.contains("adminDocumentReviewWizard")
                || uri.contains("adminPaymentWorkflow")
                || uri.contains("adminBoardChangeFusionFlow")
                || uri.contains("adminBolagsverketSubmissionFlow")
                || uri.contains("adminFinalReportCompletionFlow")) {
            adminPage = new AdminPage(BaseTest.driver);
            workflowPage = new AdminOrderWorkflowPage(BaseTest.driver);
        }
    }

    @Given("admin is logged in for workflow tests")
    public void adminIsLoggedInForWorkflowTests() {
        adminPage.ensureLoggedInAsAdmin();
    }

    @When("admin opens the stored liquidation order for workflow")
    public void adminOpensTheStoredLiquidationOrderForWorkflow() {
        workflowPage.openStoredOrderDetail();
    }

    @And("admin opens the Review Wizards tab")
    public void adminOpensTheReviewWizardsTab() {
        workflowPage.openReviewWizardsTab();
    }

    @Then("admin should see the document review wizard")
    public void adminShouldSeeTheDocumentReviewWizard() {
        workflowPage.assertReviewWizardDisplayed();
    }

    @When("admin approves the first document in review wizard")
    public void adminApprovesTheFirstDocumentInReviewWizard() {
        workflowPage.approveFirstDocumentInReviewWizard();
    }

    @When("admin marks documents received today on manage order")
    public void adminMarksDocumentsReceivedToday() {
        workflowPage.clickDocumentsReceivedToday();
    }

    @And("admin marks order ready for review")
    public void adminMarksOrderReadyForReview() {
        workflowPage.clickReadyForReview();
    }

    @When("admin completes review and moves order to waiting for payment")
    public void adminCompletesReviewAndMovesToWaitingForPayment() {
        workflowPage.clickReviewComplete();
    }

    @Then("admin should see ready for board change action")
    public void adminShouldSeeReadyForBoardChangeAction() {
        workflowPage.assertManageOrderButtonVisible("Ready for board change", "Klar för styrelseändring");
    }

    @When("admin moves order to ready for board change")
    public void adminMovesOrderToReadyForBoardChange() {
        workflowPage.clickReadyForBoardChange();
    }

    @And("admin marks board change sent to Bolagsverket")
    public void adminMarksBoardChangeSentToBolagsverket() {
        workflowPage.clickBoardChangeSentToBolagsverket();
    }

    @And("admin marks registration complete for board change")
    public void adminMarksRegistrationCompleteForBoardChange() {
        workflowPage.clickRegistrationComplete();
    }

    @And("admin marks order ready for liquidation")
    public void adminMarksOrderReadyForLiquidation() {
        workflowPage.clickReadyForLiquidation();
    }

    @Then("admin should see ready for final report action")
    public void adminShouldSeeReadyForFinalReportAction() {
        workflowPage.assertManageOrderButtonVisible("Ready for final report", "Klar för slutrapport");
    }

    @Then("admin should see liquidation sent to Bolagsverket action")
    public void adminShouldSeeLiquidationSentToBolagsverketAction() {
        workflowPage.assertManageOrderButtonVisible(
                "Liquidation sent to Bolagsverket", "Likvidation skickad till Bolagsverket");
    }

    @When("admin submits liquidation to Bolagsverket")
    public void adminSubmitsLiquidationToBolagsverket() {
        workflowPage.clickLiquidationSentToBolagsverket();
    }

    @And("admin marks order ready for final report")
    public void adminMarksOrderReadyForFinalReport() {
        workflowPage.clickReadyForFinalReport();
    }

    @And("admin submits final report to Bolagsverket")
    public void adminSubmitsFinalReportToBolagsverket() {
        workflowPage.clickFinalReportSentToBolagsverket();
    }

    @When("admin completes the liquidation order")
    public void adminCompletesTheLiquidationOrder() {
        workflowPage.clickLiquidationComplete();
    }

    @Then("the liquidation order should be marked complete")
    public void theLiquidationOrderShouldBeMarkedComplete() {
        workflowPage.assertOrderStatusIndicatorPresent("LIQ_COMPLETE");
    }
}
