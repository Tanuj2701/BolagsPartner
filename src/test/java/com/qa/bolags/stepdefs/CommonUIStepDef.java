package com.qa.bolags.stepdefs;

import com.qa.bolags.baseTest.BaseTest;
import com.qa.bolags.utility.TestUtil;
import io.cucumber.java.en.When;

/**
 * Shared UI actions reusable across feature files.
 */
public class CommonUIStepDef extends BaseTest {

    @When("User scrolls page to top")
    public void userScrollsPageToTop() {
        new TestUtil(BaseTest.driver);
        TestUtil.scrollPageToTop();
    }
}
