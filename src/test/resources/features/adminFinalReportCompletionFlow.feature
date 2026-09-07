@admin @final-report @completion @lifecycle @e2e
Feature: Admin final report and order completion

  Admin submits final report and marks liquidation order complete.

  Scenario: Admin completes final report and marks order complete
    Given the liquidation order is prepared through agreement sent state
    And admin is logged in for workflow tests
    When admin opens the stored liquidation order for workflow
    And admin marks documents received today on manage order
    And admin marks order ready for review
    When admin completes review and moves order to waiting for payment
    And admin moves order to ready for board change
    And admin marks board change sent to Bolagsverket
    And admin marks registration complete for board change
    And admin marks order ready for liquidation
    And admin submits liquidation to Bolagsverket
    And admin marks order ready for final report
    And admin submits final report to Bolagsverket
    When admin completes the liquidation order
    Then the liquidation order should be marked complete
