package com.qa.bolags.pages;

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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.qa.bolags.baseTest.BaseTest.finalUrl;

public class liquidationpage extends TestUtil {

    private static final String SWEDISH_LOWER = "abcdefghijklmnopqrstuvwxyz\u00E5\u00E4\u00F6";
    private static final String SWEDISH_UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ\u00C5\u00C4\u00D6";
    private static final Duration CTA_WAIT = Duration.ofSeconds(15);

    private static final By OFFER_PAGE_HEADER = By.xpath("//h4[contains(text(),'Offert snabbavveckling')]");
    private static final By REQUEST_RECEIVED_HEADING = By.xpath("//h2[contains(text(),'F\u00F6rfr\u00E5gan mottagen')]");

    private final By searchbar = By.xpath("(//input[@name='company'])[last()]");
    private static final By COMPANY_DROPDOWN_ITEM = By.cssSelector(".company-dropdown-item");
    /** App searches when {@code company} input length is &gt;= 3 (see Step1Company / liquidation page). */
    private static final int MIN_COMPANY_SEARCH_LENGTH = 3;
    private static final int MAX_RANDOM_SEARCH_ATTEMPTS = 10;
    private static final String RANDOM_SEARCH_ALPHABET = "abcdefghijklmnopqrstuvwxyz";
    private final By fornamnField = By.xpath("(//input[@name='firstName' or @placeholder='F\u00F6rnamn'])[last()]");
    private final By efternamnField = By.xpath("(//input[@name='lastName' or @placeholder='Efternamn'])[last()]");
    private final By epostField = By.xpath("(//input[@name='email' or @placeholder='E-post' or @type='email'])[last()]");
    private final By agreementCheckbox = By.xpath("//input[@name='privacyChecked']");
    private final By fortsattButton = By.xpath("(//button[contains(translate(normalize-space(.), '" + SWEDISH_LOWER + "', '" + SWEDISH_UPPER + "'), 'FORTS')]|//button[contains(@class,'bg-[#ffd454]')])[last()]");
    private final By save = By.xpath("//button[normalize-space()='SPARA']");
    private final By upload = By.xpath("//input[@type='file']");
    private final By streetAddress = By.xpath("//input[@placeholder='Gatuadress']");
    private final By postalCode = By.xpath("//input[@placeholder='Postnummer']");
    private final By city = By.xpath("//input[@placeholder='Ort']");
    private final By telephone = By.xpath("//input[@placeholder='Telefon']");
    /** name="car" in app markup; placeholder may vary by locale so match on name + type. */
    private final By mobileNumber = By.xpath("(//input[@type='text' and @name='car'])[last()]");
    /** Prefer name; placeholder is optional (QA may omit or localize "Business"). */
    private final By agencyName = By.xpath("(//input[@type='text' and (@name='agency' or normalize-space(@placeholder)='Business')])[last()]");
    /** Optional free-text; often a textarea below Business/agency. */
    private final By partnerMessage = By.xpath("(//*[self::textarea or (self::input and @type='text')]"
            + "[contains(@placeholder,'Message to Partner')"
            + " or contains(@placeholder,'Write your message')"
            + " or @name='message'"
            + " or @name='partnerMessage'"
            + "])[last()]");
    private final By modalOverlay = By.cssSelector("div.fixed.inset-0");

    public liquidationpage(WebDriver driver) {
        super(driver);
    }

    public void logintoapplication() {
        driver.get(QaServerCredentials.urlWithHttpBasicAuth(finalUrl));
    }

    public boolean isOfferPageDisplayed() {
        waitForSpecifiedTime(2);
        log.info("Waiting for Offer page to displayed.");
        return super.isElementDisplayed(OFFER_PAGE_HEADER);
    }

