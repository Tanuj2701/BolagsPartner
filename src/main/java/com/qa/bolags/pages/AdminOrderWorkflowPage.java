package com.qa.bolags.pages;

import com.qa.bolags.constants.LiquidationOrderIdContext;
import com.qa.bolags.constants.QaServerCredentials;
import com.qa.bolags.utility.TestUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import java.time.Duration;
import java.util.List;

/**
 * Admin post-agreement workflow: document review wizard, payment, board change, Bolagsverket, completion.
 */
public class AdminOrderWorkflowPage extends TestUtil {

    private static final Logger LOG = LoggerFactory.getLogger(AdminOrderWorkflowPage.class);
    private static final String GENERIC_ORDER_BASE = "https://qa.bolagspartner.se/app/genericOrder/list/";

    private final By manageOrderTab = By.xpath(
            "//button[contains(., 'Manage order') or contains(., 'Hantera order') or contains(., 'Manage Order')]");
    private final By reviewWizardsTab = By.xpath(
            "//button[contains(., 'Review Wizards') or contains(., 'Granskning')]");
    private final By reviewWizardContent = By.xpath(
            "//*[contains(., 'Internal comment') or contains(., 'Intern kommentar')"
                    + " or contains(., 'Message to customer') or contains(., 'Meddelande till kund')]");
    private final By approveDocStatusOption = By.xpath(
            "//option[contains(., 'Approved') or contains(., 'Godkänd')]"
                    + " | //*[contains(@class,'option') and (contains(., 'Approved') or contains(., 'Godkänd'))]");

    public AdminOrderWorkflowPage(WebDriver driver) {
        super(driver);
    }

    public void openStoredOrderDetail() {
        String orderId = LiquidationOrderIdContext.getCapturedOrderIdOrNull();
        if (orderId == null) {
            orderId = System.getProperty("workflow.orderId", "").trim();
        }
        Assert.assertNotNull(orderId, "Order id required — run liquidation E2E setup or set -Dworkflow.orderId");
        String url = GENERIC_ORDER_BASE + orderId;
        driver.get(QaServerCredentials.urlWithHttpBasicAuth(url));
        waitForLoad();
        waitForSpecifiedTime(2);
    }

    public void openManageOrderTab() {
        clickTabIfPresent(manageOrderTab);
    }

    public void openReviewWizardsTab() {
        clickTabIfPresent(reviewWizardsTab);
        waitForSpecifiedTime(2);
    }

    public void assertReviewWizardDisplayed() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        wait.until(d -> anyDisplayed(reviewWizardContent) || anyDisplayed(reviewWizardsTab));
        Assert.assertTrue(anyDisplayed(reviewWizardContent) || driver.getPageSource().contains("Granskning"),
                "Review Wizards tab content should be visible");
    }

    public void approveFirstDocumentInReviewWizard() {
        openReviewWizardsTab();
        List<WebElement> selects = driver.findElements(By.tagName("select"));
        for (WebElement select : selects) {
            try {
                if (select.isDisplayed()) {
                    org.openqa.selenium.support.ui.Select s = new org.openqa.selenium.support.ui.Select(select);
                    for (WebElement opt : s.getOptions()) {
                        String text = opt.getText();
                        if (text.contains("Approved") || text.contains("Godkänd")) {
                            s.selectByVisibleText(text);
                            waitForSpecifiedTime(2);
                            LOG.info("Set document status to Approved in review wizard");
                            return;
                        }
                    }
                }
            } catch (Exception ignored) {
                // continue
            }
        }
        LOG.warn("No review-wizard status dropdown found — wizard may be empty for this order state");
    }

    public void clickManageOrderButton(String... labelsEnSv) {
        openManageOrderTab();
        By locator = buildButtonLocator(labelsEnSv);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(35));
        scrollPageToViewElement(locator);
        wait.until(ExpectedConditions.elementToBeClickable(locator));
        clickByJS(locator);
        waitForLoad();
        waitForSpecifiedTime(3);
        LOG.info("Clicked manage-order button matching: {}", String.join(" / ", labelsEnSv));
    }

    public void clickReadyForReview() {
        clickManageOrderButton("Ready for review", "Klar för granskning", "Ready for Review");
    }

    public void clickDocumentsReceivedToday() {
        clickManageOrderButton("Documents received today", "Dokument mottagna idag", "Documents Received");
    }

    public void clickReviewComplete() {
        clickManageOrderButton("Review complete", "Granskning klar");
    }

    public void clickReadyForBoardChange() {
        clickManageOrderButton("Ready for board change", "Klar för styrelseändring");
    }

    public void clickBoardChangeSentToBolagsverket() {
        clickManageOrderButton(
                "Board Change sent to the Swedish Companies Registration office",
                "Styrelseändring skickad till Bolagsverket",
                "Board change submitted to Bolagsverket");
    }

    public void clickRegistrationComplete() {
        clickManageOrderButton("Registration Complete", "Registrering klar");
    }

    public void clickReadyForLiquidation() {
        clickManageOrderButton("Ready for Liquidation", "Klar för likvidation");
    }

    public void clickLiquidationSentToBolagsverket() {
        clickManageOrderButton(
                "Liquidation sent to Bolagsverket",
                "Likvidation skickad till Bolagsverket",
                "Liquidation submitted to Bolagsverket");
    }

    public void clickReadyForFinalReport() {
        clickManageOrderButton("Ready for final report", "Klar för slutrapport");
    }

    public void clickFinalReportSentToBolagsverket() {
        clickManageOrderButton(
                "Final report to Swedish Companies Registration Office",
                "Slutrapport till Bolagsverket",
                "Final report to Swedish Companies Registration Office");
    }

    public void clickLiquidationComplete() {
        clickManageOrderButton("Liquidation complete", "Likvidation klar");
    }

    public void assertManageOrderButtonVisible(String... labelsEnSv) {
        openManageOrderTab();
        By locator = buildButtonLocator(labelsEnSv);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        Assert.assertTrue(anyDisplayed(locator), "Expected button: " + String.join(" / ", labelsEnSv));
    }

    public void assertOrderStatusIndicatorPresent(String statusFragment) {
        waitForSpecifiedTime(2);
        Assert.assertTrue(
                driver.getPageSource().contains(statusFragment),
                "Expected order page to reference status fragment: " + statusFragment);
    }

    private By buildButtonLocator(String... labels) {
        StringBuilder xpath = new StringBuilder("//button[@type='button' and not(@disabled)][");
        for (int i = 0; i < labels.length; i++) {
            if (i > 0) {
                xpath.append(" or ");
            }
            xpath.append("contains(normalize-space(.), '").append(labels[i].replace("'", "")).append("')");
        }
        xpath.append("]");
        return By.xpath(xpath.toString());
    }

    private void clickTabIfPresent(By tab) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(25));
        wait.until(ExpectedConditions.presenceOfElementLocated(tab));
        for (WebElement el : driver.findElements(tab)) {
            try {
                if (el.isDisplayed()) {
                    scrollPageToViewElement(tab);
                    org.openqa.selenium.JavascriptExecutor js =
                            (org.openqa.selenium.JavascriptExecutor) driver;
                    js.executeScript("arguments[0].click();", el);
                    waitForSpecifiedTime(1);
                    return;
                }
            } catch (Exception ignored) {
                // retry
            }
        }
    }

    private boolean anyDisplayed(By by) {
        for (WebElement el : driver.findElements(by)) {
            try {
                if (el.isDisplayed()) {
                    return true;
                }
            } catch (Exception ignored) {
                // continue
            }
        }
        return false;
    }
}
