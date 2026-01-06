package com.qa.bolags.pages;


import com.qa.bolags.utility.TestUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.net.URLEncoder;
import java.util.List;

import static com.qa.bolags.baseTest.BaseTest.finalUrl;

public class liquidationpage extends TestUtil {


    private By plans = By.xpath("//span[text()=' Plans ']");
    private By quicksettlement = By.xpath("//h4[contains(text(),'Offert snabbavveckling')]");
    private final String username = "dev-bolagspartner";
    private final String password = "Bolags@123";
    private By searchbar = By.xpath("(//input[@name='company'])[last()]");

    // Contact information fields on the last page
    private By fornamnField = By.xpath("(//input[@name='firstName' or @placeholder='Förnamn'])[last()]");
    private By efternamnField = By.xpath("(//input[@name='lastName' or @placeholder='Efternamn'])[last()]");
    private By epostField = By.xpath("(//input[@name='email' or @placeholder='E-post' or @type='email'])[last()]");
    private By agreementCheckbox = By.xpath("(//input[@type='checkbox'])[last()]");
    private By fortsattButton = By.xpath("//button[normalize-space()='FORTSÄTT']");
    private By save = By.xpath("//button[normalize-space()='SPARA']");
    //button[normalize-space()='SPARA']
    private By upload = By.xpath("//input[@type='file']");

    private By Street_address = By.xpath("//input[@placeholder='Gatuadress']");
    private By postal_code = By.xpath("//input[@placeholder='Postnummer']");
    private By city = By.xpath("//input[@placeholder='Ort']");
    private By telephone = By.xpath("//input[@placeholder='Telefon']");
    private By mobile = By.xpath("//input[@placeholder='Mobil']");
    private By agency = By.xpath("//input[@placeholder='Business']");
    private By is_request_received_page_displayed = By.xpath("//h2[contains(text(),'Förfrågan mottagen')]");




    public liquidationpage(WebDriver driver) {
        super(driver);

    }


    public String getHomePageTitle() {
        return driver.getTitle();
    }

    public void goToPlans() {
        waitForSpecifiedTime(2);

        click(plans);
        log.info("clicked on plan.");
    }


    public void logintoapplication() {

        try {
            String encUser = URLEncoder.encode(username, "UTF-8");
            String encPass = URLEncoder.encode(password, "UTF-8");
            String authUrl = finalUrl.replaceFirst("^(https?://)", "$1" + encUser + ":" + encPass + "@");
            driver.get(authUrl);
        } catch (Exception e) {
            driver.get(finalUrl);
        }
    }

    public boolean is_disply_offerpage() {
        waitForSpecifiedTime(2);
        log.info("Waiting for Offer page to displayed.");
        return super.isElementDisplayed(quicksettlement);
    }

    public void searchCompany() {
        waitForElementToBeClickable(searchbar);
        clickByJS(searchbar);
        enterStringValueInInputField(searchbar, "cde");



        List<WebElement> companies = driver.findElements(By.cssSelector(".company-dropdown-item"));
        if (!companies.isEmpty()) {
            int randomIndex = new java.util.Random().nextInt(companies.size());
            companies.get(randomIndex).click();
        }
    }

    public void enterFornamn(String fornamn) {
//        waitForSpecifiedTime(5);
        log.info("Entering first name '{}'.", fornamn);
        enterStringValueInInputField(fornamnField, fornamn);

    }

    public void enterEfternamn(String efternamn) {
       // waitForSpecifiedTime(5);
        log.info("Entering last name '{}'.", efternamn);
        enterStringValueInInputField(efternamnField, efternamn);
    }

    public void enterEpost(String epost) {
        log.info("Entering email '{}'.", epost);
        enterStringValueInInputField(epostField, epost);
       // waitForSpecifiedTime(30);
    }

    public void agreeToTermsAndContinue() {
        click(agreementCheckbox);

    }

    public void clickonGoOn() {
        click(fortsattButton);
    }

    public void uploadDocument(String filePath) {
        waitForSpecifiedTime(3);
        WebElement uploadElement = driver.findElement(upload);
        uploadElement.sendKeys(filePath);
        log.info("File uploaded: " + filePath);
    }

    public void enterAddressDetails(String streetAddress, String postalCode, String cityName, String telephoneNumber) {
        enterStringValueInInputField(this.Street_address, streetAddress);
        enterStringValueInInputField(this.postal_code, postalCode);
        enterStringValueInInputField(this.city, cityName);
        enterStringValueInInputField(this.telephone, telephoneNumber);
        //   enterStringValueInInputField(this.mobile, mobileNumber);
        // enterStringValueInInputField(this.agency, agencyDetails);
    }

    public void userClickOnSave() {
        // waitForSpecifiedTime(2);
        waitForElementToBeClickable(save);
          click(save);
    }

    public boolean is_request_received_page_displayed() {
        By requestReceivedMessage = By.xpath("//h2[contains(text(),'Förfrågan mottagen')]");
        waitForSpecifiedTime(3);
        super.isElementDisplayed(requestReceivedMessage);
        return false;
    }


}