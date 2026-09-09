@client @documents @checklist @e2e @lifecycle
Feature: Client document upload checklist

  Client uploads a required document on the post-acceptance checklist.

  Scenario: Client uploads document on checklist after agreement sent
    Given the liquidation order is prepared through agreement sent state
    And user opens the client document upload checklist
    Then user should see the document upload checklist
    When user uploads documents for every checklist document type with LADDA UPP FIL or UPLOAD FILE button
    And user acknowledges contract information on the checklist if shown
    Then all checklist document types with upload file buttons should be uploaded successfully
