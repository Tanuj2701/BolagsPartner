@liquidation @validation @negative @smoke
Feature: Liquidation form validation

  Negative validation scenarios on the public liquidation offer wizard.

  @p1 @missing-company
  Scenario: Liquidation wizard blocks progress when company is not selected
    Given User login with a valid credentials
    Then User should land on offer page
    And User scrolls page to top
    When User clicks continue without selecting a company
    Then User should remain on liquidation offer page

  @p1 @missing-contact-details
  Scenario: Liquidation wizard blocks progress when contact details are incomplete
    Given User login with a valid credentials
    Then User should land on offer page
    Then User Seacrh with random company name
    When User clicks continue without completing contact details
    Then User should remain on liquidation offer page
