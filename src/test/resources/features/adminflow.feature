@regression @admin @e2e @uses-liquidation-order-id
Feature: Admin portal test

  Standalone admin (not in default suite). For liquidation → admin in one scenario, see e2eLiquidationToAdminOrder.feature.
  Run this file after liquidation in one JVM, or pass -DadminOrderDetailPath=/app/genericOrder/list/<id>.

  Scenario: Verify the order detail page
    # Capture / restore order id before admin login so bootstrap does not wipe the session
    Given the liquidation POST saveInitial order id is available in test context
    And User scrolls page to top
    Given User opens admin login page
    Then User clicks on LOGGA IN link from top navigation
    And User scrolls page to top
    And User logs in with valid admin credentials
    Then User Click on Login Button
    And User scrolls page to top
    When User navigates to generic order detail for the stored order id
    And User scrolls page to top
    Then User should see the order detail page
    And User scrolls page to top
    Then user enter the data for offer sent
    And User scrolls page to top
    When user clicks send quote
    And User scrolls page to top


  #Scenario: Enter offer sent financial data on generic order 100715
    #Then user enter the data for offer sent
