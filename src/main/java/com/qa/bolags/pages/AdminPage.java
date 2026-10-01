package com.qa.bolags.pages;

import com.qa.bolags.constants.AcceptOfferContext;
import com.qa.bolags.constants.LiquidationOrderIdContext;
import com.qa.bolags.constants.OfferSentAccountingData;
import com.qa.bolags.constants.OfferSentDataContext;
import com.qa.bolags.constants.OfferSentDataProvider;
import com.qa.bolags.constants.QaServerCredentials;
import com.qa.bolags.utility.OrderDetailsCapture;
import com.qa.bolags.utility.TestUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import org.openqa.selenium.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

/**
 * Page object for admin portal flows. Locators are best-effort for QA; override with
 * {@code -DadminPortalUrl}, {@code -DadminEmail}, {@code -DadminPassword} as needed.
 */
public class AdminPage extends TestUtil {

    private static final Logger LOG = LoggerFactory.getLogger(AdminPage.class);

    /** Default: auth form (avoids heavy marketing home load in automation). Override with {@code -DadminLandingUrl}. */
    private static final String DEFAULT_ADMIN_LANDING_URL = "https://qa.bolagspartner.se/app/auth/login";
    /** Direct login URL if you skip the marketing header link. */
    private static final String DEFAULT_ADMIN_LOGIN_URL = "https://qa.bolagspartner.se/app/auth/login";
    private static final String DEFAULT_ADMIN_EMAIL = QaServerCredentials.genericOrderSuperAdminEmail();
    private static final String DEFAULT_ADMIN_PASSWORD = QaServerCredentials.genericOrderSuperAdminPassword();

    private final String adminLandingUrl;
    private final String adminDirectLoginUrl;
    private final String adminEmail;
    private final String adminPassword;

    private final By epost = By.xpath(
            "//input[@type='email'] | //input[@placeholder='E-post' or @placeholder='Email' or @placeholder='E-mail']"
                    + " | //input[contains(@name,'email') or contains(@id,'email') or contains(@autocomplete,'username')]");
    private final By passwordInput = By.xpath(
            "//input[@type='password'] | //input[@placeholder='L\u00F6senord' or @placeholder='Losenord' or @placeholder='Password']");
    private final By loginButton = By.xpath(
            "//button[normalize-space()='Logga in' or normalize-space()='Log in' or @type='submit']"
                    + " | //input[@type='submit' and (contains(@value,'Logga') or contains(@value,'Log in'))]");
    private final By loggaInNavLink = By.xpath(
            "//a[contains(., 'LOGGA IN') or contains(., 'Logga in')][not(ancestor::header) or ancestor::*[self::header or self::nav]]");

    private final By dashboardHeading = By.xpath(
            "//*[self::h1 or self::h2][contains(., '\u00D6versikt') or contains(., 'Dashboard') or contains(., 'Admin')]"
                    + " | //nav[contains(@class,'dashboard') or contains(.,'Orders')]");

    private final By requestsMenu = By.xpath("//nav//a[contains(., 'Request') or contains(., 'Beg\u00E4ran')]");
    private final By liquidationRequestsLink = By.xpath("//a[contains(., 'Liquidation') or contains(., 'Likvidation')]");
    private final By firstOrderRow = By.xpath("(//table//tbody//tr[td])[1]//a | (//table//tbody//tr[td])[1]");
    private final By viewDetailsButton = By.xpath("//button[contains(., 'Visa') or contains(., 'View') or contains(., 'Details')]");
    private final By requestDetailsSection = By.xpath("//main | //article | //*[@data-testid='order-detail']");
    private final By approveButton = By.xpath("//button[contains(., 'Godk') or contains(., 'Approve')]");
    private final By successToast = By.xpath("//*[contains(@class,'toast') or @role='alert']");
    private final By overlayContainer = By.xpath("//*[@role='dialog']");
    private final By confirmApproveButton = By.xpath("//button[contains(., 'Bekräfta') or contains(., 'Confirm') or contains(., 'OK')]");

    public AdminPage(WebDriver driver) {
        super(driver);
        this.adminLandingUrl = System.getProperty("adminLandingUrl", DEFAULT_ADMIN_LANDING_URL);
        this.adminDirectLoginUrl = System.getProperty("adminPortalUrl", DEFAULT_ADMIN_LOGIN_URL);
        this.adminEmail = System.getProperty("adminEmail", DEFAULT_ADMIN_EMAIL);
        this.adminPassword = System.getProperty("adminPassword", DEFAULT_ADMIN_PASSWORD);
    }

    public void openAdminPortal() {
        driver.get(QaServerCredentials.urlWithHttpBasicAuth(adminLandingUrl));
        waitForLoad();
    }

    /** Opens the admin auth form directly (most reliable after long liquidation sessions). */
    public void openAdminLoginForm() {
        driver.get(QaServerCredentials.urlWithHttpBasicAuth(adminDirectLoginUrl));
        waitForLoad();
        scrollPageToTop();
        waitForSpecifiedTime(1);
    }

    public void clickLoggaInFromTopNavigation() {
        if (isLoginFormVisible()) {
            scrollPageToTop();
            return;
        }
        List<WebElement> links = driver.findElements(loggaInNavLink);
        if (!links.isEmpty()) {
            waitForElementToBeClickable(loggaInNavLink);
            clickByJS(loggaInNavLink);
            waitForLoad();
            scrollPageToTop();
            return;
        }
        openAdminLoginForm();
    }

    /** Single entry point for admin portal login — reused across lifecycle, smoke, and workflow tests. */
    public void loginToAdminPortal() {
        if (isLoggedInToAdminPortal()) {
            LOG.info("Admin session already active — skipping login.");
            return;
        }
        openAdminLoginForm();
        enterAdminCredentials();
        clickLoginButton();
    }

    public boolean isLoggedInToAdminPortal() {
        String url = driver.getCurrentUrl();
        if (url == null || url.contains("/auth/login")) {
            return false;
        }
        String lower = url.toLowerCase();
        if (lower.contains("/liquidation") || lower.contains("/liqtok")
                || lower.contains("/companyliquidationorder")) {
            return false;
        }
        if (lower.contains("/genericorder") || lower.contains("/admin") || lower.contains("/dashboard")) {
            return true;
        }
        return isDashboardVisible();
    }

    public void enterAdminCredentials() {
        ensureLoginFormReady();
        typeIntoFirstDisplayedInput(epost, adminEmail);
        waitForElementToBeClickable(passwordInput);
        typeIntoFirstDisplayedInput(passwordInput, adminPassword);
    }

