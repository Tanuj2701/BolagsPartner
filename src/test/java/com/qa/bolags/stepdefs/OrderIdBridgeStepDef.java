package com.qa.bolags.stepdefs;

import com.qa.bolags.baseTest.BaseTest;
import com.qa.bolags.constants.LiquidationOrderIdContext;
import com.qa.bolags.pages.AdminPage;
import com.qa.bolags.utility.LiquidationOrderBootstrap;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;

/**
 * Bridges liquidation {@code POST saveInitial} order id into admin generic order detail navigation.
 * Kept in a dedicated class so Cucumber always discovers these steps (same glue package as other step defs).
 */
public class OrderIdBridgeStepDef extends BaseTest {

    @Given("the liquidation POST saveInitial order id is available in test context")
    public void theLiquidationPostSaveInitialOrderIdIsAvailableInTestContext() {
        LiquidationOrderBootstrap.ensureSaveInitialOrderIdCaptured(BaseTest.driver);
    }

    @When("User navigates to generic order detail for the stored order id")
    public void userNavigatesToGenericOrderDetailForTheStoredOrderId() {
        AdminPage page = new AdminPage(BaseTest.driver);
        page.openGenericOrderDetail(page.getResolvedGenericOrderDetailUrl());
    }
}
