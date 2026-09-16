package com.qa.bolags.pages;

import com.qa.bolags.constants.AcceptOfferContext;
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

import java.time.Duration;
import java.util.List;

/**
 * Client decline-offer flow ({@code /app/liqTok/declineOffer}).
 */
public class DeclineOfferPage extends TestUtil {

    private static final Logger LOG = LoggerFactory.getLogger(DeclineOfferPage.class);

    private static final By DECLINE_PAGE_MARKER = By.xpath(
            "//*[contains(.,'decline') or contains(.,'Decline') or contains(.,'avböj')"
                    + " or contains(.,'not accept') or contains(.,'inte accepter')]");
    private static final By OTHER_REASON_LABEL = By.xpath(
            "//label[contains(normalize-space(.),'Other reason') or contains(normalize-space(.),'Annan orsak')"
                    + " or contains(normalize-space(.),'Annan')]");
    private static final By OTHER_REASON_RADIO = By.xpath(
            "//input[@type='radio' and (translate(@value,'other','OTHER')='OTHER'"
                    + " or @id='other' or contains(@name,'other'))]");
    private static final By DECLINE_MESSAGE = By.xpath(
            "//textarea[contains(@placeholder,'Tell us more') or contains(@placeholder,'Berätta mer')"
                    + " or @name='message' or @name='declineMessage']");
    private static final By SEND_MESSAGE_BUTTON = By.xpath(
            "//button[contains(.,'Send message') or contains(.,'Skicka meddelande')"
                    + " or contains(.,'Submit') or contains(.,'Skicka')]");

    public DeclineOfferPage(WebDriver driver) {
        super(driver);
    }

    public void openDeclineOfferFromCapturedSendOfferResponse() {
        String url = AcceptOfferContext.buildDeclineOfferUrl();
        LOG.info("Opening decline offer URL for orderId={}", AcceptOfferContext.getOrderIdOrNull());
        driver.get(QaServerCredentials.urlWithHttpBasicAuth(url));
        waitForLoad();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(45));
        try {
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.urlContains("declineOffer"),
                    ExpectedConditions.visibilityOfElementLocated(DECLINE_MESSAGE),
                    ExpectedConditions.presenceOfElementLocated(OTHER_REASON_RADIO)));
        } catch (TimeoutException e) {
            Assert.fail("Decline offer page did not load. URL: " + driver.getCurrentUrl());
        }
        waitForSpecifiedTime(3);
        revealDeclineFormFields();
    }

    public void selectOtherReasonAndSubmitDecline(String message) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(45));
        wait.until(ExpectedConditions.or(
                ExpectedConditions.visibilityOfElementLocated(DECLINE_MESSAGE),
                ExpectedConditions.presenceOfElementLocated(OTHER_REASON_RADIO),
                ExpectedConditions.urlContains("declineOffer")));

        selectOtherDeclineReason(wait);
        if (!driver.findElements(DECLINE_MESSAGE).isEmpty()) {
            enterStringValueInInputField(DECLINE_MESSAGE, message);
        }
        revealDeclineFormFields();
        scrollPageToViewElement(SEND_MESSAGE_BUTTON);
        wait.until(ExpectedConditions.elementToBeClickable(SEND_MESSAGE_BUTTON));
        WebElement submit = driver.findElement(SEND_MESSAGE_BUTTON);
        scrollElementIntoViewableArea(submit);
        try {
            new org.openqa.selenium.interactions.Actions(driver).moveToElement(submit).click().perform();
        } catch (Exception clickError) {
            LOG.warn("Retrying decline submit via JS: {}", clickError.getMessage());
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submit);
        }
        waitForLoad();
        waitForSpecifiedTime(3);
    }

    public void assertDeclineOfferSubmitted() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        try {
            wait.until(d -> {
                String src = d.getPageSource();
                return src.contains("LIQ_NOT_ACCEPTED")
                        || src.contains("NewCompletionNeeded")
                        || src.contains("not accepted")
                        || src.contains("inte accepterad")
                        || src.contains("Tack")
                        || d.findElements(SEND_MESSAGE_BUTTON).stream().noneMatch(el -> {
                            try {
                                return el.isDisplayed();
                            } catch (StaleElementReferenceException ex) {
                                return false;
                            }
                        });
            });
        } catch (TimeoutException e) {
            Assert.fail("Expected decline-offer completion UI after submitting decline form. URL: "
                    + driver.getCurrentUrl());
        }
    }

    private void revealDeclineFormFields() {
        By[] declineFields = {OTHER_REASON_LABEL, OTHER_REASON_RADIO, DECLINE_MESSAGE, SEND_MESSAGE_BUTTON};
        for (By field : declineFields) {
            for (WebElement element : driver.findElements(field)) {
                try {
                    if (element.isDisplayed() || "input".equalsIgnoreCase(element.getTagName())) {
                        scrollElementIntoViewableArea(element);
                    }
                } catch (StaleElementReferenceException ignored) {
                    // retry next element
                }
            }
        }
    }

    private void selectOtherDeclineReason(WebDriverWait wait) {
        for (WebElement label : driver.findElements(OTHER_REASON_LABEL)) {
            if (clickDisplayedOrJs(label)) {
                LOG.info("Selected Other decline reason via label");
                waitForSpecifiedTime(1);
                return;
            }
        }

        List<WebElement> radios = driver.findElements(OTHER_REASON_RADIO);
        if (!radios.isEmpty()) {
            WebElement radio = radios.get(radios.size() - 1);
            scrollElementIntoViewableArea(radio);
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript(
                    "arguments[0].click();"
                            + "arguments[0].checked = true;"
                            + "arguments[0].dispatchEvent(new Event('change', {bubbles:true}));",
                    radio);
            LOG.info("Selected Other decline reason via hidden radio input");
            waitForSpecifiedTime(1);
            return;
        }

        Assert.fail("Could not find Other/Annan decline reason on page. URL: " + driver.getCurrentUrl());
    }

    private boolean clickDisplayedOrJs(WebElement element) {
        try {
            scrollElementIntoViewableArea(element);
            if (element.isDisplayed() && element.isEnabled()) {
                element.click();
                return true;
            }
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
            return true;
        } catch (Exception e) {
            LOG.debug("Could not click decline reason element: {}", e.getMessage());
            return false;
        }
    }
}
