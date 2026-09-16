@admin @review @wizard @lifecycle @e2e
Feature: Admin document review wizard

  Admin reviews uploaded documents in the Review Wizards tab.

  Scenario: Admin opens review wizard and approves a document
    Given the liquidation order is prepared through agreement sent state
    And admin is logged in for workflow tests
    When admin opens the stored liquidation order for workflow
    And User scrolls page to top
    And admin marks documents received today on manage order
    And admin marks order ready for review
    And admin opens the Review Wizards tab
    And User scrolls page to top
    Then admin should see the document review wizard
    When admin approves the first document in review wizard
