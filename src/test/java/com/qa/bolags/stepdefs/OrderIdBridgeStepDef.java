package com.qa.bolags.stepdefs;

import com.qa.bolags.baseTest.BaseTest;
import com.qa.bolags.constants.LiquidationOrderIdContext;
import com.qa.bolags.pages.AdminPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import org.testng.Assert;

/**
 * Bridges liquidation {@code POST saveInitial} order id into admin generic order detail navigation.
 * Kept in a dedicated class so Cucumber always discovers these steps (same glue package as other step defs).
 */
public class OrderIdBridgeStepDef extends BaseTest {

    @Given("the liquidation POST saveInitial order id is available in test context")
    public void theLiquidationPostSaveInitialOrderIdIsAvailableInTestContext() {
        if (LiquidationOrderIdContext.getCapturedOrderIdOrNull() != null) {
            return;
        }
        if (!System.getProperty("adminOrderDetailUrl", "").trim().isEmpty()
                || !System.getProperty("adminOrderDetailPath", "").trim().isEmpty()) {
            return;
        }
        Assert.fail(
                "No order id from POST saveInitial. Run Scenario 'Verify Liquidation Flow' before this scenario in the "
                        + "same Maven test (features: liquidationflow then adminflow), or set -DadminOrderDetailPath=/app/genericOrder/list/<id>.");
    }

    @When("User navigates to generic order detail for the stored order id")
    public void userNavigatesToGenericOrderDetailForTheStoredOrderId() {
        AdminPage page = new AdminPage(BaseTest.driver);
        page.openGenericOrderDetail(page.getResolvedGenericOrderDetailUrl());
    }
}
