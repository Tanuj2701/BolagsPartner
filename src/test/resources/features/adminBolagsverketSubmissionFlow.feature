@regression @admin @bolagsverket @lifecycle @e2e
Feature: Admin Bolagsverket submission

  Admin submits liquidation to Bolagsverket.

  Scenario: Admin submits liquidation to Bolagsverket
    Given the liquidation order is prepared through agreement sent state
    And admin is logged in for workflow tests
    When admin opens the stored liquidation order for workflow
    And User scrolls page to top
    And admin marks documents received today on manage order
    And admin marks order ready for review
    When admin completes review and moves order to waiting for payment
    And admin moves order to ready for board change
    And admin marks board change sent to Bolagsverket
    And admin marks registration complete for board change
    And admin marks order ready for liquidation
    And admin submits liquidation to Bolagsverket
    Then admin should see ready for final report action
