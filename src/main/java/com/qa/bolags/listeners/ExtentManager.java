package com.qa.bolags.listeners;

import com.relevantcodes.extentreports.ExtentReports;

public class ExtentManager {
	
	private static ExtentReports extent;
	 
    public synchronized static ExtentReports getReporter() {
        if (extent == null) {
             extent = new ExtentReports(
                     "target/reports/" + com.qa.bolags.reporting.ReportPaths.browserMode()
                             + "/extent/Automation_Execution_Report.html", true);
        }
        return extent;
    }

}
