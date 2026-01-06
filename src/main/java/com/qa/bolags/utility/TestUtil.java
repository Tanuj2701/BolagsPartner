package com.qa.bolags.utility;

import java.awt.image.BufferedImage;
import java.io.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import com.qa.bolags.logger.Log4j;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import static com.qa.bolags.constants.Constants.*;

/**
 * @author ina291yogaknu
 *
 */
public class TestUtil {

	public static Workbook book;
	public static Sheet sheet;
	protected static WebDriver driver;
	private static WebDriverWait wait;
	static protected Logger log;
	protected static Actions act;
	public static Properties prop;
	public static WebElement webelement;


	public TestUtil(WebDriver tdriver) {
		driver = tdriver;
		log = Log4j.initLogger(TestUtil.class);
	}

	public static void switchToParentFrame() {
		driver.switchTo().parentFrame();
	}

	public static WebDriver openNewTab() {
		return driver.switchTo().newWindow(WindowType.TAB);
	}

	public static WebDriver openNewWindow() {
		return driver.switchTo().newWindow(WindowType.WINDOW);
	}

	public static WebDriver getParentWindow() {
		Set<String> handles = driver.getWindowHandles();
		List<String> ls = new ArrayList<String>(handles);
		String parentWindow = ls.get(0);
		return driver.switchTo().window(parentWindow);
	}

	public static WebDriver getChildWindow() {
		Set<String> handles = driver.getWindowHandles();
		List<String> ls = new ArrayList<String>(handles);
		String childWindow = ls.get(1);
		return driver.switchTo().window(childWindow);
	}

	public static void takeElementScreenshot(By ele, String fileName) {
		webelement = driver.findElement(ele);
		File scrImage = webelement.getScreenshotAs(OutputType.FILE);
		try {
			FileUtils.copyFile(scrImage, new File(ELEMENT_SCREENSHOT_PATH + fileName + ".png"));
		} catch (IOException e) {
			log.error("Exception in takeElementScreenshot", e);
		}
	}

	public static void getPageScreenshot(String pageName) {
		TakesScreenshot scrShot = ((TakesScreenshot) driver);
		File scrImage = scrShot.getScreenshotAs(OutputType.FILE);
		try {
			FileUtils.copyFile(scrImage, new File(PAGE_SCREENSHOT_PATH + pageName + ".png"));
		} catch (IOException e) {
			log.error("Exception in getPageScreenshot", e);
		}
	}

	public static void mouseHover(By ele) {
		act = new Actions(driver);
		act.moveToElement(driver.findElement(ele));
		highlight(ele);
	}

	public static void waitForElementToBeVisible(By ele) {

		try {
			new WebDriverWait(driver, Duration.ofSeconds(EXPLICIT_WAIT_TIMEOUT));
//			wait = new WebDriverWait(driver, EXPLICIT_WAIT_TIMEOUT);
			wait.until(ExpectedConditions.visibilityOfElementLocated(ele));
			highlight(ele);
		} catch (Exception e) {
			log.error(ele + " is not visible, hence test case got failed.", e);
			Assert.fail(ele + " is not visible , hence  test case got failed.");
		}

	}

	public static void waitForElementToBeClickable(By ele) {

		try {
			wait = new WebDriverWait(driver, Duration.ofSeconds(EXPLICIT_WAIT_TIMEOUT));
//			wait = new WebDriverWait(driver, EXPLICIT_WAIT_TIMEOUT);
			wait.until(ExpectedConditions.elementToBeClickable(ele));
			highlight(ele);
		} catch (Exception e) {
			log.error(ele + " is not clickable, hence test case got failed.", e);
			Assert.fail(ele + " is not clickable , hence  test case got failed.");
		}
	}


	public static void click(By ele) {
		WebElement element = getWebElement(ele);
		waitForLoad();
		element.click();
	}

	public static void doubleClick(By ele) {
		WebElement element = getWebElement(ele);
		highlight(ele);
		Actions actions = new Actions(driver);
		actions.doubleClick(element).perform();
	}


	public static void clickByJS(By ele) {
		WebElement element = getWebElement(ele);
		waitForLoad();
		JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;
		jsExecutor.executeScript("arguments[0].click();", element);
	}

	private static void highlight(By ele) {
		WebElement element = getWebElement(ele);
		JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;
		jsExecutor.executeScript("arguments[0].style.border='4px solid red'", element);
	}

	public static WebElement getWebElement(By ele) {
		return driver.findElement(ele);
	}

	public static void scrollPageToViewElement(By ele) {
		JavascriptExecutor executor = (JavascriptExecutor) driver;
		executor.executeScript("arguments[0].scrollIntoView();", getWebElement(ele));
	}

