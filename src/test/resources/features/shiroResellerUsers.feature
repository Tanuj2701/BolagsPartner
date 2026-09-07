@shiro @reseller @admin @e2e
Feature: Generic Order — Shiro user and Reseller company management

  Super-admin creates a Shiro user and a Reseller company via the Generic Order admin UI.

  Background:
    Given generic order QA is ready with HTTP basic credentials from configuration
    And super admin opens the application login page and signs in successfully

  @p1 @shiro-user-crud
  Scenario: Create a new Shiro user and verify it appears in the user list
    When user opens the Shiro user dashboard from Users and Resellers in the left sidebar
    And user opens the User List
    And user clicks the New User button on the user list dashboard
    And user completes the new Shiro user form with required test data
    Then the new Shiro user should be listed at the top of the user list

  @p1 @reseller-crud
  Scenario: Create a new Reseller company and verify it appears on the reseller list
    When user opens the Reseller user dashboard from the left sidebar
    And user clicks New Dealer
    And user completes the new reseller form with required test data
    Then the new reseller should appear on the last page of the reseller list via pagination
