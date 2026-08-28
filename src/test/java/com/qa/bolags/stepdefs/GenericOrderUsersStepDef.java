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
        if (scenario.getUri().toString().contains("shiroResellerUsers")) {
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

    @And("user completes the new Shiro user form with required test data")
    public void userCompletesNewShiroUserFormWithTestData() {
        genericOrderUsersPage.completeShiroUserFormWithTestData();
    }

    @Then("the new Shiro user should be listed at the top of the user list")
    public void newShiroUserListedAtTop() {
        genericOrderUsersPage.assertNewShiroUserListedAtTop();
    }

    @When("user opens the Reseller user dashboard from the left sidebar")
    public void userOpensResellerDashboardFromSidebar() {
        genericOrderUsersPage.openResellerUserDashboardFromSidebar();
    }

    @And("user clicks New Dealer")
    public void userClicksNewDealer() {
        genericOrderUsersPage.clickNewDealer();
    }

    @And("user completes the new reseller form with required test data")
    public void userCompletesNewResellerFormWithTestData() {
        genericOrderUsersPage.completeResellerFormWithTestData();
    }

    @Then("the new reseller should appear on the last page of the reseller list via pagination")
    public void newResellerOnLastPage() {
        genericOrderUsersPage.assertNewResellerOnLastPageOfList();
    }
}
