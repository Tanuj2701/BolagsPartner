package com.qa.bolags.utility;

import com.qa.bolags.constants.Constants;
import com.qa.bolags.logger.Log4j;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ExcelUtility {

    public ExcelUtility() throws IllegalStateException {
        throw new IllegalStateException("Utility class is not instantiable.");
    }

    public static Workbook book;
    public static Sheet sheet;
    static public Logger log = Log4j.initLogger(ExcelUtility.class);

    public static Sheet readExcelFile(String filePath, int sheetIndex) {

        File file = new File(filePath);
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            log.info("inputStream object is created");
        } catch (FileNotFoundException e) {

            log.error("File not found.");
        }

        XSSFWorkbook workbook= null;

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

    public static Object[][] getTestData(String sheetName) {
        FileInputStream file = null;
        try {
            file = new FileInputStream(Constants.TESTDATA);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            book = WorkbookFactory.create(file);
        } catch (IOException | InvalidFormatException e) {
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

}
