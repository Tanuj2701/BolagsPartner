package com.qa.bolags.pages;

import com.qa.bolags.utility.TestUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client accept-offer flow ({@code /app/liqTok/acceptOffer}) — shareholders section.
 */
public class AcceptOfferPage extends TestUtil {

    private static final Logger LOG = LoggerFactory.getLogger(AcceptOfferPage.class);

    private static final Pattern LEADING_INTEGER = Pattern.compile("^(\\d+)");

    private static final By ADD_SHAREHOLDER_BUTTON = By.xpath(
            "//button[contains(., 'ADD SHAREHOLDER') or contains(., 'L\u00C4GG TILL AKTIE\u00C4GARE')]");
    private static final By SHAREHOLDER_MODAL = By.cssSelector(".display-shareholders-modal");
    private static final By NEW_PERSON_RADIO = By.id("person");
    private static final By SWEDEN_RADIO = By.id("sverige");
    private static final By MODAL_ADD_BUTTON = By.xpath(
            "//div[contains(@class,'display-shareholders-modal')]//form//button[@type='submit']");
    private static final By REMAINING_SHARES_VALUE = By.xpath(
            "//span[contains(., 'Shares remaining') or contains(., 'Aktier kvar')]/following-sibling::span[1]");
    private static final By PAPER_SIGNATURE_RADIO = By.id("paper-signature");
    private static final By PAPER_SIGNATURE_LABEL = By.xpath(
            "//label[.//input[@id='paper-signature']]");
    private static final By SIGNATURE_DONE_BUTTON = By.xpath(
            "//h2[contains(., 'How would you like to sign') or contains(., 'How do you want to sign')"
                    + " or contains(., 'Hur vill ni skriva')]"
                    + "/following::button[contains(., 'DONE') or contains(., 'KLAR') or contains(., 'CLEAR')][1]");

    public AcceptOfferPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Adds one Swedish natural-person shareholder using the default QA test data and all remaining shares.
     */
    public void addDefaultSwedishPersonShareholder() {
        addSwedishPersonShareholder(
                "19781219-3519",
                "Karl",
                "Tangby",
                "tanrrr@gmail.com",
                "Main Street",
                "Test Company",
                "11129",
                "Stockholm");
    }

    public void addSwedishPersonShareholder(
            String personalId,
            String firstName,
            String lastName,
            String email,
            String street,
            String careOf,
            String zipCode,
            String city) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        int sharesToAllocate = readRemainingSharesToAllocate(wait);
        LOG.info("Remaining shares to allocate: {}", sharesToAllocate);

        scrollPageToViewElement(ADD_SHAREHOLDER_BUTTON);
        wait.until(ExpectedConditions.elementToBeClickable(ADD_SHAREHOLDER_BUTTON));
        clickByJS(ADD_SHAREHOLDER_BUTTON);
        wait.until(ExpectedConditions.visibilityOfElementLocated(SHAREHOLDER_MODAL));
        waitForSpecifiedTime(1);

        ensureRadioSelected(NEW_PERSON_RADIO, wait);
        ensureRadioSelected(SWEDEN_RADIO, wait);

        typeInModalInput("ssn", personalId, wait);
        typeInModalInput("firstname", firstName, wait);
        typeInModalInput("lastname", lastName, wait);
        typeInModalInput("email", email, wait);
        typeInModalInput("addressText", street, wait);
        typeInModalInput("addressCo", careOf, wait);
        typeInModalInput("addressZipcode", zipCode, wait);
        typeInModalInput("addressCity", city, wait);
        typeInModalInput("shares", String.valueOf(sharesToAllocate), wait);

        scrollPageToViewElement(MODAL_ADD_BUTTON);
        wait.until(ExpectedConditions.elementToBeClickable(MODAL_ADD_BUTTON));
        clickByJS(MODAL_ADD_BUTTON);

        wait.until(ExpectedConditions.invisibilityOfElementLocated(SHAREHOLDER_MODAL));
        waitForSpecifiedTime(2);
        Assert.assertTrue(
                isShareholderListed(firstName, lastName, email),
                "Shareholder " + firstName + " " + lastName + " was not listed after save");
    }

    /**
     * Selects paper/email signature and clicks the completion button (EN {@code DONE}, SV {@code KLAR}).
     */
    public void selectPaperSignatureAndComplete() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        scrollPageToViewElement(PAPER_SIGNATURE_LABEL);
        wait.until(ExpectedConditions.elementToBeClickable(PAPER_SIGNATURE_LABEL));
        if (!driver.findElement(PAPER_SIGNATURE_RADIO).isSelected()) {
            clickByJS(PAPER_SIGNATURE_LABEL);
        }
        wait.until(d -> d.findElement(PAPER_SIGNATURE_RADIO).isSelected());
        LOG.info("Selected paper signature (email) signing method");

        scrollPageToViewElement(SIGNATURE_DONE_BUTTON);
        wait.until(ExpectedConditions.elementToBeClickable(SIGNATURE_DONE_BUTTON));
        clickByJS(SIGNATURE_DONE_BUTTON);
        waitForLoad();
        waitForSpecifiedTime(3);
        Assert.assertTrue(
                isSignatureSubmissionComplete(wait),
                "Document checklist or next step did not appear after completing signature selection");
    }

    private boolean isSignatureSubmissionComplete(WebDriverWait wait) {
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

    /**
     * Parses the first number from the “Shares remaining …” row (e.g. {@code 500 (of 500 pcs)}).
     */
    int readRemainingSharesToAllocate(WebDriverWait wait) {
        WebElement valueEl = wait.until(ExpectedConditions.visibilityOfElementLocated(REMAINING_SHARES_VALUE));
        String text = valueEl.getText().trim();
        Matcher matcher = LEADING_INTEGER.matcher(text);
        if (!matcher.find()) {
            throw new IllegalStateException("Could not parse remaining shares from: [" + text + "]");
        }
        int shares = Integer.parseInt(matcher.group(1));
        if (shares <= 0) {
            throw new IllegalStateException("No shares remaining to allocate (parsed: " + shares + ")");
        }
        return shares;
    }

    private void ensureRadioSelected(By radio, WebDriverWait wait) {
        WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(radio));
        if (!el.isSelected()) {
            scrollPageToViewElement(radio);
            clickByJS(radio);
        }
    }

    private void typeInModalInput(String name, String value, WebDriverWait wait) {
        enterStringValueInInputField(
                By.cssSelector(".display-shareholders-modal input[name='" + name + "']"),
                value);
    }

    private boolean isShareholderListed(String firstName, String lastName, String email) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(20)).until(d -> {
                String src = d.getPageSource();
                return src.contains(firstName) && src.contains(lastName) && src.contains(email);
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
