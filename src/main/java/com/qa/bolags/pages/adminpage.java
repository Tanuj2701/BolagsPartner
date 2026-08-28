package com.qa.bolags.pages;

import com.qa.bolags.constants.AcceptOfferContext;
import com.qa.bolags.constants.LiquidationOrderIdContext;
import com.qa.bolags.constants.OfferSentAccountingData;
import com.qa.bolags.constants.OfferSentDataContext;
import com.qa.bolags.constants.OfferSentDataProvider;
import com.qa.bolags.constants.QaServerCredentials;
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
    private final By loginButton = By.xpath("//button[normalize-space()='Logga in']");
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

    public void clickLoggaInFromTopNavigation() {
        if (driver.findElements(epost).stream().anyMatch(WebElement::isDisplayed)) {
            return;
        }
        List<WebElement> links = driver.findElements(loggaInNavLink);
        if (!links.isEmpty()) {
            waitForElementToBeClickable(loggaInNavLink);
            clickByJS(loggaInNavLink);
            waitForLoad();
            return;
        }
        driver.get(QaServerCredentials.urlWithHttpBasicAuth(adminDirectLoginUrl));
        waitForLoad();
    }

    public void enterAdminCredentials() {
        waitForLoad();
        waitForSpecifiedTime(1);
        waitForElementToBeClickable(epost);
        typeIntoFirstDisplayedInput(epost, adminEmail);
        waitForElementToBeClickable(passwordInput);
        typeIntoFirstDisplayedInput(passwordInput, adminPassword);
    }

    /** Re-finds inputs to avoid stale references after SPA navigation. */
    private void typeIntoFirstDisplayedInput(By locator, String value) {
        for (int attempt = 0; attempt < 4; attempt++) {
            List<WebElement> fields = driver.findElements(locator);
            for (WebElement f : fields) {
                try {
                    if (f.isDisplayed()) {
                        f.clear();
                        f.sendKeys(value);
                        return;
                    }
                } catch (StaleElementReferenceException e) {
                    break;
                }
            }
            waitForSpecifiedTime(1);
        }
        throw new org.openqa.selenium.NoSuchElementException("No visible input for " + locator);
    }

    public void clickLoginButton() {
        scrollPageToViewElement(loginButton);
        waitForElementToBeClickable(loginButton);
        clickByJS(loginButton);
        waitForLoad();
        waitForSpecifiedTime(2);
    }

    public void loginWithConfiguredCredentials() {
        enterAdminCredentials();
        clickLoginButton();
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
        return anyDisplayedIgnoringHighlight(requestDetailsSection) || anyDisplayedIgnoringHighlight(approveButton);
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
            "//button[contains(., 'Hantera best\u00E4llning') or contains(., 'Manage order') or contains(., 'Manage Order')]");
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

        openGenericOrderDetail(resolveOrderDetailUrl());
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(35));
        LocalDate today = LocalDate.now();
        LOG.info(
                "Closing date of financial statements — test reference (dd/MM/yyyy): {}",
                today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        ensureManageOrderAccountingVisible(wait);
        selectClosingDateOfFinancialStatementsToday(wait, today);
        waitForSpecifiedTime(1);
        replaceNumberInput(By.name("totalAssets"), data.totalAssetsAsInput(), wait);
        waitForSpecifiedTime(1);
        replaceNumberInput(By.name("totalLiabilities"), data.totalLiabilitiesAsInput(), wait);
        waitForSpecifiedTime(1);
        replaceNumberInput(By.name("thisYearResults"), data.thisYearResultsAsInput(), wait);
        waitForSpecifiedTime(1);
        replaceNumberInput(By.name("untaxedReserves"), data.untaxedReservesAsInput(), wait);
        waitForSpecifiedTime(1);
        replaceNumberInput(By.name("nonTaxableIncome"), data.nonTaxableIncomeAsInput(), wait);
        waitForSpecifiedTime(1);
        replaceNumberInput(By.name("nonDeductibleCosts"), data.nonDeductibleCostsAsInput(), wait);
        waitForSpecifiedTime(1);
        replaceNumberInput(By.name("equity"), data.equityAsInput(), wait);
        waitForSpecifiedTime(1);
      // replaceNumberInput(By.name("offerPriceSek"), "225400", wait);
        assertOfferSentAccountingDataEnteredSuccessfully(wait, today, data);
    }

    /**
     * Clicks the primary send-offer control on the Manage order / {@code ChangeQuote} form
     * ({@code sendQuote} → EN "Send Quote", SV "Skicka offert", or "Klar för offert" for sub-company).
     * Requires a valid offer price so the button is not {@code disabled}.
     */
    public void clickSendQuoteOnManageOrder() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        ensureManageOrderAccountingVisible(wait);
        scrollPageToViewElement(SEND_QUOTE_BUTTON);
        wait.until(ExpectedConditions.elementToBeClickable(SEND_QUOTE_BUTTON));
        clickByJS(SEND_QUOTE_BUTTON);
        waitForLoad();
        waitForSpecifiedTime(2);
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
        driver.get(QaServerCredentials.urlWithHttpBasicAuth(url));
        waitForLoad();
        waitForSpecifiedTime(2);
    }

    private void ensureManageOrderAccountingVisible(WebDriverWait wait) {
        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("totalAssets")));
        List<WebElement> tabs = driver.findElements(MANAGE_ORDER_TAB);
        for (WebElement tab : tabs) {
            try {
                if (tab.isDisplayed()) {
                    scrollPageToViewElement(MANAGE_ORDER_TAB);
                    clickByJS(MANAGE_ORDER_TAB);
                    waitForSpecifiedTime(1);
                    break;
                }
            } catch (StaleElementReferenceException ignored) {
                // retry outer loop
            }
        }
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("totalAssets")));
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
                new Actions(driver).moveToElement(input).click().pause(Duration.ofMillis(120)).perform();
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
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});", el);
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
}
