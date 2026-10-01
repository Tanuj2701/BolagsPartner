package com.qa.bolags.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.Year;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Writes an order-specific SI file in the ignored local build cache. */
public final class OrderSiFileWriter {

    private static final Logger LOG = LoggerFactory.getLogger(OrderSiFileWriter.class);
    private static final Charset PC8 = Charset.forName("IBM850");
    private static final Pattern ORGNR_LINE = Pattern.compile("(?m)^#ORGNR\\s+.*$");
    private static final Pattern FNAMN_LINE = Pattern.compile("(?m)^#FNAMN\\s+.*$");
    private static final Pattern GEN_LINE = Pattern.compile("(?m)^#GEN\\s+\\d{8}\\s*$");
        private static final Pattern RAR_LINE = Pattern.compile(
            "(?m)^#RAR\\s+(-?\\d+)\\s+(\\d{4})(\\d{2})(\\d{2})\\s+(\\d{4})(\\d{2})(\\d{2})\\s*$");

    private OrderSiFileWriter() {
    }

    public static Path writeForOrder(String orderId, String organizationNumber, String companyName)
            throws IOException {
        if (orderId == null || !orderId.matches("\\d+")) {
            throw new IllegalArgumentException("A numeric saveInitial order id is required for SI export");
        }
        if (organizationNumber == null || !organizationNumber.matches("\\d{6}-\\d{4}")) {
            throw new IllegalArgumentException("A normalized Swedish organisationsnummer is required for SI export");
        }
        if (companyName == null || companyName.trim().isEmpty()) {
            throw new IllegalArgumentException("Selected company name is required for SI export");
        }

        Path template = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "templates",
                "sample-si-template.se");
        if (!Files.isRegularFile(template)) {
            throw new IOException("SI template not found: " + template);
        }

        String contents = new String(Files.readAllBytes(template), PC8);
        contents = replaceSingleHeader(contents, ORGNR_LINE, "#ORGNR " + organizationNumber, "#ORGNR");
        contents = replaceSingleHeader(contents, FNAMN_LINE,
                "#FNAMN \"" + escapeQuotedValue(companyName.trim()) + "\"", "#FNAMN");
        contents = replaceSingleHeader(contents, GEN_LINE,
                "#GEN " + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE), "#GEN");
        contents = shiftFinancialYears(contents, Year.now().getValue());

        Path output = fileForOrder(orderId);
        Files.createDirectories(output.getParent());
        Files.write(output, contents.getBytes(PC8));
        LOG.info("Created order SI file for order {} and organisationsnummer {} at {}",
                orderId, organizationNumber, output.toAbsolutePath());
        return output;
    }

    public static Path fileForOrder(String orderId) {
        if (orderId == null || !orderId.matches("\\d+")) {
            throw new IllegalArgumentException("A numeric saveInitial order id is required for SI file lookup");
        }
        Path cache = Paths.get(System.getProperty("user.dir"), "target", "cache", "si");
        return cache.resolve("order-" + orderId + ".se");
    }

    private static String replaceSingleHeader(String contents, Pattern headerPattern, String replacement,
                                              String headerName) throws IOException {
        Matcher matcher = headerPattern.matcher(contents);
        if (!matcher.find()) {
            throw new IOException("SI template is missing " + headerName + " header");
        }
        int start = matcher.start();
        int end = matcher.end();
        if (matcher.find()) {
            throw new IOException("SI template contains multiple " + headerName + " headers");
        }
        return contents.substring(0, start) + replacement + contents.substring(end);
    }

    private static String escapeQuotedValue(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String shiftFinancialYears(String contents, int currentYear) throws IOException {
        Matcher matcher = RAR_LINE.matcher(contents);
        StringBuffer shifted = new StringBuffer();
        Integer templateCurrentYear = null;
        boolean foundCurrentPeriod = false;
        while (matcher.find()) {
            if ("0".equals(matcher.group(1))) {
                if (foundCurrentPeriod) {
                    throw new IOException("SI template contains multiple #RAR 0 financial-year periods");
                }
                foundCurrentPeriod = true;
                templateCurrentYear = Integer.parseInt(matcher.group(2));
            }
        }
        if (!foundCurrentPeriod || templateCurrentYear == null) {
            throw new IOException("SI template is missing a valid #RAR 0 financial-year period");
        }

        int yearDelta = currentYear - templateCurrentYear;
        matcher.reset();
        while (matcher.find()) {
            int startYear = Integer.parseInt(matcher.group(2)) + yearDelta;
            int endYear = Integer.parseInt(matcher.group(5)) + yearDelta;
            String replacement = "#RAR " + matcher.group(1) + " " + startYear + matcher.group(3)
                    + matcher.group(4) + " " + endYear + matcher.group(6) + matcher.group(7);
            matcher.appendReplacement(shifted, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(shifted);
        return shifted.toString();
    }
}
