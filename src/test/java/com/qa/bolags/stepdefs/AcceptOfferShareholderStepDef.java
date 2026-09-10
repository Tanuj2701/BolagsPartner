package com.qa.bolags.stepdefs;

import com.qa.bolags.baseTest.BaseTest;
import com.qa.bolags.constants.AcceptOfferContext;
import com.qa.bolags.constants.ShareholderVariantType;
import com.qa.bolags.pages.AcceptOfferShareholderPage;
import com.qa.bolags.pages.AdminPage;
import com.qa.bolags.utility.SendOfferResponseCapture;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * New accept-offer shareholder variant steps — does not modify {@link adminStepDefe} or {@link AcceptOfferPage}.
 */
public class AcceptOfferShareholderStepDef extends BaseTest {

    private AdminPage adminPage;
    private AcceptOfferShareholderPage shareholderPage;

    @Before(order = 1)
    public void initAcceptOfferShareholderPages(Scenario scenario) {
        if (scenario.getUri().toString().contains("acceptOfferShareholderVariants")
                || scenario.getUri().toString().contains("sendAgreementShareholderVariants")) {
            adminPage = new AdminPage(BaseTest.driver);
            shareholderPage = new AcceptOfferShareholderPage(BaseTest.driver);
        }
    }

    @Given("admin prepares and sends offer for accept-offer shareholder tests")
    public void adminPreparesAndSendsOfferForAcceptOfferShareholderTests() {
        adminPage.enterOfferSentAccountingData();
        adminPage.clickSendQuoteOnManageOrder();
        SendOfferResponseCapture.pollAndStoreFromSendOfferChrome(BaseTest.driver, 60);
        Assert.assertNotNull(AcceptOfferContext.getMd5TokenOrNull(), "sendOffer md5Token not captured");
    }

    @When("client opens accept offer page for shareholder variant tests")
    public void clientOpensAcceptOfferPageForShareholderVariantTests() {
        adminPage.openAcceptOfferFromCapturedSendOfferResponse();
        Assert.assertTrue(adminPage.isAcceptOfferPageDisplayed(), "Accept offer page is not visible");
    }

    @And("client adds Swedish natural person shareholder registered in Sweden")
    public void clientAddsSwedishNaturalPersonShareholderRegisteredInSweden() {
        shareholderPage.addSwedishNaturalPersonShareholder();
    }

    @And("client adds Swedish legal entity shareholder registered in Sweden")
    public void clientAddsSwedishLegalEntityShareholderRegisteredInSweden() {
        shareholderPage.addSwedishLegalEntityShareholder();
    }

    @And("client adds foreign natural person shareholder registered abroad")
    public void clientAddsForeignNaturalPersonShareholderRegisteredAbroad() {
        shareholderPage.addForeignNaturalPersonShareholder();
    }

    @And("client adds foreign legal entity shareholder registered abroad")
    public void clientAddsForeignLegalEntityShareholderRegisteredAbroad() {
        shareholderPage.addForeignLegalEntityShareholder();
    }

    /**
     * Splits company total shares across multiple Add owner modals (e.g. 70/20/10 of 100 shares).
     * <pre>
     * | shareholderType      | sharePercent |
     * | swedish-person       | 70           |
     * | swedish-legal-entity | 20           |
     * | foreign-person       | 10           |
     * </pre>
     */
    @And("client allocates shares to multiple shareholders:")
    public void clientAllocatesSharesToMultipleShareholders(DataTable table) {
        List<ShareholderVariantType> types = new ArrayList<>();
        List<Integer> percents = new ArrayList<>();
        for (Map<String, String> row : table.asMaps()) {
            types.add(ShareholderVariantType.fromFeatureLabel(row.get("shareholderType")));
            percents.add(Integer.parseInt(row.get("sharePercent").trim()));
        }
        shareholderPage.allocateSharesAcrossMixedShareholders(types, percents);
    }

    @And("all company shares should be allocated on accept offer page")
    public void allCompanySharesShouldBeAllocatedOnAcceptOfferPage() {
        Assert.assertEquals(shareholderPage.readRemainingShares(), 0, "Expected 0 shares remaining");
    }

    @And("client selects paper signature and completes accept offer shareholder flow")
    public void clientSelectsPaperSignatureAndCompletesAcceptOfferShareholderFlow() {
        shareholderPage.selectPaperSignatureAndCompleteAcceptOffer();
    }

    @Then("accept offer shareholder variant flow should complete successfully")
    public void acceptOfferShareholderVariantFlowShouldCompleteSuccessfully() {
        Assert.assertTrue(
                shareholderPage.isAcceptOfferShareholderFlowComplete(
                        new org.openqa.selenium.support.ui.WebDriverWait(BaseTest.driver, java.time.Duration.ofSeconds(5))),
                "Expected document checklist or signature completion after adding shareholder");
    }
}
