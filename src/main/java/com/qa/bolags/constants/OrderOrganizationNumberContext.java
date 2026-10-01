package com.qa.bolags.constants;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Organization number selected for the current liquidation order. */
public final class OrderOrganizationNumberContext {

    private static final Pattern SWEDISH_ORG_NUMBER = Pattern.compile("(?<!\\d)(\\d{6})[-\\s]?(\\d{4})(?!\\d)");
    private static volatile String organizationNumber;
    private static volatile String companyName;

    private OrderOrganizationNumberContext() {
    }

    public static String captureFromCompanyLabel(String companyLabel) {
        if (companyLabel == null) {
            return null;
        }
        Matcher matcher = SWEDISH_ORG_NUMBER.matcher(companyLabel);
        if (!matcher.find()) {
            return null;
        }
        organizationNumber = matcher.group(1) + "-" + matcher.group(2);
        companyName = (companyLabel.substring(0, matcher.start()) + companyLabel.substring(matcher.end()))
            .replaceAll("[()\\[\\],;|]+", " ").trim().replaceAll("\\s+", " ");
        return organizationNumber;
    }

    public static String getOrganizationNumberOrNull() {
        return organizationNumber;
    }

    public static String getCompanyNameOrNull() {
        return companyName == null || companyName.isEmpty() ? null : companyName;
    }

    public static void clear() {
        organizationNumber = null;
        companyName = null;
    }
}