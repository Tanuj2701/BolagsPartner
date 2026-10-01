@regression @e2e @combined-liquidation-admin @captures-order-id @uses-liquidation-order-id
Feature: E2E — liquidation creates order (saveInitial), admin opens that order and sends quote

  One browser session: POST saveInitial orderId is captured after the first “GoOn”, stored in LiquidationOrderIdContext,
  then the same scenario continues with admin login and generic order detail.

  Scenario: Verify Liquidation Flow and Verify the order detail page
    # --- Part 1: Public liquidation (orderId captured from POST .../companyLiquidationOrders/saveInitial) ---
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
    # --- Part 2: Admin portal → /app/genericOrder/list/{orderId} ---
    Given User opens admin login page
    Then User clicks on LOGGA IN link from top navigation
    And User logs in with valid admin credentials
    Then User Click on Login Button
    Given the liquidation POST saveInitial order id is available in test context
    When User navigates to generic order detail for the stored order id
    Then User should see the order detail page
    Then user enter the data for offer sent
    When user clicks send quote
    Then user Accept the offer
    And user add the shareholder
    And user select the Signing method
    # --- Part 3: Admin sends agreement for the same accepted order ---
    When User navigates to generic order detail for the stored order id
    And user checks the shareholders updated checkbox on manage order
    When user opens the send agreement document modal
    And user clicks send agreement in the document modal
    Then the agreement should be sent successfully
