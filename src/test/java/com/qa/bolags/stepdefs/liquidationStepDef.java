package com.qa.bolags.stepdefs;

import com.qa.bolags.baseTest.BaseTest;
import com.qa.bolags.constants.LiquidationOrderIdContext;
import com.qa.bolags.pages.liquidationpage;
import com.qa.bolags.utility.SaveInitialOrderCapture;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.testng.Assert;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class liquidationStepDef extends BaseTest {

    private liquidationpage liquidationpage;
    /** First {@code User click on GoOn} completes step 1 and triggers {@code POST saveInitial}. */
    private int goOnClickCount;

    @Before(order = 1)
    public void initLiquidationPage(Scenario scenario) {
        String uri = scenario.getUri().toString();
        if (!uri.contains("adminflow") && !uri.contains("shiroResellerUsers")
                && !uri.contains("adminLoginValidation") && !uri.contains("adminOrderListSmoke")) {
            liquidationpage = new liquidationpage(BaseTest.driver);
        }
        if (uri.contains("liquidationflow") || uri.contains("e2eLiquidationToAdminOrder")
                || uri.contains("declineOfferFlow") || uri.contains("timeOptimizedUniqueStepsFlow")) {
            goOnClickCount = 0;
        }
    }

    private void quitDriverIfOpen() {
        if (BaseTest.driver != null) {
            try {
                BaseTest.driver.quit();
                log.info("Browser closed after scenario.");
            } catch (Exception e) {
                log.warn("Error while closing browser: {}", e.getMessage());
            } finally {
                BaseTest.driver = null;
            }
        }
    }

    private void loginToLiquidationApp() {
        goOnClickCount = 0;
        liquidationpage.logintoapplication();
    }

    @Given("User login with valid credentials")
    public void userLoginWithValidCredentials() {
        loginToLiquidationApp();
    }

    @Given("User login with a valid credentials")
    public void userLoginWithAValidCredentials() {
        loginToLiquidationApp();
    }

    @Then("User should land on offer page")
    public void userShouldLandOnOfferPage() {
        Assert.assertTrue(liquidationpage.isOfferPageDisplayed(), "Offer page is not displayed");
    }

    @Then("User Seacrh with random company name")
    public void userSeacrhWithRandomCompanyName() {
        liquidationpage.searchCompany();
    }

    @Then("User enters Fornamn")
    public void userEntersFornamn() {
        liquidationpage.enterFornamn("Tanuj");
    }

    @Then("User enters Efternamn")
    public void userEntersEfternamn() {
        liquidationpage.enterEfternamn("Rasane");
    }

    @Then("User enters E-post as")
    public void userEntersEPostAs() {
        liquidationpage.enterEpost("tanujrasane23@gmail.com");
    }

    @Then("user checks the checkbox")
    public void userChecksTheCheckbox() {
        liquidationpage.agreeToTermsAndContinue();
    }

    @And("User click on GoOn")
    public void userClickOnGoOn() {
        liquidationpage.clickonGoOn();
        goOnClickCount++;
        if (goOnClickCount == 1) {
            SaveInitialOrderCapture.pollAndStoreOrderIdFromChrome(BaseTest.driver, 90);
        }
    }

    @Then("upload the file")
    public void uploadTheFile() {
        Path filePath = Paths.get(System.getProperty("user.dir"), "src", "main", "ABC.pdf");
        if (!Files.exists(filePath)) {
            throw new RuntimeException("Upload file not found: " + filePath);
        }
        liquidationpage.uploadDocument(filePath.toString());
    }

    @Then("User enters address details")
    public void userEntersAddressDetails() {
        liquidationpage.enterAddressDetails(
                "Teststreet", "12345", "Stockholm", "0701234567",
                "0709876543", "Automation Agency AB",
                "Automated liquidation flow - message to partner.");
    }

    @Then("User click on Save")
    public void userClickOnSave() {
        liquidationpage.userClickOnSave();
    }

    @And("Close the browser")
    public void closeTheBrowser() {
        quitDriverIfOpen();
    }

    @Then("Verify Request Received Page")
    public void verifyRequestReceivedPage() {
        Assert.assertTrue(liquidationpage.isRequestReceivedPageDisplayed(), "Request Received page is not displayed");
        if (LiquidationOrderIdContext.getCapturedOrderIdOrNull() == null) {
            SaveInitialOrderCapture.pollAndStoreOrderIdFromChrome(BaseTest.driver, 30);
        }
    }

    @Then("the order id from saveInitial response is stored in test context")
    public void theOrderIdFromSaveInitialResponseIsStoredInTestContext() {
        if (LiquidationOrderIdContext.getCapturedOrderIdOrNull() == null) {
            SaveInitialOrderCapture.pollAndStoreOrderIdFromChrome(BaseTest.driver, 45);
        }
        Assert.assertNotNull(
                LiquidationOrderIdContext.getCapturedOrderIdOrNull(),
                "Expected POST .../companyLiquidationOrders/saveInitial JSON to contain orderId. "
                        + "If this persists, capture may be blocked by Chrome/CDP; try -DadminOrderDetailPath=/app/genericOrder/list/<id>. "
                        + "Cumulative network request map size was: " + SaveInitialOrderCapture.debugCumulativeRequestMapSize());
    }

    // Placeholder for admin scenarios; wire adminpage when that flow is automated.
    @Given("User login to admin page with valid credentials")
    public void userLoginToAdminPageWithValidCredentials() {
    }
}
