package com.qa.bolags.pages;

import com.qa.bolags.constants.AcceptOfferShareholderTestData;
import com.qa.bolags.constants.ShareholderAllocationPlan;
import com.qa.bolags.constants.ShareholderVariantType;
import com.qa.bolags.utility.TestUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Accept-offer shareholder variants — {@code /app/liqTok/acceptOffer} Add owner modal
 * ({@code addShareHolder.tsx}, {@code PersonForm.tsx}, {@code JuridiskForm.tsx}).
 * <p>Separate from {@link AcceptOfferPage} so existing accept-offer flows stay unchanged.</p>
 */
public class AcceptOfferShareholderPage extends TestUtil {

    private static final Logger LOG = LoggerFactory.getLogger(AcceptOfferShareholderPage.class);
    private static final Pattern LEADING_INTEGER = Pattern.compile("^(\\d+)");
    private static final Pattern SHARES_REMAINING_LINE = Pattern.compile(
            "^(\\d+)\\s*\\(.*?(?:of|av)\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    private static final By ADD_OWNER_BUTTON = By.xpath(
            "//button[contains(., 'ADD SHAREHOLDER') or contains(., 'L\u00C4GG TILL AKTIE\u00C4GARE')"
                    + " or contains(., 'Add owner') or contains(., 'L\u00E4gg till \u00E4gare')]");
    private static final By SHAREHOLDER_MODAL = By.cssSelector(".display-shareholders-modal");
    private static final By NEW_PERSON_RADIO = By.id("person");
    private static final By NEW_LEGAL_ENTITY_RADIO = By.id("juridisk");
    private static final By SWEDEN_RADIO = By.id("sverige");
    private static final By ABROAD_RADIO = By.id("utlandet");
    private static final By MODAL_ADD_BUTTON = By.xpath(
            "//div[contains(@class,'display-shareholders-modal')]//form//button[@type='submit']");
    private static final By REMAINING_SHARES_VALUE = By.xpath(
            "//span[contains(., 'Shares remaining') or contains(., 'Shares left to allocate')"
                    + " or contains(., 'Aktier kvar')]/following-sibling::span[1]");
    private static final By SHAREHOLDER_TABLE_ROWS = By.xpath(
            "//div[contains(@class,'grid') and contains(@class,'text-gray-600')]"
                    + "//div[contains(@class,'text-black') and contains(@class,'break-words')][1]");
    private static final By PAPER_SIGNATURE_RADIO = By.id("paper-signature");
    private static final By PAPER_SIGNATURE_LABEL = By.xpath("//label[.//input[@id='paper-signature']]");
    private static final By SIGNATURE_DONE_BUTTON = By.xpath(
            "//h2[contains(., 'How would you like to sign') or contains(., 'How do you want to sign')"
                    + " or contains(., 'Hur vill ni skriva')]"
                    + "/following::button[contains(., 'DONE') or contains(., 'KLAR') or contains(., 'CLEAR')][1]");

    private enum DataProfile {
        SINGLE,
        MULTI
    }

    public AcceptOfferShareholderPage(WebDriver driver) {
        super(driver);
    }

