@admin @board-change @fusion @lifecycle @e2e
Feature: Admin board change and fusion path

  Admin progresses order through board change and registration to ready for liquidation.

  Scenario: Admin completes board change and fusion registration path
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
