@regression @liquidation @e2e @captures-order-id
Feature: liquidation test

  Standalone liquidation (not part of default `mvn test` suite). For full chain with admin, see
  e2eLiquidationToAdminOrder.feature. Run this file only: mvn test -Dcucumber.features=src/test/resources/features/liquidationflow.feature

  Scenario: Verify Liquidation Flow
    Given User login with a valid credentials
    Then  User should land on offer page
    Then User Seacrh with random company name
    Then User enters Fornamn
    Then User enters Efternamn
    Then User enters E-post as
    Then user checks the checkbox
    And User click on GoOn
    Then upload the file
    And User click on GoOn
    Then User enters address details
   # Then user enter details
    Then User click on Save
    Then Verify Request Received Page
    Then the order id from saveInitial response is stored in test context
    #And Close the browser
