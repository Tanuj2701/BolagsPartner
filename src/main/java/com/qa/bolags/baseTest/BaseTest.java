package com.qa.bolags.baseTest;

import com.qa.bolags.constants.Constants;

import io.github.bonigarcia.wdm.WebDriverManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.poi.ss.usermodel.Sheet;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class BaseTest {

    public static WebDriver driver;
    public static Logger log = LoggerFactory.getLogger(BaseTest.class);
    public static Sheet configurationData;
    public static String browser,url,baseUrl, finalUrl,makerchecker_Url,tessdataPath;

    public void initializeDriver() {
        log.info("Initializing driver...");
        //configurationData = ExcelUtility.readExcelFile(Constants.CONFIG, 0);
        browser = "Chrome";
        //url = configurationData.getRow(1).getCell(0).getStringCellValue().trim();
        //baseUrl = System.getProperty("baseUrl");
        finalUrl = "https://qa.bolagspartner.se/app/liquidation/company";
        //tessdataPath = System.getenv("TESSDATA_PATH");
        log.info("Selected browser is --> " + browser);
        log.info("Application URL is --> " + finalUrl);


        this.launchBrowser(browser);
    }


    private WebDriver launchBrowser(String browserName) {
        log.info("Launching browser...");
       // if (configurationData.getRow(1).getCell(4).getStringCellValue().equalsIgnoreCase("Yes")) {
            switch (browserName) {
                case "Chrome":
                    log.info("Chrome browser is selected to execute test cases");
                    WebDriverManager.chromedriver().setup();
                    log.info("WebDriverManager is set to Chrome");
                    ChromeOptions options = new ChromeOptions();
                    Map<String, Object> prefs = new HashMap<>();
                    prefs.put("credentials_enable_service", false);
                    prefs.put("profile.password_manager_enabled", false);
                    if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                        prefs.put("download.default_directory", Constants.WINDOWS_DOWNLOADED_FILE_PATH);
                    } else {
                        prefs.put("download.default_directory", Constants.LINUX_DOWNLOADED_FILE_PATH);
                    }
                    prefs.put("safebrowsing.enabled", true);
                    prefs.put("download.prompt_for_download", false);
                    prefs.put("profile.content_settings.exceptions.automatic_downloads.*.setting", 1);
                    options.setExperimentalOption("prefs", prefs);
                    options.addArguments("--unsafely-treat-insecure-origin-as-secure=" + finalUrl);
                    options.setAcceptInsecureCerts(true);
                    options.addArguments("disable-popup-blocking");
                    log.info("Chrome desired Property is set.");
                    driver = new ChromeDriver(options);
                    log.info("Chrome driver object is created");
                    driver.manage().deleteAllCookies();
                    log.info("Browser cache cleared.");
                    break;

                case "Firefox":
                    log.info("Firefox browser is selected to execute test cases");
                    WebDriverManager.firefoxdriver().setup();
                    log.info("WebDriverManager is set to Firefox");
                    driver = new FirefoxDriver();
                    log.info("Firefox driver object is created");
                    break;

                case "Edge":
                    log.info("Edge browser is selected to execute test cases.");
                    WebDriverManager.edgedriver().setup();
                    log.info("WebDriverManager is set to Edge");
                    driver = new EdgeDriver();
                    log.info("Edge driver object is created");
                    break;

                case "InternetExplorer":
                    log.info("IE browser is selected to execute test cases.");
                    WebDriverManager.iedriver().setup();
                    log.info("WebDriverManager is set to IE");
                    driver = new InternetExplorerDriver();
                    log.info("Internet Explorer driver object is created");
                    break;

                default:
                    String errorMessage = "Browser name is not specified correctly, please check and try again!";
                    log.error(errorMessage);
                    throw new IllegalArgumentException(errorMessage);
            }


        openApplication();
        return driver;
    }

    private void openApplication() {
        log.info("Opening application...");

        log.info("Maximizing window...");
        driver.manage().window().maximize();

        log.info("Deleting all cookies...");
        driver.manage().deleteAllCookies();

        log.info("Setting page load timeout...");
        driver.manage().timeouts().pageLoadTimeout(Constants.PAGE_LOAD_WAIT_TIMEOUT/1000, TimeUnit.SECONDS);

        log.info("Setting implicit wait timeout...");
        driver.manage().timeouts().implicitlyWait(Constants.IMPLICIT_WAIT_TIMEOUT/10, TimeUnit.SECONDS);

        log.info("Setting script timeout...");
        driver.manage().timeouts().setScriptTimeout(Constants.SCRIPT_WAIT_TIMEOUT/1000, TimeUnit.SECONDS);


        log.info("Navigating to URL: " + finalUrl);
        driver.get(finalUrl);
        log.info("Application is launched.");

    }


}
