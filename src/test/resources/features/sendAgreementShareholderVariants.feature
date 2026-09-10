@send-agreement @shareholders @e2e @captures-order-id @liquidation
Feature: Send agreement with different shareholder types

  Admin sends liquidation agreement after the client accepts the offer and registers owners
  via the Add owner modal ({@code addShareHolder.tsx}): New person / New legal entity, Sweden / Abroad.
  Client API: POST {@code /v1/companies/createFormerRepresentative}.
  Admin API: POST {@code /v1/companyLiquidationOrders/send-agreement} (status LIQ_READY_FOR_SIGNING).

  Background:
    # --- Liquidation creates order (saveInitial orderId captured) ---
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
    # --- Admin sends offer ---
    Given User opens admin login page
    Then User clicks on LOGGA IN link from top navigation
    And User logs in with valid admin credentials
    Then User Click on Login Button
    Given the liquidation POST saveInitial order id is available in test context
    When User navigates to generic order detail for the stored order id
    Then User should see the order detail page
    Given admin prepares and sends offer for accept-offer shareholder tests
    When client opens accept offer page for shareholder variant tests

  @shareholder @swedish-person @send-agreement-single
  Scenario: Send agreement with Swedish natural person shareholder
    And client adds Swedish natural person shareholder registered in Sweden
    And client selects paper signature and completes accept offer shareholder flow
    Then accept offer shareholder variant flow should complete successfully
    When admin sends agreement for shareholder variant order
    Then send agreement shareholder variant flow should complete successfully

  @shareholder @swedish-legal-entity @send-agreement-single
  Scenario: Send agreement with Swedish legal entity shareholder
    And client adds Swedish legal entity shareholder registered in Sweden
    And client selects paper signature and completes accept offer shareholder flow
    Then accept offer shareholder variant flow should complete successfully
    When admin sends agreement for shareholder variant order
    Then send agreement shareholder variant flow should complete successfully

  @shareholder @foreign-person @send-agreement-single
  Scenario: Send agreement with foreign natural person shareholder
    And client adds foreign natural person shareholder registered abroad
    And client selects paper signature and completes accept offer shareholder flow
    Then accept offer shareholder variant flow should complete successfully
    When admin sends agreement for shareholder variant order
    Then send agreement shareholder variant flow should complete successfully

  @shareholder @foreign-legal-entity @send-agreement-single
  Scenario: Send agreement with foreign legal entity shareholder
    And client adds foreign legal entity shareholder registered abroad
    And client selects paper signature and completes accept offer shareholder flow
    Then accept offer shareholder variant flow should complete successfully
    When admin sends agreement for shareholder variant order
    Then send agreement shareholder variant flow should complete successfully

  @shareholder @multiple-shareholders @mixed-three @send-agreement-multi
  Scenario: Send agreement with three mixed shareholders splitting shares 70/20/10
    And client allocates shares to multiple shareholders:
      | shareholderType      | sharePercent |
      | swedish-person       | 70           |
      | swedish-legal-entity | 20           |
      | foreign-person       | 10           |
    And all company shares should be allocated on accept offer page
    And client selects paper signature and completes accept offer shareholder flow
    Then accept offer shareholder variant flow should complete successfully
    When admin sends agreement for shareholder variant order
    Then send agreement shareholder variant flow should complete successfully

  @shareholder @multiple-shareholders @mixed-four @send-agreement-multi
  Scenario: Send agreement with four shareholder types splitting shares 40/30/20/10
    And client allocates shares to multiple shareholders:
      | shareholderType       | sharePercent |
      | swedish-person        | 40           |
      | swedish-legal-entity  | 30           |
      | foreign-person        | 20           |
      | foreign-legal-entity  | 10           |
    And all company shares should be allocated on accept offer page
    And client selects paper signature and completes accept offer shareholder flow
    Then accept offer shareholder variant flow should complete successfully
    When admin sends agreement for shareholder variant order
    Then send agreement shareholder variant flow should complete successfully
