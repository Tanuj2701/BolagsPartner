@admin @payment @lifecycle @e2e
Feature: Admin payment workflow

  Admin completes review and advances order to waiting for payment / board change.

  Scenario: Admin completes review and moves order to payment step
    Given the liquidation order is prepared through agreement sent state
    And admin is logged in for workflow tests
    When admin opens the stored liquidation order for workflow
    And admin marks documents received today on manage order
    And admin marks order ready for review
    And admin opens the Review Wizards tab
    When admin approves the first document in review wizard   # Failing due to Pop up and SIE Activation.
    And admin completes review and moves order to waiting for payment
    Then admin should see ready for board change action
