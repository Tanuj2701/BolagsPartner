package com.qa.bolags.constants;

import java.io.File; 
import java.time.Duration;

/**
 * @author ina291yogaknu
 *
 */
public class Constants {
	
	public static final int ONESEC = 1000;
	public static final int SHORT_WAIT = 6000;
	public static final int MEDIUM_WAIT = 8000;
	public static final int LONG_WAIT = 10000;
	public static final int EXPLICIT_WAIT_TIMEOUT = 100;
	public static final int IMPLICIT_WAIT_TIMEOUT = 100;
	public static final int PAGE_LOAD_WAIT_TIMEOUT = 6000;
	public static final int FLUENT_WAIT_TIMEOUT = 20;
	public static final int POLLING_WAIT_TIMEOUT = 5;
	public final static String CONFIGPROP = "./src/main/resources/config/config.properties";
	public final static String LOGGER = "./src/main/java/com/qa/drs/logger/log4j.properties";
	public final static String CHROMEDRIVER = "./src/main/resources/chromedriver.exe";
	public final static String FIREFOXDRIVER = "./src/main/resources/geckodriver.exe";
	public final static String SCREENSHOT_ROOT = System.getProperty("user.dir") + File.separator + "ScreenShots" + File.separator;
	public final static String ELEMENT_SCREENSHOT_PATH = SCREENSHOT_ROOT + "ElementScreenShots" + File.separator;
	public final static String FAILED_SCREENSHOT_PATH = SCREENSHOT_ROOT + "FailedScreenShots" + File.separator;
	public final static String PAGE_SCREENSHOT_PATH = SCREENSHOT_ROOT + "PageScreenShots" + File.separator;
	public final static String CONFIG = "./src/main/resources/"+System.getenv("env")+"/config.xlsx";
	public final static String WINDOWS_DOWNLOADED_FILE_PATH = System.getProperty("user.dir") +File.separator+ "src" + File.separator +"downloadedFiles";
	public final static String LINUX_DOWNLOADED_FILE_PATH = System.getProperty("user.dir") +File.separator+ "src" + File.separator +"downloadedFiles";
	public final static int SCRIPT_WAIT_TIMEOUT = 90000;
	public final static String TESTDATA = "./src/main/resources/"+System.getenv("env")+"/testInputData.xlsx";

    static {
        new File(ELEMENT_SCREENSHOT_PATH).mkdirs();
        new File(FAILED_SCREENSHOT_PATH).mkdirs();
        new File(PAGE_SCREENSHOT_PATH).mkdirs();
    }
}
