package com.qa.bolags.pages;

import com.qa.bolags.constants.QaServerCredentials;
import com.qa.bolags.constants.OrderOrganizationNumberContext;
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
    private final By streetAddress = By.xpath(
            "//input[@placeholder='Gatuadress' or @name='street' or @name='streetAddress'"
                    + " or contains(@placeholder,'Street address')]");
    private final By postalCode = By.xpath(
            "//input[@placeholder='Postnummer' or @name='postalCode' or @name='zipCode'"
                    + " or contains(@placeholder,'Postal')]");
    private final By city = By.xpath(
            "//input[@placeholder='Ort' or @name='city' or contains(@placeholder,'City')]");
    private final By telephone = By.xpath(
            "//input[@placeholder='Telefon' or @name='phone' or @name='telephone'"
                    + " or contains(@placeholder,'Phone')]");
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
        log.info("Waiting for Offer page to be displayed.");
        try {
            new WebDriverWait(driver, Duration.ofSeconds(20))
                    .until(ExpectedConditions.visibilityOfElementLocated(OFFER_PAGE_HEADER));
            scrollPageToViewElement(OFFER_PAGE_HEADER);
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Types a random 3-letter query (no fixed company name), waits for suggestions, and picks one at random.
     */
    public void searchCompany() {
        scrollPageToViewElement(searchbar);
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
                String organizationNumber = OrderOrganizationNumberContext.captureFromCompanyLabel(selectedLabel);
                if (organizationNumber == null) {
                    throw new IllegalStateException(
                        "Selected company suggestion did not include a Swedish organisationsnummer: "
                            + selectedLabel);
                }
                log.info("Selecting random company ({}/{}): {}", index + 1, companies.size(), selectedLabel);
                log.info("Captured selected company organisationsnummer: {}", organizationNumber);
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", chosen);
                waitForSpecifiedTime(1);
                wait.until(ExpectedConditions.visibilityOfElementLocated(fornamnField));
                scrollOfferContactSectionIntoView();
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
        enterStringValueInInputField(searchbar, query);
        waitForSpecifiedTime(1);
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
        scrollPageToViewElement(agreementCheckbox);
        if (!isCheckboxSelected(agreementCheckbox)) {
            try {
                click(agreementCheckbox);
            } catch (Exception e) {
                log.warn("Retrying privacy checkbox via JS: {}", e.getMessage());
                clickByJS(agreementCheckbox);
            }
        }
        waitForSpecifiedTime(1);
    }

    public void clickonGoOn() {
        clickFortsattButton();
        waitForLoad();
        waitForWizardStepTransition();
        scrollToActiveWizardStep();
        waitForSpecifiedTime(1);
    }

    private void clickFortsattButton() {
        waitForElementToBeVisible(fortsattButton);
        scrollPageToViewElement(fortsattButton);
        waitForOverlayToDisappear();
        waitForElementToBeClickable(fortsattButton);
        clickFortsattButtonInView();
    }

    private void clickFortsattButtonInView() {
        WebElement button = driver.findElement(fortsattButton);
        scrollElementIntoViewableArea(button);
        try {
            new org.openqa.selenium.interactions.Actions(driver).moveToElement(button).click().perform();
        } catch (Exception intercepted) {
            log.warn("Retrying Fortsätt click via JS due to: {}", intercepted.getMessage());
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", button);
        }
    }

    /** Best-effort Fortsätt click for negative validation when the CTA may be hidden or disabled. */
    private boolean tryClickFortsattButton() {
        if (!isDisplayedSafely(fortsattButton)) {
            log.info("Fortsätt not shown — wizard already blocks progress without required data.");
            return false;
        }
        try {
            scrollPageToViewElement(fortsattButton);
            waitForOverlayToDisappear();
            new WebDriverWait(driver, Duration.ofSeconds(8))
                    .until(ExpectedConditions.elementToBeClickable(fortsattButton));
            clickFortsattButtonInView();
            return true;
        } catch (Exception e) {
            log.info("Fortsätt not actionable without required data: {}", e.getMessage());
            return false;
        }
    }

    public void uploadDocument(String filePath) {
        waitForUploadStepReady();
        scrollToActiveWizardStep();
        String resolvedPath = resolveUploadPath(filePath);
        WebElement uploadElement = new WebDriverWait(driver, Duration.ofSeconds(20))
                .until(d -> firstEnabledUploadInput(d));
        scrollElementIntoViewableArea(uploadElement);
        uploadElement.sendKeys(resolvedPath);
        log.info("File uploaded: {}", resolvedPath);
        waitForSpecifiedTime(2);
        waitForOverlayToDisappear();
    }

    public void enterAddressDetails(String streetAddressValue, String postalCodeValue, String cityName,
                                    String telephoneNumber, String mobile, String agency, String messageToPartner) {
        waitForAddressStepReady();
        scrollToActiveWizardStep();
        enterStringValueInInputField(streetAddress, streetAddressValue);
        enterStringValueInInputField(postalCode, postalCodeValue);
        enterStringValueInInputField(city, cityName);
        enterStringValueInInputField(telephone, telephoneNumber);
        log.info("Entering mobile '{}'.", mobile);
        enterStringValueInInputField(mobileNumber, mobile);
        log.info("Entering agency/business '{}'.", agency);
        enterStringValueInInputField(agencyName, agency);
        log.info("Entering message to partner (length {}).", messageToPartner != null ? messageToPartner.length() : 0);
        enterStringValueInInputField(partnerMessage, messageToPartner != null ? messageToPartner : "");
        scrollPageToViewElement(save);
    }

    public void userClickOnSave() {
        scrollPageToViewElement(save);
        waitForElementToBeClickable(save);
        WebElement saveButton = driver.findElement(save);
        scrollElementIntoViewableArea(saveButton);
        try {
            new org.openqa.selenium.interactions.Actions(driver).moveToElement(saveButton).click().perform();
        } catch (Exception e) {
            log.warn("Retrying Save click via JS: {}", e.getMessage());
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", saveButton);
        }
        waitForSpecifiedTime(1);
    }

    public boolean isRequestReceivedPageDisplayed() {
        waitForElementToBeVisible(REQUEST_RECEIVED_HEADING);
        waitForSpecifiedTime(3);
        return super.isElementDisplayed(REQUEST_RECEIVED_HEADING);
    }

    private void waitForOverlayToDisappear() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(d -> d.findElements(modalOverlay).stream().noneMatch(el -> {
                        try {
                            return el.isDisplayed();
                        } catch (StaleElementReferenceException ex) {
                            return false;
                        }
                    }));
        } catch (TimeoutException e) {
            log.warn("Modal overlay still visible — continuing best-effort.");
        }
    }

    private void waitForWizardStepTransition() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(25)).until(d ->
                    firstEnabledUploadInput(d) != null
                            || isDisplayedSafely(streetAddress)
                            || isDisplayedSafely(REQUEST_RECEIVED_HEADING));
        } catch (TimeoutException e) {
            log.warn("Wizard step transition not detected after Fortsätt.");
        }
    }

    private void waitForUploadStepReady() {
        if (firstEnabledUploadInput(driver) != null) {
            return;
        }
        try {
            new WebDriverWait(driver, Duration.ofSeconds(20))
                    .until(d -> firstEnabledUploadInput(d) != null);
        } catch (TimeoutException e) {
            log.warn("Upload step not ready — retrying Fortsätt.");
            clickFortsattButton();
            waitForLoad();
            waitForSpecifiedTime(2);
            new WebDriverWait(driver, Duration.ofSeconds(30))
                    .until(d -> firstEnabledUploadInput(d) != null);
        }
    }

    private WebElement firstEnabledUploadInput(org.openqa.selenium.SearchContext context) {
        for (WebElement el : context.findElements(upload)) {
            try {
                if (el.isEnabled()) {
                    return el;
                }
            } catch (StaleElementReferenceException ignored) {
                // retry
            }
        }
        return null;
    }

    private void waitForAddressStepReady() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(30))
                    .until(ExpectedConditions.visibilityOfElementLocated(streetAddress));
        } catch (TimeoutException e) {
            log.warn("Address step not visible — retrying Fortsätt after upload.");
            if (isDisplayedSafely(upload) && isDisplayedSafely(fortsattButton)) {
                clickFortsattButton();
                waitForLoad();
                waitForSpecifiedTime(2);
            }
            new WebDriverWait(driver, Duration.ofSeconds(25))
                    .until(ExpectedConditions.visibilityOfElementLocated(streetAddress));
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

    /** Clicks Fortsätt/GoOn without selecting a company (negative validation). */
    public void clickContinueWithoutCompanySelection() {
        scrollPageToViewElement(searchbar);
        tryClickFortsattButton();
        waitForSpecifiedTime(1);
    }

    /** Clicks Fortsätt without filling contact fields after company selection. */
    public void clickContinueWithoutContactDetails() {
        revealContactFieldsBeforeContinue();
        clickFortsattButton();
        waitForSpecifiedTime(1);
    }

    /**
     * Scrolls contact fields and the continue CTA into view so values and validation
     * are visible during headed execution. Ends on Fortsätt so the CTA stays in focus.
     */
    private void revealContactFieldsBeforeContinue() {
        By[] contactStepFields = {
                fornamnField,
                efternamnField,
                epostField,
                agreementCheckbox,
                fortsattButton
        };
        for (By field : contactStepFields) {
            if (isDisplayedSafely(field)) {
                scrollPageToViewElement(field);
            }
        }
    }

    /** Keeps the contact-details block and Fortsätt CTA inside the scrollable wizard viewport. */
    private void scrollOfferContactSectionIntoView() {
        revealContactFieldsBeforeContinue();
    }

    /** Scrolls the current wizard step content into view instead of resetting scroll to top. */
    private void scrollToActiveWizardStep() {
        if (firstEnabledUploadInput(driver) != null) {
            WebElement uploadElement = firstEnabledUploadInput(driver);
            if (uploadElement != null) {
                scrollElementIntoViewableArea(uploadElement);
            }
            return;
        }
        if (isDisplayedSafely(streetAddress)) {
            revealAddressFieldsBeforeEntry();
            return;
        }
        if (isDisplayedSafely(fornamnField)) {
            revealContactFieldsBeforeContinue();
            return;
        }
        if (isDisplayedSafely(fortsattButton)) {
            scrollPageToViewElement(fortsattButton);
        }
    }

    private void revealAddressFieldsBeforeEntry() {
        By[] addressStepFields = {
                streetAddress,
                postalCode,
                city,
                telephone,
                mobileNumber,
                agencyName,
                partnerMessage,
                save
        };
        for (By field : addressStepFields) {
            if (isDisplayedSafely(field)) {
                scrollPageToViewElement(field);
            }
        }
    }

    /** Asserts user is still on the liquidation offer step (not advanced to upload/address). */
    public void assertStillOnOfferPage() {
        boolean offerHeader = isOfferPageDisplayed();
        boolean uploadVisible = isDisplayedSafely(upload);
        boolean requestReceived = isDisplayedSafely(REQUEST_RECEIVED_HEADING);
        org.testng.Assert.assertTrue(offerHeader && !requestReceived,
                "Expected to remain on offer page. offerHeader=" + offerHeader
                        + ", uploadVisible=" + uploadVisible + ", requestReceived=" + requestReceived);
        org.testng.Assert.assertFalse(uploadVisible || requestReceived,
                "Should not advance to upload or confirmation without required data");
    }

    private boolean isDisplayedSafely(By locator) {
        return driver.findElements(locator).stream().anyMatch(el -> {
            try {
                return el.isDisplayed();
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });
    }
}
