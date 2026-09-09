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
        if (scenario.getUri().toString().contains("clientDocumentChecklistFlow")) {
            checklistPage = new ClientDocumentChecklistPage(BaseTest.driver);
        }
    }

    @And("user opens the client document upload checklist")
    public void userOpensTheClientDocumentUploadChecklist() {
        Assert.assertNotNull(ClientDocumentTokenContext.getUploadDocumentTokenOrNull(),
                "uploadDocumentToken not captured — cannot open checklist");
        checklistPage.openClientDocumentChecklist();
    }

    @Then("user should see the document upload checklist")
    public void userShouldSeeTheDocumentUploadChecklist() {
        checklistPage.assertDocumentChecklistDisplayed();
    }

    @When("user uploads documents for every checklist document type with LADDA UPP FIL or UPLOAD FILE button")
    public void userUploadsDocumentsForEveryChecklistDocumentTypeWithUploadFileButton() {
        checklistPage.uploadAllDocumentsWithUploadButtons();
    }

    @And("user acknowledges contract information on the checklist if shown")
    public void userAcknowledgesContractInformationIfShown() {
        checklistPage.acknowledgeContractInformationIfPresent();
    }

    @Then("all checklist document types with upload file buttons should be uploaded successfully")
    public void allChecklistDocumentTypesWithUploadFileButtonsShouldBeUploadedSuccessfully() {
        checklistPage.assertDocumentUploadSucceeded();
    }
}
