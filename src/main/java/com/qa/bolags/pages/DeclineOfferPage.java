package com.qa.bolags.pages;

import com.qa.bolags.constants.AcceptOfferContext;
import com.qa.bolags.constants.QaServerCredentials;
import com.qa.bolags.utility.TestUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import java.time.Duration;

/**
 * Client decline-offer flow ({@code /app/liqTok/declineOffer}).
 */
public class DeclineOfferPage extends TestUtil {

    private static final Logger LOG = LoggerFactory.getLogger(DeclineOfferPage.class);

    private static final By OTHER_REASON_RADIO = By.xpath(
            "//input[@type='radio' and (@value='OTHER' or following-sibling::*[contains(.,'Other')"
                    + " or contains(.,'Annan')])] | //label[contains(.,'Other reason') or contains(.,'Annan orsak')]"
                    + "//input[@type='radio']");
    private static final By DECLINE_MESSAGE = By.xpath(
            "//textarea[contains(@placeholder,'Tell us more') or contains(@placeholder,'Berätta mer')]");
    private static final By SEND_MESSAGE_BUTTON = By.xpath(
            "//button[contains(.,'Send message') or contains(.,'Skicka meddelande')]");
    private static final By DECLINE_FORM = By.xpath(
            "//form[.//textarea] | //*[contains(.,'cost is too high') or contains(.,'för hög')]");

    public DeclineOfferPage(WebDriver driver) {
        super(driver);
    }

    public void openDeclineOfferFromCapturedSendOfferResponse() {
        String url = AcceptOfferContext.buildDeclineOfferUrl();
        LOG.info("Opening decline offer URL for orderId={}", AcceptOfferContext.getOrderIdOrNull());
        driver.get(QaServerCredentials.urlWithHttpBasicAuth(url));
        waitForLoad();
        waitForSpecifiedTime(3);
    }

    public void selectOtherReasonAndSubmitDecline(String message) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        wait.until(ExpectedConditions.or(
                ExpectedConditions.visibilityOfElementLocated(DECLINE_FORM),
                ExpectedConditions.urlContains("declineOffer")));
        if (!driver.findElements(OTHER_REASON_RADIO).isEmpty()) {
            scrollPageToViewElement(OTHER_REASON_RADIO);
            clickByJS(OTHER_REASON_RADIO);
        }
        if (!driver.findElements(DECLINE_MESSAGE).isEmpty()) {
            enterStringValueInInputField(DECLINE_MESSAGE, message);
        }
        scrollPageToViewElement(SEND_MESSAGE_BUTTON);
        wait.until(ExpectedConditions.elementToBeClickable(SEND_MESSAGE_BUTTON));
        clickByJS(SEND_MESSAGE_BUTTON);
        waitForLoad();
        waitForSpecifiedTime(3);
    }

    public void assertDeclineOfferSubmitted() {
        String src = driver.getPageSource();
        boolean completion = src.contains("LIQ_NOT_ACCEPTED")
                || src.contains("NewCompletionNeeded")
                || src.contains("not accepted")
                || src.contains("inte accepterad")
                || src.contains("Tack")
                || !src.contains("Send message");
        Assert.assertTrue(completion, "Expected decline-offer completion UI after submitting decline form");
    }
}
