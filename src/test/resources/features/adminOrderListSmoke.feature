@admin @smoke @order-list
Feature: Admin generic order list smoke

  Verifies that an authenticated admin can open the generic order list dashboard.

  Background:
    Given User opens admin login page
    Then User clicks on LOGGA IN link from top navigation
    And User logs in with valid admin credentials
    Then User Click on Login Button

  @p0 @order-list-loads
  Scenario: Admin can open generic order list and see orders table
    When User navigates to generic order list page
    Then User should see the generic order list page
