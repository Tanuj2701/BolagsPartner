package com.qa.bolags.stepdefs;

import com.qa.bolags.baseTest.BaseTest;
import com.qa.bolags.pages.liquidationpage;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Validation steps for {@code liquidationFormValidation.feature}.
 */
public class LiquidationValidationStepDef extends BaseTest {

    private liquidationpage getLiquidationPage() {
        return new liquidationpage(BaseTest.driver);
    }

    @When("User clicks continue without selecting a company")
    public void userClicksContinueWithoutSelectingCompany() {
        getLiquidationPage().clickContinueWithoutCompanySelection();
    }

    @When("User clicks continue without completing contact details")
    public void userClicksContinueWithoutCompletingContactDetails() {
        getLiquidationPage().clickContinueWithoutContactDetails();
    }

    @Then("User should remain on liquidation offer page")
    public void userShouldRemainOnLiquidationOfferPage() {
        getLiquidationPage().assertStillOnOfferPage();
    }
}
