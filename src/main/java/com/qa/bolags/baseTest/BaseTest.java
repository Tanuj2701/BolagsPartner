package com.qa.bolags.baseTest;

import com.qa.bolags.constants.Constants;
import com.qa.bolags.constants.QaServerCredentials;
import com.qa.bolags.utility.SaveInitialOrderCapture;

import io.github.bonigarcia.wdm.WebDriverManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.poi.ss.usermodel.Sheet;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.UnexpectedAlertBehaviour;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;


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
        finalUrl = QaServerCredentials.urlWithHttpBasicAuth("https://qa.bolagspartner.se/app/liquidation/company");
        //tessdataPath = System.getenv("TESSDATA_PATH");
        log.info("Selected browser is --> " + browser);
        log.info("Application URL is --> " + finalUrl);


        this.launchBrowser(browser);
    }

    /**
     * Liquidation E2E: Chrome performance log + CDP Network so {@link SaveInitialOrderCapture} can read
     * {@code orderId} from {@code POST .../companyLiquidationOrders/saveInitial}.
     */
    public void initializeDriverForLiquidationWithSaveInitialCapture() {
        log.info("Initializing driver for liquidation (saveInitial network capture enabled)...");
        browser = "Chrome";
        finalUrl = QaServerCredentials.urlWithHttpBasicAuth("https://qa.bolagspartner.se/app/liquidation/company");
        log.info("Selected browser is --> " + browser);
        log.info("Application URL is --> " + finalUrl);
        launchChromeDriverInstance(true);
        openApplication();
    }

    /**
     * Starts Chrome with the same options as {@link #initializeDriver()} but does not open the liquidation URL.
     * Use for admin (or other) flows that navigate in the first scenario steps.
     */
    public void initializeChromeWithoutDefaultNavigation() {
        log.info("Initializing Chrome without default liquidation navigation...");
        browser = "Chrome";
        finalUrl = QaServerCredentials.urlWithHttpBasicAuth(
                System.getProperty("qaChromeOrigin", "https://qa.bolagspartner.se"));
        log.info("Selected browser is --> " + browser);
        launchChromeDriverInstance(false);
        applyCommonBrowserSettings();
        log.info("Chrome is ready (no initial navigation).");
    }

    private void launchChromeDriverInstance() {
        launchChromeDriverInstance(false);
    }

    private void launchChromeDriverInstance(boolean enablePerformanceLoggingForNetworkCapture) {
        log.info("Launching Chrome...");
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
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        options.addArguments("--unsafely-treat-insecure-origin-as-secure=" + QaServerCredentials.bareHttpsOriginForQa());
        options.setAcceptInsecureCerts(true);
        options.addArguments("disable-popup-blocking");
        options.setUnhandledPromptBehaviour(UnexpectedAlertBehaviour.ACCEPT);
        applyHeadlessChromeOptions(options);
        LoggingPreferences logPrefs = new LoggingPreferences();
        logPrefs.enable(LogType.PERFORMANCE, Level.ALL);
        options.setCapability(ChromeOptions.LOGGING_PREFS, logPrefs);
        log.info("Chrome performance logging enabled (API 4xx/5xx + saveInitial capture).");
        log.info("Chrome desired Property is set.");
        if (Constants.isHeadless()) {
            log.info("Chrome headless mode is enabled.");
        }
        driver = new ChromeDriver(options);
        log.info("Chrome driver object is created");
        if (driver instanceof ChromeDriver) {
            SaveInitialOrderCapture.enableNetworkDomain(driver);
        }
        driver.manage().deleteAllCookies();
        log.info("Browser cache cleared.");
    }

    private void applyHeadlessChromeOptions(ChromeOptions options) {
        if (!Constants.isHeadless()) {
            return;
        }
        options.addArguments("--headless=new");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--hide-scrollbars");
    }

    private void applyCommonBrowserSettings() {
        if (Constants.isHeadless()) {
            log.info("Setting headless window size 1920x1080...");
            driver.manage().window().setSize(new org.openqa.selenium.Dimension(1920, 1080));
        } else {
            log.info("Maximizing window...");
            driver.manage().window().maximize();
        }
        log.info("Deleting all cookies...");
        driver.manage().deleteAllCookies();
        log.info("Setting page load timeout...");
        driver.manage().timeouts().pageLoadTimeout(
                Duration.ofSeconds(Constants.PAGE_LOAD_WAIT_TIMEOUT));
        log.info("Setting implicit wait timeout...");
        int implicitSeconds = Constants.isTimeOptimized() ? 2 : Constants.IMPLICIT_WAIT_TIMEOUT / 10;
        driver.manage().timeouts().implicitlyWait(
                Duration.ofSeconds(implicitSeconds));
        log.info("Setting script timeout...");
        driver.manage().timeouts().scriptTimeout(
                Duration.ofSeconds(Constants.SCRIPT_WAIT_TIMEOUT / 1000));
    }


    private WebDriver launchBrowser(String browserName) {
        log.info("Launching browser...");
       // if (configurationData.getRow(1).getCell(4).getStringCellValue().equalsIgnoreCase("Yes")) {
            switch (browserName) {
                case "Chrome":
                    log.info("Chrome browser is selected to execute test cases");
                    launchChromeDriverInstance();
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
        applyCommonBrowserSettings();
        log.info("Navigating to URL: " + finalUrl);
        driver.get(finalUrl);
        log.info("Application is launched.");

    }


}
