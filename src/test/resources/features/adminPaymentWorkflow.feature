@regression @admin @payment @lifecycle @e2e
Feature: Admin payment workflow

  Admin completes review and advances order to waiting for payment / board change.

  Scenario: Admin completes review and moves order to payment step
    Given the liquidation order is prepared through agreement sent state
    And admin is logged in for workflow tests
    When admin opens the stored liquidation order for workflow
    And User scrolls page to top
    And admin marks documents received today on manage order
    And admin marks order ready for review
    And admin opens the Review Wizards tab
    And User scrolls page to top
    When admin approves the first document in review wizard
    And admin completes review and moves order to waiting for payment
    Then admin should see ready for board change action
