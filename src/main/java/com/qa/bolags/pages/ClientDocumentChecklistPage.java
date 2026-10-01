package com.qa.bolags.pages;

import com.qa.bolags.constants.Constants;
import com.qa.bolags.constants.ClientDocumentTokenContext;
import com.qa.bolags.constants.LiquidationOrderIdContext;
import com.qa.bolags.utility.UploadFixtures;
import com.qa.bolags.utility.OrderSiFileWriter;
import com.qa.bolags.constants.QaServerCredentials;
import com.qa.bolags.utility.TestUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import java.awt.GraphicsEnvironment;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
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
        String filePath = UploadFixtures.defaultPdf();
        WebDriverWait wait = new WebDriverWait(driver, CHECKLIST_WAIT);

        WebElement uploadButton = firstDisplayed(driver.findElements(UPLOAD_FILE_BUTTON));
        if (uploadButton != null) {
            filePath = filePathForCategory(uploadButton, filePath);
            scrollIntoViewQuiet(uploadButton);
            clickElementQuiet(uploadButton);
        }

        int mentionsBefore = uploadedFileMentions();
        WebElement fileInput = wait.until(ExpectedConditions.presenceOfElementLocated(FILE_INPUT));
        sendFileToInput(fileInput, filePath);
        waitForUploadConfirmation(mentionsBefore);
        LOG.info("Uploaded document from checklist: {}", filePath);
    }

    /**
     * Uploads the suite default file ({@code ABC.pdf}) once for every visible checklist category.
     */
    public void uploadOneDocumentPerChecklistCategory() {
        String filePath = UploadFixtures.defaultPdf();
        int expected = Math.max(displayedUploadButtons().size(), 1);
        int uploaded = 0;

        if (displayedUploadButtons().isEmpty()) {
            uploadFirstAvailableDocument();
            return;
        }

        for (int i = 0; i < expected; i++) {
            boolean uploadedThisCategory = false;
            for (int attempt = 0; attempt < 3 && !uploadedThisCategory; attempt++) {
                try {
                    uploadedThisCategory = uploadDocumentForCategoryIndex(i, filePath);
                    if (uploadedThisCategory) {
                        uploaded++;
                    }
                } catch (StaleElementReferenceException e) {
                    LOG.debug("Checklist upload retry after stale element (category {}, attempt {}): {}",
                            i, attempt + 1, e.getMessage());
                }
            }
            if (!uploadedThisCategory && displayedUploadButtons().isEmpty()) {
                break;
            }
        }

        Assert.assertTrue(uploaded >= 1, "Expected at least one document upload across checklist categories");
        LOG.info("Uploaded one document for {} checklist categories", uploaded);
    }

    private boolean uploadDocumentForCategoryIndex(int index, String filePath) {
        int mentionsBefore = uploadedFileMentions();
        List<WebElement> remaining = displayedUploadButtons();
        if (!remaining.isEmpty()) {
            int buttonIndex = Math.min(index, remaining.size() - 1);
            WebElement button = remaining.get(buttonIndex);
            String category = categoryLabelFor(button);
            String categoryFilePath = filePathForCategory(button, filePath);
            scrollIntoViewQuiet(button);
            try {
                button.click();
            } catch (StaleElementReferenceException e) {
                throw e;
            } catch (Exception clickError) {
                remaining = displayedUploadButtons();
                if (remaining.isEmpty()) {
                    throw clickError;
                }
                button = remaining.get(Math.min(buttonIndex, remaining.size() - 1));
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", button);
            }
            WebElement fileInput = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.presenceOfElementLocated(FILE_INPUT));
            sendFileToInput(fileInput, categoryFilePath);
            waitForUploadConfirmation(mentionsBefore);
            LOG.info("Uploaded {} for checklist category: {}", fileName(categoryFilePath), category);
            return true;
        }

        List<WebElement> inputs = driver.findElements(FILE_INPUT);
        if (index < inputs.size()) {
            String categoryFilePath = filePathForCategory(inputs.get(index), filePath);
            sendFileToInput(inputs.get(index), categoryFilePath);
            waitForUploadConfirmation(mentionsBefore);
            LOG.info("Uploaded {} via file input index {}", fileName(categoryFilePath), index);
            return true;
        }
        return false;
    }

    private String filePathForCategory(WebElement categoryElement, String defaultPath) {
        if (!isSieCategory(categoryElement)) {
            return defaultPath;
        }
        String orderId = LiquidationOrderIdContext.getCapturedOrderIdOrNull();
        Assert.assertNotNull(orderId, "Cannot upload the SIE document: current liquidation order id is missing");
        java.nio.file.Path sieFile = OrderSiFileWriter.fileForOrder(orderId);
        Assert.assertTrue(Files.isRegularFile(sieFile),
                "Cannot upload the SIE document: generated order SI file is missing: " + sieFile);
        return sieFile.toAbsolutePath().toString();
    }

    private boolean isSieCategory(WebElement categoryElement) {
        try {
            Object result = ((JavascriptExecutor) driver).executeScript(
                    "var node = arguments[0];"
                            + "var pattern = /(^|[^a-z0-9])SIE(?:\\s*4)?(?=$|[^a-z0-9])/i;"
                            + "for (var depth = 0; node && depth < 8; depth++, node = node.parentElement) {"
                            + "  var text = (node.innerText || node.textContent || '').replace(/\\s+/g, ' ').trim();"
                            + "  if (pattern.test(text) && node.querySelectorAll('button').length <= 3) return true;"
                            + "}"
                            + "return false;",
                    categoryElement);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            LOG.debug("Could not inspect checklist row for SIE category: {}", e.getMessage());
            return false;
        }
    }

    private String fileName(String filePath) {
        return java.nio.file.Paths.get(filePath).getFileName().toString();
    }

    private List<WebElement> displayedUploadButtons() {
        List<WebElement> displayed = new ArrayList<>();
        for (WebElement element : driver.findElements(UPLOAD_FILE_BUTTON)) {
            try {
                if (element.isDisplayed()) {
                    displayed.add(element);
                }
            } catch (Exception ignored) {
                // stale — skip
            }
        }
        return displayed;
    }

    private String categoryLabelFor(WebElement uploadButton) {
        try {
            Object label = ((JavascriptExecutor) driver).executeScript(
                    "var el = arguments[0];"
                            + "var row = el.closest('li,article,section,tr,div');"
                            + "for (var i = 0; i < 8 && row; i++) {"
                            + "  var heading = row.querySelector('h2,h3,h4,p,span,label');"
                            + "  if (heading && heading.textContent && heading.textContent.trim()) {"
                            + "    return heading.textContent.trim().substring(0, 80);"
                            + "  }"
                            + "  row = row.parentElement;"
                            + "}"
                            + "return (el.innerText || 'checklist-category').trim();",
                    uploadButton);
            if (label != null && !String.valueOf(label).trim().isEmpty()) {
                return String.valueOf(label);
            }
        } catch (Exception ignored) {
            // fall through
        }
        return "checklist-category";
    }

    private void sendFileToInput(WebElement fileInput, String filePath) {
        fileInput.sendKeys(filePath);
        dismissNativeFileChooser();
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                    fileInput);
        } catch (Exception ignored) {
            // Selenium sendKeys already fires change for file inputs
        }
    }

    private void dismissNativeFileChooser() {
        if (GraphicsEnvironment.isHeadless() || Constants.isHeadless()) {
            return;
        }
        try {
            Robot keyboard = new Robot();
            keyboard.setAutoDelay(80);
            keyboard.keyPress(KeyEvent.VK_ESCAPE);
            keyboard.keyRelease(KeyEvent.VK_ESCAPE);
        } catch (Exception e) {
            LOG.debug("Native file chooser was not dismissed by Escape: {}", e.getMessage());
        }
    }

    private void waitForUploadConfirmation(int mentionsBefore) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(20)).until(d ->
                    uploadedFileMentions() > mentionsBefore
                            || uploadToastVisible(d)
                            || uploadProgressPercent(d) > 0);
        } catch (Exception e) {
            LOG.warn("Upload confirmation not observed after attaching file: {}", e.getMessage());
        }
        waitForLoad();
    }

    private boolean uploadToastVisible(WebDriver d) {
        for (WebElement el : d.findElements(UPLOAD_TOAST)) {
            try {
                if (el.isDisplayed()) {
                    return true;
                }
            } catch (Exception ignored) {
                // stale
            }
        }
        String src = d.getPageSource();
        return src.contains("Upload complete") || src.contains("Uppladdning klar");
    }

    private int uploadedFileMentions() {
        String src = driver.getPageSource();
        String orderId = LiquidationOrderIdContext.getCapturedOrderIdOrNull();
        String siFile = orderId == null ? "" : "order-" + orderId + ".si";
        return countOccurrences(src, "ABC.pdf")
            + countOccurrences(src, "review-wizard-12p")
            + (siFile.isEmpty() ? 0 : countOccurrences(src, siFile));
    }

    private int countOccurrences(String src, String token) {
        int count = 0;
        int from = 0;
        while (from < src.length()) {
            int at = src.indexOf(token, from);
            if (at < 0) {
                break;
            }
            count++;
            from = at + token.length();
        }
        return count;
    }

    private int uploadProgressPercent(WebDriver d) {
        try {
            Object value = ((JavascriptExecutor) d).executeScript(
                    "var t = document.body ? document.body.innerText : '';"
                            + "var m = t.match(/([1-9]\\d*)\\s*%\\s*(approved|godkända)/i);"
                            + "return m ? parseInt(m[1], 10) : 0;");
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
        } catch (Exception ignored) {
            // fall through
        }
        return 0;
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
            wait.until(d -> uploadToastVisible(d)
                    || uploadedFileMentions() > 0
                    || uploadProgressPercent(d) > 0);
        } catch (Exception e) {
            LOG.warn("Upload confirmation not observed on checklist: {}", e.getMessage());
        }
        Assert.assertTrue(
                uploadToastVisible(driver) || uploadedFileMentions() > 0 || uploadProgressPercent(driver) > 0,
                "Expected upload confirmation (toast, ABC.pdf, or progress > 0%) on checklist");
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

}
