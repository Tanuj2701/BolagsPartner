package com.qa.bolags.pages;

import com.qa.bolags.constants.QaServerCredentials;
import com.qa.bolags.utility.TestUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.testng.Assert;

import java.util.List;

/**
 * Generic Order flows: Shiro users and Reseller companies on QA.
 * Locators are best-effort; align with live markup when automating against QA.
 */
public class GenericOrderUsersPage extends TestUtil {

    public static final String QA_LOGIN_URL = "https://qa.bolagspartner.se/app/auth/login";
    public static final String SHIRO_USER_DASHBOARD_URL = "https://qa.bolagspartner.se/app/genericOrder/shiroUser/show";
    public static final String RESELLER_DASHBOARD_URL = "https://qa.bolagspartner.se/app/genericOrder/resellerCompany/show";

    private String lastCreatedShiroEmail;
    private String lastResellerOrgNumber;
    private String lastResellerCompanyName;

    public GenericOrderUsersPage(WebDriver driver) {
        super(driver);
    }

    public String getLastCreatedShiroEmail() {
        return lastCreatedShiroEmail;
    }

    public void loginAsSuperAdmin() {
        driver.get(QaServerCredentials.urlWithHttpBasicAuth(QA_LOGIN_URL));
        waitForLoad();
        String email = QaServerCredentials.genericOrderSuperAdminEmail();
        String pwd = QaServerCredentials.genericOrderSuperAdminPassword();
        By emailField = By.xpath("//input[@type='email'] | //input[contains(@placeholder,'E-post') or contains(@placeholder,'mail')]");
        By passwordField = By.xpath("//input[@type='password']");
        By submit = By.xpath("//button[normalize-space()='Logga in' or contains(.,'Log in')]");
        waitForElementToBeClickable(emailField);
        enterStringValueInInputField(emailField, email);
        waitForElementToBeClickable(passwordField);
        enterStringValueInInputField(passwordField, pwd);
        waitForElementToBeClickable(submit);
        clickByJS(submit);
        waitForLoad();
        waitForSpecifiedTime(2);
    }

    public void openShiroUserDashboardFromSidebar() {
        driver.get(QaServerCredentials.urlWithHttpBasicAuth(SHIRO_USER_DASHBOARD_URL));
        waitForLoad();
        clickIfPresent(By.xpath(
                "//aside//a[contains(.,'Users') and contains(.,'Reseller')]"
                        + " | //a[contains(.,'Users & Resellers') or contains(.,'Users and Resellers')]"));
        waitForSpecifiedTime(1);
    }

    public void openUserList() {
        By userList = By.xpath("//a[contains(.,'User List') or contains(.,'User list') or contains(.,'Användarlista')]");
        if (!driver.findElements(userList).isEmpty()) {
            waitForElementToBeClickable(userList);
            clickByJS(userList);
            waitForLoad();
        }
    }

    public void clickNewUserOnUserListDashboard() {
        By newUser = By.xpath("//button[contains(.,'New User') or contains(.,'+ New User') or contains(.,'Ny användare')]"
                + " | //a[contains(.,'New User') or contains(.,'+ New User')]");
        waitForElementToBeClickable(newUser);
        clickByJS(newUser);
        waitForLoad();
    }

    public void completeShiroUserFormWithTestData() {
        lastCreatedShiroEmail = "shiro.auto+" + System.currentTimeMillis() + "@bolagspartner.test";
        String pwd = "TempPass!" + (System.currentTimeMillis() % 10000);
        typeByLabelOrPlaceholder("E-mail", lastCreatedShiroEmail);
        typeByLabelOrPlaceholder("Email", lastCreatedShiroEmail);
        typeByLabelOrPlaceholder("Password", pwd);
        typeByLabelOrPlaceholder("Confirm password", pwd);
        typeByLabelOrPlaceholder("Contact company", "Automation Shiro Co");
        typeByLabelOrPlaceholder("Street address", "Testgatan 1");
        typeByLabelOrPlaceholder("ZIP code", "11122");
        typeByLabelOrPlaceholder("City", "Stockholm");
        typeByLabelOrPlaceholder("Phone number", "0701234567");
        typeByLabelOrPlaceholder("Fax", "0701234568");
        typeByLabelOrPlaceholder("Mobile", "0709876543");
        typeByLabelOrPlaceholder("First name", "Shiro");
        typeByLabelOrPlaceholder("Surname", "Automation");
        selectFirstOptionForLabelContaining("Roller");
        selectFirstOptionForLabelContaining("Reseller");
        submitPrimaryForm();
    }

    public void assertNewShiroUserListedAtTop() {
        waitForSpecifiedTime(2);
        By firstRow = By.xpath("(//table//tbody//tr[td])[1]");
        waitForElementToBeVisible(firstRow);
        String text = driver.findElement(firstRow).getText();
        Assert.assertTrue(
                text.contains(lastCreatedShiroEmail) || text.contains(lastCreatedShiroEmail.split("@")[0]),
                "Expected new Shiro user in first row. Row text: " + text);
    }

    public void openResellerUserDashboardFromSidebar() {
        driver.get(QaServerCredentials.urlWithHttpBasicAuth(RESELLER_DASHBOARD_URL));
        waitForLoad();
        clickIfPresent(By.xpath("//aside//a[contains(.,'Reseller') or contains(.,'ÅF')] | //nav//a[contains(.,'Reseller')]"));
        waitForSpecifiedTime(1);
    }

