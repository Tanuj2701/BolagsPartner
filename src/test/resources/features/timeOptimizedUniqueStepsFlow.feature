@time-optimized @captures-order-id @uses-liquidation-order-id
Feature: Time-optimized unique steps in one browser session

  Unique application-flow steps from all features, one scenario, one WebDriver.
  Duplicate placement / re-login / overlapping list assertions are omitted.
  Decline-offer is omitted: it requires a second full order (placement + send quote already covered).

  Run: mvn test -Ptimeopt
  Headless: mvn test -Ptimeopt -Dheadless=true

  Scenario: Unique steps from placement through completion, order list and Reseller
    # --- Liquidation validation then order placement ---
    Given User login with a valid credentials
    Then User should land on offer page
    When User clicks continue without selecting a company
    Then User should remain on liquidation offer page
    Then User Seacrh with random company name
    When User clicks continue without completing contact details
    Then User should remain on liquidation offer page
    Then User enters Fornamn
    Then User enters Efternamn
    Then User enters E-post as
    Then user checks the checkbox
    And User click on GoOn
    Then upload the file
    And User click on GoOn
    Then User enters address details
    Then User click on Save
    Then Verify Request Received Page
    Then the order id from saveInitial response is stored in test context

    # --- Admin login with valid credentials ---
    Given User opens admin login page
    Then User clicks on LOGGA IN link from top navigation
    And User logs in with valid admin credentials
    Then User Click on Login Button

    # --- Admin liquidation: offer, accept, shareholder, former representatives, agreement ---
    Given the liquidation POST saveInitial order id is available in test context
    When User navigates to generic order detail for the stored order id
    Then User should see the order detail page
    Then user enter the data for offer sent
    When user clicks send quote
    Then user Accept the offer
    And user add the shareholder
    And user select the Signing method
    When User navigates to generic order detail for the stored order id
    And user checks the shareholders updated checkbox on manage order
    When user opens the send agreement document modal
    And user clicks send agreement in the document modal
    Then the agreement should be sent successfully

    # --- Client checklist: one document per category ---
    And user opens the client document upload checklist
    Then user should see the document upload checklist
    When user uploads one document for each checklist category
    And user acknowledges contract information on the checklist if shown
    Then the document upload on checklist should succeed

    # --- Admin lifecycle: review, payment, board/fusion, Bolagsverket, final report ---
    # Same unique sequence as adminDocumentReviewWizard + adminPaymentWorkflow +
    # adminBoardChangeFusionFlow + adminBolagsverketSubmissionFlow + adminFinalReportCompletionFlow
    # (scroll-only steps omitted). Re-login is required after the client checklist URL.
    And admin is logged in for workflow tests
    When admin opens the stored liquidation order for workflow
    And admin marks documents received today on manage order
    And admin marks order ready for review
    And admin opens the Review Wizards tab
    Then admin should see the document review wizard
    When admin approves the first document in review wizard
    When admin completes review and moves order to waiting for payment
    And admin closes the review wizard and returns to manage order

    # --- Reseller (empty-form validation + create) ---+ Ny användare

    When user opens the Reseller user dashboard from the left sidebar
    And user clicks the "Ny återförsäljare" button on the reseller list dashboard
    When user clicks Skapa on the empty reseller form
    Then the empty reseller form error should be displayed
    And user completes the new reseller form with required test data
    Then the new reseller should appear on the last page of the reseller list via pagination
    And the created organisationsnummer should be visible in the reseller list table
