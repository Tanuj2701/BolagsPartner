package com.qa.bolags.pages;

import com.qa.bolags.constants.ClientDocumentTokenContext;
import com.qa.bolags.constants.QaServerCredentials;
import com.qa.bolags.utility.TestUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Client document upload checklist ({@code /app/companyLiquidationOrder/show/{orderId}?token=}).
 */
public class ClientDocumentChecklistPage extends TestUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ClientDocumentChecklistPage.class);

    private static final By CHECKLIST_HEADING = By.xpath(
            "//*[contains(.,'Checklist for') or contains(.,'Checklista för')]");
    private static final By PROGRESS_INDICATOR = By.xpath(
            "//*[contains(.,'%') and (contains(.,'approved') or contains(.,'godkända'))]");
    /** Each document type card in the unapproved checklist grid. */
    private static final By DOCUMENT_CARD = By.xpath(
            "//div[contains(@class,'grid-cols-1') and contains(@class,'xl:grid-cols-2')]"
                    + "//div[contains(@class,'flex flex-col') and contains(@class,'w-full')][.//h1]");
    /** Main card upload CTA (large yellow button — not shareholder/BankID mini buttons). */
    private static final By MAIN_CARD_UPLOAD_BUTTON = By.xpath(
            ".//button[contains(@class,'py-[15px]')][.//span["
                    + "contains(normalize-space(.), 'LADDA UPP FIL') or contains(normalize-space(.), 'UPLOAD FILE')"
                    + " or contains(normalize-space(.), 'LADDA UPP KOPIA') or contains(normalize-space(.), 'UPLOAD COPY')"
                    + "]]");
    private static final By FILE_INPUT = By.xpath("//input[@type='file']");
    private static final By CONTRACT_CHECKBOX = By.xpath(
            "//input[@type='checkbox'][following::*[contains(.,'taken note') or contains(.,'tagit del')]"
                    + " or preceding::*[contains(.,'taken note') or contains(.,'tagit del')]]"
                    + " | //label[contains(.,'taken note') or contains(.,'tagit del')]//input[@type='checkbox']");
    private static final By UPLOAD_TOAST = By.xpath(
            "//*[contains(.,'Upload complete') or contains(.,'Upload Complete')"
                    + " or contains(.,'Uppladdning klar') or contains(.,'Uppladdningen är klar')]");
    private static final By UPLOAD_PROGRESS = By.cssSelector(".CircularProgressbar");

    private int lastUploadedDocumentCount;
    private final Set<String> uploadedDocumentTitles = new HashSet<>();

    public ClientDocumentChecklistPage(WebDriver driver) {
        super(driver);
    }

    public int getLastUploadedDocumentCount() {
        return lastUploadedDocumentCount;
    }

    public Set<String> getUploadedDocumentTitles() {
        return new HashSet<>(uploadedDocumentTitles);
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

    /**
     * Uploads one file per checklist document card that shows {@code LADDA UPP FIL}/{@code UPLOAD FILE}.
     * When a card only shows {@code LADDA UPP KOPIA}/{@code UPLOAD COPY} (first upload for that type),
     * uploads once so every document type receives a file.
     */
    public int uploadAllDocumentsWithUploadButtons() {
        lastUploadedDocumentCount = 0;
        uploadedDocumentTitles.clear();
        String pdfPath = resolveUploadFixture();

        scrollChecklistToLoadAllCards();
        logPendingUploadCards();

        final int maxPasses = 40;
        for (int pass = 1; pass <= maxPasses; pass++) {
            DocumentCardUploadTarget target = findNextCardNeedingUpload();
            if (target == null) {
                LOG.info("No more document cards to upload after pass {}", pass - 1);
                break;
            }

            LOG.info("Pass {} — uploading to document card [{}] via button [{}]",
                    pass, target.documentTitle, target.buttonLabel);

            scrollIntoView(target.uploadButton);
            clickElement(target.uploadButton);
            waitForSpecifiedTime(1);

            WebElement fileInput = findActiveFileInput();
            Assert.assertNotNull(fileInput, "File input not found for document card: " + target.documentTitle);
            fileInput.sendKeys(pdfPath);
            waitForUploadToComplete();

            uploadedDocumentTitles.add(target.documentTitle);
            lastUploadedDocumentCount++;
            waitForChecklistRefresh();
        }

        logUploadSummary();
        Assert.assertTrue(lastUploadedDocumentCount > 0,
                "No checklist document cards with LADDA UPP FIL/UPLOAD FILE (or KOPIA for first upload) were uploaded");
        return lastUploadedDocumentCount;
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
        List<String> unprocessed = new ArrayList<>();
        for (WebElement card : driver.findElements(DOCUMENT_CARD)) {
            try {
                if (!card.isDisplayed()) {
                    continue;
                }
                String title = getDocumentCardTitle(card);
                if (title.isEmpty() || uploadedDocumentTitles.contains(title)) {
                    continue;
                }
                WebElement btn = findUploadFileButtonInCard(card);
                if (btn != null) {
                    unprocessed.add(title + " [" + safeElementText(btn) + "]");
                }
            } catch (Exception ignored) {
                // continue
            }
        }

        Assert.assertTrue(lastUploadedDocumentCount > 0,
                "Expected at least one checklist document card to be uploaded");
        Assert.assertTrue(unprocessed.isEmpty(),
                "Expected uploads on every document card with LADDA UPP FIL/UPLOAD FILE (or KOPIA for first file). "
                        + "Not uploaded: " + unprocessed + ". Uploaded: " + uploadedDocumentTitles);

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        try {
            wait.until(d -> d.getPageSource().contains("godkända") || d.getPageSource().contains("approved")
                    || d.getPageSource().contains("Dokument mottagna") || d.getPageSource().contains("mottagna"));
        } catch (TimeoutException e) {
            LOG.warn("Approved/waiting-for-review indicators not confirmed: {}", e.getMessage());
        }
    }

    private DocumentCardUploadTarget findNextCardNeedingUpload() {
        for (WebElement card : findDocumentCards()) {
            String title = getDocumentCardTitle(card);
            if (title.isEmpty() || uploadedDocumentTitles.contains(title)) {
                continue;
            }
            WebElement uploadButton = findUploadFileButtonInCard(card);
            if (uploadButton == null) {
                continue;
            }
            return new DocumentCardUploadTarget(title, safeElementText(uploadButton), uploadButton);
        }
        return null;
    }

    /**
     * Prefer {@code LADDA UPP FIL}/{@code UPLOAD FILE}; fall back to {@code LADDA UPP KOPIA}/{@code UPLOAD COPY}
     * for the first file on a document type (app shows KOPIA until one file exists).
     */
    private WebElement findUploadFileButtonInCard(WebElement card) {
        WebElement filButton = null;
        WebElement kopiaButton = null;
        for (WebElement button : card.findElements(MAIN_CARD_UPLOAD_BUTTON)) {
            try {
                if (!button.isDisplayed() || !button.isEnabled()) {
                    continue;
                }
                String label = normalizeLabel(safeElementText(button));
                if (isUploadFileLabel(label)) {
                    filButton = button;
                } else if (isUploadCopyLabel(label)) {
                    kopiaButton = button;
                }
            } catch (StaleElementReferenceException ignored) {
                // re-query on next pass
            }
        }
        return filButton != null ? filButton : kopiaButton;
    }

    private boolean isUploadFileLabel(String label) {
        return label.contains("LADDA UPP FIL") || label.contains("UPLOAD FILE");
    }

    private boolean isUploadCopyLabel(String label) {
        return label.contains("LADDA UPP KOPIA") || label.contains("UPLOAD COPY");
    }

    private List<WebElement> findDocumentCards() {
        List<WebElement> cards = new ArrayList<>();
        for (WebElement card : driver.findElements(DOCUMENT_CARD)) {
            try {
                if (card.isDisplayed() && findUploadFileButtonInCard(card) != null) {
                    cards.add(card);
                }
            } catch (StaleElementReferenceException ignored) {
                // skip stale card
            }
        }
        return cards;
    }

    private List<String> listDocumentCardsWithUploadFileButton() {
        List<String> pending = new ArrayList<>();
        for (WebElement card : findDocumentCards()) {
            String title = getDocumentCardTitle(card);
            if (!title.isEmpty() && !uploadedDocumentTitles.contains(title)) {
                WebElement btn = findUploadFileButtonInCard(card);
                if (btn != null && isUploadFileLabel(normalizeLabel(safeElementText(btn)))) {
                    pending.add(title);
                }
            }
        }
        return pending;
    }

    private String getDocumentCardTitle(WebElement card) {
        try {
            WebElement heading = card.findElement(By.xpath(".//h1[1]"));
            return heading.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    private void logPendingUploadCards() {
        List<String> titles = new ArrayList<>();
        for (WebElement card : driver.findElements(DOCUMENT_CARD)) {
            try {
                if (!card.isDisplayed()) {
                    continue;
                }
                String title = getDocumentCardTitle(card);
                WebElement btn = findUploadFileButtonInCard(card);
                if (!title.isEmpty() && btn != null) {
                    titles.add(title + " -> " + safeElementText(btn));
                }
            } catch (Exception ignored) {
                // continue
            }
        }
        LOG.info("Checklist document cards with upload button ({}): {}", titles.size(), titles);
    }

    private void logUploadSummary() {
        List<String> remainingFil = listDocumentCardsWithUploadFileButton();
        LOG.info("Checklist upload summary — uploaded: {} cards {}, remaining FIL/UPLOAD FILE cards: {}",
                lastUploadedDocumentCount, uploadedDocumentTitles, remainingFil);
    }

    private void scrollChecklistToLoadAllCards() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
        waitForSpecifiedTime(2);
        js.executeScript("window.scrollTo(0, 0);");
        waitForSpecifiedTime(1);
    }

    private void waitForChecklistRefresh() {
        waitForSpecifiedTime(3);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        try {
            wait.until(ExpectedConditions.invisibilityOfElementLocated(UPLOAD_PROGRESS));
        } catch (Exception ignored) {
            // spinner may not appear for fast uploads
        }
        waitForSpecifiedTime(2);
    }

    private WebElement findActiveFileInput() {
        List<WebElement> inputs = driver.findElements(FILE_INPUT);
        return inputs.isEmpty() ? null : inputs.get(0);
    }

    private void waitForUploadToComplete() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(90));
        try {
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.visibilityOfElementLocated(UPLOAD_TOAST),
                    ExpectedConditions.invisibilityOfElementLocated(UPLOAD_PROGRESS)));
        } catch (TimeoutException e) {
            LOG.warn("Upload completion not confirmed within timeout: {}", e.getMessage());
        }
        waitForSpecifiedTime(2);
        clearFileInputs();
    }

    private void clearFileInputs() {
        for (WebElement input : driver.findElements(FILE_INPUT)) {
            try {
                ((JavascriptExecutor) driver).executeScript("arguments[0].value = '';", input);
            } catch (Exception ignored) {
                // continue
            }
        }
    }

    private String normalizeLabel(String label) {
        return label == null ? "" : label.toUpperCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private void scrollIntoView(WebElement el) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'nearest'});", el);
        waitForSpecifiedTime(1);
    }

    private String safeElementText(WebElement el) {
        try {
            return el.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    private void clickElement(WebElement el) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
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

    private static final class DocumentCardUploadTarget {
        final String documentTitle;
        final String buttonLabel;
        final WebElement uploadButton;

        DocumentCardUploadTarget(String documentTitle, String buttonLabel, WebElement uploadButton) {
            this.documentTitle = documentTitle;
            this.buttonLabel = buttonLabel;
            this.uploadButton = uploadButton;
        }
    }
}
