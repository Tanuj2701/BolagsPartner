package com.qa.bolags.utility;

import java.math.BigDecimal;

/**
 * Parses numbers displayed in ChangeQuote / Swedish admin UI.
 * Handles {@code Intl.NumberFormat('sv-SE')} output: {@code 234 831,97}, {@code −105 423,52}, {@code 246,435.09 kr}.
 */
public final class SwedishNumberParser {

    private SwedishNumberParser() {
    }

    public static BigDecimal parse(String display) {
        if (display == null) {
            return BigDecimal.ZERO;
        }
        String t = display.trim();
        if (t.isEmpty() || "-".equals(t) || "\u2212".equals(t)) {
            return BigDecimal.ZERO;
        }

        t = t.replaceAll("(?i)kr", "").trim();
        t = t.replace('\u00a0', ' ').replace('\u202f', ' ').replace('\u2009', ' ');
        t = t.replace('\u2212', '-').replace('\u2013', '-').replace('\u2014', '-').replace('\uFE63', '-');

        boolean negative = false;
        if (t.startsWith("-")) {
            negative = true;
            t = t.substring(1).trim();
        } else if (t.startsWith("(") && t.endsWith(")")) {
            negative = true;
            t = t.substring(1, t.length() - 1).trim();
        }

        t = t.replace(" ", "");

        int lastComma = t.lastIndexOf(',');
        int lastDot = t.lastIndexOf('.');

        if (lastComma >= 0 && lastDot >= 0) {
            if (lastComma > lastDot) {
                t = t.replace(".", "").replace(',', '.');
            } else {
                t = t.replace(",", "");
            }
        } else if (lastComma >= 0) {
            int digitsAfterComma = t.length() - lastComma - 1;
            if (digitsAfterComma <= 2) {
                t = t.replace(',', '.');
            } else {
                t = t.replace(",", "");
            }
        }

        t = t.replaceAll("[^0-9.]", "");
        if (t.isEmpty() || ".".equals(t)) {
            return BigDecimal.ZERO;
        }

        BigDecimal value = new BigDecimal(t);
        return negative ? value.negate() : value;
    }

    /** Safe parse — never throws; logs are left to callers. */
    public static BigDecimal parseSafe(String display) {
        try {
            return parse(display);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