    /**
     * Types a random 3-letter query (no fixed company name), waits for suggestions, and picks one at random.
     */
    public void searchCompany() {
        waitForElementToBeClickable(searchbar);
        clickByJS(searchbar);

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int attempt = 1; attempt <= MAX_RANDOM_SEARCH_ATTEMPTS; attempt++) {
            String query = randomSearchQuery(random);
            log.info("Company search attempt {}/{} with random query '{}'", attempt, MAX_RANDOM_SEARCH_ATTEMPTS, query);
            clearAndTypeCompanySearch(query);

            try {
                wait.until(d -> !d.findElements(COMPANY_DROPDOWN_ITEM).isEmpty());
                List<WebElement> companies = driver.findElements(COMPANY_DROPDOWN_ITEM);
                int index = random.nextInt(companies.size());
                WebElement chosen = companies.get(index);
                String selectedLabel = chosen.getText().trim();
                log.info("Selecting random company ({}/{}): {}", index + 1, companies.size(), selectedLabel);
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", chosen);
                waitForSpecifiedTime(1);
                wait.until(ExpectedConditions.visibilityOfElementLocated(fornamnField));
                return;
            } catch (TimeoutException | StaleElementReferenceException e) {
                log.warn("No company suggestions for query '{}': {}", query, e.getMessage());
            }
        }
        throw new IllegalStateException(
                "Could not select a company after " + MAX_RANDOM_SEARCH_ATTEMPTS + " random search attempts");
    }

    private static String randomSearchQuery(ThreadLocalRandom random) {
        StringBuilder query = new StringBuilder(MIN_COMPANY_SEARCH_LENGTH);
        for (int i = 0; i < MIN_COMPANY_SEARCH_LENGTH; i++) {
            query.append(RANDOM_SEARCH_ALPHABET.charAt(random.nextInt(RANDOM_SEARCH_ALPHABET.length())));
        }
        return query.toString();
    }

    private void clearAndTypeCompanySearch(String query) {
        WebElement input = driver.findElement(searchbar);
        input.clear();
        input.sendKeys(query);
        waitForSpecifiedTime(2);
    }

    public void enterFornamn(String fornamn) {
        log.info("Entering first name '{}'.", fornamn);
        enterStringValueInInputField(fornamnField, fornamn);
    }

    public void enterEfternamn(String efternamn) {
        log.info("Entering last name '{}'.", efternamn);
        enterStringValueInInputField(efternamnField, efternamn);
    }

    public void enterEpost(String epost) {
        log.info("Entering email '{}'.", epost);
        enterStringValueInInputField(epostField, epost);
    }

    public void agreeToTermsAndContinue() {
        click(agreementCheckbox);
    }

    public void clickonGoOn() {
        waitForElementToBeVisible(fortsattButton);
        scrollPageToViewElement(fortsattButton);
        waitForOverlayToDisappear();
        waitForElementToBeClickable(fortsattButton);
        try {
            click(fortsattButton);
        } catch (Exception intercepted) {
            log.warn("Retrying Fortsätt click via JS due to: {}", intercepted.getMessage());
            clickByJS(fortsattButton);
        }
    }

    public void uploadDocument(String filePath) {
        waitForSpecifiedTime(3);
        String resolvedPath = resolveUploadPath(filePath);
        WebElement uploadElement = driver.findElement(upload);
        uploadElement.sendKeys(resolvedPath);
        log.info("File uploaded: {}", resolvedPath);
    }

    public void enterAddressDetails(String streetAddressValue, String postalCodeValue, String cityName,
                                    String telephoneNumber, String mobile, String agency, String messageToPartner) {
        enterStringValueInInputField(streetAddress, streetAddressValue);
        enterStringValueInInputField(postalCode, postalCodeValue);
        enterStringValueInInputField(city, cityName);
        enterStringValueInInputField(telephone, telephoneNumber);
        log.info("Entering mobile '{}'.", mobile);
        waitForElementToBeVisible(mobileNumber);
        scrollPageToViewElement(mobileNumber);
        enterStringValueInInputField(mobileNumber, mobile);
        log.info("Entering agency/business '{}'.", agency);
        waitForElementToBeVisible(agencyName);
        scrollPageToViewElement(agencyName);
        enterStringValueInInputField(agencyName, agency);
        log.info("Entering message to partner (length {}).", messageToPartner != null ? messageToPartner.length() : 0);
        waitForElementToBeVisible(partnerMessage);
        scrollPageToViewElement(partnerMessage);
        enterStringValueInInputField(partnerMessage, messageToPartner != null ? messageToPartner : "");
    }

    public void userClickOnSave() {
        waitForElementToBeClickable(save);
        click(save);
    }

    public boolean isRequestReceivedPageDisplayed() {
        waitForElementToBeVisible(REQUEST_RECEIVED_HEADING);
        waitForSpecifiedTime(3);
        return super.isElementDisplayed(REQUEST_RECEIVED_HEADING);
    }

    private void waitForOverlayToDisappear() {
        List<WebElement> overlays = driver.findElements(modalOverlay);
        if (!overlays.isEmpty()) {
            new WebDriverWait(driver, CTA_WAIT)
                    .until(ExpectedConditions.invisibilityOfAllElements(overlays));
        }
    }

    private String resolveUploadPath(String candidatePath) {
        Path path = Paths.get(candidatePath).normalize();
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir")).resolve(path).normalize();
        }
        if (!Files.exists(path)) {
            Path fallback = Paths.get(System.getProperty("user.dir"), "src", "main", "ABC.pdf");
            if (Files.exists(fallback)) {
                log.warn("Upload file {} missing, falling back to {}", path, fallback);
                path = fallback;
            } else {
                throw new RuntimeException("Upload file not found at " + path);
            }
        }
        return path.toAbsolutePath().toString();
    }
}
