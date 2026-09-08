package com.qa.bolags.pages;

import com.qa.bolags.constants.QaServerCredentials;
import com.qa.bolags.utility.TestUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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
        waitForSpecifiedTime(2);
        ensureShiroUserListReady();
    }

    public void openUserList() {
        By userList = By.xpath("//a[contains(.,'User List') or contains(.,'User list') or contains(.,'Användarlista')]");
        if (!driver.findElements(userList).isEmpty()) {
            waitForElementToBeClickable(userList);
            clickByJS(userList);
            waitForLoad();
        }
        ensureShiroUserListReady();
    }

    public void clickNewUserOnUserListDashboard() {
        ensureShiroUserListReady();
        clickFirstDisplayedAction(
                "new Shiro user",
                "New User", "Ny användare", "+ Ny användare",
                "Nuevo usuario", "Nueva usuaria", "Añadir usuario");
        waitForFormToOpen("Shiro user", "E-mail", "E-post", "Correo electrónico");
    }

    private void ensureShiroUserListReady() {
        By newUserButton = By.xpath(
                "//button[contains(normalize-space(.),'Ny användare')"
                        + " or contains(normalize-space(.),'New User')]");
        if (firstDisplayed(driver.findElements(newUserButton)) != null) {
            return;
        }

        driver.get(QaServerCredentials.urlWithHttpBasicAuth(SHIRO_USER_DASHBOARD_URL));
        waitForLoad();
        try {
            new WebDriverWait(driver, Duration.ofSeconds(30))
                    .until(d -> firstDisplayed(d.findElements(newUserButton)) != null);
        } catch (TimeoutException e) {
            Assert.fail("Shiro user list did not become ready at " + SHIRO_USER_DASHBOARD_URL
                    + ". Current URL: " + driver.getCurrentUrl()
                    + ". Visible controls: " + describeVisibleControls());
        }
    }

    public void submitEmptyShiroUserForm() {
        clickFirstDisplayedAction(
                "empty Shiro user form submit",
                "Skapa", "Create", "Save", "Guardar", "Crear");
    }

    public void assertRequiredShiroUserValidationErrors() {
        String[] expectedErrors = {
                "Validation failed. Please correct the following errors:",
                "Email address is required",
                "First name is required",
                "Last name is required",
                "Zipcode must be 5 digits",
                "Invalid phone number format"
        };
        try {
            new WebDriverWait(driver, Duration.ofSeconds(20)).until(d -> {
                String pageText = d.findElement(By.tagName("body")).getText();
                return Arrays.stream(expectedErrors).allMatch(pageText::contains);
            });
        } catch (TimeoutException e) {
            String pageText = driver.findElement(By.tagName("body")).getText();
            List<String> missingErrors = new ArrayList<>();
            for (String expectedError : expectedErrors) {
                if (!pageText.contains(expectedError)) {
                    missingErrors.add(expectedError);
                }
            }
            Assert.fail("Missing Shiro validation errors: " + missingErrors);
        }
    }

    public void completeShiroUserFormWithTestData() {
        lastCreatedShiroEmail = "shiro.auto+" + System.currentTimeMillis() + "@bolagspartner.test";
        String pwd = "TempPass!" + (System.currentTimeMillis() % 10000);
        typeRequiredNamedField("email", lastCreatedShiroEmail);
        typeRequiredNamedField("password", pwd);
        typeRequiredNamedField("confirmPassword", pwd);
        typeRequiredNamedField("contactCompany", "Automation Shiro Co");
        typeRequiredNamedField("contactAddressText", "Testgatan 1");
        typeRequiredNamedField("postalCode", "11122");
        typeRequiredNamedField("location", "Stockholm");
        typeRequiredNamedField("telephoneNumber", "0701234567");
        typeRequiredNamedField("fax", "0701234568");
        typeRequiredNamedField("car", "0709876543");
        typeRequiredNamedField("firstName", "Shiro");
        typeRequiredNamedField("lastName", "Automation");
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

    public void assertNewShiroUserVisibleInUserList() {
        assertValueVisibleInTable(
                lastCreatedShiroEmail,
                "Expected the created Shiro user to remain visible in the user list");
    }

    public void openResellerUserDashboardFromSidebar() {
        driver.get(QaServerCredentials.urlWithHttpBasicAuth(RESELLER_DASHBOARD_URL));
        waitForLoad();
        clickIfPresent(By.xpath("//aside//a[contains(.,'Reseller') or contains(.,'ÅF')] | //nav//a[contains(.,'Reseller')]"));
        waitForSpecifiedTime(2);
        ensureResellerListReady();
    }

    public void clickNewDealer() {
        ensureResellerListReady();
        clickFirstDisplayedAction(
                "new reseller",
                "New Dealer", "Ny återförsäljare",
                "Nuevo distribuidor", "Nuevo revendedor", "Nuevo comerciante");
        waitForFormToOpen("reseller", "Organisationsnummer", "Organization number", "Número de organización");
    }

    public void submitEmptyResellerForm() {
        scrollResellerFormToBottom();
        clickBottomFormSubmitEvenIfDisabled(
                "empty reseller form submit",
                "Skapa", "Create", "Save", "Guardar", "Crear");
    }

    public void assertEmptyResellerFormError() {
        String expectedError = "Something went wrong. Please check your input.";
        try {
            new WebDriverWait(driver, Duration.ofSeconds(20)).until(d ->
                    d.findElement(By.tagName("body")).getText().contains(expectedError));
        } catch (TimeoutException e) {
            Assert.fail("Missing reseller empty-form error: " + expectedError
                    + ". Current URL: " + driver.getCurrentUrl());
        }
    }

    private void ensureResellerListReady() {
        By newResellerButton = By.xpath(
                "//button[contains(normalize-space(.),'Ny återförsäljare')"
                        + " or contains(normalize-space(.),'New Dealer')]");
        if (firstDisplayed(driver.findElements(newResellerButton)) != null) {
            return;
        }

        driver.get(QaServerCredentials.urlWithHttpBasicAuth(RESELLER_DASHBOARD_URL));
        waitForLoad();
        try {
            new WebDriverWait(driver, Duration.ofSeconds(30))
                    .until(d -> firstDisplayed(d.findElements(newResellerButton)) != null);
        } catch (TimeoutException e) {
            Assert.fail("Reseller list did not become ready at " + RESELLER_DASHBOARD_URL
                    + ". Current URL: " + driver.getCurrentUrl()
                    + ". Visible controls: " + describeVisibleControls());
        }
    }

    public void completeResellerFormWithTestData() {
        lastResellerOrgNumber = generateValidSwedishOrganisationNumber();
        lastResellerCompanyName = "Auto Reseller " + System.currentTimeMillis();
        typeRequiredNamedField("bankAccount", "1234-5678");
        typeRequiredNamedField("organizationNumber", lastResellerOrgNumber);
        selectNamedOption("bankAccountType", "Bankgiro");
        typeRequiredNamedField("name", lastResellerCompanyName);
        typeRequiredNamedField(
                "email",
                "reseller.auto+" + System.currentTimeMillis() + "@bolagspartner.test",
                "Contact email", "Kontakt e-post", "E-post", "Correo electrónico");
        typeRequiredNamedField("kickBackPaymentPeriod", "030");
        typeRequiredNamedField("address", "Resellergatan 2");
        clearNamedField("addressCo");
        typeRequiredNamedField("postalCode", "22233");
        typeRequiredNamedField("city", "Sweden");
        selectNamedOption("country", "Guatemala");
        typeRequiredNamedField("note", "Automation reseller");
        typeRequiredNamedField("liquidationPrice", "25000");
        scrollResellerFormToBottom();
        submitPrimaryForm();
    }

    private String generateValidSwedishOrganisationNumber() {
        String firstNineDigits = "5567"
                + String.format("%05d", ThreadLocalRandom.current().nextInt(100000));
        int sum = 0;
        for (int index = 0; index < firstNineDigits.length(); index++) {
            int digit = Character.digit(firstNineDigits.charAt(index), 10);
            int product = digit * (index % 2 == 0 ? 2 : 1);
            sum += product > 9 ? product - 9 : product;
        }
        int checkDigit = (10 - (sum % 10)) % 10;
        return firstNineDigits + checkDigit;
    }

    public void assertNewResellerOnLastPageOfList() {
        goToLastResellerListPage();
        waitForSpecifiedTime(2);
        List<WebElement> rows = driver.findElements(By.xpath("//table//tbody//tr[td]"));
        boolean found = rows.stream()
                .map(WebElement::getText)
                .anyMatch(text -> text.contains(lastResellerOrgNumber)
                        || text.contains(lastResellerCompanyName));
        Assert.assertTrue(found,
                "Expected reseller on the last page. Generated organisation number: "
                        + lastResellerOrgNumber + ", company: " + lastResellerCompanyName);
    }

    public void assertCreatedOrganisationNumberVisibleInResellerList() {
        assertValueVisibleInTable(
                lastResellerOrgNumber,
                "Expected the created organisationsnummer in the reseller list");
    }

    public void assertNewResellerVisibleInResellerList() {
        assertValueVisibleInTable(
                lastResellerCompanyName,
                "Expected the created reseller in the reseller company list");
    }

    private void assertValueVisibleInTable(String expectedValue, String failureMessage) {
        Assert.assertNotNull(expectedValue, failureMessage + ": no generated value was stored");
        waitForSpecifiedTime(1);
        List<WebElement> rows = driver.findElements(By.xpath("//table//tbody//tr[td]"));
        boolean visible = rows.stream().anyMatch(row -> row.getText().contains(expectedValue));
        Assert.assertTrue(visible, failureMessage + ". Expected value: " + expectedValue);
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

        By numericPagination = By.xpath(
                "//*[self::a or self::button][not(ancestor::tbody)"
                        + " and string-length(normalize-space(.)) > 0"
                        + " and not(translate(normalize-space(.),'0123456789',''))]");
        WebElement highestPage = null;
        int highestPageNumber = -1;
        for (WebElement candidate : driver.findElements(numericPagination)) {
            try {
                if (!candidate.isDisplayed()) {
                    continue;
                }
                int pageNumber = Integer.parseInt(candidate.getText().trim());
                if (pageNumber > highestPageNumber) {
                    highestPageNumber = pageNumber;
                    highestPage = candidate;
                }
            } catch (Exception ignored) {
                // Ignore detached controls and non-page numeric content.
            }
        }
        if (highestPage != null) {
            clickElementWithJs(highestPage);
            waitForSpecifiedTime(2);
            return;
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
        clickFirstDisplayedAction(
                "save form",
                "Save", "Spara", "Skapa", "Create", "Guardar", "Crear");
        waitForSpecifiedTime(2);
    }

    private void clickFirstDisplayedAction(String actionName, String... labels) {
        clickActionByLabels(actionName, false, labels);
    }

    private void scrollResellerFormToBottom() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript(
                "window.scrollTo(0, document.body.scrollHeight);"
                        + "var nodes = document.querySelectorAll('main, form, [role=main], .overflow-auto, .overflow-y-auto, .overflow-scroll');"
                        + "for (var i = 0; i < nodes.length; i++) {"
                        + "  var n = nodes[i];"
                        + "  if (n.scrollHeight > n.clientHeight) { n.scrollTop = n.scrollHeight; }"
                        + "}");
        waitForSpecifiedTime(1);
    }

    private void clickBottomFormSubmitEvenIfDisabled(String actionName, String... labels) {
        List<String> predicates = new ArrayList<>();
        for (String label : labels) {
            predicates.add("contains(normalize-space(.),'" + xpathLiteral(label) + "')");
        }
        By action = By.xpath(
                "//*[self::button or self::a or @role='button']["
                        + String.join(" or ", predicates) + "]");
        try {
            new WebDriverWait(driver, Duration.ofSeconds(25)).until(d -> {
                WebElement submit = lastDisplayedEvenIfDisabled(d.findElements(action));
                if (submit == null) {
                    return false;
                }
                try {
                    ((JavascriptExecutor) d).executeScript(
                            "arguments[0].scrollIntoView({block:'end', inline:'nearest'});", submit);
                    try {
                        submit.click();
                    } catch (Exception ignored) {
                        clickElementWithJs(submit);
                    }
                    return true;
                } catch (Exception clickError) {
                    log.debug("Re-finding {} action after click failure: {}",
                            actionName, clickError.getMessage());
                    return false;
                }
            });
            waitForLoad();
        } catch (TimeoutException e) {
            Assert.fail("Could not find a visible " + actionName + " action using labels "
                    + Arrays.toString(labels) + ". Current URL: " + driver.getCurrentUrl()
                    + ". Invalid fields: " + describeInvalidFields()
                    + ". Empty named fields: " + describeEmptyNamedFields()
                    + ". Visible controls: " + describeVisibleControls());
        }
    }

    private void clickActionByLabels(String actionName, boolean includeDisabled, String... labels) {
        List<String> predicates = new ArrayList<>();
        for (String label : labels) {
            predicates.add("contains(normalize-space(.),'" + xpathLiteral(label) + "')");
        }
        By action = By.xpath(
                "//*[self::button or self::a or @role='button']["
                        + String.join(" or ", predicates) + "]");
        try {
            new WebDriverWait(driver, Duration.ofSeconds(25)).until(d -> {
                WebElement element = includeDisabled
                        ? firstDisplayedEvenIfDisabled(d.findElements(action))
                        : firstDisplayed(d.findElements(action));
                if (element == null) {
                    return false;
                }
                try {
                    ((JavascriptExecutor) d).executeScript(
                            "arguments[0].scrollIntoView({block:'end', inline:'nearest'});", element);
                    if (includeDisabled) {
                        clickElementWithJs(element);
                    } else {
                        element.click();
                    }
                    return true;
                } catch (Exception clickError) {
                    log.debug("Re-finding {} action after click failure: {}",
                            actionName, clickError.getMessage());
                    return false;
                }
            });
            waitForLoad();
        } catch (TimeoutException e) {
            Assert.fail("Could not find a visible " + actionName + " action using labels "
                    + Arrays.toString(labels) + ". Current URL: " + driver.getCurrentUrl()
                    + ". Invalid fields: " + describeInvalidFields()
                    + ". Empty named fields: " + describeEmptyNamedFields()
                    + ". Visible controls: " + describeVisibleControls());
        }
    }

    private void waitForFormToOpen(String formName, String... identifyingLabels) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(25))
                    .until(d -> findDisplayedField(identifyingLabels) != null);
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(d -> !d.findElements(By.xpath("//input | //textarea | //select")).isEmpty());
            waitForSpecifiedTime(1);
        } catch (TimeoutException e) {
            Assert.fail("The " + formName + " form did not open after clicking its create button. "
                    + "Current URL: " + driver.getCurrentUrl()
                    + ". Visible controls: " + describeVisibleControls());
        }
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

    private void typeRequiredNamedField(String name, String value, String... fallbackAliases) {
        WebElement field = firstDisplayed(driver.findElements(By.name(name)));
        if (field == null && fallbackAliases.length > 0) {
            field = findDisplayedField(fallbackAliases);
        }
        Assert.assertNotNull(
                field,
                "Required field with name '" + name + "' was not found. Current URL: "
                        + driver.getCurrentUrl() + ". Visible controls: " + describeVisibleControls());
        field.clear();
        field.sendKeys(value);
    }

    private void clearNamedField(String name) {
        WebElement field = firstDisplayed(driver.findElements(By.name(name)));
        if (field != null) {
            field.clear();
        }
    }

    private void selectNamedOption(String name, String desiredOption) {
        By controlBy = By.name(name);
        WebElement control = new WebDriverWait(driver, Duration.ofSeconds(20))
                .until(d -> firstDisplayed(d.findElements(controlBy)));
        if ("select".equalsIgnoreCase(control.getTagName())) {
            Select select = new Select(control);
            for (WebElement option : select.getOptions()) {
                if (option.getText().toLowerCase().contains(desiredOption.toLowerCase())) {
                    select.selectByVisibleText(option.getText());
                    return;
                }
            }
            Assert.fail("Option '" + desiredOption + "' was not found in select '" + name + "'");
        }

        new WebDriverWait(driver, Duration.ofSeconds(20)).until(d -> {
            WebElement currentControl = firstDisplayed(d.findElements(controlBy));
            if (currentControl == null) {
                return false;
            }
            try {
                currentControl.click();
                return true;
            } catch (Exception ignored) {
                return false;
            }
        });

        WebElement searchInput = firstDisplayed(driver.findElements(By.xpath(
                "//*[@role='listbox']//input | //input[@role='combobox']")));
        if (searchInput != null) {
            searchInput.clear();
            searchInput.sendKeys(desiredOption.substring(0, Math.min(5, desiredOption.length())));
        }

        By desiredOptionBy = By.xpath(
                "//*[normalize-space(.)='" + xpathLiteral(desiredOption) + "'"
                        + " and not(.//*[normalize-space(.)='" + xpathLiteral(desiredOption) + "'])]");
        try {
            WebElement option = new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(d -> firstDisplayed(d.findElements(desiredOptionBy)));
            try {
                option.click();
            } catch (Exception clickError) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", option);
            }
        } catch (TimeoutException e) {
            Assert.fail("Option '" + desiredOption + "' was not visible for control '" + name
                    + "'. Visible controls: " + describeVisibleControls());
        }
    }

    private WebElement findDisplayedField(String... aliases) {
        for (String alias : aliases) {
            String safe = xpathLiteral(alias);
            String semanticToken = alias.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
            By field = By.xpath(
                    "//*[self::input or self::textarea][contains(@placeholder,'" + safe + "')]"
                            + " | //label[contains(normalize-space(.),'" + safe + "')]"
                            + "//*[self::input or self::textarea]"
                            + " | //label[contains(normalize-space(.),'" + safe + "')]"
                            + "/following::*[self::input or self::textarea][1]"
                            + (semanticToken.isEmpty() ? "" :
                            " | //*[self::input or self::textarea][contains("
                                    + "translate(@name,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'"
                                    + semanticToken + "') or contains("
                                    + "translate(@id,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'"
                                    + semanticToken + "')]"));
            WebElement displayed = firstDisplayed(driver.findElements(field));
            if (displayed != null) {
                return displayed;
            }
        }
        return null;
    }

    private WebElement firstDisplayed(List<WebElement> elements) {
        return firstMatchingDisplayed(elements, false);
    }

    private WebElement firstDisplayedEvenIfDisabled(List<WebElement> elements) {
        return firstMatchingDisplayed(elements, true);
    }

    private WebElement lastDisplayedEvenIfDisabled(List<WebElement> elements) {
        WebElement last = null;
        for (WebElement element : elements) {
            try {
                if (element.isDisplayed()) {
                    last = element;
                }
            } catch (Exception ignored) {
                // Try the next matching element after a stale or detached node.
            }
        }
        return last;
    }

    private WebElement firstMatchingDisplayed(List<WebElement> elements, boolean includeDisabled) {
        for (WebElement element : elements) {
            try {
                if (element.isDisplayed() && (includeDisabled || element.isEnabled())) {
                    return element;
                }
            } catch (Exception ignored) {
                // Try the next matching element after a stale or detached node.
            }
        }
        return null;
    }

    private String xpathLiteral(String value) {
        return value.replace("'", "");
    }

    private String describeVisibleControls() {
        List<String> descriptions = new ArrayList<>();
        for (WebElement element : driver.findElements(
                By.xpath("//button | //a | //input | //textarea | //select | //*[@role='button' or @role='combobox']"))) {
            try {
                if (!element.isDisplayed()) {
                    continue;
                }
                String tag = element.getTagName();
                String text = element.getText().trim();
                String name = element.getAttribute("name");
                String id = element.getAttribute("id");
                String placeholder = element.getAttribute("placeholder");
                String href = element.getAttribute("href");
                descriptions.add(tag
                        + "[text=" + text
                        + ", name=" + name
                        + ", id=" + id
                        + ", placeholder=" + placeholder
                        + ", href=" + href + "]");
                if (descriptions.size() == 40) {
                    break;
                }
            } catch (Exception ignored) {
                // Ignore controls detached while the SPA is rendering.
            }
        }
        return descriptions.toString();
    }

    private String describeInvalidFields() {
        List<String> invalidFields = new ArrayList<>();
        JavascriptExecutor js = (JavascriptExecutor) driver;
        for (WebElement field : driver.findElements(By.xpath("//input | //textarea | //select"))) {
            try {
                if (!field.isDisplayed()) {
                    continue;
                }
                Boolean valid = (Boolean) js.executeScript("return arguments[0].checkValidity();", field);
                if (Boolean.TRUE.equals(valid)) {
                    continue;
                }
                String message = String.valueOf(
                        js.executeScript("return arguments[0].validationMessage;", field));
                invalidFields.add(field.getAttribute("name") + ": " + message);
            } catch (Exception ignored) {
                // Ignore controls detached while validation renders.
            }
        }
        return invalidFields.toString();
    }

    private String describeEmptyNamedFields() {
        List<String> emptyFields = new ArrayList<>();
        for (WebElement field : driver.findElements(
                By.xpath("//input[@name] | //textarea[@name] | //select[@name]"))) {
            try {
                if (field.isDisplayed() && field.getAttribute("value").trim().isEmpty()) {
                    emptyFields.add(field.getAttribute("name"));
                }
            } catch (Exception ignored) {
                // Ignore controls detached while React rerenders.
            }
        }
        return emptyFields.toString();
    }

}
