package com.qa.bolags.stepdefs;

import com.qa.bolags.pages.liquidationpage;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import org.testng.Assert;
import com.qa.bolags.utility.TestUtil;
import com.qa.bolags.baseTest.BaseTest;

public class liquidationStepDef extends BaseTest {
    private liquidationpage liquidationpage;
    private TestUtil utilpage;

//    @Before
//    public void setUp() {
//        initializeDriver();
//        liquidationpage = new liquidationpage(BaseTest.driver);
//        utilpage = new TestUtil(BaseTest.driver);
//    }

    @Before
    public void setUp() {
        if (BaseTest.driver == null) {
            initializeDriver();
        }
        liquidationpage = new liquidationpage(BaseTest.driver);
        utilpage = new TestUtil(BaseTest.driver);
    }

    @After
    //    public void tearDown() {
//        if (BaseTest.driver != null) {
//            BaseTest.driver.quit();
//            BaseTest.driver = null;
//        }
//    }

    @Then("Home page title should be as expected")
    public void theHomePageTitleShouldBe() {

        String actualTitle = liquidationpage.getHomePageTitle();
        String expectedTitle="Company partner";
        Assert.assertEquals(actualTitle, expectedTitle, "Home page title does not match!");
    }


    @Given("User login with valid credentials")
    public void userLoginWithValidCredentials() {
        liquidationpage.logintoapplication();

    }

    @Then("User should land on offer page")
    public void userShouldLandOnOfferPage() {

        Assert.assertTrue(liquidationpage.is_disply_offerpage(), "Offer page is not displayed");

    }




    @Then("User Seacrh with random company name")//
    public void userSeacrhWithRandomCompanyName() {
        liquidationpage.searchCompany();
    }




    @Given("User login with a valid credentials")
    public void userLoginWithAValidCredentials() {
        liquidationpage.logintoapplication();
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
        liquidationpage.enterEpost("tanujrasane@gmail.com");
    }

    @Then("user checks the checkbox")
    public void userChecksTheCheckbox() {
        liquidationpage.agreeToTermsAndContinue();
    }

    @And("User click on GoOn")
    public void userClickOnGoOn() {
        liquidationpage.clickonGoOn();
    }

    @Then("upload the file")
    public void uploadTheFile() {
        liquidationpage.uploadDocument("/Users/BolagsPartner_Automation/src/main/AktiebokMall_BCD Holding AB.pdf");
    }

    @Then("User enters address details")
    public void userEntersAddressDetails() {
        liquidationpage.enterAddressDetails("Teststreet", "12345", "Stockholm", "0701234567");
    }


    @Then("User click on Save")
    public void userClickOnSave() {
        liquidationpage.userClickOnSave();

    }

    @And("Close the browser")
    public void closeTheBrowser() {
   }

    @Then("Verify Request Received Page")
    public void verifyRequestReceivedPage() {
        Assert.assertTrue(liquidationpage.is_request_received_page_displayed(), "Request Received page is not displayed");
    }

   @Given("User login to admin page with valid credentials")
    public void userLoginToAdminPageWithValidCredentials() {

  }
}
