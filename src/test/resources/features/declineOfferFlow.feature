@regression @decline @e2e @captures-order-id @liquidation @admin
Feature: Decline offer flow

  Client declines the liquidation offer after admin sends quote.

  Scenario: Client declines offer after admin sends quote
    Given User login with a valid credentials
    Then User should land on offer page
    Then User Seacrh with random company name
    Then User enters Fornamn
    Then User enters Efternamn
    Then User enters E-post as
    Then user checks the checkbox
    And User click on GoOn
    Then upload the file
    And User click on GoOn
    Then User enters address details
    Then User click on Save
    Then Verify Request Received Page
    Then the order id from saveInitial response is stored in test context
    Given User opens admin login page
    Then User clicks on LOGGA IN link from top navigation
    And User logs in with valid admin credentials
    Then User Click on Login Button
    Given the liquidation POST saveInitial order id is available in test context
    When User navigates to generic order detail for the stored order id
    Then User should see the order detail page
    Then user enter the data for offer sent
    When user clicks send quote
    When user opens decline offer page from captured sendOffer response
    And user submits decline offer with other reason and message "Automation decline — cost too high for test"
    Then the decline offer should be submitted successfully