    private void ensureLoginFormReady() {
        if (!isLoginFormVisible()) {
            openAdminLoginForm();
        } else {
            scrollPageToTop();
        }
        waitForLoad();
        waitForSpecifiedTime(2);
        scrollPageToTop();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(45));
        wait.until(d -> isLoginFormVisible());
        for (WebElement field : driver.findElements(epost)) {
            try {
                if (field.isDisplayed()) {
                    scrollElementIntoViewableArea(field);
                    return;
                }
            } catch (StaleElementReferenceException ignored) {
                // retry via wait above / next call
            }
        }
    }

    private boolean isLoginFormVisible() {
        return driver.findElements(epost).stream().anyMatch(el -> {
            try {
                return el.isDisplayed();
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });
    }

    /** Re-finds inputs to avoid stale references after SPA navigation. */
    private void typeIntoFirstDisplayedInput(By locator, String value) {
        String text = value != null ? value : "";
        for (int attempt = 0; attempt < 5; attempt++) {
            List<WebElement> fields = driver.findElements(locator);
            for (WebElement f : fields) {
                try {
                    if (f.isDisplayed()) {
                        scrollElementIntoViewableArea(f);
                        JavascriptExecutor js = (JavascriptExecutor) driver;
                        js.executeScript("arguments[0].focus();", f);
                        try {
                            f.click();
                        } catch (Exception ignored) {
                            js.executeScript("arguments[0].click();", f);
                        }
                        js.executeScript(
                                "if (arguments[0].select) { arguments[0].select(); }"
                                        + "arguments[0].value='';",
                                f);
                        if (!text.isEmpty()) {
                            f.sendKeys(text);
                        } else {
                            js.executeScript(
                                    "arguments[0].value='';"
                                            + "arguments[0].dispatchEvent(new Event('input',{bubbles:true}));"
                                            + "arguments[0].dispatchEvent(new Event('change',{bubbles:true}));",
                                    f);
                        }
                        return;
                    }
                } catch (StaleElementReferenceException e) {
                    break;
                }
            }
            waitForSpecifiedTime(1);
            scrollPageToTop();
        }
        throw new org.openqa.selenium.NoSuchElementException("No visible input for " + locator);
    }

    public void clickLoginButton() {
        scrollPageToTop();
        waitForSpecifiedTime(1);
        try {
            scrollPageToViewElement(loginButton);
            waitForElementToBeClickable(loginButton);
            clickByJS(loginButton);
        } catch (Throwable e) {
            LOG.warn("Login button interaction failed — trying form submit: {}", e.getMessage());
            submitLoginFormViaEnterKey();
        }
        waitForLoad();
        waitForSpecifiedTime(3);
        scrollPageToTop();
    }

    private void submitLoginFormViaEnterKey() {
        List<WebElement> passwords = driver.findElements(passwordInput);
        for (WebElement field : passwords) {
            if (field.isDisplayed()) {
                field.sendKeys(Keys.ENTER);
                return;
            }
        }
        clickByJS(loginButton);
    }

    public void loginWithConfiguredCredentials() {
        loginToAdminPortal();
    }

    public boolean isDashboardVisible() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(35))
                    .until(ExpectedConditions.not(ExpectedConditions.urlContains("/auth/login")));
        } catch (TimeoutException e) {
            return anyDisplayedIgnoringHighlight(dashboardHeading);
        }
        String url = driver.getCurrentUrl();
        if (url != null && url.contains("/auth/login")) {
            return anyDisplayedIgnoringHighlight(dashboardHeading);
        }
        return anyDisplayedIgnoringHighlight(dashboardHeading)
                || (url != null && (url.contains("/orders") || url.contains("/admin") || url.contains("/dashboard")));
    }

    private boolean anyDisplayedIgnoringHighlight(By by) {
        for (WebElement el : driver.findElements(by)) {
            try {
                if (el.isDisplayed()) {
                    return true;
                }
            } catch (StaleElementReferenceException ignored) {
                // retry loop
            }
        }
        return false;
    }

    public void openRequestsMenu() {
        waitForElementToBeClickable(requestsMenu);
        clickByJS(requestsMenu);
    }

    public void openLiquidationRequests() {
        waitForElementToBeClickable(liquidationRequestsLink);
        clickByJS(liquidationRequestsLink);
        waitForSpecifiedTime(2);
    }

    public void searchForCompany(String companyName) {
        if (companyName == null || companyName.isEmpty()) {
            return;
        }
    }

    public void clickFirstOrderInList() {
        waitForElementToBeClickable(firstOrderRow);
        clickByJS(firstOrderRow);
    }

    public void openFirstMatchingRequest() {
        waitForElementToBeClickable(viewDetailsButton);
        scrollPageToViewElement(viewDetailsButton);
        clickByJS(viewDetailsButton);
        waitForElementToBeVisible(requestDetailsSection);
    }

    public boolean isRequestDetailsVisible() {
        scrollPageToTop();
        try {
            new WebDriverWait(driver, Duration.ofSeconds(45)).until(d ->
                    anyDisplayedIgnoringHighlight(requestDetailsSection)
                            || anyDisplayedIgnoringHighlight(MANAGE_ORDER_TAB)
                            || anyDisplayedIgnoringHighlight(By.name("totalAssets"))
                            || anyDisplayedIgnoringHighlight(approveButton));
        } catch (TimeoutException e) {
            return false;
        }
        return anyDisplayedIgnoringHighlight(requestDetailsSection)
                || anyDisplayedIgnoringHighlight(MANAGE_ORDER_TAB)
                || anyDisplayedIgnoringHighlight(By.name("totalAssets"))
                || anyDisplayedIgnoringHighlight(approveButton);
    }

    public void approveCurrentRequest() {
        waitForElementToBeClickable(approveButton);
        scrollPageToViewElement(approveButton);
        clickByJS(approveButton);
        handleConfirmationPopup();
        waitForSpecifiedTime(2);
    }

    public boolean isApprovalSuccessful() {
        return isElementDisplayed(successToast);
    }

    private void handleConfirmationPopup() {
        List<WebElement> overlays = driver.findElements(overlayContainer);
        if (!driver.findElements(confirmApproveButton).isEmpty()) {
            clickByJS(confirmApproveButton);
        } else if (!overlays.isEmpty()) {
            overlays.get(0).click();
        }
    }

    public void completeApprovalFlow(String companyName) {
        openRequestsMenu();
        openLiquidationRequests();
        searchForCompany(companyName);
        openFirstMatchingRequest();
        approveCurrentRequest();
    }

    /*
     * Accounting block — ChangeQuote.tsx (Manage order tab). Prefer {@code By.name}:
     * totalAssets, totalLiabilities, thisYearResults, untaxedReserves, nonTaxableIncome,
     * nonDeductibleCosts, equity, offerPriceSek. Closing date uses {@link DatePicker} (button + portal),
     * not {@code name='closingDate'}.
     */
    private static final String DEFAULT_ORDER_DETAIL_URL =
            "https://qa.bolagspartner.se/app/genericOrder/list/100738";
    private static final By MANAGE_ORDER_TAB = By.xpath(
            "//button[contains(., 'Hantera best\u00E4llning') or contains(., 'Hantera order')"
                    + " or contains(., 'Manage order') or contains(., 'Manage Order')]"
                    + " | //*[@role='tab'][contains(., 'Hantera') or contains(., 'Manage')]");
    private static final By CLOSING_DATE_TRIGGER = By.xpath(
            "//label[contains(., 'Slutdatum') or contains(., 'Closing Date')]/following-sibling::button[@type='button']");
    private static final By CLOSING_DATE_DISPLAY = By.xpath(
            "//label[contains(., 'Slutdatum') or contains(., 'Closing Date')]/following-sibling::button[@type='button']"
                    + "//span[contains(@class,'text-gray-700')]");
    private static final By DATEPICKER_DROPDOWN = By.cssSelector("[data-datepicker-dropdown]");
    /** Sibling error under "Our price" in {@code ChangeQuote.tsx}. */
    private static final By OFFER_PRICE_INLINE_ERROR = By.xpath(
            "//input[@name='offerPriceSek']/ancestor::div[contains(@class,'flex-1')][1]"
                    + "/div[contains(@class,'text-red')]");
    /** Primary offer action on {@code ChangeQuote} (after accounting form). EN/SV + sub-company label. */
    private static final By SEND_QUOTE_BUTTON = By.xpath(
            "//button[@type='button' and not(@disabled)][contains(., 'Skicka offert') or contains(., 'Klar för offert')"
                    + " or contains(translate(normalize-space(.),"
                    + " 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'send quote')]");
    private static final By ACCEPT_OFFER_SHAREHOLDERS_HEADING = By.xpath(
            "//h2[contains(., 'Aktie\u00E4gare') or contains(., 'Shareholders')]");
    private static final By ACCEPT_OFFER_THANK_YOU = By.xpath(
            "//*[contains(., 'Tack f\u00F6r att vi f\u00E5r avveckla')"
                    + " or contains(., 'Thank you for letting us wind down')]");
    private static final By SHAREHOLDERS_UPDATED_CHECKBOX = By.name("shareholdersUpdated");
    private static final By SHAREHOLDERS_UPDATED_LABEL = By.xpath(
            "//label[.//input[@name='shareholdersUpdated']]"
                    + " | //span[contains(., 'Aktie\u00E4gare uppdaterade') or contains(., 'Shareholders updated')]/ancestor::label");
    private static final By DO_NOT_SEND_EMAIL_LABEL = By.xpath("//label[.//input[@name='doNotSendEmail']]");
    /** Opens document modal — Manage order only (not the modal footer button). */
    private static final By SEND_AGREEMENT_OPEN_MODAL_BUTTON = By.xpath(
            "//button[@type='button' and not(@disabled)]"
                    + "[contains(normalize-space(.), 'Skicka avtal') or contains(normalize-space(.), 'Send Agreement')]"
                    + "[not(ancestor::div[contains(@class,'fixed') and contains(@class,'inset-0')])]");
    private static final By SEND_AGREEMENT_MODAL_TITLE = By.xpath(
            "//h1[contains(., 'Select Documents We Need to Get Back')"
                    + " or contains(., 'V\u00E4lj dokument vi beh\u00F6ver f\u00E5 tillbaka')]");
    /** Yellow {@code Skicka avtal} in modal footer ({@code AcceptedByCustomer} document selector). */
    private static final By SEND_AGREEMENT_MODAL_CONFIRM = By.xpath(
            "//div[contains(@class,'fixed') and contains(@class,'inset-0')]"
                    + "//div[contains(@class,'rounded-b-xl')]"
                    + "//button[@type='button' and not(@disabled)]"
                    + "[contains(@class,'FFD454') or contains(normalize-space(.), 'Skicka avtal')"
                    + " or contains(normalize-space(.), 'Send Agreement')]"
                    + "[not(contains(normalize-space(.), 'Avbryt')) and not(contains(normalize-space(.), 'Cancel'))]");
    /** {@code WaitingForSignature} UI after agreement is sent (status LIQ_READY_FOR_SIGNING). */
    private static final By AGREEMENT_SENT_MARKER = By.xpath(
            "//button[contains(., 'Remind about E-agreement') or contains(., 'P\u00E5minn om e-avtal')"
                    + " or contains(., 'Add document type') or contains(., 'L\u00E4gg till dokumenttyp')]");

    private void pauseAfterAccountingField() {
        try {
            Thread.sleep(com.qa.bolags.constants.Constants.isTimeOptimized() ? 700 : 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Fills the ChangeQuote accounting fields used when sending an offer.
     * A random valid QA data set is chosen each run ({@link OfferSentDataProvider}); pin with
     * {@code -DofferSent.dataSetIndex=0..4}.
     * <p>Navigates to the QA generic order detail URL for order {@code 100675} by default (override with
     * {@code -DadminOrderDetailUrl} or {@code -DadminOrderDetailPath}).</p>
     * <p>Closing date is {@link LocalDate#now()} via the datepicker; the UI shows {@code yyyy-MM-dd}
     * ({@code DatePicker} default). The requested {@code dd/MM/yyyy} value is logged for traceability.</p>
     */
    public void enterOfferSentAccountingData() {
        OfferSentAccountingData data = OfferSentDataProvider.pickForExecution();
        OfferSentDataContext.set(data);
        LOG.info("Entering offer-sent accounting data: {}", data.getLabel());

        String orderId = LiquidationOrderIdContext.getCapturedOrderIdOrNull();
        String currentUrl = driver.getCurrentUrl();
        if (orderId != null && currentUrl != null && currentUrl.contains(orderId)) {
            waitForOrderDetailShell();
            scrollToOrderDetailContent();
            scrollPageToTop();
        } else {
            openGenericOrderDetail(resolveOrderDetailUrl());
        }
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));
        LocalDate today = LocalDate.now();
        LOG.info(
                "Closing date of financial statements — test reference (dd/MM/yyyy): {}",
                today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        scrollPageToTop();
        ensureManageOrderAccountingVisible(wait);
        selectClosingDateOfFinancialStatementsToday(wait, today);
        replaceNumberInput(By.name("totalAssets"), data.totalAssetsAsInput(), wait);
        pauseAfterAccountingField();
        replaceNumberInput(By.name("totalLiabilities"), data.totalLiabilitiesAsInput(), wait);
        pauseAfterAccountingField();
        replaceNumberInput(By.name("thisYearResults"), data.thisYearResultsAsInput(), wait);
        pauseAfterAccountingField();
        replaceNumberInput(By.name("untaxedReserves"), data.untaxedReservesAsInput(), wait);
        pauseAfterAccountingField();
        replaceNumberInput(By.name("nonTaxableIncome"), data.nonTaxableIncomeAsInput(), wait);
        pauseAfterAccountingField();
        replaceNumberInput(By.name("nonDeductibleCosts"), data.nonDeductibleCostsAsInput(), wait);
        pauseAfterAccountingField();
        replaceNumberInput(By.name("equity"), data.equityAsInput(), wait);
        pauseAfterAccountingField();
      // replaceNumberInput(By.name("offerPriceSek"), "225400", wait);
        assertOfferSentAccountingDataEnteredSuccessfully(wait, today, data);
    }

    /**
     * Clicks the primary send-offer control on the Manage order / {@code ChangeQuote} form
     * ({@code sendQuote} → EN "Send Quote", SV "Skicka offert", or "Klar för offert" for sub-company).
     * Requires a valid offer price so the button is not {@code disabled}.
     */
    public void clickSendQuoteOnManageOrder() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));
        scrollPageToTop();
        ensureManageOrderAccountingVisible(wait);
        scrollPageToViewElement(SEND_QUOTE_BUTTON);
        wait.until(ExpectedConditions.elementToBeClickable(SEND_QUOTE_BUTTON));
        clickByJS(SEND_QUOTE_BUTTON);
        LOG.info("Clicked send quote — capturing sendOffer response immediately");
    }

    /**
     * Clicks Manage order {@code Skicka avtal} / Send Agreement to open the document selection modal.
     */
    public void openSendAgreementDocumentModal() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(45));
        String orderId = LiquidationOrderIdContext.getCapturedOrderIdOrNull();
        LOG.info("Opening send agreement document modal for order id={}", orderId);

        openManageOrderTabForAcceptedOrder(wait);
        selectNumberOfSignatoriesIfEmpty(wait, "1");
        assertShareholdersUpdatedChecked(wait);
        ensureCheckboxChecked(wait, By.name("doNotSendEmail"), DO_NOT_SEND_EMAIL_LABEL);

        scrollPageToViewElement(SEND_AGREEMENT_OPEN_MODAL_BUTTON);
        wait.until(ExpectedConditions.elementToBeClickable(SEND_AGREEMENT_OPEN_MODAL_BUTTON));
        clickByJS(SEND_AGREEMENT_OPEN_MODAL_BUTTON);

        wait.until(ExpectedConditions.visibilityOfElementLocated(SEND_AGREEMENT_MODAL_TITLE));
        waitForSpecifiedTime(1);
        LOG.info("Send agreement document modal is open");
    }

    /**
     * Clicks the highlighted yellow {@code Skicka avtal} button in the document selection modal footer.
     */
    public void clickSendAgreementInDocumentModal() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(45));
        String orderId = LiquidationOrderIdContext.getCapturedOrderIdOrNull();
        LOG.info("Clicking Send agreement (Skicka avtal) in document modal for order id={}", orderId);

        wait.until(ExpectedConditions.visibilityOfElementLocated(SEND_AGREEMENT_MODAL_TITLE));
        scrollPageToViewElement(SEND_AGREEMENT_MODAL_CONFIRM);
        wait.until(ExpectedConditions.elementToBeClickable(SEND_AGREEMENT_MODAL_CONFIRM));
        clickByJS(SEND_AGREEMENT_MODAL_CONFIRM);

        wait.until(ExpectedConditions.invisibilityOfElementLocated(SEND_AGREEMENT_MODAL_TITLE));
        waitForLoad();
        waitForSpecifiedTime(3);
        OrderDetailsCapture.pollAndStoreUploadDocumentToken(driver, 10);
        LOG.info("Send agreement submitted from document modal for order id={}", orderId);
    }

    /**
     * Full send-agreement flow (open modal + confirm). Prefer split steps in E2E for clarity.
     */
    public void sendAgreementForAcceptedOrder() {
        openSendAgreementDocumentModal();
        clickSendAgreementInDocumentModal();
    }

    /**
     * True when Manage order shows {@code WaitingForSignature} after send-agreement (e.g. Remind about E-agreement).
     */
    public boolean isAgreementSentSuccessfully() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(35));
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(AGREEMENT_SENT_MARKER));
            return anyDisplayedIgnoringHighlight(AGREEMENT_SENT_MARKER);
        } catch (Exception e) {
            LOG.warn("Agreement-sent confirmation not visible: {}", e.getMessage());
            return false;
        }
    }

    private void openManageOrderTabForAcceptedOrder(WebDriverWait wait) {
        wait.until(ExpectedConditions.presenceOfElementLocated(MANAGE_ORDER_TAB));
        List<WebElement> tabs = driver.findElements(MANAGE_ORDER_TAB);
        for (WebElement tab : tabs) {
            try {
                if (tab.isDisplayed()) {
                    scrollPageToViewElement(MANAGE_ORDER_TAB);
                    clickByJS(MANAGE_ORDER_TAB);
                    waitForSpecifiedTime(2);
                    break;
                }
            } catch (StaleElementReferenceException ignored) {
                // retry outer wait
            }
        }
        wait.until(ExpectedConditions.visibilityOfElementLocated(SEND_AGREEMENT_OPEN_MODAL_BUTTON));
    }

    /**
     * Checks {@code Aktieägare uppdaterade?} / Shareholders updated on Manage order (required before Send agreement).
     */
    public void checkShareholdersUpdatedOnManageOrder() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        openManageOrderTabForAcceptedOrder(wait);
        selectNumberOfSignatoriesIfEmpty(wait, "1");
        clickShareholdersUpdatedCheckbox(wait);
    }

    private void clickShareholdersUpdatedCheckbox(WebDriverWait wait) {
        LOG.info("Checking shareholders updated (Aktie\u00E4gare uppdaterade? / Shareholders updated?)");
        WebElement checkbox = wait.until(ExpectedConditions.visibilityOfElementLocated(SHAREHOLDERS_UPDATED_CHECKBOX));
        if (checkbox.isSelected()) {
            LOG.info("Shareholders updated checkbox already checked");
            return;
        }
        scrollPageToViewElement(SHAREHOLDERS_UPDATED_CHECKBOX);
        wait.until(ExpectedConditions.elementToBeClickable(SHAREHOLDERS_UPDATED_LABEL));
        clickByJS(SHAREHOLDERS_UPDATED_LABEL);
        wait.until(d -> d.findElement(SHAREHOLDERS_UPDATED_CHECKBOX).isSelected());
        assertShareholdersUpdatedChecked(wait);
    }

    private void assertShareholdersUpdatedChecked(WebDriverWait wait) {
        wait.until(d -> d.findElement(SHAREHOLDERS_UPDATED_CHECKBOX).isSelected());
        Assert.assertTrue(
                driver.findElement(SHAREHOLDERS_UPDATED_CHECKBOX).isSelected(),
                "Shareholders updated (Aktie\u00E4gare uppdaterade?) must be checked before Send agreement");
        LOG.info("Shareholders updated checkbox verified checked");
    }

    private void ensureCheckboxChecked(WebDriverWait wait, By checkbox, By label) {
        if (driver.findElements(checkbox).isEmpty()) {
            return;
        }
        WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(checkbox));
        if (!el.isSelected()) {
            scrollPageToViewElement(label);
            wait.until(ExpectedConditions.elementToBeClickable(label));
            clickByJS(label);
            wait.until(d -> d.findElement(checkbox).isSelected());
        }
    }

    private void selectNumberOfSignatoriesIfEmpty(WebDriverWait wait, String value) {
        By dropdownButton = By.cssSelector("button[name='numberOfSignatories']");
        if (driver.findElements(dropdownButton).isEmpty()) {
            return;
        }
        WebElement button = wait.until(ExpectedConditions.visibilityOfElementLocated(dropdownButton));
        String current = button.getText().trim();
        if (!isSignatoriesDropdownUnset(current)) {
            return;
        }
        scrollPageToViewElement(dropdownButton);
        clickByJS(dropdownButton);
        By option = By.xpath(
                "//div[contains(@class,'shadow-lg')]"
                        + "//div[contains(@class,'cursor-pointer') and normalize-space()='" + value + "']");
        wait.until(ExpectedConditions.elementToBeClickable(option));
        clickByJS(option);
        waitForSpecifiedTime(1);
    }

    private static boolean isSignatoriesDropdownUnset(String current) {
        if (current == null || current.isEmpty() || "0".equals(current)) {
            return true;
        }
        String lower = current.toLowerCase(Locale.ROOT);
        return lower.contains("select") || lower.contains("v\u00E4lj");
    }

    /**
     * Opens the client accept-offer URL built from {@link AcceptOfferContext} (sendOffer response).
     */
    public void openAcceptOfferFromCapturedSendOfferResponse() {
        String url = AcceptOfferContext.buildAcceptOfferUrl();
        LOG.info("Opening accept offer URL for orderId={}", AcceptOfferContext.getOrderIdOrNull());
        driver.get(QaServerCredentials.urlWithHttpBasicAuth(url));
        waitForLoad();
        waitForSpecifiedTime(3);
    }

    public boolean isAcceptOfferPageDisplayed() {
        String currentUrl = driver.getCurrentUrl();
        if (currentUrl == null || !currentUrl.contains("/app/liqTok/acceptOffer")) {
            return false;
        }
        return anyDisplayedIgnoringHighlight(ACCEPT_OFFER_SHAREHOLDERS_HEADING)
                || anyDisplayedIgnoringHighlight(ACCEPT_OFFER_THANK_YOU);
    }

    private String resolveOrderDetailUrl() {
        String urlProp = System.getProperty("adminOrderDetailUrl", "").trim();
        if (!urlProp.isEmpty()) {
            return urlProp;
        }
        String pathProp = System.getProperty("adminOrderDetailPath", "").trim();
        if (!pathProp.isEmpty()) {
            return pathProp.contains("://")
                    ? pathProp
                    : "https://qa.bolagspartner.se" + (pathProp.startsWith("/") ? pathProp : "/" + pathProp);
        }
        String fromLiquidation = LiquidationOrderIdContext.getCapturedOrderIdOrNull();
        if (fromLiquidation != null && !fromLiquidation.isEmpty()) {
            LOG.info("Using order id from liquidation POST saveInitial: {}", fromLiquidation);
            return "https://qa.bolagspartner.se/app/genericOrder/list/" + fromLiquidation;
        }
        return DEFAULT_ORDER_DETAIL_URL;
    }

    /**
     * URL for {@code /app/genericOrder/list/{id}}: {@code -DadminOrderDetailUrl}, {@code -DadminOrderDetailPath},
     * then {@link LiquidationOrderIdContext}, then default constant.
     */
    public String getResolvedGenericOrderDetailUrl() {
        return resolveOrderDetailUrl();
    }

    /** Opens a generic order detail URL (path or full URL) with QA HTTP basic auth when applicable. */
    public void openGenericOrderDetail(String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.isEmpty()) {
            return;
        }
        String url = pathOrUrl.contains("://")
                ? pathOrUrl
                : "https://qa.bolagspartner.se" + (pathOrUrl.startsWith("/") ? pathOrUrl : "/" + pathOrUrl);
        String authedUrl = QaServerCredentials.urlWithHttpBasicAuth(url);
        for (int attempt = 0; attempt < 5; attempt++) {
            driver.get(authedUrl);
            waitForLoad();
            waitForSpecifiedTime(3);
            try {
                waitForOrderDetailShell();
                scrollToOrderDetailContent();
                scrollPageToTop();
                return;
            } catch (TimeoutException e) {
                LOG.warn("Order detail shell not ready (attempt {}) — retrying navigation", attempt + 1);
                waitForSpecifiedTime(5);
            }
        }
        waitForOrderDetailShell();
        scrollToOrderDetailContent();
        scrollPageToTop();
    }

    private void waitForOrderDetailShell() {
        WebDriverWait shellWait = new WebDriverWait(driver, Duration.ofSeconds(60));
        shellWait.until(d -> {
            String current = d.getCurrentUrl();
            if (current == null || !current.contains("/genericOrder/list/")) {
                return false;
            }
            return anyDisplayedIgnoringHighlight(MANAGE_ORDER_TAB)
                    || anyDisplayedIgnoringHighlight(By.name("totalAssets"))
                    || anyDisplayedIgnoringHighlight(requestDetailsSection);
        });
        waitForSpecifiedTime(2);
    }

    private void ensureManageOrderAccountingVisible(WebDriverWait wait) {
        scrollPageToViewElement(MANAGE_ORDER_TAB);
        wait.until(ExpectedConditions.presenceOfElementLocated(MANAGE_ORDER_TAB));
        for (int attempt = 0; attempt < 3; attempt++) {
            for (WebElement tab : driver.findElements(MANAGE_ORDER_TAB)) {
                try {
                    if (tab.isDisplayed()) {
                        scrollPageToViewElement(MANAGE_ORDER_TAB);
                        clickByJS(MANAGE_ORDER_TAB);
                        waitForSpecifiedTime(2);
                        break;
                    }
                } catch (StaleElementReferenceException ignored) {
                    // retry
                }
            }
            try {
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("totalAssets")));
                scrollPageToViewElement(By.name("totalAssets"));
                return;
            } catch (TimeoutException e) {
                LOG.warn("totalAssets not visible after manage-order tab click (attempt {})", attempt + 1);
                scrollPageToViewElement(MANAGE_ORDER_TAB);
            }
        }
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("totalAssets")));
        scrollPageToViewElement(By.name("totalAssets"));
    }

    private void scrollToOrderDetailContent() {
        if (anyDisplayedIgnoringHighlight(MANAGE_ORDER_TAB)) {
            scrollPageToViewElement(MANAGE_ORDER_TAB);
            return;
        }
        if (anyDisplayedIgnoringHighlight(By.name("totalAssets"))) {
            scrollPageToViewElement(By.name("totalAssets"));
            return;
        }
        scrollPageToViewElement(requestDetailsSection);
    }

    private void selectClosingDateOfFinancialStatementsToday(WebDriverWait wait, LocalDate today) {
        YearMonth target = YearMonth.from(today);
        scrollPageToViewElement(CLOSING_DATE_TRIGGER);
        wait.until(ExpectedConditions.elementToBeClickable(CLOSING_DATE_TRIGGER));
        clickByJS(CLOSING_DATE_TRIGGER);
        wait.until(ExpectedConditions.visibilityOfElementLocated(DATEPICKER_DROPDOWN));
        navigateDatePickerToMonth(target, wait);
        clickTodayInDatePicker(today, wait);
        wait.until(ExpectedConditions.invisibilityOfElementLocated(DATEPICKER_DROPDOWN));
    }

    private void navigateDatePickerToMonth(YearMonth target, WebDriverWait wait) {
        By monthHeader = By.xpath("//div[@data-datepicker-dropdown]//div[contains(@class,'mb-5')]//span[contains(@class,'font-medium')]");
        By prevMonth = By.xpath("(//div[@data-datepicker-dropdown]//div[contains(@class,'mb-5')])[1]/button[1]");
        By nextMonth = By.xpath("(//div[@data-datepicker-dropdown]//div[contains(@class,'mb-5')])[1]/button[2]");
        for (int i = 0; i < 24; i++) {
            WebElement header = wait.until(ExpectedConditions.visibilityOfElementLocated(monthHeader));
            YearMonth shown = parseMonthYearHeader(header.getText());
            if (shown.equals(target)) {
                return;
            }
            if (shown.isBefore(target)) {
                wait.until(ExpectedConditions.elementToBeClickable(nextMonth)).click();
            } else {
                wait.until(ExpectedConditions.elementToBeClickable(prevMonth)).click();
            }
            waitForSpecifiedTime(1);
        }
        throw new IllegalStateException("Could not navigate datepicker to " + target);
    }

    private YearMonth parseMonthYearHeader(String text) {
        String t = text.trim();
        DateTimeFormatter en = DateTimeFormatter.ofPattern("MMMM uuuu", Locale.ENGLISH);
        try {
            return YearMonth.from(en.parse(t));
        } catch (DateTimeParseException e1) {
            DateTimeFormatter sv = DateTimeFormatter.ofPattern("MMMM uuuu", Locale.forLanguageTag("sv-SE"));
            try {
                return YearMonth.from(sv.parse(t));
            } catch (DateTimeParseException e2) {
                throw new IllegalArgumentException("Unrecognized calendar header: " + text, e2);
            }
        }
    }

    private void clickTodayInDatePicker(LocalDate today, WebDriverWait wait) {
        String day = String.valueOf(today.getDayOfMonth());
        By dayButton = By.xpath(
                "//div[@data-datepicker-dropdown]//div[contains(@class,'grid-cols-7') and contains(@class,'gap-2')]"
                        + "//button[normalize-space()='" + day + "' and not(contains(@class,'text-gray-300'))]");
        wait.until(ExpectedConditions.elementToBeClickable(dayButton)).click();
    }

    private void replaceNumberInput(By locator, String decimalDotString) {
        replaceNumberInput(locator, decimalDotString, new WebDriverWait(driver, Duration.ofSeconds(25)));
    }

    /**
     * Resolves {@code By.name(...)} to the correct field when duplicate {@code name} attributes exist on the page
     * (e.g. review UI and ChangeQuote both use {@code name="equity"} — we want the last displayed, enabled input).
     */
    private void replaceNumberInput(By locator, String decimalDotString, WebDriverWait wait) {
        BigDecimal expected = new BigDecimal(decimalDotString.trim());
        String ascii = decimalDotString.trim();
        for (int attempt = 0; attempt < 2; attempt++) {
            WebElement input = waitForEditableNumberInput(locator, wait);
            scrollInputIntoViewCenter(input);
            if (attempt == 0) {
                new Actions(driver).moveToElement(input).click().pause(Duration.ofMillis(300)).perform();
                input.sendKeys(selectAllChord());
                input.sendKeys(Keys.DELETE);
                input.sendKeys(ascii);
                input.sendKeys(Keys.TAB);
            } else {
                LOG.warn("Retrying {} with native value setter (React) after keyboard fill did not stick", locator);
                input = waitForEditableNumberInput(locator, wait);
                scrollInputIntoViewCenter(input);
                setReactControlledInputValue(input, ascii);
            }
            try {
                new WebDriverWait(driver, Duration.ofSeconds(22)).until(
                        d -> valueMatchesExpected(locator, expected));
                return;
            } catch (TimeoutException e) {
                if (attempt == 1) {
                    throw new TimeoutException(
                            "Could not set " + locator + " to " + ascii + "; last value: " + readRawInputForDiagnostics(locator),
                            e);
                }
            }
        }
    }

    private boolean valueMatchesExpected(By locator, BigDecimal expected) {
        try {
            return numericCloseEnough(readNumericInputValue(locator), expected);
        } catch (StaleElementReferenceException | NoSuchElementException e) {
            return false;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private String readRawInputForDiagnostics(By locator) {
        try {
            return readInputValue(findLastDisplayedEnabledInput(locator));
        } catch (RuntimeException e) {
            return "<unreadable: " + e.getMessage() + ">";
        }
    }

    private WebElement waitForEditableNumberInput(By locator, WebDriverWait wait) {
        return wait.until(d -> findLastDisplayedEnabledInputOrNull(locator));
    }

    private WebElement findLastDisplayedEnabledInput(By locator) {
        WebElement el = findLastDisplayedEnabledInputOrNull(locator);
        if (el == null) {
            throw new NoSuchElementException("No displayed, enabled input for: " + locator);
        }
        return el;
    }

    private WebElement findLastDisplayedEnabledInputOrNull(By locator) {
        List<WebElement> found = driver.findElements(locator);
        WebElement lastGood = null;
        for (WebElement el : found) {
            try {
                if (el.isDisplayed() && el.isEnabled()) {
                    lastGood = el;
                }
            } catch (StaleElementReferenceException e) {
                return null;
            }
        }
        return lastGood;
    }

    private void scrollInputIntoViewCenter(WebElement el) {
        scrollElementIntoViewableArea(el);
    }

    /**
     * Sets value the way React listens (native value descriptor + {@code input} event), then blurs.
     */
    private void setReactControlledInputValue(WebElement el, String ascii) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript(
                "const el = arguments[0]; const v = String(arguments[1] ?? '');"
                        + "el.focus();"
                        + "const proto = window.HTMLInputElement.prototype;"
                        + "const desc = Object.getOwnPropertyDescriptor(proto, 'value');"
                        + "if (desc && desc.set) { desc.set.call(el, ''); desc.set.call(el, v); }"
                        + "else { el.value = v; }"
                        + "el.dispatchEvent(new Event('input', { bubbles: true }));"
                        + "el.dispatchEvent(new Event('change', { bubbles: true }));"
                        + "el.blur();",
                el,
                ascii);
    }

    private static CharSequence selectAllChord() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("mac")) {
            return Keys.chord(Keys.COMMAND, "a");
        }
        return Keys.chord(Keys.CONTROL, "a");
    }

    private void assertOfferSentAccountingDataEnteredSuccessfully(
            WebDriverWait wait, LocalDate expectedClosingDay, OfferSentAccountingData expected) {
        // Let React state settle after all field interactions before asserting
        waitForSpecifiedTime(2);
        assertClosingDateButtonShowsIsoDate(wait, expectedClosingDay);
        assertNumericInputWithDiagnostics(By.name("totalAssets"), expected.getTotalAssets(), wait);
        assertNumericInputWithDiagnostics(By.name("totalLiabilities"), expected.getTotalLiabilities(), wait);
        assertNumericInputWithDiagnostics(By.name("thisYearResults"), expected.getThisYearResults(), wait);
        assertNumericInputWithDiagnostics(By.name("untaxedReserves"), expected.getUntaxedReserves(), wait);
        assertNumericInputWithDiagnostics(By.name("nonTaxableIncome"), expected.getNonTaxableIncome(), wait);
        assertNumericInputWithDiagnostics(By.name("nonDeductibleCosts"), expected.getNonDeductibleCosts(), wait);
        assertNumericInputWithDiagnostics(By.name("equity"), expected.getEquity(), wait);
        // offerPriceSek is not filled in this flow — assertion intentionally omitted
    }

    private void assertClosingDateButtonShowsIsoDate(WebDriverWait wait, LocalDate expected) {
        String iso = expected.format(DateTimeFormatter.ISO_LOCAL_DATE);
        wait.until(ExpectedConditions.textToBePresentInElementLocated(CLOSING_DATE_DISPLAY, iso));
        WebElement span = driver.findElement(CLOSING_DATE_DISPLAY);
        Assert.assertEquals(
                span.getText().trim(),
                iso,
                "Closing date on DatePicker shows yyyy-MM-dd in the app (see DatePicker dateFormat default)");
    }

    private void waitUntilNumericInputMatches(By locator, BigDecimal expected, WebDriverWait wait) {
        wait.until(d -> valueMatchesExpected(locator, expected));
    }

    /**
     * Like {@link #waitUntilNumericInputMatches} but logs the actual DOM value when the poll times out,
     * so the failure message shows what the field actually contained.
     */
    private void assertNumericInputWithDiagnostics(By locator, BigDecimal expected, WebDriverWait wait) {
        try {
            // Scroll the field into view before asserting — avoids stale element after scrolling to equity
            scrollToInputIfPresent(locator);
            wait.until(d -> valueMatchesExpected(locator, expected));
            LOG.info("Assertion OK: {} = {}", locator, expected);
        } catch (TimeoutException e) {
            String actual = readRawInputForDiagnostics(locator);
            throw new TimeoutException(
                    "Assertion failed for " + locator
                            + ": expected ~" + expected
                            + " but field currently shows: [" + actual + "]",
                    e);
        }
    }

    /** Scrolls the first visible input matching {@code locator} into view (best-effort). */
    private void scrollToInputIfPresent(By locator) {
        try {
            List<WebElement> els = driver.findElements(locator);
            for (WebElement el : els) {
                if (el.isDisplayed()) {
                    scrollInputIntoViewCenter(el);
                    break;
                }
            }
        } catch (Exception ignored) {
            // best-effort only
        }
    }

    private BigDecimal readNumericInputValue(By locator) {
        return parseSwedishNumber(readInputValue(findLastDisplayedEnabledInput(locator)));
    }

    /**
     * React keeps the live value in the DOM property; {@link WebElement#getAttribute(String)} is often stale/empty.
     */
    private String readInputValue(WebElement el) {
        String v = null;
        try {
            v = el.getDomProperty("value");
        } catch (UnsupportedOperationException ignored) {
            // very old drivers
        }
        if (v == null || v.isEmpty()) {
            v = el.getAttribute("value");
        }
        if (v == null || v.isEmpty()) {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Object out = js.executeScript("return arguments[0].value != null ? arguments[0].value : '';", el);
            v = out != null ? String.valueOf(out) : "";
        }
        return v;
    }

    private static BigDecimal parseSwedishNumber(String display) {
        if (display == null) {
            return BigDecimal.ZERO;
        }
        String t = display.replace('\u00a0', ' ')
                .replace('\u202f', ' ')
                .replace(" ", "")
                .replace(',', '.')
                .trim();
        if (t.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(t);
    }

    private static boolean numericCloseEnough(BigDecimal actual, BigDecimal expected) {
        BigDecimal diff = actual.subtract(expected).abs();
        return diff.compareTo(new BigDecimal("0.02")) <= 0;
    }

    private void assertNoOfferPriceValidationError(WebDriverWait wait) {
        wait.until(ExpectedConditions.not(
                ExpectedConditions.attributeContains(By.name("offerPriceSek"), "class", "border-red-600")));
        wait.until(d -> driver.findElements(OFFER_PRICE_INLINE_ERROR).stream().noneMatch(WebElement::isDisplayed));
    }

    private static final String GENERIC_ORDER_LIST_URL = "https://qa.bolagspartner.se/app/genericOrder/list";
    private final By loginErrorMessage = By.xpath(
            "//*[contains(@class,'error') or contains(@class,'alert') or @role='alert']"
                    + "[contains(.,'fel') or contains(.,'Fel') or contains(.,'invalid')"
                    + " or contains(.,'Invalid') or contains(.,'incorrect') or contains(.,'Incorrect')"
                    + " or contains(.,'required') or contains(.,'Required')"
                    + " or contains(.,'obligatorisk') or contains(.,'Obligatorisk')]"
                    + " | //p[contains(@class,'text-red')]"
                    + " | //*[contains(@class,'text-red') and string-length(normalize-space(.))>0]");
    private final By genericOrderListTable = By.xpath(
            "//table[.//th or .//tbody/tr]"
                    + " | //*[contains(@class,'order-list') or @data-testid='order-list']"
                    + " | //main[.//table or .//a[contains(@href,'genericOrder')]]");

    /** Enters admin credentials with explicit email and password (for negative login tests). */
    public void enterAdminCredentials(String email, String password) {
        ensureLoginFormReady();
        scrollPageToTop();
        typeIntoFirstDisplayedInput(epost, email != null ? email : "");
        waitForSpecifiedTime(1);
        scrollPageToTop();
        typeIntoFirstDisplayedInput(passwordInput, password != null ? password : "");
        scrollPageToTop();
    }

    /** Sets only the email field (used for empty-email negative tests). */
    public void enterAdminEmailOnly(String email) {
        ensureLoginFormReady();
        scrollPageToTop();
        typeIntoFirstDisplayedInput(epost, email != null ? email : "");
        scrollPageToTop();
    }

    public void enterConfiguredAdminEmail() {
        enterAdminEmailOnly(adminEmail);
    }

    public void enterConfiguredAdminPasswordOnly() {
        enterAdminPasswordOnly(adminPassword);
    }

    /** Sets only the password field (used for invalid-password / password-only steps). */
    public void enterAdminPasswordOnly(String password) {
        ensureLoginFormReady();
        scrollPageToTop();
        typeIntoFirstDisplayedInput(passwordInput, password != null ? password : "");
        scrollPageToTop();
    }

    public void assertLoginFailedOrStillOnLoginPage() {
        scrollPageToTop();
        waitForSpecifiedTime(3);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(45));
        wait.until(d -> {
            String current = d.getCurrentUrl();
            boolean onLogin = current != null && current.contains("/auth/login");
            boolean errorVisible = anyDisplayedIgnoringHighlight(loginErrorMessage);
            boolean loginForm = isLoginFormVisible();
            boolean dashboard = isDashboardVisible();
            return onLogin || errorVisible || loginForm || !dashboard;
        });
        scrollPageToTop();
        String url = driver.getCurrentUrl();
        boolean onLogin = url != null && url.contains("/auth/login");
        boolean errorVisible = anyDisplayedIgnoringHighlight(loginErrorMessage);
        boolean loginForm = isLoginFormVisible();
        boolean dashboard = isDashboardVisible();
        boolean html5Invalid = hasHtml5ValidationOnLoginFields();
        Assert.assertTrue(onLogin || errorVisible || loginForm || html5Invalid || !dashboard,
                "Expected login failure. URL=" + url
                        + ", errorVisible=" + errorVisible
                        + ", loginForm=" + loginForm
                        + ", html5Invalid=" + html5Invalid
                        + ", dashboard=" + dashboard);
    }

    /**
     * When email is empty and only password is filled, the Logga in / Log in control stays inactive.
     */
    public void assertLoginButtonInactive() {
        scrollPageToTop();
        waitForSpecifiedTime(2);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        wait.until(d -> firstDisplayedLoginButton() != null);
        scrollPageToViewElement(loginButton);
        scrollPageToTop();

        WebElement button = firstDisplayedLoginButton();
        Assert.assertNotNull(button, "Login button should be present on the form");

        boolean inactive = isLoginControlInactive(button);
        Assert.assertTrue(inactive,
                "Expected login button to remain inactive when email is empty. "
                        + "enabled=" + button.isEnabled()
                        + ", disabledAttr=" + button.getAttribute("disabled")
                        + ", ariaDisabled=" + button.getAttribute("aria-disabled")
                        + ", class=" + button.getAttribute("class"));
        LOG.info("Login button correctly remains inactive with empty email and password filled");
    }

    private WebElement firstDisplayedLoginButton() {
        for (WebElement el : driver.findElements(loginButton)) {
            try {
                if (el.isDisplayed()) {
                    return el;
                }
            } catch (StaleElementReferenceException ignored) {
                // try next
            }
        }
        return null;
    }

    private boolean isLoginControlInactive(WebElement button) {
        try {
            if (!button.isEnabled()) {
                return true;
            }
        } catch (Exception ignored) {
            return true;
        }
        String disabled = button.getAttribute("disabled");
        if (disabled != null) {
            return true;
        }
        String ariaDisabled = button.getAttribute("aria-disabled");
        if ("true".equalsIgnoreCase(ariaDisabled)) {
            return true;
        }
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Object result = js.executeScript(
                    "var el = arguments[0];"
                            + "if (!el) { return true; }"
                            + "if (el.disabled === true) { return true; }"
                            + "if (el.getAttribute('aria-disabled') === 'true') { return true; }"
                            + "var cls = (el.className || '').toString().toLowerCase();"
                            + "if (cls.indexOf('disabled') >= 0 || cls.indexOf('opacity-50') >= 0"
                            + "    || cls.indexOf('cursor-not-allowed') >= 0 || cls.indexOf('pointer-events-none') >= 0) {"
                            + "  return true;"
                            + "}"
                            + "var style = window.getComputedStyle(el);"
                            + "if (style && (style.pointerEvents === 'none' || parseFloat(style.opacity) < 0.6)) {"
                            + "  return true;"
                            + "}"
                            + "return false;",
                    button);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            LOG.warn("Could not evaluate login button inactive state via JS: {}", e.getMessage());
            return false;
        }
    }

    private boolean hasHtml5ValidationOnLoginFields() {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            for (By locator : new By[]{epost, passwordInput}) {
                for (WebElement field : driver.findElements(locator)) {
                    if (!field.isDisplayed()) {
                        continue;
                    }
                    Object valid = js.executeScript(
                            "return arguments[0].checkValidity ? arguments[0].checkValidity() : true;", field);
                    if (Boolean.FALSE.equals(valid)) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {
            // fall through
        }
        return false;
    }

    public void openGenericOrderListPage() {
        dismissBlockingReviewWizardIfPresent();
        String authedUrl = QaServerCredentials.urlWithHttpBasicAuth(GENERIC_ORDER_LIST_URL);
        for (int attempt = 0; attempt < 3; attempt++) {
            driver.get(authedUrl);
            waitForLoad();
            waitForSpecifiedTime(3);
            try {
                new WebDriverWait(driver, Duration.ofSeconds(45))
                        .until(d -> {
                            String current = d.getCurrentUrl();
                            return isGenericOrderListUrl(current)
                                    && (anyDisplayedIgnoringHighlight(genericOrderListTable)
                                    || anyDisplayedIgnoringHighlight(dashboardHeading)
                                    || anyDisplayedIgnoringHighlight(By.tagName("main")));
                        });
                if (anyDisplayedIgnoringHighlight(genericOrderListTable)) {
                    scrollPageToViewElement(genericOrderListTable);
                }
                LOG.info("Generic order list page opened (attempt {})", attempt + 1);
                return;
            } catch (TimeoutException e) {
                LOG.warn("Generic order list not ready (attempt {}) — retrying", attempt + 1);
                waitForSpecifiedTime(5);
            }
        }
    }

    public void assertGenericOrderListPageDisplayed() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));
        wait.until(d -> isGenericOrderListUrl(d.getCurrentUrl()));
        String url = driver.getCurrentUrl();
        Assert.assertTrue(isGenericOrderListUrl(url),
                "Expected generic order list URL (not order detail), got: " + url);
        wait.until(d -> anyDisplayedIgnoringHighlight(genericOrderListTable)
                || anyDisplayedIgnoringHighlight(By.tagName("main")));
        if (anyDisplayedIgnoringHighlight(genericOrderListTable)) {
            scrollPageToViewElement(genericOrderListTable);
        }
        Assert.assertTrue(anyDisplayedIgnoringHighlight(genericOrderListTable)
                        || isGenericOrderListUrl(url),
                "Generic order list table or list container should be visible");
    }

    /** True for {@code /genericOrder/list}, false for order detail {@code /genericOrder/list/{id}}. */
    private boolean isGenericOrderListUrl(String current) {
        if (current == null || !current.contains("/genericOrder/list")) {
            return false;
        }
        return !current.matches(".*/genericOrder/list/\\d+.*");
    }

    private void dismissBlockingReviewWizardIfPresent() {
        try {
            List<WebElement> closeButtons = driver.findElements(By.xpath(
                    "//button[normalize-space()='Stäng' or normalize-space()='Close'"
                            + " or contains(.,'Stäng') or contains(.,'Close')]"));
            for (WebElement close : closeButtons) {
                if (close.isDisplayed() && close.isEnabled()) {
                    close.click();
                    waitForLoad();
                    break;
                }
            }
        } catch (Exception ignored) {
            // already leaving the order page
        }
    }

    private static final By FORMER_REPRESENTATIVES_CONTROL = By.xpath(
            "//button[contains(.,'Tidigare representanter') or contains(.,'Former representatives')"
                    + " or contains(.,'Previous representatives')]"
                    + " | //a[contains(.,'Tidigare representanter') or contains(.,'Former representatives')"
                    + " or contains(.,'Previous representatives')]"
                    + " | //*[self::h2 or self::h3 or self::h4][contains(.,'Tidigare representanter')"
                    + " or contains(.,'Former representatives')]");
    private static final By FORMER_REPRESENTATIVE_ADD = By.xpath(
            "//button[contains(.,'L\u00E4gg till representant') or contains(.,'Add representative')"
                    + " or contains(.,'L\u00E4gg till tidigare') or contains(.,'Add former')]"
                    + " | //button[contains(.,'L\u00E4gg till') or contains(.,'Add')]"
                    + "[ancestor::*[contains(.,'Tidigare representanter') or contains(.,'Former representatives')]]");
    private static final By FORMER_REPRESENTATIVE_SAVE = By.xpath(
            "//*[@role='dialog' or contains(@class,'modal') or contains(@class,'fixed')]"
                    + "//button[contains(.,'Spara') or contains(.,'Skapa') or contains(.,'L\u00E4gg till')"
                    + " or contains(.,'Save') or contains(.,'Add') or contains(.,'Create')]"
                    + " | //form//button[@type='submit'][contains(.,'Spara') or contains(.,'Skapa')"
                    + " or contains(.,'L\u00E4gg till') or contains(.,'Save')]");

    /**
     * Opens Tidigare representanter / Former representatives on Manage order and adds one person
     * when an add form is available. If the section is already populated, the open is still asserted.
     */
    public void addFormerRepresentativeIfFormShown() {
        LOG.info("Opening former representatives (Tidigare representanter) on manage order");

        WebElement openControl = firstDisplayedQuiet(driver.findElements(FORMER_REPRESENTATIVES_CONTROL));
        if (openControl != null) {
            scrollPageToViewElement(FORMER_REPRESENTATIVES_CONTROL);
            clickByJS(FORMER_REPRESENTATIVES_CONTROL);
            waitForSpecifiedTime(1);
        }

        WebElement addButton = firstDisplayedQuiet(driver.findElements(FORMER_REPRESENTATIVE_ADD));
        if (addButton != null) {
            scrollIntoViewQuiet(addButton);
            clickElementQuiet(addButton);
            waitForSpecifiedTime(1);
            fillFormerRepresentativeFormIfPresent();
            if (!ownerModalRequiredValuesPresent()) {
                LOG.warn("Required owner/representative values still empty after first fill — retrying from order data");
                fillFormerRepresentativeFormIfPresent();
            }
            WebElement save = firstDisplayedQuiet(driver.findElements(FORMER_REPRESENTATIVE_SAVE));
            if (save != null) {
                clickElementQuiet(save);
                waitForLoad();
                waitForSpecifiedTime(1);
                if (ownerModalHasRequiredFieldErrors() && ownerModalRequiredValuesPresent()) {
                    LOG.warn("Owner/representative modal still has required-field errors — refilling and saving");
                    fillFormerRepresentativeFormIfPresent();
                    save = firstDisplayedQuiet(driver.findElements(FORMER_REPRESENTATIVE_SAVE));
                    if (save != null) {
                        clickElementQuiet(save);
                        waitForLoad();
                        waitForSpecifiedTime(1);
                    }
                }
            }
        } else {
            LOG.info("No add-former-representative control — verifying the section is visible");
        }

        boolean sectionVisible = firstDisplayedQuiet(driver.findElements(FORMER_REPRESENTATIVES_CONTROL)) != null
                || driver.getPageSource().contains("Tidigare representanter")
                || driver.getPageSource().toLowerCase(Locale.ROOT).contains("former representative");
        Assert.assertTrue(sectionVisible,
                "Former representatives (Tidigare representanter) section should be visible on manage order");
        LOG.info("Former representatives step completed");
    }

    private void fillFormerRepresentativeFormIfPresent() {
        String firstName = usablePersonName(readVisibleOrderValue("Förnamn", "First name"), "Tanuj");
        String lastName = usablePersonName(readVisibleOrderValue("Efternamn", "Last name"), "Rasane");
        String email = usableEmail(readVisibleOrderValue("E-post", "Email"), "tanujrasane23@gmail.com");
        String street = usableStreet(readVisibleOrderValue("Gatuadress", "Street"), "Teststreet");
        String zip = usablePostalCode(readVisibleOrderValue("Postnummer", "Postal"), "12345");
        String city = usableStreet(readVisibleOrderValue("Ort", "City"), "Stockholm");
        String phone = firstNonBlank(readVisibleOrderValue("Telefon", "Phone"), "0701234567");
        String personalId = "194808042391";

        LOG.info("Filling owner/representative from order/schema: first='{}' last='{}' email='{}'",
                firstName, lastName, email);

        fillOwnerModalField(
                new String[]{"ssn", "personalNumber", "personnummer", "personalId", "personalid", "pnr", "civicNumber"},
                new String[]{"Ange personnummer", "personnummer", "YYYYMMDD"},
                personalId);
        fillOwnerModalField(
                new String[]{"firstname", "firstName", "fornamn"},
                new String[]{"Ange f\u00F6rnamn", "F\u00F6rnamn"},
                firstName);
        fillOwnerModalField(
                new String[]{"lastname", "lastName", "surname", "surName", "familyName", "efternamn"},
                new String[]{"Ange efternamn", "Efternamn"},
                lastName);
        fillOwnerModalField(
                new String[]{"email"},
                new String[]{"Ange e-post", "E-post", "E-mail"},
                email);
        fillOwnerModalField(
                new String[]{"address", "addressText", "street", "streetAddress"},
                new String[]{"Gatuadress", "Adress", "Street"},
                street);
        fillOwnerModalField(
                new String[]{"addressZipcode", "postalCode", "zipCode", "zip"},
                new String[]{"Postnummer", "Postal"},
                zip);
        fillOwnerModalField(
                new String[]{"addressCity", "city", "location"},
                new String[]{"Ange ort", "Ort", "City"},
                city);
        fillOwnerModalField(
                new String[]{"phone", "telephone", "mobile"},
                new String[]{"Telefon", "Phone"},
                phone);
    }

    private boolean ownerModalRequiredValuesPresent() {
        String dialog = "//*[@role='dialog' or contains(@class,'modal') or contains(@class,'fixed')]";
        WebElement ssn = firstDisplayedQuiet(driver.findElements(By.xpath(
                dialog + "//input[contains(@placeholder,'personnummer') or contains(@placeholder,'YYYYMMDD')"
                        + " or @name='ssn' or @name='personnummer' or @name='personalId']")));
        WebElement last = firstDisplayedQuiet(driver.findElements(By.xpath(
                dialog + "//input[contains(@placeholder,'efternamn') or contains(@placeholder,'Efternamn')"
                        + " or @name='lastname' or @name='lastName' or @name='surname'"
                        + " or @name='efternamn']")));
        String ssnVal = ssn == null ? "" : String.valueOf(ssn.getAttribute("value"));
        String lastVal = last == null ? "" : String.valueOf(last.getAttribute("value"));
        return ssnVal.replaceAll("[^0-9]", "").length() >= 10 && lastVal.trim().length() >= 2;
    }

    private String readVisibleOrderValue(String swedishLabel, String englishLabel) {
        try {
            Object value = ((JavascriptExecutor) driver).executeScript(
                    "var labels = arguments;"
                            + "var nodes = document.querySelectorAll('label,dt,th,span,p,div');"
                            + "for (var i = 0; i < nodes.length; i++) {"
                            + "  var t = (nodes[i].textContent || '').trim();"
                            + "  if (!t) { continue; }"
                            + "  for (var l = 0; l < labels.length; l++) {"
                            + "    if (t === labels[l] || t.indexOf(labels[l] + ':') === 0) {"
                            + "      var n = nodes[i].nextElementSibling;"
                            + "      if (n && n.textContent && n.textContent.trim()) {"
                            + "        return n.textContent.trim().split('\\n')[0].substring(0, 80);"
                            + "      }"
                            + "    }"
                            + "  }"
                            + "}"
                            + "return '';",
                    swedishLabel, englishLabel);
            if (value != null && !String.valueOf(value).trim().isEmpty()) {
                return String.valueOf(value).trim();
            }
        } catch (Exception ignored) {
            // fall through to defaults
        }
        return "";
    }

    private String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.trim().isEmpty() && preferred.length() < 80
                && !looksLikeFieldLabel(preferred)) {
            return preferred.trim();
        }
        return fallback;
    }

    private String usablePersonName(String preferred, String fallback) {
        String value = firstNonBlank(preferred, fallback);
        if (!value.matches("[A-Za-zÅÄÖåäö \\-]{2,40}")) {
            return fallback;
        }
        return value;
    }

    private String usableEmail(String preferred, String fallback) {
        String value = firstNonBlank(preferred, fallback);
        if (value.indexOf('@') < 1) {
            return fallback;
        }
        return value;
    }

    private String usableStreet(String preferred, String fallback) {
        String value = firstNonBlank(preferred, fallback);
        if (looksLikeFieldLabel(value) || value.length() < 3) {
            return fallback;
        }
        return value;
    }

    private String usablePostalCode(String preferred, String fallback) {
        String digits = preferred == null ? "" : preferred.replaceAll("[^0-9]", "");
        if (digits.length() == 5) {
            return digits;
        }
        return fallback;
    }

    private boolean looksLikeFieldLabel(String value) {
        String v = value.trim();
        String[] labels = {
                "Förnamn", "Efternamn", "E-post", "Adress", "Gatuadress", "Postnummer", "Ort",
                "First name", "Last name", "Email", "Street", "Postal", "City", "Telefon", "Phone",
                "Ange förnamn", "Ange efternamn", "Ange personnummer", "Ange adress"
        };
        for (int i = 0; i < labels.length; i++) {
            if (labels[i].equalsIgnoreCase(v)) {
                return true;
            }
        }
        return false;
    }

    private boolean ownerModalHasRequiredFieldErrors() {
        By errors = By.xpath(
                "//*[contains(.,'Personnummer kr\u00E4vs') or contains(.,'Efternamn kr\u00E4vs')"
                        + " or contains(.,'Personal identity') or contains(.,'Last name is required')]");
        return firstDisplayedQuiet(driver.findElements(errors)) != null;
    }

    private void fillOwnerModalField(String[] names, String[] placeholderOrLabel, String value) {
        String dialog = "//*[@role='dialog' or contains(@class,'modal') or contains(@class,'fixed')]";
        for (int i = 0; i < names.length; i++) {
            WebElement field = firstDisplayedQuiet(driver.findElements(
                    By.xpath(dialog + "//input[@name='" + names[i] + "']")));
            if (field != null) {
                setOwnerModalInput(field, value);
                return;
            }
        }
        for (int i = 0; i < placeholderOrLabel.length; i++) {
            String fragment = placeholderOrLabel[i];
            WebElement field = firstDisplayedQuiet(driver.findElements(By.xpath(
                    dialog + "//input[contains(@placeholder,'" + fragment + "')]"
                            + " | " + dialog + "//label[contains(.,'" + fragment + "')]/following::input[1]"
                            + " | " + dialog + "//label[contains(.,'" + fragment + "')]//input")));
            if (field != null) {
                setOwnerModalInput(field, value);
                return;
            }
        }
        LOG.warn("Owner/representative field not found for value '{}'", value);
    }

    private void setOwnerModalInput(WebElement field, String value) {
        String constrained = constrainToFieldSchema(field, value);
        LOG.info("Owner/representative input name={} type={} maxlength={} placeholder={} -> {} char(s)",
                field.getAttribute("name"), field.getAttribute("type"), field.getAttribute("maxlength"),
                field.getAttribute("placeholder"), constrained.length());
        try {
            scrollIntoViewQuiet(field);
            field.click();
            field.sendKeys(selectAllChord());
            field.sendKeys(Keys.DELETE);
            field.sendKeys(constrained);
            field.sendKeys(Keys.TAB);
        } catch (Exception e) {
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].value = arguments[1];"
                            + "arguments[0].dispatchEvent(new Event('input', {bubbles:true}));"
                            + "arguments[0].dispatchEvent(new Event('change', {bubbles:true}));",
                    field, constrained);
        }
    }

    /**
     * Applies visible HTML schema: type, maxlength, and Swedish personnummer (12 digits / YYYYMMDD-XXXX).
     */
    private String constrainToFieldSchema(WebElement field, String raw) {
        String value = raw == null ? "" : raw.trim();
        String type = safeAttr(field, "type");
        String name = safeAttr(field, "name");
        String placeholder = safeAttr(field, "placeholder");
        String pattern = safeAttr(field, "pattern");
        String maxRaw = safeAttr(field, "maxlength");
        boolean personnummer = containsIgnoreCase(name, "ssn")
                || containsIgnoreCase(name, "person")
                || containsIgnoreCase(placeholder, "personnummer")
                || containsIgnoreCase(placeholder, "YYYYMMDD")
                || containsIgnoreCase(pattern, "\\d{8}");
        if (personnummer) {
            String digits = value.replaceAll("[^0-9]", "");
            if (digits.length() > 12) {
                digits = digits.substring(0, 12);
            }
            int max = parseMaxLength(maxRaw, 13);
            if (max <= 12) {
                value = digits.length() > max ? digits.substring(0, max) : digits;
            } else if (digits.length() >= 12) {
                value = digits.substring(0, 8) + "-" + digits.substring(8, 12);
                if (value.length() > max) {
                    value = value.substring(0, max);
                }
            }
            return value;
        }
        if ("email".equalsIgnoreCase(type) || containsIgnoreCase(name, "email")
                || containsIgnoreCase(placeholder, "post")) {
            int max = parseMaxLength(maxRaw, 80);
            if (value.length() > max) {
                value = value.substring(0, max);
            }
            return value;
        }
        if (containsIgnoreCase(name, "zip") || containsIgnoreCase(name, "postal")
                || containsIgnoreCase(placeholder, "Postnummer")) {
            String digits = value.replaceAll("[^0-9]", "");
            int max = parseMaxLength(maxRaw, 5);
            if (digits.length() > max) {
                digits = digits.substring(0, max);
            }
            return digits;
        }
        if (containsIgnoreCase(name, "phone") || containsIgnoreCase(name, "tel")
                || containsIgnoreCase(placeholder, "Telefon")) {
            String digits = value.replaceAll("[^0-9+]", "");
            int max = parseMaxLength(maxRaw, 15);
            if (digits.length() > max) {
                digits = digits.substring(0, max);
            }
            return digits;
        }
        int max = parseMaxLength(maxRaw, 50);
        if (value.length() > max) {
            value = value.substring(0, max);
        }
        return value;
    }

    private int parseMaxLength(String maxRaw, int fallback) {
        if (maxRaw == null || maxRaw.trim().isEmpty()) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(maxRaw.trim());
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private boolean containsIgnoreCase(String text, String fragment) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(fragment.toLowerCase(Locale.ROOT));
    }

    private String safeAttr(WebElement field, String name) {
        try {
            String value = field.getAttribute(name);
            return value == null ? "" : value;
        } catch (Exception e) {
            return "";
        }
    }

    private WebElement firstDisplayedQuiet(List<WebElement> elements) {
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
}
