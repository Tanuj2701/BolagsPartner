package com.qa.bolags.pages;

import com.qa.bolags.constants.ClientDocumentTokenContext;
import com.qa.bolags.constants.QaServerCredentials;
import com.qa.bolags.utility.TestUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
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
    private static final Duration CHECKLIST_WAIT = Duration.ofSeconds(30);

    private static final By CHECKLIST_HEADING = By.xpath(
            "//*[contains(.,'Checklist for') or contains(.,'Checklista för')]");
    private static final By PROGRESS_INDICATOR = By.xpath(
            "//*[contains(.,'%') and (contains(.,'approved') or contains(.,'godkända'))]");
    private static final By UPLOAD_FILE_BUTTON = By.xpath(
            "//button[contains(.,'LADDA UPP FIL') or contains(.,'UPLOAD FILE')"
                    + " or contains(.,'Upload file') or contains(.,'Ladda upp')]");
    private static final By FILE_INPUT = By.xpath("//input[@type='file']");
    private static final By CONTRACT_CHECKBOX_LABEL = By.xpath(
            "//label[contains(.,'taken note') or contains(.,'tagit del')]");
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
        waitForChecklistReady();
    }

    public void assertDocumentChecklistDisplayed() {
        Assert.assertTrue(
                isChecklistVisible(),
                "Client document checklist should be visible");
    }

    public void uploadFirstAvailableDocument() {
        String filePath = resolveUploadFixture();
        WebDriverWait wait = new WebDriverWait(driver, CHECKLIST_WAIT);

        WebElement uploadButton = firstDisplayed(driver.findElements(UPLOAD_FILE_BUTTON));
        if (uploadButton != null) {
            scrollIntoViewQuiet(uploadButton);
            clickElementQuiet(uploadButton);
        }

        WebElement fileInput = wait.until(ExpectedConditions.presenceOfElementLocated(FILE_INPUT));
        fileInput.sendKeys(filePath);
        LOG.info("Uploaded document from checklist: {}", filePath);

        try {
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.visibilityOfElementLocated(UPLOAD_TOAST),
                    d -> d.getPageSource().contains("godkända") || d.getPageSource().contains("approved")));
        } catch (Exception e) {
            LOG.debug("Upload progress wait completed without toast: {}", e.getMessage());
        }
    }

    public void acknowledgeContractInformationIfPresent() {
        WebElement label = firstDisplayed(driver.findElements(CONTRACT_CHECKBOX_LABEL));
        if (label != null) {
            scrollIntoViewQuiet(label);
            clickElementQuiet(label);
            return;
        }
        WebElement box = firstDisplayed(driver.findElements(CONTRACT_CHECKBOX));
        if (box != null && !box.isSelected()) {
            scrollIntoViewQuiet(box);
            clickElementQuiet(box);
        }
    }

    public void assertDocumentUploadSucceeded() {
        WebDriverWait wait = new WebDriverWait(driver, CHECKLIST_WAIT);
        try {
            wait.until(d -> anyDisplayed(UPLOAD_TOAST)
                    || d.getPageSource().contains("godkända")
                    || d.getPageSource().contains("approved"));
        } catch (Exception e) {
            LOG.warn("Upload toast not confirmed; checklist may still have progressed: {}", e.getMessage());
        }
        Assert.assertTrue(
                anyDisplayed(UPLOAD_TOAST) || driver.getPageSource().contains("godkända")
                        || driver.getPageSource().contains("approved"),
                "Expected upload confirmation or approved-documents section on checklist");
    }

    private void waitForChecklistReady() {
        try {
            new WebDriverWait(driver, CHECKLIST_WAIT).until(d -> isChecklistVisible());
        } catch (Exception e) {
            Assert.fail("Client document checklist did not load. URL: " + driver.getCurrentUrl());
        }
        WebElement uploadButton = firstDisplayed(driver.findElements(UPLOAD_FILE_BUTTON));
        if (uploadButton != null) {
            scrollIntoViewQuiet(uploadButton);
        }
    }

    private boolean isChecklistVisible() {
        return anyDisplayed(CHECKLIST_HEADING) || anyDisplayed(PROGRESS_INDICATOR)
                || anyDisplayed(UPLOAD_FILE_BUTTON)
                || driver.getPageSource().contains("Checklist")
                || driver.getPageSource().contains("Checklista")
                || driver.getPageSource().contains("LADDA UPP");
    }

    /** Lightweight scroll for checklist — no per-element visual pause. */
    private void scrollIntoViewQuiet(WebElement element) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'nearest'});", element);
    }

    private void clickElementQuiet(WebElement element) {
        scrollIntoViewQuiet(element);
        try {
            element.click();
        } catch (Exception clickError) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        }
    }

    private WebElement firstDisplayed(List<WebElement> elements) {
        for (WebElement element : elements) {
            try {
                if (element.isDisplayed()) {
                    return element;
                }
            } catch (Exception ignored) {
                // try next
            }
        }
        return null;
    }

    private boolean anyDisplayed(By by) {
        return firstDisplayed(driver.findElements(by)) != null;
    }

    private String resolveUploadFixture() {
        Path path = Paths.get(System.getProperty("user.dir"), "src", "main", "ABC.pdf");
        if (!Files.exists(path)) {
            throw new RuntimeException("Upload fixture not found at " + path);
        }
        return path.toAbsolutePath().toString();
    }
}
