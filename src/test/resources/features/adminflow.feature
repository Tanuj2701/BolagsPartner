@admin @e2e @uses-liquidation-order-id
Feature: Admin portal test

  Standalone admin (not in default suite). For liquidation → admin in one scenario, see e2eLiquidationToAdminOrder.feature.
  Run this file after liquidation in one JVM, or pass -DadminOrderDetailPath=/app/genericOrder/list/<id>.

  Background:
    Given User opens admin login page
    Then User clicks on LOGGA IN link from top navigation
    And User logs in with valid admin credentials
    Then User Click on Login Button


  Scenario: Verify the order detail page
    Given the liquidation POST saveInitial order id is available in test context
    When User navigates to generic order detail for the stored order id
    Then User should see the order detail page
    Then user enter the data for offer sent
    When user clicks send quote


  #Scenario: Enter offer sent financial data on generic order 100715
    #Then user enter the data for offer sent