    public void clickNewDealer() {
        By newDealer = By.xpath("//button[contains(.,'New Dealer') or contains(.,'Ny återförsäljare')]"
                + " | //a[contains(.,'New Dealer')]");
        waitForElementToBeClickable(newDealer);
        clickByJS(newDealer);
        waitForLoad();
    }

    public void completeResellerFormWithTestData() {
        lastResellerOrgNumber = "5566" + String.format("%08d", System.currentTimeMillis() % 100000000L);
        lastResellerCompanyName = "Auto Reseller " + System.currentTimeMillis();
        typeByLabelOrPlaceholder("Organization number", lastResellerOrgNumber);
        typeByLabelOrPlaceholder("Name", lastResellerCompanyName);
        typeByLabelOrPlaceholder("Contact email", "reseller.auto+" + System.currentTimeMillis() + "@bolagspartner.test");
        typeByLabelOrPlaceholder("Kickback payment period", "30");
        typeByLabelOrPlaceholder("Address", "Resellergatan 2");
        typeByLabelOrPlaceholder("Address c/o", "Box 1");
        typeByLabelOrPlaceholder("ZIP code", "22233");
        typeByLabelOrPlaceholder("City", "Göteborg");
        typeByLabelOrPlaceholder("Information about this", "Automation reseller");
        typeByLabelOrPlaceholder("Price settlement", "Monthly");
        selectFirstOptionForLabelContaining("Type of bank account");
        selectFirstOptionForLabelContaining("Country");
        submitPrimaryForm();
    }

    public void assertNewResellerOnLastPageOfList() {
        goToLastResellerListPage();
        waitForSpecifiedTime(2);
        By lastRow = By.xpath("(//table//tbody//tr[td])[last()]");
        waitForElementToBeVisible(lastRow);
        String text = driver.findElement(lastRow).getText();
        Assert.assertTrue(
                text.contains(lastResellerOrgNumber) || text.contains(lastResellerCompanyName),
                "Expected reseller on last page. Row: " + text);
    }

    private void goToLastResellerListPage() {
        By lastLink = By.xpath("//a[contains(.,'Last') or contains(@aria-label,'Last') or contains(.,'»')]");
        List<WebElement> lastLinks = driver.findElements(lastLink);
        if (!lastLinks.isEmpty()) {
            for (WebElement el : lastLinks) {
                if (el.isDisplayed()) {
                    clickElementWithJs(el);
                    waitForLoad();
                    waitForSpecifiedTime(1);
                    return;
                }
            }
        }
        By nextBtn = By.xpath("//button[contains(.,'Next') or contains(@aria-label,'next')]"
                + " | //a[contains(.,'Next') or contains(@aria-label,'Next')]");
        for (int i = 0; i < 40; i++) {
            List<WebElement> next = driver.findElements(nextBtn);
            WebElement active = null;
            for (WebElement n : next) {
                if (n.isDisplayed() && n.isEnabled()) {
                    active = n;
                    break;
                }
            }
            if (active == null) {
                break;
            }
            clickElementWithJs(active);
            waitForSpecifiedTime(1);
        }
    }

    private void clickElementWithJs(WebElement el) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].click();", el);
        waitForLoad();
    }

    private void submitPrimaryForm() {
        By save = By.xpath("//button[contains(.,'Save') or contains(.,'Spara') or contains(.,'Skapa') or contains(.,'Create')]");
        waitForElementToBeClickable(save);
        clickByJS(save);
        waitForLoad();
        waitForSpecifiedTime(2);
    }

    private void clickIfPresent(By by) {
        List<WebElement> els = driver.findElements(by);
        for (WebElement e : els) {
            try {
                if (e.isDisplayed()) {
                    clickElementWithJs(e);
                    return;
                }
            } catch (Exception ignored) {
                // continue
            }
        }
    }

    private void typeByLabelOrPlaceholder(String label, String value) {
        String safe = label.replace("'", "\'");
        By by = By.xpath(
                "//input[contains(@placeholder,'" + safe + "')]"
                        + " | //textarea[contains(@placeholder,'" + safe + "')]"
                        + " | //label[contains(.,'" + safe + "')]/following::input[1]"
                        + " | //label[contains(.,'" + safe + "')]/following::textarea[1]");
        for (WebElement el : driver.findElements(by)) {
            try {
                if (el.isDisplayed()) {
                    el.clear();
                    el.sendKeys(value);
                    return;
                }
            } catch (Exception ignored) {
                // try next match
            }
        }
        log.warn("Field not found for label/placeholder containing: {}", label);
    }

    private void selectFirstOptionForLabelContaining(String labelPart) {
        String safe = labelPart.replace("'", "\'");
        By selectBy = By.xpath(
                "//label[contains(.,'" + safe + "')]/following::select[1]"
                        + " | //select[preceding::label[contains(.,'" + safe + "')][1]]");
        for (WebElement el : driver.findElements(selectBy)) {
            try {
                if (el.isDisplayed() && "select".equalsIgnoreCase(el.getTagName())) {
                    Select s = new Select(el);
                    if (s.getOptions().size() > 1) {
                        s.selectByIndex(1);
                        return;
                    }
                }
            } catch (Exception ignored) {
                // continue
            }
        }
        for (WebElement el : driver.findElements(By.tagName("select"))) {
            try {
                if (el.isDisplayed()) {
                    Select s = new Select(el);
                    if (s.getOptions().size() > 1) {
                        s.selectByIndex(1);
                        return;
                    }
                }
            } catch (Exception ignored) {
                // continue
            }
        }
    }
}