	public static String returnCurrentPageTitle() {
		return driver.getTitle();
	}

	public static boolean isElementDisplayed(By ele) {
		waitForLoad();
		highlight(ele);
		int ele_size = driver.findElements(ele).size();

		if (ele_size == 0) {
			return false;
		} else {
			return true;
		}
	}


	public boolean isElementEnabled(By ele) {
		highlight(ele);
		return getWebElement(ele).isEnabled();
	}

	public boolean isCheckboxSelected(By ele) {
		highlight(ele);
		return getWebElement(ele).isSelected();
	}

	public static String getElementText(By ele) {
		highlight(ele);
		waitForElementToBeVisible(ele);
		return getWebElement(ele).getText().trim();
	}

	public boolean isRadioButtonSelected(By ele) {
		highlight(ele);
		return getWebElement(ele).isSelected();

	}

	public String getAttributeValue(By ele, String attributeName) {
		highlight(ele);
		return getWebElement(ele).getAttribute(attributeName).trim();

	}

	public String getTextOfElement(By ele) {
		highlight(ele);
		JavascriptExecutor jse = (JavascriptExecutor) driver;
		String text = (String) jse.executeScript("return arguments[0].innerHTML", driver.findElement(ele));
		return text.trim();

	}

	public void switchToAlert() {
		driver.switchTo().alert();
	}

	public void alertAccept() {
		driver.switchTo().alert().accept();
	}

	public void alertCancel() {
		driver.switchTo().alert().dismiss();
	}

	public static String replaceString(String xpathString, String replaceBy) {
		StringBuffer xpathBuffer = new StringBuffer(xpathString);
		xpathBuffer.replace(xpathBuffer.indexOf("$"), xpathBuffer.lastIndexOf("$") + 1, replaceBy);
		return xpathBuffer.toString();
	}

	public static void enterStringValueInInputField(By ele, String value) {
		highlight(ele);
		log.info("Typing '{}' into input {}.", value, ele);
		WebElement element = getWebElement(ele);
		element.clear();
		element.sendKeys(value);
	}

	public void enterNumericValueInInputField(By ele, int value) {
		highlight(ele);
		getWebElement(ele).clear();
		getWebElement(ele).sendKeys("" + value);
	}

	public static void enterUsingSendKeys(By ele) {
		highlight(ele);
		getWebElement(ele).sendKeys(Keys.ENTER);
	}

	public void tabUsingSendKeys(By ele) {
		highlight(ele);
		getWebElement(ele).sendKeys(Keys.TAB);
	}


	public void selectValueFromInputDropdown(By ele, String text) {
		highlight(ele);
		enterStringValueInInputField(ele, text);
		enterUsingSendKeys(ele);
	}

	public void selectByVisibleText(By ele, String text) {
		highlight(ele);
		Select sel = new Select(getWebElement(ele));
		sel.selectByVisibleText(text);
		log.info(text + " text is selected.");
	}

	public void selectByValue(By ele, String value) {
		highlight(ele);
		Select sel = new Select(getWebElement(ele));
		sel.selectByValue(value);
		log.info(value + " value is selected.");
	}

	public void selectByIndex(By ele, int index) {
		highlight(ele);
		Select sel = new Select(getWebElement(ele));
		sel.selectByIndex(index);
		log.info(index + " index is selected.");
	}

	public static void waitForSpecifiedTime(int i) {
		try {
			Thread.sleep(i * ONESEC);
		} catch (InterruptedException e) {
			log.error("Exception occured while waiting: " + e.getMessage());
		}

	}

