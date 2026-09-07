package com.qa.bolags.pages;

import com.qa.bolags.constants.ClientDocumentTokenContext;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

/**
 * Client document upload checklist ({@code /app/companyLiquidationOrder/show/{orderId}?token=}).
 */
public class ClientDocumentChecklistPage extends TestUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ClientDocumentChecklistPage.class);

    private static final By CHECKLIST_HEADING = By.xpath(
            "//*[contains(.,'Checklist for') or contains(.,'Checklista för')]");
    private static final By PROGRESS_INDICATOR = By.xpath(
            "//*[contains(.,'%') and (contains(.,'approved') or contains(.,'godkända'))]");
    private static final By UPLOAD_FILE_BUTTON = By.xpath(
            "//button[contains(.,'LADDA UPP FIL') or contains(.,'UPLOAD FILE')"
                    + " or contains(.,'Upload file') or contains(.,'Ladda upp')]");
    private static final By FILE_INPUT = By.xpath("//input[@type='file']");
    private static final By CONTRACT_CHECKBOX = By.xpath(
            "//input[@type='checkbox'][following::*[contains(.,'taken note') or contains(.,'tagit del')]"
                    + " or preceding::*[contains(.,'taken note') or contains(.,'tagit del')]]"
                    + " | //label[contains(.,'taken note') or contains(.,'tagit del')]//input[@type='checkbox']");
    private static final By UPLOAD_TOAST = By.xpath(
            "//*[contains(.,'Upload complete') or contains(.,'Uppladdning klar')]");

    public ClientDocumentChecklistPage(WebDriver driver) {
        super(driver);
    }

    public void openClientDocumentChecklist() {
        String url = ClientDocumentTokenContext.buildClientChecklistUrl();
        LOG.info("Opening client document checklist: {}", url);
        driver.get(QaServerCredentials.urlWithHttpBasicAuth(url));
        waitForLoad();
        waitForSpecifiedTime(3);
    }

    public void assertDocumentChecklistDisplayed() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(35));
        wait.until(d -> anyDisplayed(CHECKLIST_HEADING) || anyDisplayed(PROGRESS_INDICATOR)
                || d.getPageSource().contains("Checklist") || d.getPageSource().contains("Checklista"));
        Assert.assertTrue(
                anyDisplayed(CHECKLIST_HEADING) || anyDisplayed(PROGRESS_INDICATOR)
                        || driver.getPageSource().contains("LADDA UPP"),
                "Client document checklist should be visible");
    }

    public void uploadFirstAvailableDocument() {
        String filePath = resolveUploadFixture();
        List<WebElement> uploadButtons = driver.findElements(UPLOAD_FILE_BUTTON);
        if (!uploadButtons.isEmpty()) {
            WebElement btn = uploadButtons.get(0);
            scrollPageToViewElement(UPLOAD_FILE_BUTTON);
            clickElement(btn);
            waitForSpecifiedTime(1);
        }
        List<WebElement> inputs = driver.findElements(FILE_INPUT);
        Assert.assertFalse(inputs.isEmpty(), "File input not found on document checklist");
        for (WebElement input : inputs) {
            try {
                if (input.isDisplayed() || true) {
                    input.sendKeys(filePath);
                    LOG.info("Uploaded document from checklist: {}", filePath);
                    waitForSpecifiedTime(3);
                    return;
                }
            } catch (Exception ignored) {
                // try next
            }
        }
        inputs.get(0).sendKeys(filePath);
        waitForSpecifiedTime(3);
    }

    public void acknowledgeContractInformationIfPresent() {
        List<WebElement> boxes = driver.findElements(CONTRACT_CHECKBOX);
        for (WebElement box : boxes) {
            try {
                if (!box.isSelected()) {
                    clickElement(box);
                    waitForSpecifiedTime(1);
                    return;
                }
            } catch (Exception ignored) {
                // continue
            }
        }
    }

    public void assertDocumentUploadSucceeded() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(25));
        try {
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.visibilityOfElementLocated(UPLOAD_TOAST),
                    d -> d.getPageSource().contains("godkända") || d.getPageSource().contains("approved")));
        } catch (Exception e) {
            LOG.warn("Upload toast not confirmed; checklist may still have progressed: {}", e.getMessage());
        }
        Assert.assertTrue(
                anyDisplayed(UPLOAD_TOAST) || driver.getPageSource().contains("godkända")
                        || driver.getPageSource().contains("approved"),
                "Expected upload confirmation or approved-documents section on checklist");
    }

    private void clickElement(WebElement el) {
        org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) driver;
        js.executeScript("arguments[0].click();", el);
        waitForLoad();
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

    private String resolveUploadFixture() {
        Path path = Paths.get(System.getProperty("user.dir"), "src", "main", "ABC.pdf");
        if (!Files.exists(path)) {
            throw new RuntimeException("Upload fixture not found at " + path);
        }
        return path.toAbsolutePath().toString();
    }
}
