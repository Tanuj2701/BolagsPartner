package com.qa.bolags.stepdefs;

import com.qa.bolags.baseTest.BaseTest;
import com.qa.bolags.pages.AdminPage;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Additional admin steps for {@code adminLoginValidation.feature} and {@code adminOrderListSmoke.feature}.
 * Does not modify existing admin flow step definitions.
 */
public class AdminExtendedStepDef extends BaseTest {

    private AdminPage adminPage;

    @Before(order = 1)
    public void initAdminExtendedPage(Scenario scenario) {
        String uri = scenario.getUri().toString();
        if (uri.contains("adminLoginValidation") || uri.contains("adminOrderListSmoke")) {
            adminPage = new AdminPage(BaseTest.driver);
        }
    }

    @When("User enters admin email with configured valid email")
    public void userEntersAdminEmailWithConfiguredValidEmail() {
        adminPage.enterConfiguredAdminEmail();
    }

    @And("User enters admin password as {string}")
    public void userEntersAdminPasswordAs(String password) {
        adminPage.enterAdminCredentials(
                System.getProperty("adminEmail", com.qa.bolags.constants.QaServerCredentials.genericOrderSuperAdminEmail()),
                password);
    }

    @When("User enters admin email as {string}")
    public void userEntersAdminEmailAs(String email) {
        adminPage.enterAdminCredentials(email, "");
    }

    @And("User enters admin password with configured valid password")
    public void userEntersAdminPasswordWithConfiguredValidPassword() {
        adminPage.enterConfiguredAdminPasswordOnly();
    }

    @Then("User should remain on admin login page or see login error")
    public void userShouldRemainOnAdminLoginPageOrSeeLoginError() {
        adminPage.assertLoginFailedOrStillOnLoginPage();
    }

    @When("User navigates to generic order list page")
    public void userNavigatesToGenericOrderListPage() {
        adminPage.openGenericOrderListPage();
    }

    @Then("User should see the generic order list page")
    public void userShouldSeeTheGenericOrderListPage() {
        adminPage.assertGenericOrderListPageDisplayed();
    }
}
