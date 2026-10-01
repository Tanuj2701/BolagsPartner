package com.qa.bolags.stepdefs;

import com.qa.bolags.baseTest.BaseTest;
import com.qa.bolags.pages.GenericOrderUsersPage;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps for {@code shiroResellerUsers.feature} (Generic Order — Shiro & Reseller).
 */
public class GenericOrderUsersStepDef extends BaseTest {

    private GenericOrderUsersPage genericOrderUsersPage;

    @Before(order = 1)
    public void initGenericOrderPage(Scenario scenario) {
        String uri = scenario.getUri().toString();
        if (uri.contains("shiroResellerUsers") || uri.contains("timeOptimizedUniqueStepsFlow")) {
            genericOrderUsersPage = new GenericOrderUsersPage(BaseTest.driver);
        }
    }

    @Given("generic order QA is ready with HTTP basic credentials from configuration")
    public void genericOrderQaReadyWithHttpBasicFromConfiguration() {
        log.info("Using QA HTTP Basic credentials from {} (see also super admin keys).",
                com.qa.bolags.constants.Constants.CONFIGPROP);
    }

    @And("super admin opens the application login page and signs in successfully")
    public void superAdminOpensApplicationLoginAndSignsIn() {
        genericOrderUsersPage.loginAsSuperAdmin();
    }

    @When("user opens the Shiro user dashboard from Users and Resellers in the left sidebar")
    public void userOpensShiroUserDashboardFromSidebar() {
        genericOrderUsersPage.openShiroUserDashboardFromSidebar();
    }

    @And("user opens the User List")
    public void userOpensTheUserList() {
        genericOrderUsersPage.openUserList();
    }

    @And("user clicks the New User button on the user list dashboard")
    public void userClicksNewUserOnUserListDashboard() {
        genericOrderUsersPage.clickNewUserOnUserListDashboard();
    }

    @And("user clicks the {string} button on the user list dashboard")
    public void userClicksNamedButtonOnUserListDashboard(String buttonLabel) {
        genericOrderUsersPage.clickNewUserOnUserListDashboard(buttonLabel);
    }

    @When("user clicks Skapa on the empty Shiro user form")
    public void userClicksSkapaOnEmptyShiroUserForm() {
        genericOrderUsersPage.submitEmptyShiroUserForm();
    }

    @Then("the required Shiro user validation errors should be displayed")
    public void requiredShiroUserValidationErrorsDisplayed() {
        genericOrderUsersPage.assertRequiredShiroUserValidationErrors();
    }

    @And("user completes the new Shiro user form with required test data")
    public void userCompletesNewShiroUserFormWithTestData() {
        genericOrderUsersPage.completeShiroUserFormWithTestData();
    }

    @Then("the new Shiro user should be listed at the top of the user list")
    public void newShiroUserListedAtTop() {
        genericOrderUsersPage.assertNewShiroUserListedAtTop();
    }

    @And("the new Shiro user should be visible on the Shiro user dashboard user list")
    public void newShiroUserVisibleOnDashboardUserList() {
        genericOrderUsersPage.assertNewShiroUserVisibleInUserList();
    }

    @When("user opens the Reseller user dashboard from the left sidebar")
    public void userOpensResellerDashboardFromSidebar() {
        genericOrderUsersPage.openResellerUserDashboardFromSidebar();
    }

    @And("user clicks New Dealer")
    public void userClicksNewDealer() {
        genericOrderUsersPage.clickNewDealer();
    }

    @And("user clicks the {string} button on the reseller list dashboard")
    public void userClicksNamedButtonOnResellerListDashboard(String buttonLabel) {
        genericOrderUsersPage.clickNewDealer(buttonLabel);
    }

    @When("user clicks Skapa on the empty reseller form")
    public void userClicksSkapaOnEmptyResellerForm() {
        genericOrderUsersPage.submitEmptyResellerForm();
    }

    @Then("the empty reseller form error should be displayed")
    public void emptyResellerFormErrorDisplayed() {
        genericOrderUsersPage.assertEmptyResellerFormError();
    }

    @And("user completes the new reseller form with required test data")
    public void userCompletesNewResellerFormWithTestData() {
        genericOrderUsersPage.completeResellerFormWithTestData();
    }

    @Then("the new reseller should appear on the last page of the reseller list via pagination")
    public void newResellerOnLastPage() {
        genericOrderUsersPage.assertNewResellerOnLastPageOfList();
    }

    @And("the created organisationsnummer should be visible in the reseller list table")
    public void createdOrganisationNumberVisibleInResellerList() {
        genericOrderUsersPage.assertCreatedOrganisationNumberVisibleInResellerList();
    }

    @And("the new reseller should be visible on the reseller company dashboard list")
    public void newResellerVisibleOnDashboardList() {
        genericOrderUsersPage.assertNewResellerVisibleInResellerList();
    }
}
