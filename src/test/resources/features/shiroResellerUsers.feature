@genericOrder @shiro @reseller @admin @e2e
Feature: Shiro and Reseller user flows

  Super-admin creates a Shiro user and a Reseller company via the Generic Order admin UI.

  Background:
    Given generic order QA is ready with HTTP basic credentials from configuration
    And super admin opens the application login page and signs in successfully

  @p1 @shiroUser @shiro-user-crud
  Scenario: Create a new Shiro user and verify it appears in the user list
    When user opens the Shiro user dashboard from Users and Resellers in the left sidebar
    And User scrolls page to top
    And user opens the User List
    And user clicks the "+ Ny användare" button on the user list dashboard
    And User scrolls page to top
    When user clicks Skapa on the empty Shiro user form
    Then the required Shiro user validation errors should be displayed
    And user completes the new Shiro user form with required test data
    Then the new Shiro user should be listed at the top of the user list
    And the new Shiro user should be visible on the Shiro user dashboard user list

  @p1 @resellerUser @reseller-crud
  Scenario: Create a new Reseller company and verify it appears on the reseller list
    When user opens the Reseller user dashboard from the left sidebar
    And User scrolls page to top
    And user clicks the "Ny återförsäljare" button on the reseller list dashboard
    And User scrolls page to top
    When user clicks Skapa on the empty reseller form
    Then the empty reseller form error should be displayed
    And user completes the new reseller form with required test data
    Then the new reseller should appear on the last page of the reseller list via pagination
    And the created organisationsnummer should be visible in the reseller list table
    And the new reseller should be visible on the reseller company dashboard list