	public static void shortWait() {
		try {
			Thread.sleep(SHORT_WAIT);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void mediumWait() {
		try {
			Thread.sleep(MEDIUM_WAIT);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void longWait() {
		try {
			Thread.sleep(LONG_WAIT);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


	public static void deleteFile(String fileName) {
		String userDir = System.getProperty("user.dir");
		File file = new File(userDir + "\\Download\\" + fileName);
		if (!file.exists()) {
			log.info("File is not present");
		} else {
			// delete a file
			log.info("File already exist");
			file.delete();
		}
	}

	public static Sheet readExcelFile(String filePath, int sheetIndex) {
		File file = new File(filePath);
		FileInputStream inputStream = null;
		try {
			inputStream = new FileInputStream(file);
			log.info("inputStream object is created");
		} catch (FileNotFoundException e) {

			log.error("testData file not found");
		}

		Workbook workbook = null;

		try {
			workbook = new XSSFWorkbook(inputStream);
			log.info("workbook object is created");
		} catch (IOException e) {
			log.error("IO exception has occured");
		}

		Sheet sheet = (Sheet) workbook.getSheetAt(sheetIndex);
		log.info("Data sheet is found");
		return sheet;
	}

	public static JSONObject readJSONFile(String filePath) {
		String jsonFileContent = "";
		String line = "";

		FileInputStream fis = null;
		BufferedReader reader = null;

		try {
			// change in file path - read from excel file and store in getter n setter class
			// and read from there
			fis = new FileInputStream(new File(filePath));

			reader = new BufferedReader(new InputStreamReader(fis));
			while ((line = reader.readLine()) != null) {
				jsonFileContent = jsonFileContent + line.trim();
			}

		} catch (FileNotFoundException e) {
			log.error("Request parameter file was not available. Detailed error - " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			log.error("Error while processing request parameter file. Detailed error - " + e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				reader.close();
				fis.close();
			} catch (IOException ex) {
				log.error("Failed to read json file. Detailed error - " + ex.getMessage());
			}
		}
		JSONParser expectedOutputParser = new JSONParser();
		JSONObject expectedOutputJSONResponse = new JSONObject();
		try {
			expectedOutputJSONResponse = (JSONObject) expectedOutputParser.parse(jsonFileContent);
		} catch (Exception ParseException) {
			System.out.println("Parsing has failed." + ParseException.getMessage());
		}
		return expectedOutputJSONResponse;
	}

	public static void waitForLoad() {

		ExpectedCondition<Boolean> pageLoadCondition = new ExpectedCondition<Boolean>() {
			public Boolean apply(WebDriver driver) {
				JavascriptExecutor js;
				String pageLoadStatus = null;
				do {

					js = (JavascriptExecutor) driver;

					pageLoadStatus = (String) js.executeScript("return document.readyState");

				} while (!pageLoadStatus.equals("complete"));

				return js.executeScript("return document.readyState").equals("complete");
			}
		};
		try {
			new WebDriverWait(driver, Duration.ofSeconds(SHORT_WAIT));
//			wait = new WebDriverWait(driver, SHORT_WAIT);
			wait.until(pageLoadCondition);
		} catch (Throwable error) {
			System.out.println(
					"------------------------------------------------Javascript execution error caught------------------------------------");

		}
	}

	public static Object[][] getTestData(String sheetName) {
		FileInputStream file = null;
		try {
			file = new FileInputStream(TESTDATA);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		try {
			book = WorkbookFactory.create(file);
		} catch (InvalidFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		sheet = book.getSheet(sheetName);

		Object data[][] = new Object[sheet.getLastRowNum()][sheet.getRow(0).getLastCellNum()];

		for (int i = 0; i < sheet.getLastRowNum(); i++) {
			for (int k = 0; k < sheet.getRow(0).getLastCellNum(); k++) {
				data[i][k] = sheet.getRow(i + 1).getCell(k).toString();
			}
		}

		return data;

	}

	public static Properties initializeProperties() {
		prop = new Properties();
		try {
			FileInputStream ip = new FileInputStream(CONFIGPROP);
			prop.load(ip);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return prop;
	}

	public static void triggerEvent(String eventName, WebElement element) {
		JavascriptExecutor js = (JavascriptExecutor) driver;
		String script = "var event = new Event(arguments[0], { bubbles: true, cancelable: true }); arguments[1].dispatchEvent(event);";
		js.executeScript(script, eventName, element);
	}

	public static void switchToNewWindow(WebDriver driver, String parentWindow) {
		for (String windowHandle : driver.getWindowHandles()) {
			if (!windowHandle.equals(parentWindow)) {
				driver.switchTo().window(windowHandle);
				break;
			}
		}
	}

//	public static String solveCaptcha(WebDriver driver, By captchaImgBy, String tessDataPath) {
//		try {
//			WebElement captchaImage = driver.findElement(captchaImgBy);
//			File captchaFile = captchaImage.getScreenshotAs(OutputType.FILE); // Direct element screenshot
//
//			net.sourceforge.tess4j.ITesseract instance = new net.sourceforge.tess4j.Tesseract();
//			instance.setDatapath(tessDataPath);
//			String captchaText = instance.doOCR(captchaFile).replaceAll("\\s+", "");
//
//			 //Optionally delete the file after reading
//			// captchaFile.delete();
//
//			return captchaText;
//		} catch (Exception e) {
//			System.err.println("Error solving captcha: " + e.getMessage());
//			return "";
//		}
//	}


//	public static String solveCaptcha(WebDriver driver, By captchaImgBy, String tessDataPath) {
//		try {
//			WebElement captchaImage = driver.findElement(captchaImgBy);
//			String userDir = System.getProperty("user.dir");
//			String imagePath = userDir + "\\images\\captcha_" + System.currentTimeMillis() + ".png";
//			File captchaFile = captchaImage.getScreenshotAs(OutputType.FILE);
//			FileUtils.copyFile(captchaFile, new File(imagePath));
//
//			net.sourceforge.tess4j.ITesseract instance = new net.sourceforge.tess4j.Tesseract();
//			instance.setDatapath(tessDataPath);
//			String captchaText = instance.doOCR(new File(imagePath)).replaceAll("\\s+", "");
//			//captchaFile.delete();
//			return captchaText;
//		} catch (Exception e) {
//			System.err.println("Error solving captcha: " + e.getMessage());
//			return "";
//		}
//	}


//	public static String solveCaptcha(WebDriver driver, By captchaImgBy, String tessDataPath) {
//		try {
//			WebElement captchaImage = driver.findElement(captchaImgBy);
//			String userDir = System.getProperty("user.dir");
//			String imagePath = userDir + "\\images\\captcha_" + System.currentTimeMillis() + ".png";
//			File captchaFile = captchaImage.getScreenshotAs(OutputType.FILE);
//			FileUtils.copyFile(captchaFile, new File(imagePath));
//
//			net.sourceforge.tess4j.ITesseract instance = new net.sourceforge.tess4j.Tesseract();
//			instance.setDatapath(tessDataPath);
//			instance.setLanguage("eng"); // Set language to English
//
//			// Optionally, you can set OCR engine mode or page segmentation mode for better results:
//			// instance.setOcrEngineMode(1); // LSTM only
//			// instance.setPageSegMode(6); // Assume a single uniform block of text
//
//			String captchaText = instance.doOCR(new File(imagePath)).replaceAll("\\s+", "");
//			return captchaText;
//		} catch (Exception e) {
//			System.err.println("Error solving captcha: " + e.getMessage());
//			return "";
//		}
//	}

//		public static String solveCaptcha(WebDriver driver, By captchaImgBy, String tessDataPath) {
//			try {
//			WebElement captchaImage = driver.findElement(captchaImgBy);
//			String userDir = System.getProperty("user.dir");
//			String imagePath = userDir + "\\images\\captcha_" + System.currentTimeMillis() + ".png";
//			File captchaFile = captchaImage.getScreenshotAs(OutputType.FILE);
//			FileUtils.copyFile(captchaFile, new File(imagePath));
//
//			net.sourceforge.tess4j.ITesseract instance = new net.sourceforge.tess4j.Tesseract();
//			instance.setDatapath(tessDataPath);
//			instance.setLanguage("eng");
//
//			instance.setTessVariable("tessedit_char_whitelist", "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");
//			instance.setOcrEngineMode(1); // LSTM only
//			instance.setPageSegMode(6);   // Assume a single uniform block of text
//            waitForSpecifiedTime(2);
//			String captchaText = instance.doOCR(new File(imagePath)).replaceAll("\\s+", "");
//			return captchaText;
//		} catch (Exception e) {
//			System.err.println("Error solving captcha: " + e.getMessage());
//			return "";
//		}
//	}

	public static String solveCaptcha(WebDriver driver, By captchaImgBy, String tessDataPath) {
		try {
			WebElement captchaImg = driver.findElement(captchaImgBy);
			String base64Image = captchaImg.getAttribute("src").split(",")[1];
			byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Image);
			InputStream is = new java.io.ByteArrayInputStream(imageBytes);
			BufferedImage captchaImage = javax.imageio.ImageIO.read(is);
			String userDir = System.getProperty("user.dir");
			String imagePath = userDir + "\\images\\captcha_" + System.currentTimeMillis() + ".png";
			File captchaFile = new File(imagePath);
			javax.imageio.ImageIO.write(captchaImage, "png", captchaFile);
			net.sourceforge.tess4j.ITesseract tesseract = new net.sourceforge.tess4j.Tesseract();
			tesseract.setDatapath(tessDataPath);
			tesseract.setLanguage("eng");
			tesseract.setTessVariable("tessedit_char_whitelist", "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");
			tesseract.setOcrEngineMode(1);
			tesseract.setPageSegMode(6);
			waitForSpecifiedTime(2);
			String captchaText = tesseract.doOCR(captchaFile).replaceAll("\\s+", "");
			return captchaText;
		} catch (Exception e) {
			System.err.println("Error solving captcha: " + e.getMessage());
			return "";
		}
	}

}
