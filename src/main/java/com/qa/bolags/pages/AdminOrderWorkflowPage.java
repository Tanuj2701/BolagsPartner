package com.qa.bolags.pages;

import com.qa.bolags.constants.LiquidationOrderIdContext;
import com.qa.bolags.constants.QaServerCredentials;
import com.qa.bolags.utility.TestUtil;
import com.qa.bolags.utility.UploadFixtures;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Admin post-agreement workflow: document review wizard, payment, board change, Bolagsverket, completion.
 */
public class AdminOrderWorkflowPage extends TestUtil {

    private static final Logger LOG = LoggerFactory.getLogger(AdminOrderWorkflowPage.class);
    private static final String GENERIC_ORDER_BASE = "https://qa.bolagspartner.se/app/genericOrder/list/";
    private static final int TAB_WAIT_SECONDS = 60;
    private static final int ACTION_WAIT_SECONDS = 60;

    /** Matches {@link AdminPage} manage-order tab (SV "Hantera beställning" + role=tab). */
    private final By manageOrderTab = By.xpath(
            "//button[contains(., 'Hantera best\u00E4llning') or contains(., 'Hantera order')"
                    + " or contains(., 'Manage order') or contains(., 'Manage Order')]"
                    + " | //*[@role='tab'][contains(., 'Hantera') or contains(., 'Manage')]");
    private final By reviewWizardsTab = By.xpath(
            "//*[@role='tab'][contains(., 'Review Wizards') or contains(., 'Review wizard')"
                    + " or contains(., 'Review Upload') or contains(., 'Granska dokument')"
                    + " or normalize-space()='Granskning']"
                    + " | //button[normalize-space()='Review Upload' or contains(., 'Granska dokument')"
                    + " or normalize-space()='Review Wizards' or normalize-space()='Review wizard'"
                    + " or normalize-space()='Granskning']");
    private final By orderDetailShell = By.xpath("//main | //article | //*[@data-testid='order-detail']");
    private final By reviewWizardContent = By.xpath(
            "//*[contains(., 'Internal comment') or contains(., 'Intern kommentar')"
                    + " or contains(., 'Message to customer') or contains(., 'Meddelande till kund')"
                    + " or contains(., 'Granska dokument') or contains(., 'Dokumentatstatus')"
                    + " or contains(., 'Skriv kommentar')]");
    private final By approveDocStatusOption = By.xpath(
            "//option[contains(., 'Approved') or contains(., 'Godkänd')]"
                    + " | //*[contains(@class,'option') and (contains(., 'Approved') or contains(., 'Godkänd'))]");
    /**
     * Review wizard section row: chevron-right image + {@code Dokument} label
     * ({@code div.flex.items-center} with {@code img[alt=Chevron]} and {@code span.font-medium}).
     */
    private final By reviewWizardDokumentSectionRow = By.xpath(
            "//div[contains(@class,'flex') and contains(@class,'items-center')]"
                    + "[.//img[@alt='Chevron' and contains(@src,'chevron-right')]"
                    + " and .//span[contains(@class,'font-medium') and (normalize-space()='Dokument'"
                    + " or normalize-space()='Document')]]");
    private final By reviewWizardDokumentChevron = By.xpath(
            "//div[contains(@class,'flex') and contains(@class,'items-center')]"
                    + "[.//span[contains(@class,'font-medium') and (normalize-space()='Dokument'"
                    + " or normalize-space()='Document')]]"
                    + "//img[@alt='Chevron' and contains(@src,'chevron-right')]");
    private final By reviewWizardDokumentLabel = By.xpath(
            "//div[contains(@class,'flex') and contains(@class,'items-center')]"
                    + "//span[contains(@class,'font-medium') and (normalize-space()='Dokument'"
                    + " or normalize-space()='Document')]");
    private boolean reviewFixturesUploaded;
    /** Set after step 70 walks Granska dokument; avoids reopening Review Upload on step 71. */
    private static boolean reviewWizardWalkCompleted;
    private static boolean postReviewWorkflowAdvanced;

    public AdminOrderWorkflowPage(WebDriver driver) {
        super(driver);
    }

    public static void resetWorkflowSessionState() {
        reviewWizardWalkCompleted = false;
        postReviewWorkflowAdvanced = false;
    }

    public void openStoredOrderDetail() {
        ensureAdminSessionForWorkflow();
        String orderId = LiquidationOrderIdContext.getCapturedOrderIdOrNull();
        if (orderId == null) {
            orderId = System.getProperty("workflow.orderId", "").trim();
        }
        Assert.assertNotNull(orderId, "Order id required — run liquidation E2E setup or set -Dworkflow.orderId");
        String url = GENERIC_ORDER_BASE + orderId;
        String authedUrl = QaServerCredentials.urlWithHttpBasicAuth(url);
        for (int attempt = 0; attempt < 5; attempt++) {
            driver.get(authedUrl);
        waitForLoad();
            waitForSpecifiedTime(1);
            if (isOnAdminLoginPage()) {
                LOG.info("Landed on admin login while opening order {} — signing in and retrying", orderId);
                new AdminPage(driver).loginToAdminPortal();
                driver.get(authedUrl);
                waitForLoad();
            }
            try {
                waitForOrderDetailShell();
                scrollToOrderDetailContent();
                return;
            } catch (TimeoutException e) {
                LOG.warn("Order detail shell not ready (attempt {}) — retrying navigation", attempt + 1);
        waitForSpecifiedTime(2);
            }
        }
        waitForOrderDetailShell();
        scrollToOrderDetailContent();
    }

    private void ensureAdminSessionForWorkflow() {
        AdminPage adminPage = new AdminPage(driver);
        if (!adminPage.isLoggedInToAdminPortal()) {
            adminPage.loginToAdminPortal();
        }
    }

    private boolean isOnAdminLoginPage() {
        String current = driver.getCurrentUrl();
        return current != null && current.contains("/auth/login");
    }

    public void openManageOrderTab() {
        clickTabIfPresent(manageOrderTab);
    }

    public void openManageOrderTabOnly() {
        openManageOrderTab();
    }

    public boolean isReviewWizardScreenActive() {
        return isOnReviewWizardScreen();
    }

    public void openReviewWizardsTab() {
        acceptUploadAlertIfPresent();
        if (!tryClickManageOrderButtonIfPresent(
                "Review Upload", "Review upload", "Granska dokument", "Review documents")) {
            clickTabIfPresent(reviewWizardsTab);
        }
        waitForReviewWizardReady(Duration.ofSeconds(10));
        scrollToReviewDocumentPanel();
        acceptUploadAlertIfPresent();
        expandDokumentSectionsInOrder();
    }

    private void waitForReviewWizardReady(Duration timeout) {
        new WebDriverWait(driver, timeout).until(d -> {
            String body = d.findElement(By.tagName("body")).getText();
            boolean reviewTitle = body.contains("Granska dokument") || body.contains("Review Upload");
            boolean documentStatus = body.contains("Dokumentatstatus") || body.contains("Document status");
            boolean pageNavigation = anyDisplayed(By.xpath(
                    "//button[normalize-space()='Nästa' or normalize-space()='Next']"));
            return reviewTitle && (documentStatus || pageNavigation || hasReviewStatusControl(d));
        });
    }

