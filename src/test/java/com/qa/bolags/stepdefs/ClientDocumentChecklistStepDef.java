package com.qa.bolags.stepdefs;

import com.qa.bolags.baseTest.BaseTest;
import com.qa.bolags.constants.ClientDocumentTokenContext;
import com.qa.bolags.pages.ClientDocumentChecklistPage;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;

/**
 * Steps for {@code clientDocumentChecklistFlow.feature}.
 */
public class ClientDocumentChecklistStepDef extends BaseTest {

    private ClientDocumentChecklistPage checklistPage;

    @Before(order = 1)
    public void initChecklistPages(Scenario scenario) {
        String uri = scenario.getUri().toString();
        if (uri.contains("clientDocumentChecklistFlow") || uri.contains("timeOptimizedUniqueStepsFlow")) {
            checklistPage = new ClientDocumentChecklistPage(BaseTest.driver);
        }
    }

    @And("user opens the client document upload checklist")
    public void userOpensTheClientDocumentUploadChecklist() {
        if (ClientDocumentTokenContext.getUploadDocumentTokenOrNull() == null) {
            com.qa.bolags.utility.OrderDetailsCapture.pollAndStoreUploadDocumentToken(BaseTest.driver, 20);
        }
        Assert.assertNotNull(ClientDocumentTokenContext.getUploadDocumentTokenOrNull(),
                "uploadDocumentToken not captured — cannot open checklist");
        checklistPage.openClientDocumentChecklist();
    }

    @Then("user should see the document upload checklist")
    public void userShouldSeeTheDocumentUploadChecklist() {
        checklistPage.assertDocumentChecklistDisplayed();
    }

    @When("user uploads a document on the checklist")
    public void userUploadsADocumentOnTheChecklist() {
        checklistPage.uploadFirstAvailableDocument();
    }

    @When("user uploads one document for each checklist category")
    public void userUploadsOneDocumentForEachChecklistCategory() {
        checklistPage.uploadOneDocumentPerChecklistCategory();
    }

    @And("user acknowledges contract information on the checklist if shown")
    public void userAcknowledgesContractInformationIfShown() {
        checklistPage.acknowledgeContractInformationIfPresent();
    }

    @Then("the document upload on checklist should succeed")
    public void theDocumentUploadOnChecklistShouldSucceed() {
        checklistPage.assertDocumentUploadSucceeded();
    }
}
