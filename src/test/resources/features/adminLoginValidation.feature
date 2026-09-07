@admin @smoke @validation @negative
Feature: Admin login validation

  Negative and boundary scenarios for admin portal authentication.

  Background:
    Given User opens admin login page
    Then User clicks on LOGGA IN link from top navigation

  @p0 @invalid-password
  Scenario: Admin login fails with invalid password
    When User enters admin email with configured valid email
    And User enters admin password as "InvalidPassword!123"
    And User Click on Login Button
    Then User should remain on admin login page or see login error

  @p1 @empty-email
  Scenario: Admin login fails when email is empty
    When User enters admin email as ""
    And User enters admin password with configured valid password
    And User Click on Login Button
    Then User should remain on admin login page or see login error
