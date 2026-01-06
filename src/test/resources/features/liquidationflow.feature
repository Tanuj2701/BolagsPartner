Feature: liquidation test

  Scenario: Verify Liquidation Flow

    Given User login with a valid credentials
    Then  User should land on offer page
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
    #And Close the browser

  Scenario: Verify Admin Liquidation Flow

    Given Admin login with a valid credentials
    Then Admin should land on Dashboard page
    Then Admin click on Requests
    Then Admin click on Liquidation Requests
    Then Admin search with company name
    Then Admin click on View Details
    Then Admin verify the details and Approve the request
  #  Then Close the browser