    public void addSwedishNaturalPersonShareholder() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        addShareholder(ShareholderVariantType.SWEDISH_PERSON, readRemainingSharesToAllocate(wait), DataProfile.SINGLE, wait);
    }

    public void addSwedishLegalEntityShareholder() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        addShareholder(ShareholderVariantType.SWEDISH_LEGAL_ENTITY, readRemainingSharesToAllocate(wait), DataProfile.SINGLE, wait);
    }

    public void addForeignNaturalPersonShareholder() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        addShareholder(ShareholderVariantType.FOREIGN_PERSON, readRemainingSharesToAllocate(wait), DataProfile.SINGLE, wait);
    }

    public void addForeignLegalEntityShareholder() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        addShareholder(ShareholderVariantType.FOREIGN_LEGAL_ENTITY, readRemainingSharesToAllocate(wait), DataProfile.SINGLE, wait);
    }

    /**
     * Adds multiple shareholders with percentage split (e.g. 70/20/10 of total company shares).
     */
    public void allocateSharesAcrossMixedShareholders(
            java.util.List<ShareholderVariantType> types, java.util.List<Integer> percents) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(35));
        waitForCompanyTotalSharesReady(wait);
        int totalShares = readTotalSharesFromPage(wait);
        LOG.info("Company total shares on accept-offer page: {}", totalShares);

        ShareholderAllocationPlan resolved =
                ShareholderAllocationPlan.fromPercentages(totalShares, types, percents);

        for (ShareholderAllocationPlan.ShareholderAllocation allocation : resolved.allocations()) {
            LOG.info(
                    "Adding {} with {} shares ({}%)",
                    allocation.getType(),
                    allocation.getShares(),
                    allocation.getPercent());
            addShareholder(allocation.getType(), allocation.getShares(), DataProfile.MULTI, wait);
        }
        assertAllSharesAllocated(wait);
    }

    public void assertAllSharesAllocated() {
        assertAllSharesAllocated(new WebDriverWait(driver, Duration.ofSeconds(15)));
    }

    public int readRemainingShares() {
        return readRemainingSharesToAllocate(new WebDriverWait(driver, Duration.ofSeconds(15)));
    }

    public int readTotalShares() {
        return readTotalSharesFromPage(new WebDriverWait(driver, Duration.ofSeconds(15)));
    }

    public void selectPaperSignatureAndCompleteAcceptOffer() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        scrollPageToViewElement(PAPER_SIGNATURE_LABEL);
        wait.until(ExpectedConditions.elementToBeClickable(PAPER_SIGNATURE_LABEL));
        if (!driver.findElement(PAPER_SIGNATURE_RADIO).isSelected()) {
            clickByJS(PAPER_SIGNATURE_LABEL);
        }
        wait.until(d -> d.findElement(PAPER_SIGNATURE_RADIO).isSelected());
        scrollPageToViewElement(SIGNATURE_DONE_BUTTON);
        wait.until(ExpectedConditions.elementToBeClickable(SIGNATURE_DONE_BUTTON));
        clickByJS(SIGNATURE_DONE_BUTTON);
        waitForLoad();
        waitForSpecifiedTime(3);
        Assert.assertTrue(isAcceptOfferShareholderFlowComplete(wait),
                "Document checklist or next step did not appear after signature selection");
    }

    public boolean isAcceptOfferShareholderFlowComplete(WebDriverWait wait) {
        try {
            wait.until(d -> {
                if (!d.findElements(By.cssSelector(".CircularProgressbar")).isEmpty()) {
                    return true;
                }
                String src = d.getPageSource();
                return !src.contains("id=\"paper-signature\"")
                        || src.contains("DocumentChecklist")
                        || src.contains("uploadDocument");
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void addShareholder(
            ShareholderVariantType type, int shares, DataProfile profile, WebDriverWait wait) {
        int remainingBefore = readRemainingSharesToAllocate(wait);
        openAddOwnerModal(wait);
        switch (type) {
            case SWEDISH_PERSON:
                fillSwedishPerson(profile, shares, wait);
                break;
            case SWEDISH_LEGAL_ENTITY:
                fillSwedishLegalEntity(profile, shares, wait);
                break;
            case FOREIGN_PERSON:
                fillForeignPerson(profile, shares, wait);
                break;
            case FOREIGN_LEGAL_ENTITY:
                fillForeignLegalEntity(profile, shares, wait);
                break;
            default:
                throw new IllegalArgumentException("Unsupported shareholder type: " + type);
        }
        submitAddOwnerModal(wait);
        int expectedRemaining = remainingBefore - shares;
        waitForRemainingShares(wait, expectedRemaining);
        assertShareholderVisibleAfterAdd(type, profile);
    }

    private void fillSwedishPerson(DataProfile profile, int shares, WebDriverWait wait) {
        selectNewPerson(wait);
        selectSweden(wait);
        if (profile == DataProfile.MULTI) {
            typeInModalInput("ssn", AcceptOfferShareholderTestData.MULTI_SV_PERSON_SSN, wait);
            typeInModalInput("firstname", AcceptOfferShareholderTestData.MULTI_SV_PERSON_FIRST, wait);
            typeInModalInput("lastname", AcceptOfferShareholderTestData.MULTI_SV_PERSON_LAST, wait);
            typeInModalInput("email", AcceptOfferShareholderTestData.MULTI_SV_PERSON_EMAIL, wait);
        } else {
            typeInModalInput("ssn", AcceptOfferShareholderTestData.SV_PERSON_SSN, wait);
            typeInModalInput("firstname", AcceptOfferShareholderTestData.SV_PERSON_FIRST, wait);
            typeInModalInput("lastname", AcceptOfferShareholderTestData.SV_PERSON_LAST, wait);
            typeInModalInput("email", AcceptOfferShareholderTestData.DEFAULT_EMAIL, wait);
        }
        typeInModalInput("addressText", AcceptOfferShareholderTestData.SV_PERSON_STREET, wait);
        typeInModalInput("addressCo", AcceptOfferShareholderTestData.SV_PERSON_CO, wait);
        typeInModalInput("addressZipcode", AcceptOfferShareholderTestData.SV_PERSON_ZIP, wait);
        typeInModalInput("addressCity", AcceptOfferShareholderTestData.SV_PERSON_CITY, wait);
        typeInModalInput("shares", String.valueOf(shares), wait);
    }

    private void fillSwedishLegalEntity(DataProfile profile, int shares, WebDriverWait wait) {
        selectNewLegalEntity(wait);
        selectSweden(wait);
        if (profile == DataProfile.MULTI) {
            typeInModalInput("orgnr", AcceptOfferShareholderTestData.MULTI_SV_LEGAL_ORG, wait);
            typeInModalInput("juridicalName", AcceptOfferShareholderTestData.MULTI_SV_LEGAL_NAME, wait);
            typeInModalInput("juridicalPersonEmail", AcceptOfferShareholderTestData.MULTI_SV_LEGAL_EMAIL, wait);
        } else {
            typeInModalInput("orgnr", AcceptOfferShareholderTestData.SV_LEGAL_ORG, wait);
            typeInModalInput("juridicalName", AcceptOfferShareholderTestData.SV_LEGAL_NAME, wait);
            typeInModalInput("juridicalPersonEmail", AcceptOfferShareholderTestData.DEFAULT_EMAIL, wait);
        }
        typeInModalInput("addressText", AcceptOfferShareholderTestData.SV_LEGAL_STREET, wait);
        typeInModalInput("addressCo", AcceptOfferShareholderTestData.SV_LEGAL_CO, wait);
        typeInModalInput("addressZipcode", AcceptOfferShareholderTestData.SV_LEGAL_ZIP, wait);
        typeInModalInput("addressCity", AcceptOfferShareholderTestData.SV_LEGAL_CITY, wait);
        typeInModalInput("shares", String.valueOf(shares), wait);
    }

    private void fillForeignPerson(DataProfile profile, int shares, WebDriverWait wait) {
        selectNewPerson(wait);
        selectAbroad(wait);
        if (profile == DataProfile.MULTI) {
            selectModalDropdown("nationality", AcceptOfferShareholderTestData.MULTI_FOREIGN_PERSON_NATIONALITY, wait);
            typeInModalInput("ssn", AcceptOfferShareholderTestData.MULTI_FOREIGN_PERSON_DOB, wait);
            typeInModalInput("firstname", AcceptOfferShareholderTestData.MULTI_FOREIGN_PERSON_FIRST, wait);
            typeInModalInput("lastname", AcceptOfferShareholderTestData.MULTI_FOREIGN_PERSON_LAST, wait);
            typeInModalInput("email", AcceptOfferShareholderTestData.MULTI_FOREIGN_PERSON_EMAIL, wait);
        } else {
            selectModalDropdown("nationality", AcceptOfferShareholderTestData.FOREIGN_PERSON_NATIONALITY, wait);
            typeInModalInput("ssn", AcceptOfferShareholderTestData.FOREIGN_PERSON_DOB, wait);
            typeInModalInput("firstname", AcceptOfferShareholderTestData.FOREIGN_PERSON_FIRST, wait);
            typeInModalInput("lastname", AcceptOfferShareholderTestData.FOREIGN_PERSON_LAST, wait);
            typeInModalInput("email", AcceptOfferShareholderTestData.DEFAULT_EMAIL, wait);
        }
        typeInModalInput("addressText", AcceptOfferShareholderTestData.FOREIGN_PERSON_STREET, wait);
        typeInModalInput("addressCo", AcceptOfferShareholderTestData.FOREIGN_PERSON_CO, wait);
        typeInModalInput("addressZipcode", AcceptOfferShareholderTestData.FOREIGN_PERSON_ZIP, wait);
        typeInModalInput("addressCity", AcceptOfferShareholderTestData.FOREIGN_PERSON_CITY, wait);
        typeInModalInput("shares", String.valueOf(shares), wait);
    }

    private void fillForeignLegalEntity(DataProfile profile, int shares, WebDriverWait wait) {
        selectNewLegalEntity(wait);
        selectAbroad(wait);
        if (profile == DataProfile.MULTI) {
            typeInModalInput("orgnr", AcceptOfferShareholderTestData.MULTI_FOREIGN_LEGAL_ORG, wait);
            typeInModalInput("juridicalName", AcceptOfferShareholderTestData.MULTI_FOREIGN_LEGAL_NAME, wait);
            typeInModalInput("juridicalPersonEmail", AcceptOfferShareholderTestData.MULTI_FOREIGN_LEGAL_EMAIL, wait);
        } else {
            typeInModalInput("orgnr", AcceptOfferShareholderTestData.FOREIGN_LEGAL_ORG, wait);
            typeInModalInput("juridicalName", AcceptOfferShareholderTestData.FOREIGN_LEGAL_NAME, wait);
            typeInModalInput("juridicalPersonEmail", AcceptOfferShareholderTestData.DEFAULT_EMAIL, wait);
        }
        typeInModalInput("addressText", AcceptOfferShareholderTestData.FOREIGN_LEGAL_STREET, wait);
        typeInModalInput("addressCo", AcceptOfferShareholderTestData.FOREIGN_LEGAL_CO, wait);
        typeInModalInput("addressZipcode", AcceptOfferShareholderTestData.FOREIGN_LEGAL_ZIP, wait);
        typeInModalInput("addressCity", AcceptOfferShareholderTestData.FOREIGN_LEGAL_CITY, wait);
        selectModalDropdown("addressCountry", AcceptOfferShareholderTestData.FOREIGN_LEGAL_COUNTRY, wait);
        typeInModalInput("shares", String.valueOf(shares), wait);
    }

    private void assertShareholderVisibleAfterAdd(ShareholderVariantType type, DataProfile profile) {
        switch (type) {
            case SWEDISH_PERSON:
                if (profile == DataProfile.MULTI) {
                    assertShareholderListedInTable(
                            AcceptOfferShareholderTestData.MULTI_SV_PERSON_FIRST,
                            AcceptOfferShareholderTestData.MULTI_SV_PERSON_EMAIL,
                            null);
                } else {
                    assertShareholderListedInTable(
                            AcceptOfferShareholderTestData.SV_PERSON_FIRST,
                            AcceptOfferShareholderTestData.DEFAULT_EMAIL,
                            null);
                }
                break;
            case SWEDISH_LEGAL_ENTITY:
                if (profile == DataProfile.MULTI) {
                    assertShareholderListedInTable(
                            AcceptOfferShareholderTestData.MULTI_SV_LEGAL_NAME,
                            null,
                            AcceptOfferShareholderTestData.MULTI_SV_LEGAL_ORG);
                } else {
                    assertShareholderListedInTable(
                            AcceptOfferShareholderTestData.SV_LEGAL_NAME,
                            null,
                            AcceptOfferShareholderTestData.SV_LEGAL_ORG);
                }
                break;
            case FOREIGN_PERSON:
                if (profile == DataProfile.MULTI) {
                    assertShareholderListedInTable(
                            AcceptOfferShareholderTestData.MULTI_FOREIGN_PERSON_FIRST,
                            AcceptOfferShareholderTestData.MULTI_FOREIGN_PERSON_EMAIL,
                            null);
                } else {
                    assertShareholderListedInTable(
                            AcceptOfferShareholderTestData.FOREIGN_PERSON_FIRST,
                            AcceptOfferShareholderTestData.DEFAULT_EMAIL,
                            null);
                }
                break;
            case FOREIGN_LEGAL_ENTITY:
                if (profile == DataProfile.MULTI) {
                    assertShareholderListedInTable(
                            AcceptOfferShareholderTestData.MULTI_FOREIGN_LEGAL_NAME,
                            null,
                            AcceptOfferShareholderTestData.MULTI_FOREIGN_LEGAL_ORG);
                } else {
                    assertShareholderListedInTable(
                            AcceptOfferShareholderTestData.FOREIGN_LEGAL_NAME,
                            null,
                            AcceptOfferShareholderTestData.FOREIGN_LEGAL_ORG);
                }
                break;
            default:
                break;
        }
    }

    private void waitForCompanyTotalSharesReady(WebDriverWait wait) {
        wait.until(d -> readTotalSharesFromPage(new WebDriverWait(d, Duration.ofSeconds(5))) > 0);
        LOG.info("Company total shares loaded on accept-offer page");
    }

    private void waitForRemainingShares(WebDriverWait wait, int expectedRemaining) {
        wait.until(d -> readRemainingSharesToAllocate(new WebDriverWait(d, Duration.ofSeconds(5))) == expectedRemaining);
        LOG.info("Remaining shares updated to {}", expectedRemaining);
    }

    private void assertAllSharesAllocated(WebDriverWait wait) {
        waitForRemainingShares(wait, 0);
        LOG.info("All company shares allocated — remaining shares is 0");
    }

    private int readTotalSharesFromPage(WebDriverWait wait) {
        SharesSnapshot snapshot = readSharesSnapshot(wait);
        return snapshot.total;
    }

    private int readRemainingSharesToAllocate(WebDriverWait wait) {
        return readSharesSnapshot(wait).remaining;
    }

    private SharesSnapshot readSharesSnapshot(WebDriverWait wait) {
        WebElement valueEl = wait.until(ExpectedConditions.visibilityOfElementLocated(REMAINING_SHARES_VALUE));
        String text = valueEl.getText().trim().replace('\u00a0', ' ');
        Matcher lineMatcher = SHARES_REMAINING_LINE.matcher(text);
        if (lineMatcher.find()) {
            return new SharesSnapshot(
                    Integer.parseInt(lineMatcher.group(1)),
                    Integer.parseInt(lineMatcher.group(2)));
        }
        Matcher leading = LEADING_INTEGER.matcher(text);
        if (leading.find()) {
            int remaining = Integer.parseInt(leading.group(1));
            return new SharesSnapshot(remaining, remaining);
        }
        throw new IllegalStateException("Could not parse shares remaining line: [" + text + "]");
    }

    private void openAddOwnerModal(WebDriverWait wait) {
        scrollPageToViewElement(ADD_OWNER_BUTTON);
        wait.until(ExpectedConditions.elementToBeClickable(ADD_OWNER_BUTTON));
        clickByJS(ADD_OWNER_BUTTON);
        wait.until(ExpectedConditions.visibilityOfElementLocated(SHAREHOLDER_MODAL));
        waitForSpecifiedTime(1);
    }

    private void submitAddOwnerModal(WebDriverWait wait) {
        waitForSpecifiedTime(1);
        scrollPageToViewElement(MODAL_ADD_BUTTON);
        wait.until(ExpectedConditions.elementToBeClickable(MODAL_ADD_BUTTON));
        clickByJS(MODAL_ADD_BUTTON);
        try {
            wait.until(ExpectedConditions.invisibilityOfElementLocated(SHAREHOLDER_MODAL));
        } catch (org.openqa.selenium.TimeoutException e) {
            String modalText = "";
            if (!driver.findElements(SHAREHOLDER_MODAL).isEmpty()) {
                modalText = driver.findElement(SHAREHOLDER_MODAL).getText();
            }
            throw new IllegalStateException(
                    "Add owner modal did not close — form validation or API error. Modal text: "
                            + modalText.replace('\n', ' '),
                    e);
        }
        waitForSpecifiedTime(2);
    }

    private void selectNewPerson(WebDriverWait wait) {
        ensureRadioSelected(NEW_PERSON_RADIO, wait);
    }

    private void selectNewLegalEntity(WebDriverWait wait) {
        ensureRadioSelected(NEW_LEGAL_ENTITY_RADIO, wait);
    }

    private void selectSweden(WebDriverWait wait) {
        ensureRadioSelected(SWEDEN_RADIO, wait);
    }

    private void selectAbroad(WebDriverWait wait) {
        ensureRadioSelected(ABROAD_RADIO, wait);
    }

    private void ensureRadioSelected(By radio, WebDriverWait wait) {
        WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(radio));
        if (!el.isSelected()) {
            scrollPageToViewElement(radio);
            clickByJS(radio);
            waitForSpecifiedTime(1);
        }
    }

    private void typeInModalInput(String name, String value, WebDriverWait wait) {
        By locator = By.cssSelector(".display-shareholders-modal input[name='" + name + "']");
        for (int attempt = 0; attempt < 4; attempt++) {
            WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
            try {
                scrollPageToViewElement(locator);
                setReactInputValue(input, value);
                if ("shares".equals(name)) {
                    waitForSpecifiedTime(1);
                }
                return;
            } catch (StaleElementReferenceException e) {
                waitForSpecifiedTime(1);
            }
        }
        throw new org.openqa.selenium.NoSuchElementException("Could not type into modal input: " + name);
    }

    /** React controlled inputs need native value setter + input event (especially shares on submit). */
    private void setReactInputValue(WebElement input, String value) {
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "const el = arguments[0];"
                        + "const value = arguments[1];"
                        + "const setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;"
                        + "setter.call(el, value);"
                        + "el.dispatchEvent(new Event('input', { bubbles: true }));"
                        + "el.dispatchEvent(new Event('change', { bubbles: true }));"
                        + "el.blur();",
                input,
                value);
    }

    private void selectModalDropdown(String name, String value, WebDriverWait wait) {
        By locator = By.cssSelector(".display-shareholders-modal select[name='" + name + "']");
        WebElement selectEl = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        scrollPageToViewElement(locator);
        new Select(selectEl).selectByValue(value);
        waitForSpecifiedTime(1);
    }

    /**
     * Legal entities expose {@code juridicalPersonEmail} in the API payload but the table renders {@code person.email},
     * so legal-entity rows are verified by company name + org number instead of email.
     */
    private void assertShareholderListedInTable(String displayName, String email, String orgOrSsn) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        wait.until(d -> {
            java.util.List<WebElement> rows = d.findElements(SHAREHOLDER_TABLE_ROWS);
            for (WebElement row : rows) {
                String rowText = row.getText();
                if (!rowText.contains(displayName)) {
                    continue;
                }
                if (email != null && !rowText.contains(email)) {
                    continue;
                }
                if (orgOrSsn != null) {
                    String normalizedOrg = orgOrSsn.replace("-", "");
                    String normalizedRow = rowText.replace("-", "").replace(" ", "");
                    if (!normalizedRow.contains(normalizedOrg)) {
                        continue;
                    }
                }
                return true;
            }
            return false;
        });
        LOG.info("Shareholder listed in table — name={}, email={}, orgOrSsn={}", displayName, email, orgOrSsn);
    }

    private static final class SharesSnapshot {
        private final int remaining;
        private final int total;

        private SharesSnapshot(int remaining, int total) {
            this.remaining = remaining;
            this.total = total;
        }
    }
}
