package com.qa.bolags.utility;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Mirrors {@code ChangeOfferFinancialCalculator} / {@code ChangeQuote.tsx} formulas for LIQ_NEW offer data.
 *
 * <pre>
 *   fiscal = thisYearResults + untaxedReserves + nonDeductibleCosts - nonTaxableIncome
 *   tax    = fiscal > 0 ? fiscal * 0.206 : 0
 *   fee    = equity - offerPriceSek
 * </pre>
 */
public final class ChangeOfferCalculator {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.206");
    private static final BigDecimal EQUITY_AUTO_OFFER_THRESHOLD = new BigDecimal("300000");
    private static final BigDecimal DEFAULT_DEDUCT = new BigDecimal("9500");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private ChangeOfferCalculator() {
    }

    public static BigDecimal fiscalResultForTheYear(
            BigDecimal thisYearResults,
            BigDecimal untaxedReserves,
            BigDecimal nonDeductibleCosts,
            BigDecimal nonTaxableIncome) {
        return nz(thisYearResults)
                .add(nz(untaxedReserves))
                .add(nz(nonDeductibleCosts))
                .subtract(nz(nonTaxableIncome))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal estimatedTaxOnYearsProfit(BigDecimal fiscalResultForTheYear) {
        if (nz(fiscalResultForTheYear).compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return nz(fiscalResultForTheYear).multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Auto offer when equity {@code <= 300000}: {@code ceil((equity - 9500) / 100) * 100}.
     */
    public static BigDecimal suggestedOfferPriceSek(BigDecimal equity) {
        BigDecimal eq = nz(equity);
        if (eq.compareTo(EQUITY_AUTO_OFFER_THRESHOLD) > 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal raw = eq.subtract(DEFAULT_DEDUCT);
        BigDecimal hundreds = raw.divide(HUNDRED, 0, RoundingMode.CEILING);
        return hundreds.multiply(HUNDRED).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal fee(BigDecimal equity, BigDecimal offerPriceSek) {
        return nz(equity).subtract(nz(offerPriceSek)).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
