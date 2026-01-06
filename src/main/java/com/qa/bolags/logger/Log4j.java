package com.qa.bolags.logger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 *
 * This class is developed for SLF4J logger to write execution log in console and log file.
 */
public class Log4j {
    @SuppressWarnings("rawtypes")
    public static Logger initLogger(Class cl) {
        return LoggerFactory.getLogger(cl);
    }
}
