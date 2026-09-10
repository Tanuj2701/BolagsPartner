@admin @bolagsverket @e2e @captures-order-id @liquidation
Feature: Admin Bolagsverket company update before send offer

  When company data differs from Bolagsverket, the order detail shows a warning banner.
  Admin opens the diff modal, applies the update (Lägg Till), then sends the offer successfully.

  Scenario: Admin updates company from Bolagsverket and sends offer
    # --- Part 1: Public liquidation (orderId captured from POST saveInitial) ---
    Given User login with a valid credentials
    Then User should land on offer page
    Then User Seacrh with random company name
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
    # --- Part 2: Admin portal → order detail ---
    Given User opens admin login page
    Then User clicks on LOGGA IN link from top navigation
    And User logs in with valid admin credentials
    Then User Click on Login Button
    Given the liquidation POST saveInitial order id is available in test context
    When User navigates to generic order detail for the stored order id
    Then User should see the order detail page
    # --- Part 3: Bolagsverket sync before offer (redirect clears form if done after data entry) ---
    When admin syncs company from Bolagsverket if required on order detail
    Then the Bolagsverket company update should be applied successfully
    # --- Part 4: Accounting data + send offer ---
    Then user enter the data for offer sent
    When user sends offer quote with Bolagsverket sync if required
    Then send offer should complete successfully with captured token
