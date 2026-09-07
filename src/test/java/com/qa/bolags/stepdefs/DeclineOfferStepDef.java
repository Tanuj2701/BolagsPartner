package com.qa.bolags.stepdefs;

import com.qa.bolags.baseTest.BaseTest;
import com.qa.bolags.constants.AcceptOfferContext;
import com.qa.bolags.pages.AdminPage;
import com.qa.bolags.pages.DeclineOfferPage;
import com.qa.bolags.pages.liquidationpage;
import com.qa.bolags.utility.SendOfferResponseCapture;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;

/**
 * Steps for {@code declineOfferFlow.feature}.
 */
public class DeclineOfferStepDef extends BaseTest {

    private liquidationpage liquidationpage;
    private AdminPage adminPage;
    private DeclineOfferPage declineOfferPage;

    @Before(order = 1)
    public void initDeclineOfferPages(Scenario scenario) {
        if (scenario.getUri().toString().contains("declineOfferFlow")) {
            liquidationpage = new liquidationpage(BaseTest.driver);
            adminPage = new AdminPage(BaseTest.driver);
            declineOfferPage = new DeclineOfferPage(BaseTest.driver);
        }
    }

    @When("user opens decline offer page from captured sendOffer response")
    public void userOpensDeclineOfferPageFromCapturedResponse() {
        if (AcceptOfferContext.getMd5TokenOrNull() == null) {
            SendOfferResponseCapture.pollAndStoreFromSendOfferChrome(BaseTest.driver, 45);
        }
        declineOfferPage.openDeclineOfferFromCapturedSendOfferResponse();
    }

    @And("user submits decline offer with other reason and message {string}")
    public void userSubmitsDeclineOfferWithOtherReason(String message) {
        declineOfferPage.selectOtherReasonAndSubmitDecline(message);
    }

    @Then("the decline offer should be submitted successfully")
    public void theDeclineOfferShouldBeSubmittedSuccessfully() {
        declineOfferPage.assertDeclineOfferSubmitted();
    }
}