    public void assertReviewWizardDisplayed() {
        if (!isOnReviewWizardScreen()) {
            openReviewWizardsTab();
        }
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(8));
        try {
            wait.until(d -> anyDisplayed(reviewWizardContent) || hasReviewStatusControl(d));
        } catch (TimeoutException e) {
            logReviewPanelSnapshot("wizard-assert-timeout");
            logVisibleManageOrderButtons();
        }
        Assert.assertTrue(
                anyDisplayed(reviewWizardContent) || hasReviewStatusControl(driver),
                "Review Upload / Granska dokument should show document review controls");
    }

    public void approveFirstDocumentInReviewWizard() {
        if (!isOnReviewWizardScreen()) {
            openReviewWizardsTab();
        }
        waitForReviewWizardReady(Duration.ofSeconds(8));

        boolean approved = reviewAllWizardDocuments();
        if (!approved) {
            logReviewPanelSnapshot("approve-no-status-control");
            LOG.warn("No review-wizard status control found after opening Review Upload");
        }
        markOutstandingDocumentTypesIfPresent();
        acceptUploadAlertIfPresent();
        exitReviewWizardToManageOrder();
    }

    /**
     * Leaves Granska dokument / Review Upload and lands on Hantera beställning for workflow steps 72+.
     */
    public void exitReviewWizardToManageOrder() {
        if (!isOnReviewWizardScreen()) {
            openManageOrderTab();
            acceptUploadAlertIfPresent(1);
            return;
        }
        ensureReviewWizardClosed();
        if (isOnReviewWizardScreen()) {
            reloadStoredOrderAndOpenManageOrder();
        } else {
            openManageOrderTab();
        }
        acceptUploadAlertIfPresent(1);
        if (isOnReviewWizardScreen()) {
            LOG.warn("Review wizard still visible — forcing order reload");
            reloadStoredOrderAndOpenManageOrder();
        } else {
            LOG.info("Review wizard closed — continuing on Hantera beställning");
        }
    }

    /**
     * After client checklist uploads: expand Dokument, click Slå samman on every uploaded file,
     * verify the viewer, and log whether former representatives / Aktieägare are shown.
     */
    private boolean reviewAllWizardDocuments() {
        boolean any = mergeClientUploadsAcrossCategories();
        reviewWizardWalkCompleted = true;
        LOG.info("All review wizard sections navigated — saving and leaving review wizard");
        return any;
    }

    /** Set document status if needed, save, then exit Granska dokument (caller may reload manage order). */
    private void finalizeReviewWizardBeforeExit() {
        if (!reviewWizardWalkCompleted) {
            setDokumentatstatusGodkand();
        }
        saveReviewWizardChangesIfNeeded();
        clickReviewWizardStangButton();
        acceptUploadAlertIfPresent(1);
    }

    private boolean mergeClientUploadsAcrossCategories() {
        goToFirstDocumentInWizard();
        boolean any = false;
        Set<String> seen = new HashSet<String>();
        for (int i = 0; i < 50; i++) {
            acceptUploadAlertIfPresent();
            String key = currentReviewDocumentKey();
            if (key.length() > 8 && !seen.add(key)) {
                LOG.info("Review wizard wrapped to a seen category — stopping walk");
                reviewWizardWalkCompleted = true;
                finalizeReviewWizardBeforeExit();
                break;
            }
            expandDokumentSectionsInOrder();
            int merged = clickMergeOnAllUploadedDocuments();
            boolean statusSet = setDokumentatstatusGodkand();
            if (merged > 0 || statusSet) {
                any = true;
                LOG.info("Review page {} merged={} statusGodkand={}", i + 1, merged, statusSet);
            } else {
                LOG.warn("Review page {} had no merge action or Dokumentatstatus control", i + 1);
            }
            if (isLastReviewDocument()) {
                LOG.info("Last review-wizard page reached; closing after save");
                reviewWizardWalkCompleted = true;
                finalizeReviewWizardBeforeExit();
                break;
            }
            if (!clickNextDocumentInWizard()) {
                reviewWizardWalkCompleted = true;
                finalizeReviewWizardBeforeExit();
                break;
            }
        }
        return any;
    }

    /**
     * Activates the left-sidebar {@code Dokument} section in Granska dokument, then expands chevrons
     * so the Slå samman / upload table is visible.
     */
    private void expandDokumentSectionsInOrder() {
        if (isDokumentMergeListVisible()) {
            LOG.info("Dokument merge list already expanded");
            return;
        }
        if (clickReviewWizardDokumentSidebarSection()) {
            waitForMergeListBrief();
        }
        if (isDokumentMergeListVisible()) {
            return;
        }
        if (clickDokumentExpandArrowsInOrder()) {
            waitForMergeListBrief();
        }
        if (isDokumentMergeListVisible()) {
            return;
        }
        List<WebElement> toggles = findDokumentSectionToggleElements();
        int expanded = 0;
        for (int i = 0; i < toggles.size(); i++) {
            WebElement el = toggles.get(i);
            try {
                if (!el.isDisplayed()) {
                    continue;
                }
                if (isDokumentMergeListVisible()) {
                    break;
                }
                if (!shouldExpandDokumentAccordion(el)) {
                    continue;
                }
                safeClick(el);
                waitForMergeListBrief();
                expanded++;
                LOG.info("Expanded Dokument accordion {} of {}", expanded, toggles.size());
            } catch (StaleElementReferenceException ignored) {
                toggles = findDokumentSectionToggleElements();
            } catch (Exception ignored) {
                // try next control
            }
        }
        if (expanded == 0 && !isDokumentMergeListVisible()) {
            LOG.info("No Dokument expand arrow or accordion on this review category");
        }
    }

    private void waitForMergeListBrief() {
        try {
            new WebDriverWait(driver, Duration.ofMillis(900)).until(d -> isDokumentMergeListVisible());
        } catch (TimeoutException ignored) {
            // Some categories have no expandable merge list.
        }
    }

    private List<WebElement> findDokumentSectionToggleElements() {
        List<WebElement> rows = driver.findElements(reviewWizardDokumentSectionRow);
        if (!rows.isEmpty()) {
            return rows;
        }
        return driver.findElements(By.xpath(
                "//button[normalize-space()='Dokument' and (@aria-expanded='false' or not(@aria-expanded))]"
                        + " | //*[@role='button'][normalize-space()='Dokument' and (@aria-expanded='false' or not(@aria-expanded))]"
                        + " | //summary[normalize-space()='Dokument']"));
    }

    /**
     * Clicks the review wizard {@code Dokument} section: {@code img[alt=Chevron]} then the flex row /
     * label (chevron is left of the {@code span.font-medium} text).
     */
    private boolean clickReviewWizardDokumentSidebarSection() {
        if (clickEachDisplayed(reviewWizardDokumentChevron, "Dokument chevron-right")) {
            return true;
        }
        if (clickEachDisplayed(reviewWizardDokumentSectionRow, "Dokument flex row")) {
            return true;
        }
        if (clickEachDisplayed(reviewWizardDokumentLabel, "Dokument label")) {
            return true;
        }
        WebElement clickableParent = firstDisplayedClickableParentOfDokumentRow();
        if (clickableParent != null) {
            safeClick(clickableParent);
            LOG.info("Clicked Dokument section via clickable parent ({})", clickableParent.getTagName());
            return true;
        }
        return false;
    }

    private WebElement firstDisplayedClickableParentOfDokumentRow() {
        for (WebElement row : driver.findElements(reviewWizardDokumentSectionRow)) {
            try {
                if (!row.isDisplayed()) {
                    continue;
                }
                WebElement parent = row;
                for (int up = 0; up < 4; up++) {
                    parent = (WebElement) ((JavascriptExecutor) driver).executeScript(
                            "return arguments[0].parentElement;", parent);
                    if (parent == null) {
                        break;
                    }
                    String role = parent.getAttribute("role");
                    String tag = parent.getTagName();
                    if ("button".equalsIgnoreCase(tag) || "BUTTON".equals(role)
                            || parent.getAttribute("onclick") != null
                            || "true".equals(parent.getAttribute("tabindex"))) {
                        if (parent.isDisplayed()) {
                            return parent;
                        }
                    }
                }
            } catch (Exception ignored) {
                // try next row
            }
        }
        return null;
    }

    private boolean clickEachDisplayed(By locator, String labelForLog) {
        for (WebElement el : driver.findElements(locator)) {
            try {
                if (!el.isDisplayed()) {
                    continue;
                }
                safeClick(el);
                LOG.info("Clicked review wizard {} ({})", labelForLog, locator);
                return true;
            } catch (StaleElementReferenceException ignored) {
                // retry outer loop on next find
            } catch (Exception ignored) {
                // try next match
            }
        }
        return false;
    }

    /** Clicks {@code chevron-right.svg} to the left of each {@code Dokument} section label (DOM order). */
    private boolean clickDokumentExpandArrowsInOrder() {
        int clicked = 0;
        By[] arrowBesideDokument = new By[]{
                reviewWizardDokumentChevron,
                By.xpath("//span[contains(@class,'font-medium') and normalize-space()='Dokument']"
                        + "/preceding-sibling::span//img[@alt='Chevron' and contains(@src,'chevron-right')]"),
                By.xpath("//span[contains(@class,'font-medium') and normalize-space()='Document']"
                        + "/preceding-sibling::span//img[@alt='Chevron' and contains(@src,'chevron-right')]"),
                reviewWizardDokumentSectionRow
        };
        for (int loc = 0; loc < arrowBesideDokument.length; loc++) {
            List<WebElement> arrows = driver.findElements(arrowBesideDokument[loc]);
            for (int i = 0; i < arrows.size(); i++) {
                WebElement arrow = arrows.get(i);
                try {
                    if (!arrow.isDisplayed()) {
                        continue;
                    }
                    if (isDokumentMergeListVisible()) {
                        return clicked > 0;
                    }
                    safeClick(arrow);
                    waitForMergeListBrief();
                    clicked++;
                    LOG.info("Clicked Dokument chevron/row {} (locator index {})", clicked, loc + 1);
                    if (isDokumentMergeListVisible()) {
                        return true;
                    }
                } catch (StaleElementReferenceException ignored) {
                    arrows = driver.findElements(arrowBesideDokument[loc]);
                } catch (Exception ignored) {
                    // try next
                }
            }
        }
        return clicked > 0;
    }

    /** Scroll down so Dokumentatstatus, Aktieägare, and comment fields are reachable after merge. */
    private void scrollToReviewWizardActionsBelowMerge(int mergedCount) {
        if (mergedCount <= 0 && !isDokumentMergeListVisible()) {
            return;
        }
        scrollToReviewWizardLowerSections();
    }

    private void scrollToReviewWizardLowerSections() {
        String[] sectionLabels = {
                "Dokumentatstatus", "Dokumentstatus", "Aktieägare", "Standardsvar",
                "Meddelande till kund", "Intern kommentar", "Godkänd", "Mottagen"
        };
        for (int i = 0; i < sectionLabels.length; i++) {
            String label = sectionLabels[i];
            By section = By.xpath(
                    "//label[contains(., '" + label + "')]"
                            + " | //*[normalize-space()='" + label + "']"
                            + " | //*[starts-with(normalize-space(), '" + label + "')]");
            for (WebElement el : driver.findElements(section)) {
                try {
                    if (el.isDisplayed()) {
                        scrollElementIntoViewableArea(el);
                        LOG.info("Scrolled review wizard to section: {}", label);
                        return;
                    }
                } catch (Exception ignored) {
                    // try next match
                }
            }
        }
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "var nodes = document.querySelectorAll("
                            + "'.overflow-auto,.overflow-y-auto,.overflow-scroll,main,[role=main]');"
                            + "for (var i = 0; i < nodes.length; i++) {"
                            + "  var n = nodes[i];"
                            + "  if (n.scrollHeight > n.clientHeight + 60) {"
                            + "    n.scrollTop = Math.min(n.scrollTop + 360, n.scrollHeight);"
                            + "    return;"
                            + "  }"
                            + "}"
                            + "window.scrollBy(0, 360);");
            LOG.info("Scrolled review wizard panel down for post-merge actions");
        } catch (Exception e) {
            LOG.debug("Review wizard scroll down skipped: {}", e.getMessage());
        }
    }

    private boolean shouldExpandDokumentAccordion(WebElement el) {
        try {
            String expanded = el.getAttribute("aria-expanded");
            if ("true".equalsIgnoreCase(expanded)) {
                return false;
            }
            if ("false".equalsIgnoreCase(expanded)) {
                return true;
            }
        } catch (Exception ignored) {
            // fall through
        }
        return !isDokumentMergeListVisible();
    }

    private boolean isDokumentMergeListVisible() {
        if (!displayedMergeButtons().isEmpty()) {
            return true;
        }
        if (anyDisplayed(By.xpath(
                "//*[@role='dialog']//*[normalize-space()='Dokumentnamn' or normalize-space()='Document name']"
                        + " | //*[normalize-space()='Dokumentnamn' or normalize-space()='Document name']"))) {
            return true;
        }
        try {
            Object visible = ((JavascriptExecutor) driver).executeScript(
                    "function vis(el) {"
                            + "  if (!el) { return false; }"
                            + "  var r = el.getBoundingClientRect();"
                            + "  return r.width > 0 && r.height > 0 && r.bottom > 0 && r.top < window.innerHeight;"
                            + "}"
                            + "var root = document.querySelector('[role=dialog]') || document.body;"
                            + "var t = root.innerText || '';"
                            + "if (t.indexOf('Slå samman') < 0 && t.indexOf('Merge') < 0) { return false; }"
                            + "var btns = root.querySelectorAll('button');"
                            + "for (var i = 0; i < btns.length; i++) {"
                            + "  var b = btns[i];"
                            + "  var txt = (b.innerText || '').trim();"
                            + "  if ((txt === 'Slå samman' || txt.indexOf('Slå samman') >= 0) && vis(b)) {"
                            + "    return true;"
                            + "  }"
                            + "}"
                            + "return t.indexOf('Dokumentnamn') >= 0 && t.indexOf('Uppladdad av') >= 0;");
            return Boolean.TRUE.equals(visible);
        } catch (Exception e) {
            return false;
        }
    }

    private int clickMergeOnAllUploadedDocuments() {
        int merged = 0;
        for (int i = 0; i < 50; i++) {
            acceptUploadAlertIfPresent();
            List<WebElement> buttons = displayedMergeButtons();
            if (buttons.isEmpty()) {
                break;
            }
            WebElement merge = buttons.get(0);
            try {
                jsClick(merge);
                acceptUploadAlertIfPresent(1);
                int before = buttons.size();
                try {
                    new WebDriverWait(driver, Duration.ofSeconds(1))
                            .until(d -> displayedMergeButtons().size() < before);
                } catch (TimeoutException ignored) {
                    // Merge can leave the row in place while its status updates.
                }
                merged++;
                LOG.info("Clicked Slå samman on uploaded document ({})", merged);
            } catch (UnhandledAlertException e) {
                acceptUploadAlertIfPresent();
                merged++;
            } catch (StaleElementReferenceException ignored) {
                // row refreshed after merge
            }
        }
        if (merged == 0) {
            LOG.info("No Slå samman buttons on this review category");
        }
        return merged;
    }

    private List<WebElement> displayedMergeButtons() {
        List<WebElement> shown = new java.util.ArrayList<WebElement>();
        By merge = By.xpath(
                "//button[normalize-space()='Slå samman' or contains(., 'Slå samman')"
                        + " or normalize-space()='Merge' or contains(., 'Merge files')]");
        for (WebElement el : driver.findElements(merge)) {
            try {
                if (el.isDisplayed() && el.isEnabled()
                        && !"true".equalsIgnoreCase(String.valueOf(el.getAttribute("disabled")))) {
                    shown.add(el);
                }
            } catch (Exception ignored) {
                // stale
            }
        }
        return shown;
    }

    private boolean verifyDocumentInViewer() {
        By[] locators = new By[]{
                By.cssSelector("canvas"),
                By.cssSelector("embed, object"),
                By.xpath("//iframe[contains(@src,'blob') or contains(@src,'pdf') or contains(@title,'pdf')]"),
                By.xpath("//*[contains(@class,'pdf') or contains(@class,'react-pdf')"
                        + " or contains(@class,'document-preview')]"),
                By.xpath("//img[contains(@src,'pdf') or contains(@alt,'pdf') or contains(@alt,'ABC')]")
        };
        for (int i = 0; i < locators.length; i++) {
            for (WebElement el : driver.findElements(locators[i])) {
                try {
                    if (el.isDisplayed() && el.getSize().getWidth() > 40 && el.getSize().getHeight() > 40) {
                        LOG.info("Document is visible in the review viewer ({})", el.getTagName());
                        return true;
                    }
                } catch (Exception ignored) {
                    // stale
                }
            }
        }
        try {
            Object hasFile = ((JavascriptExecutor) driver).executeScript(
                    "var t = document.body && document.body.innerText ? document.body.innerText : '';"
                            + "return t.indexOf('ABC.pdf') >= 0 || t.indexOf('.pdf') >= 0"
                            + " || t.indexOf('.doc') >= 0;");
            if (Boolean.TRUE.equals(hasFile) && displayedMergeButtons().isEmpty()) {
                LOG.info("Merged document filename is present after Slå samman");
                return true;
            }
        } catch (Exception ignored) {
            // fall through
        }
        LOG.warn("Document viewer is still empty after Slå samman");
        return false;
    }

    /**
     * Aktieägare / Tidigare representanter come from the client order. Log shown vs not shown.
     */
    private void verifyFormerRepresentativesIfShown() {
        try {
            Object result = ((JavascriptExecutor) driver).executeScript(
                    "var t = document.body && document.body.innerText ? document.body.innerText : '';"
                            + "var hasFormer = t.indexOf('Tidigare representanter') >= 0"
                            + " || t.indexOf('Former representative') >= 0;"
                            + "var hasShare = t.indexOf('Aktieägare') >= 0"
                            + " || t.indexOf('Shareholder') >= 0;"
                            + "function namesAfter(label) {"
                            + "  var i = t.indexOf(label);"
                            + "  if (i < 0) { return ''; }"
                            + "  return t.substring(i, i + 280).replace(/\\s+/g, ' ');"
                            + "}"
                            + "return JSON.stringify({"
                            + "  former: hasFormer,"
                            + "  shareholders: hasShare,"
                            + "  formerText: namesAfter('Tidigare representanter') || namesAfter('Former representative'),"
                            + "  shareText: namesAfter('Aktieägare') || namesAfter('Shareholder')"
                            + "});");
            String json = result == null ? "{}" : String.valueOf(result);
            if (json.contains("\"former\":true")) {
                LOG.info("Former representatives (Tidigare representanter) ARE shown: {}", json);
            } else {
                LOG.info("Former representatives (Tidigare representanter) are NOT shown on this category");
            }
            if (json.contains("\"shareholders\":true")) {
                LOG.info("Client-side Aktieägare ARE shown: {}", json);
            } else {
                LOG.info("Client-side Aktieägare are NOT shown on this category");
            }
        } catch (Exception e) {
            LOG.warn("Could not inspect former representatives / Aktieägare: {}", e.getMessage());
        }
    }

    private boolean setDokumentatstatusGodkand() {
        scrollToReviewWizardLowerSections();
        if (approveViaPendingStatusTrigger()) {
            return true;
        }
        if (approveViaStatusChip()) {
            return true;
        }
        By labeledSelect = By.xpath(
                "//label[contains(., 'Dokumentatstatus') or contains(., 'Dokumentstatus')]/following::select[1]"
                        + " | //*[normalize-space()='Dokumentatstatus' or normalize-space()='Dokumentstatus']"
                        + "/following::select[1]");
        for (WebElement select : driver.findElements(labeledSelect)) {
            try {
                if (!select.isDisplayed()) {
                    continue;
                }
                Select s = new Select(select);
                    for (WebElement opt : s.getOptions()) {
                        String text = opt.getText();
                    if (text.contains("Godkänd") || text.contains("Approved")) {
                            s.selectByVisibleText(text);
                        acceptUploadAlertIfPresent(2);
                        LOG.info("Set Dokumentatstatus to {}", text.trim());
                        return true;
                    }
                }
            } catch (Exception ignored) {
                // try next
            }
        }
        return approveViaNativeSelect() || approveViaCombobox();
    }

    /**
     * Walk unique documents only. Nästa wraps in this UI — stop when the document repeats
     * or the last index is reached. Caller closes the wizard afterwards.
     */
    private boolean walkAndApproveReviewDocuments(boolean fillFields) {
        goToFirstDocumentInWizard();
        boolean any = false;
        Set<String> seen = new HashSet<String>();
        for (int i = 0; i < 12; i++) {
            acceptUploadAlertIfPresent();
            waitForStatusChips();
            String key = currentReviewDocumentKey();
            if (key.length() > 8 && !seen.add(key)) {
                LOG.info("Review wizard wrapped to a seen document — stopping walk");
                break;
            }
            boolean approved = approveViaStatusChip();
            if (fillFields) {
                fillLogicalReviewFieldsFromOrder();
                waitForStatusChips();
                acceptUploadAlertIfPresent();
                approved = approved || approveViaStatusChip();
            }
            approved = approved
                    || approveViaNativeSelect()
                    || approveViaCombobox()
                    || approveViaPendingStatusTrigger()
                    || approveViaCustomOption()
                    || approveViaActionButtons();
            if (!approved) {
                approved = approveAllNativeSelects();
            }
            if (approved) {
                any = true;
                saveReviewWizardChangesIfNeeded();
                LOG.info("Approved document {} in Review Upload wizard", i + 1);
            } else {
                LOG.warn("Could not set Godkänd on review document {}", i + 1);
            }
            if (isLastReviewDocument()) {
                LOG.info("Last review-wizard document reached");
                break;
            }
            if (!clickNextDocumentInWizard()) {
                break;
            }
        }
        return any;
    }

    private String currentReviewDocumentKey() {
        try {
            Object text = ((JavascriptExecutor) driver).executeScript(
                    "var t = document.body && document.body.innerText ? document.body.innerText : '';"
                            + "var m = t.match(/(\\d+)\\s*(?:av|of|\\/)\\s*(\\d+)/);"
                            + "var idx = m ? m[0] : '';"
                            + "var i = t.indexOf('Granska dokument');"
                            + "if (i < 0) { i = t.indexOf('Dokumentatstatus'); }"
                            + "var snippet = i >= 0 ? t.substring(i, i + 160).replace(/\\s+/g, ' ') : '';"
                            + "var cat = '';"
                            + "var sel = document.querySelector('select, [role=combobox]');"
                            + "if (sel) { cat = (sel.value || sel.innerText || '').trim().split('\\n')[0]; }"
                            + "return (cat + '|' + idx + '|' + snippet).substring(0, 180);");
            return text == null ? "" : String.valueOf(text).trim();
        } catch (Exception e) {
            return "";
        }
    }

    private boolean isLastReviewDocument() {
        try {
            Object last = ((JavascriptExecutor) driver).executeScript(
                    "var t = document.body && document.body.innerText ? document.body.innerText : '';"
                            + "var m = t.match(/(\\d+)\\s*(?:av|of|\\/)\\s*(\\d+)/);"
                            + "if (!m) { return false; }"
                            + "return parseInt(m[1], 10) >= parseInt(m[2], 10);");
            return Boolean.TRUE.equals(last);
        } catch (Exception e) {
            return false;
        }
    }

    private void closeReviewWizard() {
        acceptUploadAlertIfPresent();
        if (clickReviewWizardStangButton()) {
            acceptUploadAlertIfPresent(1);
            waitForReviewWizardClosed(Duration.ofSeconds(2));
            return;
        }
        if (clickOnCurrentPageIfPresent(
                "Stäng", "Close", "Avbryt", "Cancel", "Stäng granskning", "Close review")) {
            LOG.info("Closed review wizard via dismiss control");
            acceptUploadAlertIfPresent();
            return;
        }
        By closeIcon = By.xpath(
                "//*[@role='dialog']//button[@aria-label='Close' or @aria-label='Stäng' or @aria-label='close'"
                        + " or contains(@class,'close')]"
                        + " | //*[@role='dialog']//button[normalize-space()='\u00D7' or normalize-space()='x']"
                        + " | //button[@aria-label='Close' or @aria-label='Stäng' or @aria-label='close']");
        for (WebElement el : driver.findElements(closeIcon)) {
            try {
                if (el.isDisplayed()) {
                    safeClick(el);
                    acceptUploadAlertIfPresent();
                    LOG.info("Closed review wizard via close icon");
                    return;
                }
            } catch (Exception ignored) {
                // try next
            }
        }
        if (clickOnCurrentPageIfPresent("Tillbaka", "Back")) {
            LOG.info("Left review wizard via Tillbaka");
            acceptUploadAlertIfPresent();
            return;
        }
        openManageOrderTab();
        LOG.info("No Stäng control found — switched to Hantera beställning tab");
    }

    /**
     * Granska dokument is a full-screen view: it stays open while {@code Stäng} is visible.
     * Hantera beställning alone does not dismiss it (old detection caused infinite tab clicks).
     */
    private boolean isReviewWizardPopupOpen() {
        if (!isOnReviewWizardScreen()) {
            return false;
        }
        return isReviewWizardDismissControlVisible();
    }

    private boolean isOnReviewWizardScreen() {
        try {
            Object onReview = ((JavascriptExecutor) driver).executeScript(
                    "function vis(el) {"
                            + "  if (!el) { return false; }"
                            + "  var r = el.getBoundingClientRect();"
                            + "  return r.width > 8 && r.height > 8 && r.bottom > 0 && r.top < window.innerHeight;"
                            + "}"
                            + "var nodes = document.querySelectorAll('h1,h2,h3,header p,header span');"
                            + "for (var i = 0; i < nodes.length; i++) {"
                            + "  if (!vis(nodes[i])) { continue; }"
                            + "  var t = (nodes[i].innerText || '').trim();"
                            + "  if (t.indexOf('Granska dokument') === 0 || t === 'Review Upload') {"
                            + "    return true;"
                            + "  }"
                            + "}"
                            + "var hasNasta = false;"
                            + "var hasViewer = false;"
                            + "var btns = document.querySelectorAll('button');"
                            + "for (var j = 0; j < btns.length; j++) {"
                            + "  if (!vis(btns[j])) { continue; }"
                            + "  if ((btns[j].innerText || '').trim() === 'Nästa') { hasNasta = true; }"
                            + "}"
                            + "var canvases = document.querySelectorAll('canvas');"
                            + "for (var k = 0; k < canvases.length; k++) {"
                            + "  if (!vis(canvases[k])) { continue; }"
                            + "  var cr = canvases[k].getBoundingClientRect();"
                            + "  if (cr.width > 120 && cr.height > 120) { hasViewer = true; break; }"
                            + "}"
                            + "var body = document.body && document.body.innerText ? document.body.innerText : '';"
                            + "return hasNasta && hasViewer && body.indexOf('Dokumentatstatus') >= 0;");
            return Boolean.TRUE.equals(onReview);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isReviewWizardDismissControlVisible() {
        return anyDisplayed(By.xpath(
                "//button[normalize-space()='Stäng' or normalize-space()='Close']"))
                || anyDisplayed(By.xpath(
                "//button[@aria-label='Stäng' or @aria-label='Close' or @aria-label='close']"));
    }

    private void waitForReviewWizardClosed(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(d -> !isOnReviewWizardScreen());
        } catch (TimeoutException ignored) {
            LOG.debug("Review wizard close transition exceeded {} ms", timeout.toMillis());
        }
    }

    /** Clicks the top {@code Stäng} on the Granska dokument screen (not manage-order tab). */
    private boolean clickReviewWizardStangButton() {
        try {
            Object clicked = ((JavascriptExecutor) driver).executeScript(
                    "function vis(el) {"
                            + "  if (!el) { return false; }"
                            + "  var r = el.getBoundingClientRect();"
                            + "  return r.width > 0 && r.height > 0 && r.bottom > 0 && r.top < window.innerHeight;"
                            + "}"
                            + "var best = null;"
                            + "var bestTop = 1e9;"
                            + "var nodes = document.querySelectorAll('button, [role=button]');"
                            + "for (var i = 0; i < nodes.length; i++) {"
                            + "  var b = nodes[i];"
                            + "  if (!vis(b)) { continue; }"
                            + "  var txt = (b.innerText || '').trim();"
                            + "  var aria = (b.getAttribute('aria-label') || '').trim();"
                            + "  if (txt !== 'Stäng' && txt !== 'Close' && aria !== 'Stäng' && aria !== 'Close') {"
                            + "    continue;"
                            + "  }"
                            + "  var top = b.getBoundingClientRect().top;"
                            + "  if (top < bestTop) { best = b; bestTop = top; }"
                            + "}"
                            + "if (!best) { return false; }"
                            + "best.scrollIntoView({block: 'center'});"
                            + "best.click();"
                            + "return true;");
            if (Boolean.TRUE.equals(clicked)) {
                LOG.info("Clicked Stäng to exit Granska dokument");
                return true;
            }
        } catch (Exception e) {
            LOG.debug("JS Stäng click failed: {}", e.getMessage());
        }
        for (WebElement el : driver.findElements(By.xpath(
                "//button[normalize-space()='Stäng' or normalize-space()='Close']"))) {
            try {
                if (el.isDisplayed() && el.isEnabled()) {
                    safeClick(el);
                    LOG.info("Clicked Stäng to exit review wizard (XPath)");
                    return true;
                }
            } catch (Exception ignored) {
                // try next
            }
        }
        return false;
    }

    /** Leave Granska dokument, then open Hantera beställning once (reload if still stuck). */
    private void ensureReviewWizardClosed() {
        if (!isOnReviewWizardScreen()) {
            return;
        }
        finalizeReviewWizardBeforeExit();
        for (int attempt = 0; attempt < 2; attempt++) {
            if (!isOnReviewWizardScreen()) {
                LOG.info("Left Granska dokument (attempt {})", attempt + 1);
                openManageOrderTab();
                return;
            }
            closeReviewWizard();
            waitForSpecifiedTime(2);
        }
        if (isOnReviewWizardScreen()) {
            LOG.warn("Still on Granska dokument — reloading order detail to continue workflow");
            reloadStoredOrderAndOpenManageOrder();
            return;
        }
        openManageOrderTab();
    }

    private boolean hasReviewStatusControl(WebDriver d) {
        return anyDisplayed(reviewWizardContent)
                || customerUploadsPresent(d)
                || displayedSelectCount(d) > 0
                || !d.findElements(By.xpath("//*[@role='combobox']")).isEmpty()
                || anyDisplayed(By.xpath(
                "//button[normalize-space()='Mottagen' or normalize-space()='Received'"
                        + " or contains(., 'Approved') or contains(., 'Godkänd') or contains(., 'Approve')"
                        + " or contains(., 'Godkänn') or contains(., 'Pending') or contains(., 'Väntar')"
                        + " or contains(., 'To review') or contains(., 'Att granska')"
                        + " or contains(., 'Ej granskad') or contains(., 'Not reviewed')"
                        + " or normalize-space()='Nästa']"));
    }

    private int displayedSelectCount(WebDriver d) {
        int count = 0;
        for (WebElement select : d.findElements(By.tagName("select"))) {
            try {
                if (select.isDisplayed()) {
                    count++;
                }
            } catch (Exception ignored) {
                // stale
            }
        }
        return count;
    }

    /**
     * Review UI is Review Upload / Granska dokument. Customer checklist files already appear as
     * "Uppladdat av kund"; remaining "Ej mottaget" types are optional missing documents.
     */
    private void prepareDocumentsForReviewIfNeeded() {
        acceptUploadAlertIfPresent();
        openReviewWizardsTab();
        if (hasReviewStatusControl(driver) || customerUploadsPresent(driver)) {
            LOG.info("Review documents are available — skipping extra Ej mottaget attachments");
            return;
        }
        receiveOutstandingDocumentTypes();
        openReviewWizardsTab();
    }

    private boolean customerUploadsPresent(WebDriver d) {
        try {
            String src = d.getPageSource();
            return src.contains("Uppladdat av kund")
                    || src.contains("Uploaded by customer")
                    || src.contains("Nyligen uppladdade")
                    || src.contains("Granska dokument")
                    || src.contains("Dokumentatstatus");
        } catch (UnhandledAlertException e) {
            acceptUploadAlertIfPresent();
            return false;
        }
    }

    private int notReceivedDocumentTypeCount() {
        acceptUploadAlertIfPresent();
        int count = 0;
        try {
            for (WebElement button : driver.findElements(notReceivedDocumentTypeButton())) {
                try {
                    if (button.isDisplayed()) {
                        count++;
                    }
                } catch (Exception ignored) {
                    // stale
                }
            }
        } catch (UnhandledAlertException e) {
            acceptUploadAlertIfPresent();
        }
        return count;
    }

    private By notReceivedDocumentTypeButton() {
        return By.xpath("//button[contains(., 'Ej mottaget') or contains(., 'Not received')]");
    }

    private void confirmPendingUploadAssignments() {
        By assignActions = By.xpath(
                "//button[@type='button' and not(@disabled)]"
                        + "[contains(., 'Mottaget') or contains(., 'Received') or contains(., 'Assign')"
                        + " or contains(., 'Koppla') or contains(., 'Spara') or contains(., 'Save')"
                        + " or contains(., 'Bekräfta') or contains(., 'Confirm') or contains(., 'Använd')"
                        + " or contains(., 'Godkänn uppladdning')]");
        boolean clicked = false;
        for (WebElement button : driver.findElements(assignActions)) {
            try {
                if (button.isDisplayed() && button.isEnabled()) {
                    scrollElementIntoViewableArea(button);
                    jsClick(button);
                    waitForSpecifiedTime(1);
                    clicked = true;
                }
            } catch (Exception ignored) {
                // continue
            }
        }
        for (WebElement select : driver.findElements(By.tagName("select"))) {
            try {
                if (!select.isDisplayed()) {
                    continue;
                }
                Select s = new Select(select);
                if (s.getOptions().size() > 1) {
                    s.selectByIndex(1);
                    waitForSpecifiedTime(1);
                    clicked = true;
                }
            } catch (Exception ignored) {
                // continue
            }
        }
        if (clicked) {
            saveReviewWizardChangesIfNeeded();
            LOG.info("Confirmed pending Review Upload assignments");
        }
    }

    private void receiveOutstandingDocumentTypes() {
        String filePath = resolveAdminUploadFixture();
        if (filePath == null) {
            LOG.warn("ABC.pdf fixture missing — cannot attach files to Ej mottaget document types");
            return;
        }
        int attached = 0;
        for (int attempt = 0; attempt < 13; attempt++) {
            List<WebElement> outstanding = displayedNotReceivedDocumentTypes();
            if (outstanding.isEmpty()) {
                break;
            }
            WebElement typeButton = outstanding.get(0);
            try {
                if (attachFileToDocumentType(typeButton, filePath)) {
                    attached++;
                    waitForLoad();
                    waitForSpecifiedTime(1);
                } else {
                    LOG.warn("Could not attach file to remaining Ej mottaget type");
                    break;
                }
            } catch (StaleElementReferenceException e) {
                LOG.debug("Ej mottaget row went stale — retrying");
            }
        }
        if (attached > 0) {
            LOG.info("Attached fixture to {} Ej mottaget document types", attached);
        } else {
            LOG.info("No Ej mottaget document types received a file attachment");
        }
    }

    private List<WebElement> displayedNotReceivedDocumentTypes() {
        List<WebElement> displayed = new java.util.ArrayList<WebElement>();
        for (WebElement button : driver.findElements(notReceivedDocumentTypeButton())) {
            try {
                if (button.isDisplayed()) {
                    displayed.add(button);
                }
            } catch (Exception ignored) {
                // stale
            }
        }
        return displayed;
    }

    private boolean attachFileToDocumentType(WebElement typeButton, String filePath) {
        int before = notReceivedDocumentTypeCount();
        WebElement rowInput = fileInputNear(typeButton);
        if (rowInput != null) {
            sendAdminFile(rowInput, filePath);
            return waitForNotReceivedCountToDrop(before);
        }
        scrollElementIntoViewableArea(typeButton);
        jsClick(typeButton);
        waitForSpecifiedTime(1);
        List<WebElement> inputs = driver.findElements(By.xpath("//input[@type='file']"));
        if (!inputs.isEmpty()) {
            sendAdminFile(inputs.get(inputs.size() - 1), filePath);
            return waitForNotReceivedCountToDrop(before);
        }
        confirmPendingUploadAssignments();
        return waitForNotReceivedCountToDrop(before);
    }

    private boolean waitForNotReceivedCountToDrop(int before) {
        acceptUploadAlertIfPresent();
        try {
            new WebDriverWait(driver, Duration.ofSeconds(8)).until(d -> {
                acceptUploadAlertIfPresent();
                return notReceivedDocumentTypeCount() < before;
            });
            return true;
        } catch (TimeoutException e) {
            LOG.warn("Ej mottaget count stayed at {} after attach attempt", before);
            return false;
        } catch (UnhandledAlertException e) {
            acceptUploadAlertIfPresent();
            return notReceivedDocumentTypeCount() < before;
        }
    }

    private WebElement fileInputNear(WebElement typeButton) {
        try {
            Object found = ((JavascriptExecutor) driver).executeScript(
                    "var el = arguments[0];"
                            + "var row = el.closest('li,article,section,tr,div');"
                            + "for (var i = 0; i < 8 && row; i++) {"
                            + "  var input = row.querySelector('input[type=file]');"
                            + "  if (input) { return input; }"
                            + "  row = row.parentElement;"
                            + "}"
                            + "return null;",
                    typeButton);
            if (found instanceof WebElement) {
                return (WebElement) found;
            }
        } catch (Exception ignored) {
            // fall through
        }
        return null;
    }

    private void sendAdminFile(WebElement fileInput, String filePath) {
        fileInput.sendKeys(filePath);
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                    fileInput);
        } catch (Exception ignored) {
            // sendKeys is enough for native file inputs
        }
        acceptUploadAlertIfPresent();
        waitForSpecifiedTime(1);
    }

    private void acceptUploadAlertIfPresent() {
        acceptUploadAlertIfPresent(0);
    }

    private void acceptUploadAlertIfPresent(int waitSeconds) {
        try {
            if (waitSeconds > 0) {
                new WebDriverWait(driver, Duration.ofSeconds(waitSeconds))
                        .until(ExpectedConditions.alertIsPresent());
            }
            String text = driver.switchTo().alert().getText();
            driver.switchTo().alert().accept();
            LOG.info("Accepted alert: {}", text);
        } catch (TimeoutException | NoAlertPresentException ignored) {
            // none
        } catch (UnhandledAlertException e) {
            try {
                driver.switchTo().alert().accept();
            } catch (Exception ignored) {
                // already dismissed by Chromedriver
            }
        } catch (Exception ignored) {
            // no alert
        }
    }

    private void scrollToReviewDocumentPanel() {
        // Intentionally no scrollIntoView / zoom — keep the review layout stable.
    }

    private String resolveAdminUploadFixture() {
        try {
            return UploadFixtures.defaultPdf();
        } catch (RuntimeException e) {
            LOG.warn("{}", e.getMessage());
            return null;
        }
    }

    private void uploadMultiPageReviewDocuments() {
        if (reviewFixturesUploaded) {
            return;
        }
        List<String> files = UploadFixtures.reviewWizardMultiPageFiles();
        int limit = Math.min(3, files.size());
        for (int i = 0; i < limit; i++) {
            acceptUploadAlertIfPresent();
            scrollToReviewDocumentPanel();
            WebElement fileInput = firstDisplayedFileInput();
            if (fileInput == null) {
                WebElement upload = firstDisplayedButton("Ladda upp fil", "Upload file", "Ladda upp");
                if (upload != null) {
                    jsClick(upload);
                    waitForSpecifiedTime(1);
                    fileInput = firstDisplayedFileInput();
                }
            }
            if (fileInput == null) {
                List<WebElement> inputs = driver.findElements(By.xpath("//input[@type='file']"));
                if (!inputs.isEmpty()) {
                    fileInput = inputs.get(inputs.size() - 1);
                }
            }
            if (fileInput == null) {
                LOG.warn("No file input in Review Wizards for fixture {}", files.get(i));
                continue;
            }
            fileInput.sendKeys(files.get(i));
            acceptUploadAlertIfPresent(5);
            LOG.info("Uploaded multi-page review fixture: {}", files.get(i));
        }
        reviewFixturesUploaded = true;
    }

    private WebElement firstDisplayedFileInput() {
        for (WebElement input : driver.findElements(By.xpath("//input[@type='file']"))) {
            try {
                if (input.isDisplayed()) {
                    return input;
                }
            } catch (Exception ignored) {
                // stale
            }
        }
        return null;
    }

    private void fillLogicalReviewFieldsFromOrder() {
        LocalDate period = reviewPeriodFromOrderDetails();
        selectReviewChoice("Månad", "Month", swedishMonthName(period.getMonthValue()));
        selectReviewChoice("År", "Year", String.valueOf(period.getYear()));
        typeReviewComment("Intern kommentar", "Internal comment",
                "Automation review — VAT period from order " + period.getMonthValue()
                        + "/" + period.getYear());
        typeReviewComment("Meddelande till kund", "Message to customer",
                "Review completed by automation.");
    }

    private LocalDate reviewPeriodFromOrderDetails() {
        try {
            Object text = ((JavascriptExecutor) driver).executeScript(
                    "var t = document.body && document.body.innerText ? document.body.innerText : '';"
                            + "var m = t.match(/20\\d{2}[-.\\/](0[1-9]|1[0-2])/);"
                            + "return m ? m[0] : '';");
            String raw = text == null ? "" : String.valueOf(text).trim();
            if (raw.length() >= 7) {
                int year = Integer.parseInt(raw.substring(0, 4));
                String monthPart = raw.substring(raw.length() - 2);
                int month = Integer.parseInt(monthPart);
                return LocalDate.of(year, month, 1);
            }
        } catch (Exception ignored) {
            // fall through to today
        }
        return LocalDate.now();
    }

    private String swedishMonthName(int month) {
        String[] names = {
                "januari", "februari", "mars", "april", "maj", "juni",
                "juli", "augusti", "september", "oktober", "november", "december"
        };
        if (month < 1 || month > 12) {
            return "september";
        }
        return names[month - 1];
    }

    private void clickIfDisplayed(By by) {
        for (WebElement el : driver.findElements(by)) {
            try {
                if (el.isDisplayed()) {
                    jsClick(el);
                    waitForSpecifiedTime(1);
                    LOG.info("Clicked review-section control");
                    return;
                }
            } catch (Exception ignored) {
                // try next
            }
        }
    }

    private void selectReviewChoice(String swedishLabel, String englishLabel, String optionText) {
        By labeled = By.xpath(
                "//label[contains(., '" + swedishLabel + "') or contains(., '" + englishLabel + "')]"
                        + "/following::*[self::select or self::button or @role='combobox'][1]");
        for (WebElement control : driver.findElements(labeled)) {
            try {
                if (!control.isDisplayed()) {
                    continue;
                }
                if ("select".equalsIgnoreCase(control.getTagName())) {
                    Select select = new Select(control);
                    for (WebElement opt : select.getOptions()) {
                        if (opt.getText().toLowerCase().contains(optionText.toLowerCase())) {
                            select.selectByVisibleText(opt.getText());
                            LOG.info("Selected {} = {}", swedishLabel, opt.getText());
                            return;
                        }
                    }
                    if (select.getOptions().size() > 1) {
                        select.selectByIndex(1);
                        return;
                    }
                } else {
                    jsClick(control);
                    waitForSpecifiedTime(1);
                    By option = By.xpath(
                            "//*[@role='option'][contains(., '" + optionText + "')]"
                                    + " | //li[contains(., '" + optionText + "')]");
                    List<WebElement> options = driver.findElements(option);
                    if (!options.isEmpty()) {
                        jsClick(options.get(0));
                        LOG.info("Selected {} = {}", swedishLabel, optionText);
                    }
                    return;
                }
            } catch (Exception ignored) {
                // try next control
            }
        }
    }

    private void typeReviewComment(String swedishLabel, String englishLabel, String text) {
        By field = By.xpath(
                "//label[contains(., '" + swedishLabel + "') or contains(., '" + englishLabel + "')]"
                        + "/following::*[self::textarea or self::input][1]");
        WebElement area = null;
        for (WebElement el : driver.findElements(field)) {
            try {
                if (el.isDisplayed()) {
                    area = el;
                    break;
                }
            } catch (Exception ignored) {
                // stale
            }
        }
        if (area == null) {
            return;
        }
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].value = arguments[1];"
                            + "arguments[0].dispatchEvent(new Event('input', {bubbles:true}));"
                            + "arguments[0].dispatchEvent(new Event('change', {bubbles:true}));",
                    area, text);
            LOG.info("Entered {}", swedishLabel);
        } catch (Exception e) {
            LOG.debug("Could not set {}: {}", swedishLabel, e.getMessage());
        }
    }

    private void logReviewPanelSnapshot(String phase) {
        try {
            Object text = ((JavascriptExecutor) driver).executeScript(
                    "var t = document.body && document.body.innerText ? document.body.innerText : '';"
                            + "var i = t.indexOf('Granska dokument');"
                            + "if (i < 0) { i = t.indexOf('Dokumentatstatus'); }"
                            + "if (i >= 0) { t = t.substring(i); }"
                            + "return t.length > 2500 ? t.substring(0, 2500) : t;");
            LOG.info("Review panel snapshot ({}): {}", phase, String.valueOf(text).replace("\n", " | "));
        } catch (Exception e) {
            LOG.warn("Could not snapshot review panel: {}", e.getMessage());
        }
    }

    private void reloadStoredOrderAndOpenManageOrder() {
        String orderId = LiquidationOrderIdContext.getCapturedOrderIdOrNull();
        if (orderId == null || orderId.isEmpty()) {
            orderId = System.getProperty("workflow.orderId", "").trim();
        }
        if (orderId != null && !orderId.isEmpty()) {
            driver.get(QaServerCredentials.urlWithHttpBasicAuth(GENERIC_ORDER_BASE + orderId));
            waitForLoad();
            waitForSpecifiedTime(3);
            waitForOrderDetailShell();
            scrollPageToTop();
        }
        openManageOrderTab();
        scrollPageToTop();
    }

    private boolean approveAllNativeSelects() {
        boolean any = false;
        for (WebElement select : driver.findElements(By.tagName("select"))) {
            try {
                Select s = new Select(select);
                for (WebElement opt : s.getOptions()) {
                    String text = opt.getText();
                    if (text.contains("Approved") || text.contains("Godkänd")) {
                        try {
                            scrollElementIntoViewableArea(select);
                        } catch (Exception ignored) {
                            // hidden native select is still operable
                        }
                        s.selectByVisibleText(text);
                        acceptUploadAlertIfPresent(1);
                        any = true;
                        break;
                    }
                }
            } catch (UnhandledAlertException e) {
                acceptUploadAlertIfPresent();
                any = true;
            } catch (Exception ignored) {
                // continue
            }
        }
        if (any) {
            LOG.info("Approved one or more documents via native selects");
        }
        return any;
    }

    private boolean approveViaActionButtons() {
        By approveButtons = By.xpath(
                "//button[not(@disabled)][contains(., 'Approve') or contains(., 'Godkänn')"
                        + " or contains(., 'Approved') or contains(., 'Godkänd')]"
                        + " | //a[contains(., 'Approve') or contains(., 'Godkänn')]");
        boolean clicked = false;
        for (WebElement button : driver.findElements(approveButtons)) {
            try {
                if (button.isDisplayed() && button.isEnabled()) {
                    scrollElementIntoViewableArea(button);
                    jsClick(button);
                    waitForSpecifiedTime(2);
                    clicked = true;
                    LOG.info("Clicked approve action button in review wizard");
                }
            } catch (Exception ignored) {
                // continue
            }
        }
        return clicked;
    }

    public void clickManageOrderButton(String... labelsEnSv) {
        openManageOrderTab();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(ACTION_WAIT_SECONDS));
        scrollPageToTop();
        By presenceLocator = buildButtonPresenceLocator(labelsEnSv);
        for (int attempt = 0; attempt < 4; attempt++) {
            try {
                scrollPageToTop();
                wait.until(ExpectedConditions.presenceOfElementLocated(presenceLocator));
                WebElement button = firstClickableManageOrderButton(labelsEnSv, ACTION_WAIT_SECONDS);
                scrollElementIntoViewableArea(button);
                jsClick(button);
        waitForLoad();
        waitForSpecifiedTime(3);
        scrollPageToTop();
        LOG.info("Clicked manage-order button matching: {}", String.join(" / ", labelsEnSv));
                return;
            } catch (TimeoutException | StaleElementReferenceException | org.openqa.selenium.NoSuchElementException e) {
                LOG.warn("Manage-order button not ready (attempt {}) — scrolling and retrying: {}",
                        attempt + 1, String.join(" / ", labelsEnSv));
                scrollPageToTop();
                openManageOrderTab();
                waitForSpecifiedTime(3);
            }
        }
        WebElement button = firstClickableManageOrderButton(labelsEnSv, ACTION_WAIT_SECONDS);
        scrollElementIntoViewableArea(button);
        jsClick(button);
        waitForLoad();
        waitForSpecifiedTime(3);
        scrollPageToTop();
        LOG.info("Clicked manage-order button matching: {}", String.join(" / ", labelsEnSv));
    }

    /** Last-resort: click a matching button even if still @disabled (UI lag after review approval). */
    private boolean forceClickManageOrderButtonIfPresent(String... labelsEnSv) {
        openManageOrderTab();
        scrollPageToTop();
        By presence = buildButtonPresenceLocator(labelsEnSv);
        List<WebElement> buttons = driver.findElements(presence);
        logVisibleManageOrderButtons();
        for (WebElement button : buttons) {
            try {
                if (button.isDisplayed()) {
                    scrollElementIntoViewableArea(button);
                    jsClick(button);
                    waitForLoad();
                    waitForSpecifiedTime(3);
                    scrollPageToTop();
                    LOG.info("Force-clicked manage-order button matching: {}", String.join(" / ", labelsEnSv));
                    return true;
                }
            } catch (Exception ignored) {
                // continue
            }
        }
        return false;
    }

    private void logVisibleManageOrderButtons() {
        try {
            StringBuilder sb = new StringBuilder("Visible manage-order buttons: ");
            for (WebElement b : driver.findElements(By.xpath("//button[@type='button' or @role='button']"))) {
                try {
                    if (b.isDisplayed()) {
                        String text = b.getText();
                        if (text != null && !text.trim().isEmpty()) {
                            sb.append("[").append(text.trim().replace('\n', ' ')).append("] ");
                        }
                    }
                } catch (Exception ignored) {
                    // continue
                }
            }
            LOG.info(sb.toString());
        } catch (Exception e) {
            LOG.warn("Could not list manage-order buttons: {}", e.getMessage());
        }
    }

    public void clickReadyForReview() {
        clickManageOrderButton("Ready for review", "Klar för granskning", "Ready for Review");
    }

    public void clickDocumentsReceivedToday() {
        acceptUploadAlertIfPresent();
        if (!tryClickManageOrderButtonIfPresent(
                "Documents received today", "Dokument mottagna idag", "Documents Received",
                "Dokument mottagna", "Documents received", "Mottagna idag")) {
            LOG.info("Documents received today not shown — order may already be past this step");
        }
    }

    public void clickReviewComplete() {
        try {
            acceptUploadAlertIfPresent(2);
            if (isOnReviewWizardScreen()) {
                exitReviewWizardToManageOrder();
            } else {
                openManageOrderTab();
                scrollPageToTop();
            }
            if (reviewWizardWalkCompleted) {
                LOG.info("Review wizard already completed in prior step — post-review workflow only");
                finishRemainingManageOrderDocumentsIfNeeded();
                advancePostReviewWorkflowButtons();
                if (canContinueAfterReviewPhase()) {
                    return;
                }
            }
            if (isInk2BokslutOrderStatusReached()) {
                LOG.info("Order already at INK2/Bokslut — skipping further lifecycle actions");
                postReviewWorkflowAdvanced = true;
                return;
            }
            if (orderAlreadyPastReview() || isReadyForBoardChangeVisible()) {
                LOG.info("Order already past review or ready for board change");
                return;
            }
            if (clickReviewCompleteIfPresent()) {
                return;
            }
            if (!reviewDocumentsAlreadyApprovedOnManageOrder()) {
                LOG.info("Review complete not available — finishing remaining manage-order document rows");
                completeGranskningTabInPlace();
                finishRemainingManageOrderDocumentsIfNeeded();
            } else {
                LOG.info("Skipping second Review Upload pass — {} Godkänd controls on manage order",
                        countDisplayedGodkandChips());
                finishRemainingManageOrderDocumentsIfNeeded();
            }
            acceptUploadAlertIfPresent(2);
            if (isOnReviewWizardScreen()) {
                exitReviewWizardToManageOrder();
            }
            if (isInk2BokslutOrderStatusReached()
                    || orderAlreadyPastReview()
                    || isReadyForBoardChangeVisible()
                    || clickReviewCompleteIfPresent()) {
                return;
            }
            advancePostReviewWorkflowButtons();
            if (canContinueAfterReviewPhase()) {
                return;
            }
            logVisibleManageOrderButtons();
            Assert.fail("Review complete / Granskning klar was not available after approving documents in Review Wizards.");
        } finally {
            if (isOnReviewWizardScreen()) {
                reloadStoredOrderAndOpenManageOrder();
            }
        }
    }

    private void finishRemainingManageOrderDocumentsIfNeeded() {
        markOutstandingDocumentTypesIfPresent();
        if (countDisplayedGodkandChips() < 8) {
            approveRemainingCustomerUploadRows();
        }
    }

    private boolean canContinueAfterReviewPhase() {
        if (isInk2BokslutOrderStatusReached()) {
            LOG.info("INK2/Bokslut status reached — review phase complete for timeopt handoff");
            postReviewWorkflowAdvanced = true;
            return true;
        }
        if (orderAlreadyPastReview() || isReadyForBoardChangeVisible()) {
            return true;
        }
        if (clickReviewCompleteIfPresent()) {
            return true;
        }
        if (isOnReviewWizardScreen()) {
            return false;
        }
        openManageOrderTab();
        if (isInk2BokslutOrderStatusReached()) {
            postReviewWorkflowAdvanced = true;
            return true;
        }
        int godkand = countDisplayedGodkandChips();
        if (godkand >= 8) {
            LOG.info("Review phase complete — {} Godkänd on manage order (no Granskning klar required)", godkand);
            return true;
        }
        if (postReviewWorkflowAdvanced) {
            LOG.info("Post-review workflow advanced (e.g. INK2/Bokslut) — continuing");
            return true;
        }
        return isPostReviewFortnoxOrInk2State();
    }

    private boolean isReadyForBoardChangeVisible() {
        return anyDisplayed(buildButtonPresenceLocator(
                "Ready for board change", "Klar för styrelseändring"));
    }

    private boolean isBoardChangeLabel(String label) {
        if (label == null) {
            return false;
        }
        String lower = label.toLowerCase();
        return lower.contains("board change") || lower.contains("styrelse");
    }

    private int countDisplayedGodkandChips() {
        int count = 0;
        for (WebElement button : driver.findElements(
                By.xpath("//button[normalize-space()='Godkänd' or normalize-space()='Approved']"))) {
            try {
                if (button.isDisplayed()) {
                    count++;
                }
            } catch (Exception ignored) {
                // stale
            }
        }
        return count;
    }

    private boolean reviewDocumentsAlreadyApprovedOnManageOrder() {
        if (reviewWizardWalkCompleted) {
            return true;
        }
        openManageOrderTab();
        return countDisplayedGodkandChips() >= 8;
    }

    private boolean isPostReviewFortnoxOrInk2State() {
        if (isInk2BokslutOrderStatusReached()) {
            return true;
        }
        if (!reviewDocumentsAlreadyApprovedOnManageOrder()) {
            return false;
        }
        return anyDisplayed(buildButtonPresenceLocator(
                "Väntar på Fortnox", "Waiting for Fortnox", "INK2/Bokslut", "INK2", "Bokslut"));
    }

    /**
     * Header / status chip showing {@code INK2/Bokslut}. Timeopt stops here and continues to Shiro.
     * Does not click the INK2 chip — that keeps the order stuck on this status.
     */
    public boolean isInk2BokslutOrderStatusReached() {
        try {
            if (isOnReviewWizardScreen()) {
                return false;
            }
            By ink2Status = By.xpath(
                    "//*[contains(normalize-space(),'INK2/Bokslut')]"
                            + " | //*[contains(.,'INK2') and contains(.,'Bokslut')]");
            if (anyDisplayed(ink2Status)) {
                return true;
            }
            String body = driver.findElement(By.tagName("body")).getText();
            return body != null && body.contains("INK2/Bokslut");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Drive Fortnox / review-complete chips only until INK2/Bokslut is visible.
     * Do not click INK2 or board-change — later Gherkin steps (when enabled) own those.
     */
    private void advancePostReviewWorkflowButtons() {
        openManageOrderTab();
        scrollPageToTop();
        if (isInk2BokslutOrderStatusReached()) {
            postReviewWorkflowAdvanced = true;
            LOG.info("Order already at INK2/Bokslut — not advancing past this status");
            return;
        }
        String[][] workflowSteps = new String[][]{
                {"Granskning klar", "Slutför granskning", "Review complete", "Review Complete"},
                {"Klar för betalning", "Väntar på betalning", "Waiting for payment"},
                {"Väntar på Fortnox", "Waiting for Fortnox"}
        };
        for (int step = 0; step < workflowSteps.length; step++) {
            if (isInk2BokslutOrderStatusReached()) {
                postReviewWorkflowAdvanced = true;
                LOG.info("INK2/Bokslut reached after post-review chip — stopping");
                return;
            }
            if (!tryClickWorkflowChipFast(workflowSteps[step])) {
                continue;
            }
            postReviewWorkflowAdvanced = true;
            LOG.info("Advanced post-review workflow via: {}", String.join(" / ", workflowSteps[step]));
            acceptUploadAlertIfPresent(2);
            completeWorkflowModalIfOpen();
            waitForSpecifiedTime(2);
            if (isInk2BokslutOrderStatusReached() || orderAlreadyPastReview() || isReadyForBoardChangeVisible()) {
                return;
            }
        }
        openManageOrderTab();
        scrollPageToTop();
        if (isInk2BokslutOrderStatusReached()) {
            postReviewWorkflowAdvanced = true;
        }
    }

    private boolean tryClickWorkflowChipFast(String... labels) {
        acceptUploadAlertIfPresent();
        openManageOrderTab();
        scrollPageToTop();
        for (WebElement button : driver.findElements(buildButtonPresenceLocator(labels))) {
            try {
                if (button.isDisplayed() && button.isEnabled()
                        && !"true".equalsIgnoreCase(String.valueOf(button.getAttribute("disabled")))) {
                    scrollElementIntoViewableArea(button);
                    jsClick(button);
                    waitForLoad();
                    waitForSpecifiedTime(2);
                    LOG.info("Clicked workflow chip matching: {}", String.join(" / ", labels));
                    return true;
                }
            } catch (Exception ignored) {
                // try next
            }
        }
        return false;
    }

    private void completeWorkflowModalIfOpen() {
        clickOnCurrentPageIfPresent(
                "Spara", "Save", "OK", "Ok", "Bekräfta", "Confirm", "Fortsätt", "Continue", "Stäng", "Close");
        acceptUploadAlertIfPresent(2);
    }

    private boolean orderAlreadyPastReview() {
        return isReadyForBoardChangeVisible()
                || anyDisplayed(buildButtonPresenceLocator(
                "Waiting for payment", "Väntar på betalning", "Klar för betalning",
                "Review complete", "Granskning klar", "Slutför granskning"));
    }

    private void completeGranskningTabInPlace() {
        if (reviewWizardWalkCompleted) {
            LOG.info("Skipping in-place Granskning tab — review wizard walk already completed");
            return;
        }
        if (!isOnReviewWizardScreen() && reviewDocumentsAlreadyApprovedOnManageOrder()) {
            LOG.info("Skipping in-place Granskning tab — documents already Godkänd on manage order");
            return;
        }
        openReviewWizardsTab();
        acceptUploadAlertIfPresent(2);
        mergeClientUploadsAcrossCategories();
        clickOnCurrentPageIfPresent(
                "Review complete", "Granskning klar", "Slutför granskning");
        exitReviewWizardToManageOrder();
    }

    private boolean clickOnCurrentPageIfPresent(String... labels) {
        acceptUploadAlertIfPresent();
        for (WebElement button : driver.findElements(buildButtonPresenceLocator(labels))) {
            try {
                if (button.isDisplayed() && button.isEnabled()
                        && !"true".equalsIgnoreCase(String.valueOf(button.getAttribute("disabled")))) {
                    jsClick(button);
                    acceptUploadAlertIfPresent(2);
                    waitForLoad();
                    waitForSpecifiedTime(2);
                    LOG.info("Clicked on-page control matching: {}", String.join(" / ", labels));
                    return true;
                }
            } catch (Exception ignored) {
                // try next
            }
        }
        return false;
    }

    private boolean clickReviewCompleteIfPresent() {
        return tryClickManageOrderButtonIfPresent(
                "Review complete", "Review Complete", "Complete review",
                "Granskning klar", "Slutför granskning", "Granskning slutförd",
                "Waiting for payment", "Väntar på betalning", "Klar för betalning")
                || forceClickManageOrderButtonIfPresent(
                "Review complete", "Review Complete", "Complete review",
                "Granskning klar", "Slutför granskning", "Granskning slutförd",
                "Klar för betalning");
    }

    public void clickReadyForBoardChange() {
        exitReviewWizardToManageOrder();
        if (!isReadyForBoardChangeVisible()) {
            advancePostReviewWorkflowButtons();
        }
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
        exitReviewWizardToManageOrder();
        boolean expectBoardChange = false;
        for (int i = 0; i < labelsEnSv.length; i++) {
            if (isBoardChangeLabel(labelsEnSv[i])) {
                expectBoardChange = true;
                break;
            }
        }
        if (expectBoardChange && !isReadyForBoardChangeVisible()) {
            advancePostReviewWorkflowButtons();
            reloadStoredOrderAndOpenManageOrder();
        }
        By locator = buildButtonPresenceLocator(labelsEnSv);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(ACTION_WAIT_SECONDS));
        scrollPageToTop();
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                if (anyDisplayed(locator)) {
                    scrollPageToViewElement(locator);
                }
                wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
                Assert.assertTrue(anyDisplayed(locator), "Expected button: " + String.join(" / ", labelsEnSv));
                return;
            } catch (TimeoutException e) {
                LOG.warn("Expected manage-order button not visible (attempt {}) — {}", attempt + 1,
                        String.join(" / ", labelsEnSv));
                scrollPageToTop();
        openManageOrderTab();
                waitForSpecifiedTime(2);
            }
        }
        wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        Assert.assertTrue(anyDisplayed(locator), "Expected button: " + String.join(" / ", labelsEnSv));
    }

    public void assertOrderStatusIndicatorPresent(String statusFragment) {
        scrollPageToTop();
        waitForSpecifiedTime(3);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(ACTION_WAIT_SECONDS));
        wait.until(d -> d.getPageSource().contains(statusFragment)
                || d.getPageSource().contains("complete")
                || d.getPageSource().contains("klar"));
        Assert.assertTrue(
                driver.getPageSource().contains(statusFragment)
                        || driver.getPageSource().toLowerCase().contains("liq_complete")
                        || driver.getPageSource().contains("Likvidation klar"),
                "Expected order page to reference status fragment: " + statusFragment);
    }

    private By buildButtonLocator(String... labels) {
        return buildButtonLocator(labels, true);
    }

    private By buildButtonPresenceLocator(String... labels) {
        return buildButtonLocator(labels, false);
    }

    private By buildButtonLocator(String[] labels, boolean requireEnabled) {
        StringBuilder xpath = new StringBuilder("//button[(@type='button' or @role='button')");
        if (requireEnabled) {
            xpath.append(" and not(@disabled)");
        }
        xpath.append("][");
        for (int i = 0; i < labels.length; i++) {
            if (i > 0) {
                xpath.append(" or ");
            }
            xpath.append("contains(., '").append(labels[i].replace("'", "")).append("')");
        }
        xpath.append("]");
        return By.xpath(xpath.toString());
    }

    private boolean tryClickManageOrderButtonIfPresent(String... labelsEnSv) {
        acceptUploadAlertIfPresent();
        openManageOrderTab();
        acceptUploadAlertIfPresent();
        try {
            scrollPageToTop();
        } catch (UnhandledAlertException e) {
            acceptUploadAlertIfPresent();
        }
        By presenceLocator = buildButtonPresenceLocator(labelsEnSv);
        WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(15));
        try {
            shortWait.until(ExpectedConditions.presenceOfElementLocated(presenceLocator));
        } catch (TimeoutException e) {
            return false;
        } catch (UnhandledAlertException e) {
            acceptUploadAlertIfPresent();
        }
        try {
            WebElement button = firstClickableManageOrderButton(labelsEnSv, 15);
            scrollElementIntoViewableArea(button);
            jsClick(button);
            waitForLoad();
            waitForSpecifiedTime(3);
            try {
                scrollPageToTop();
            } catch (UnhandledAlertException e) {
                acceptUploadAlertIfPresent();
            }
            LOG.info("Clicked manage-order button matching: {}", String.join(" / ", labelsEnSv));
            return true;
        } catch (TimeoutException | org.openqa.selenium.NoSuchElementException | UnhandledAlertException e) {
            acceptUploadAlertIfPresent();
            LOG.warn("Manage-order button present but not clickable: {}", String.join(" / ", labelsEnSv));
            return false;
        }
    }

    private WebElement firstClickableManageOrderButton(String[] labels, int timeoutSeconds) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return wait.until(d -> {
            for (WebElement button : d.findElements(buildButtonLocator(labels))) {
                try {
                    if (button.isDisplayed() && button.isEnabled()) {
                        return button;
                    }
                } catch (StaleElementReferenceException ignored) {
                    // retry
                }
            }
            return null;
        });
    }

    private boolean approveViaNativeSelect() {
        for (WebElement select : driver.findElements(By.tagName("select"))) {
            try {
                Select s = new Select(select);
                for (WebElement opt : s.getOptions()) {
                    String text = opt.getText();
                    if (text.contains("Approved") || text.contains("Godkänd")) {
                        try {
                            scrollElementIntoViewableArea(select);
                        } catch (Exception ignored) {
                            // hidden native select is still operable
                        }
                        s.selectByVisibleText(text);
                        acceptUploadAlertIfPresent(2);
                        LOG.info("Set document status to Approved via native select ({})", text.trim());
                        return true;
                    }
                }
            } catch (UnhandledAlertException e) {
                acceptUploadAlertIfPresent();
                return true;
            } catch (Exception ignored) {
                // continue
            }
        }
        return false;
    }

    private boolean approveViaCombobox() {
        By combobox = By.xpath(
                "//*[@role='combobox'] | //button[contains(@aria-haspopup,'listbox')]");
        for (WebElement control : driver.findElements(combobox)) {
            try {
                if (!control.isDisplayed()) {
                    continue;
                }
                scrollElementIntoViewableArea(control);
                jsClick(control);
                waitForSpecifiedTime(1);
                By approvedOption = By.xpath(
                        "//*[@role='option'][contains(., 'Approved') or contains(., 'Godkänd')]"
                                + " | //*[@role='listbox']//li[contains(., 'Approved') or contains(., 'Godkänd')]");
                WebElement option = new WebDriverWait(driver, Duration.ofSeconds(10))
                        .until(ExpectedConditions.elementToBeClickable(approvedOption));
                scrollElementIntoViewableArea(option);
                jsClick(option);
                waitForSpecifiedTime(2);
                LOG.info("Set document status to Approved via combobox");
                return true;
            } catch (Exception ignored) {
                // continue
            }
        }
        return false;
    }

    private boolean approveViaPendingStatusTrigger() {
        By pendingStatus = By.xpath(
                "//button[normalize-space()='Mottagen' or normalize-space()='Received'"
                        + " or normalize-space()='Pending' or normalize-space()='Väntar'"
                        + " or normalize-space()='To review' or normalize-space()='Att granska'"
                        + " or normalize-space()='Not reviewed' or normalize-space()='Ej granskad']"
                        + " | //*[@role='combobox'][normalize-space()='Mottagen' or normalize-space()='Received'"
                        + " or normalize-space()='Pending' or normalize-space()='Väntar']"
                        + " | //label[contains(., 'Dokumentatstatus') or contains(., 'Dokumentstatus')]"
                        + "/following::*[self::button or self::select or @role='combobox'][1]");
        for (WebElement control : driver.findElements(pendingStatus)) {
            try {
                if (!control.isDisplayed()) {
                    continue;
                }
                scrollElementIntoViewableArea(control);
                jsClick(control);
                By approvedOption = By.xpath(
                        "//*[@role='option'][contains(., 'Approved') or contains(., 'Godkänd')]"
                                + " | //li[contains(., 'Approved') or contains(., 'Godkänd')]"
                                + " | //button[normalize-space()='Godkänd' or normalize-space()='Approved']");
                WebElement option = new WebDriverWait(driver, Duration.ofSeconds(8))
                        .until(ExpectedConditions.elementToBeClickable(approvedOption));
                jsClick(option);
                acceptUploadAlertIfPresent();
                LOG.info("Set document status to Approved via pending-status control");
                return true;
            } catch (UnhandledAlertException e) {
                acceptUploadAlertIfPresent();
                return true;
            } catch (Exception ignored) {
                // try next
            }
        }
        return false;
    }

    private void markOutstandingDocumentTypesIfPresent() {
        markOutstandingDocumentTypesNotNeeded();
    }

    private void markOutstandingDocumentTypesNotNeeded() {
        openManageOrderTab();
        if (displayedNotReceivedDocumentTypes().isEmpty()) {
            return;
        }
        for (int i = 0; i < 8; i++) {
            acceptUploadAlertIfPresent();
            List<WebElement> outstanding = displayedNotReceivedDocumentTypes();
            if (outstanding.isEmpty()) {
                return;
            }
            WebElement typeButton = outstanding.get(0);
            try {
                jsClick(typeButton);
                waitForSpecifiedTime(1);
                clickIfDisplayed(By.xpath(
                        "//*[normalize-space()='behövs ej' or normalize-space()='Behövs ej'"
                                + " or contains(., 'not needed') or contains(., 'Not needed')]"
                                + "[self::button or self::label or self::span or self::div]"));
                approveViaStatusChip();
                saveReviewWizardChangesIfNeeded();
                acceptUploadAlertIfPresent(2);
                LOG.info("Marked remaining Ej mottaget document type as not needed / Godkänd");
            } catch (Exception e) {
                LOG.debug("Could not mark Ej mottaget type: {}", e.getMessage());
                break;
            }
        }
    }

    private boolean approveViaStatusChip() {
        try {
            Object clicked = ((JavascriptExecutor) driver).executeScript(
                    "function norm(s) { return (s || '').replace(/\\s+/g, ' ').trim(); }"
                            + "var nodes = document.querySelectorAll('button,label,span,div,li,p,input');"
                            + "var best = null;"
                            + "var bestLen = 999999;"
                            + "for (var i = 0; i < nodes.length; i++) {"
                            + "  var t = norm(nodes[i].innerText || nodes[i].textContent || nodes[i].value);"
                            + "  if (t !== 'Godkänd' && t !== 'Approved') { continue; }"
                            + "  var len = (nodes[i].outerHTML || '').length;"
                            + "  if (len < bestLen) { best = nodes[i]; bestLen = len; }"
                            + "}"
                            + "if (!best) { return false; }"
                            + "var target = best;"
                            + "if (best.htmlFor) {"
                            + "  var byId = document.getElementById(best.htmlFor);"
                            + "  if (byId) { target = byId; }"
                            + "}"
                            + "var radio = best.querySelector ? best.querySelector('input') : null;"
                            + "if (radio) { target = radio; }"
                            + "target.click();"
                            + "if (target.checked === false) { target.checked = true; }"
                            + "target.dispatchEvent(new Event('input', {bubbles:true}));"
                            + "target.dispatchEvent(new Event('change', {bubbles:true}));"
                            + "return true;");
            if (Boolean.TRUE.equals(clicked)) {
                acceptUploadAlertIfPresent();
                LOG.info("Set document status to Godkänd via status chip (JS exact text)");
                return true;
            }
        } catch (UnhandledAlertException e) {
            acceptUploadAlertIfPresent();
            return true;
        } catch (Exception ignored) {
            // fall through to locator click
        }
        By chips = By.xpath(
                "//*[normalize-space()='Godkänd' or normalize-space()='Approved']"
                        + "[self::button or self::label or self::span or self::div or self::li or self::p]");
        for (WebElement chip : driver.findElements(chips)) {
            try {
                if (!chip.isDisplayed()) {
                    continue;
                }
                String tag = chip.getTagName();
                if ("html".equalsIgnoreCase(tag) || "body".equalsIgnoreCase(tag) || "main".equalsIgnoreCase(tag)) {
                    continue;
                }
                jsClick(chip);
                acceptUploadAlertIfPresent();
                LOG.info("Set document status to Godkänd via status chip ({})", tag);
                return true;
            } catch (UnhandledAlertException e) {
                acceptUploadAlertIfPresent();
                return true;
            } catch (Exception ignored) {
                // try next match
            }
        }
        return false;
    }

    private void goToFirstDocumentInWizard() {
        By previous = By.xpath(
                "//button[@type='button'][normalize-space()='Föregående' or normalize-space()='Previous']");
        for (int i = 0; i < 50; i++) {
            boolean clicked = false;
            String keyBeforeClick = currentReviewDocumentKey();
            for (WebElement button : driver.findElements(previous)) {
                try {
                    if (button.isDisplayed() && button.isEnabled()
                            && !"true".equalsIgnoreCase(String.valueOf(button.getAttribute("disabled")))) {
                        jsClick(button);
                        acceptUploadAlertIfPresent();
                        clicked = true;
                        break;
                    }
                } catch (Exception ignored) {
                    // try next
                }
            }
            if (!clicked) {
                return;
            }
            try {
                new WebDriverWait(driver, Duration.ofSeconds(2))
                        .until(d -> {
                            String currentKey = currentReviewDocumentKey();
                            return currentKey.length() > 8 && !currentKey.equals(keyBeforeClick);
                        });
            } catch (TimeoutException e) {
                return;
            }
        }
    }

    private boolean approveAllDocumentsInWizard() {
        return walkAndApproveReviewDocuments(false);
    }

    private void waitForStatusChips() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5)).until(d ->
                    anyDisplayed(By.xpath(
                            "//*[normalize-space()='Godkänd' or normalize-space()='Approved']"
                                    + "[self::button or self::label or self::span or self::div or self::li]")));
        } catch (TimeoutException ignored) {
            // chip may already be selected / not yet painted
        }
    }

    private void approveRemainingCustomerUploadRows() {
        openManageOrderTab();
        scrollPageToTop();
        if (countDisplayedGodkandChips() >= 8) {
            LOG.info("Skipping Uppladdat av kund loop — already {} Godkänd on manage order",
                    countDisplayedGodkandChips());
            return;
        }
        int approved = 0;
        for (int i = 0; i < 10; i++) {
            WebElement pending = firstDisplayedButton(
                    "Uppladdat av kund", "Uploaded by customer");
            if (pending == null) {
                break;
            }
            scrollElementIntoViewableArea(pending);
            jsClick(pending);
            acceptUploadAlertIfPresent(1);
            waitForStatusChips();
            if (approveViaStatusChip()) {
                approved++;
                waitForLoad();
                waitForSpecifiedTime(1);
            } else {
                LOG.warn("Could not set Godkänd on remaining Uppladdat av kund row");
                break;
            }
        }
        if (approved > 0) {
            LOG.info("Approved {} remaining customer-upload document rows", approved);
        }
    }

    private WebElement firstDisplayedButton(String... labels) {
        By locator = buildButtonPresenceLocator(labels);
        for (WebElement button : driver.findElements(locator)) {
            try {
                if (button.isDisplayed()) {
                    return button;
                }
            } catch (Exception ignored) {
                // stale
            }
        }
        return null;
    }

    private boolean clickNextDocumentInWizard() {
        acceptUploadAlertIfPresent();
        String previousKey = currentReviewDocumentKey();
        By next = By.xpath(
                "//button[@type='button'][normalize-space()='Nästa' or normalize-space()='Next']");
        try {
            for (WebElement button : driver.findElements(next)) {
                try {
                    if (button.isDisplayed() && button.isEnabled()
                            && !"true".equalsIgnoreCase(String.valueOf(button.getAttribute("disabled")))) {
                        jsClick(button);
                        acceptUploadAlertIfPresent(1);
                        waitForReviewPageChange(previousKey, Duration.ofSeconds(3));
                        LOG.info("Moved to next document in review wizard");
                        return true;
                    }
                } catch (UnhandledAlertException e) {
                    acceptUploadAlertIfPresent();
                    LOG.info("Moved to next document in review wizard (alert accepted)");
                    return true;
                } catch (StaleElementReferenceException ignored) {
                    // try next match
                }
            }
        } catch (UnhandledAlertException e) {
            acceptUploadAlertIfPresent();
            return true;
        }
        return false;
    }

    private boolean approveViaCustomOption() {
        for (WebElement option : driver.findElements(approveDocStatusOption)) {
            try {
                if (option.isDisplayed()) {
                    scrollElementIntoViewableArea(option);
                    jsClick(option);
                    waitForSpecifiedTime(2);
                    LOG.info("Set document status to Approved via custom option");
                    return true;
                }
            } catch (Exception ignored) {
                // continue
            }
        }
        return false;
    }

    private void saveReviewWizardChangesIfNeeded() {
        By saveButton = By.xpath(
                "//button[@type='button' and not(@disabled)]"
                        + "[contains(., 'Save') or contains(., 'Spara') or contains(., 'Update') or contains(., 'Uppdatera')]");
        for (WebElement button : driver.findElements(saveButton)) {
            try {
                if (button.isDisplayed()) {
                    scrollElementIntoViewableArea(button);
                    jsClick(button);
                    acceptUploadAlertIfPresent(1);
                    LOG.info("Saved review-wizard changes");
                    return;
                }
            } catch (Exception ignored) {
                // continue
            }
        }
    }

    private void waitForReviewPageChange(String previousKey, Duration timeout) {
        if (previousKey == null || previousKey.isEmpty()) {
            return;
        }
        try {
            new WebDriverWait(driver, timeout).until(d -> {
                String currentKey = currentReviewDocumentKey();
                return currentKey.length() > 8 && !currentKey.equals(previousKey);
            });
        } catch (TimeoutException e) {
            LOG.debug("Review page transition did not change the document key within {} ms", timeout.toMillis());
        }
    }

    private void clickTabIfPresent(By tab) {
        acceptUploadAlertIfPresent();
        scrollPageToTop();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(TAB_WAIT_SECONDS));
        try {
            wait.until(d -> {
                acceptUploadAlertIfPresent();
                return anyDisplayed(tab) || anyDisplayed(orderDetailShell);
            });
        } catch (UnhandledAlertException e) {
            acceptUploadAlertIfPresent();
        }
        for (int attempt = 0; attempt < 4; attempt++) {
            List<WebElement> tabs = driver.findElements(tab);
            for (WebElement el : tabs) {
            try {
                if (el.isDisplayed()) {
                        scrollElementIntoViewableArea(el);
                        LOG.info("Opening tab: {}", el.getText().replace('\n', ' ').trim());
                        jsClick(el);
                        waitForSpecifiedTime(2);
                        scrollPageToTop();
                    return;
                }
                } catch (StaleElementReferenceException ignored) {
                    break;
            } catch (Exception ignored) {
                    // retry next element
                }
            }
            LOG.warn("Tab not clickable yet (attempt {}) — scrolling to top and retrying", attempt + 1);
            scrollPageToTop();
            waitForSpecifiedTime(2);
        }
        wait.until(ExpectedConditions.presenceOfElementLocated(tab));
        scrollPageToViewElement(tab);
        clickByJS(tab);
        waitForSpecifiedTime(2);
        scrollPageToTop();
    }

    private void waitForOrderDetailShell() {
        WebDriverWait shellWait = new WebDriverWait(driver, Duration.ofSeconds(ACTION_WAIT_SECONDS));
        shellWait.until(d -> {
            String current = d.getCurrentUrl();
            if (current == null || !current.contains("/genericOrder/list/")) {
                return false;
            }
            return anyDisplayed(manageOrderTab)
                    || anyDisplayed(By.name("totalAssets"))
                    || anyDisplayed(orderDetailShell);
        });
        waitForSpecifiedTime(2);
    }

    private void scrollToOrderDetailContent() {
        if (anyDisplayed(manageOrderTab)) {
            scrollPageToViewElement(manageOrderTab);
            return;
        }
        if (anyDisplayed(By.name("totalAssets"))) {
            scrollPageToViewElement(By.name("totalAssets"));
            return;
        }
        scrollPageToViewElement(orderDetailShell);
    }

    private boolean anyDisplayed(By by) {
        try {
        for (WebElement el : driver.findElements(by)) {
            try {
                if (el.isDisplayed()) {
                    return true;
                }
            } catch (Exception ignored) {
                // continue
            }
            }
        } catch (UnhandledAlertException e) {
            acceptUploadAlertIfPresent();
        }
        return false;
    }

    private void jsClick(WebElement element) {
        safeClick(element);
    }

    /** Scroll into view, then native click with Actions / JS fallback (review wizard sidebars). */
    private void safeClick(WebElement element) {
        try {
            scrollElementIntoViewableArea(element);
        } catch (Exception ignored) {
            // element may still be clickable
        }
        try {
            new Actions(driver).moveToElement(element).click().perform();
            return;
        } catch (Exception ignored) {
            // fall through
        }
        try {
            element.click();
        } catch (Exception ignored) {
            ((JavascriptExecutor) driver).executeScript(
                    "var el = arguments[0];"
                            + "el.dispatchEvent(new MouseEvent('mousedown', {bubbles:true}));"
                            + "el.dispatchEvent(new MouseEvent('mouseup', {bubbles:true}));"
                            + "el.click();",
                    element);
        }
    }
}
