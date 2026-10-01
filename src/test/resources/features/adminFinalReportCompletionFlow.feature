@regression @admin @final-report @completion @lifecycle @e2e
Feature: Admin final report and order completion

  Admin submits final report and marks liquidation order complete.

  Scenario: Admin completes final report and marks order complete
    Given the liquidation order is prepared through agreement sent state
    And client documents are uploaded on the checklist
    And admin is logged in for workflow tests
    When admin opens the stored liquidation order for workflow
    And User scrolls page to top
    And admin marks documents received today on manage order
    And User scrolls page to top
    And admin marks order ready for review
    And User scrolls page to top
    And admin opens the Review Wizards tab
    And User scrolls page to top
    When admin approves the first document in review wizard
    And User scrolls page to top
    When admin completes review and moves order to waiting for payment
    And User scrolls page to top
    And admin moves order to ready for board change
    And User scrolls page to top
    And admin marks board change sent to Bolagsverket
    And User scrolls page to top
    And admin marks registration complete for board change
    And User scrolls page to top
    And admin marks order ready for liquidation
    And User scrolls page to top
    And admin submits liquidation to Bolagsverket
    And User scrolls page to top
    And admin marks order ready for final report
    And User scrolls page to top
    And admin submits final report to Bolagsverket
    And User scrolls page to top
    When admin completes the liquidation order
    And User scrolls page to top
    Then the liquidation order should be marked complete
