package com.qa.bolags.constants;

import com.qa.bolags.utility.ChangeOfferCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * One valid ChangeQuote accounting row (Manage order → offer sent).
 * Values match QA-valid combinations for totalAssets, liabilities, results, and equity.
 */
public final class OfferSentAccountingData {

    private final String label;
    private final BigDecimal totalAssets;
    private final BigDecimal totalLiabilities;
    private final BigDecimal thisYearResults;
    private final BigDecimal untaxedReserves;
    private final BigDecimal nonTaxableIncome;
    private final BigDecimal nonDeductibleCosts;
    private final BigDecimal equity;
    /** Manual offer price (Example 1: 225400). Null = let autosave calculate. */
    private final BigDecimal offerPriceSek;

    public OfferSentAccountingData(
            String label,
            String totalAssets,
            String totalLiabilities,
            String thisYearResults,
            String untaxedReserves,
            String nonTaxableIncome,
            String nonDeductibleCosts,
            String equity) {
        this(label, totalAssets, totalLiabilities, thisYearResults, untaxedReserves,
                nonTaxableIncome, nonDeductibleCosts, equity, null);
    }

    public OfferSentAccountingData(
            String label,
            String totalAssets,
            String totalLiabilities,
            String thisYearResults,
            String untaxedReserves,
            String nonTaxableIncome,
            String nonDeductibleCosts,
            String equity,
            String offerPriceSek) {
        this.label = Objects.requireNonNull(label, "label");
        this.totalAssets = new BigDecimal(totalAssets);
        this.totalLiabilities = new BigDecimal(totalLiabilities);
        this.thisYearResults = new BigDecimal(thisYearResults);
        this.untaxedReserves = new BigDecimal(untaxedReserves);
        this.nonTaxableIncome = new BigDecimal(nonTaxableIncome);
        this.nonDeductibleCosts = new BigDecimal(nonDeductibleCosts);
        this.equity = new BigDecimal(equity);
        this.offerPriceSek = offerPriceSek != null && !offerPriceSek.trim().isEmpty()
                ? new BigDecimal(offerPriceSek) : null;
    }

    public String getLabel() {
        return label;
    }

    public String totalAssetsAsInput() {
        return totalAssets.toPlainString();
    }

    public String totalLiabilitiesAsInput() {
        return totalLiabilities.toPlainString();
    }

    public String thisYearResultsAsInput() {
        return thisYearResults.toPlainString();
    }

    public String untaxedReservesAsInput() {
        return untaxedReserves.toPlainString();
    }

    public String nonTaxableIncomeAsInput() {
        return nonTaxableIncome.toPlainString();
    }

    public String nonDeductibleCostsAsInput() {
        return nonDeductibleCosts.toPlainString();
    }

    public String equityAsInput() {
        return equity.toPlainString();
    }

    /**
     * Offer price for ChangeQuote. Uses explicit manual price when set (Example 1: 225400),
     * otherwise at least 25% of equity ({@code ChangeQuote.tsx} validation).
     */
    public String offerPriceSekAsInput() {
        if (offerPriceSek != null) {
            return offerPriceSek.setScale(2, RoundingMode.HALF_UP).toPlainString();
        }
        BigDecimal minValid = equity.multiply(new BigDecimal("0.25"));
        BigDecimal offer = equity.multiply(new BigDecimal("0.50"));
        if (offer.compareTo(minValid) < 0) {
            offer = minValid;
        }
        return offer.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    public BigDecimal getOfferPriceSekOrNull() {
        return offerPriceSek;
    }

    public boolean hasExplicitOfferPrice() {
        return offerPriceSek != null;
    }

    /** Auto-calculated fiscal result ({@code ChangeOfferFinancialCalculator}). */
    public BigDecimal getFiscalResultForTheYear() {
        return ChangeOfferCalculator.fiscalResultForTheYear(
                thisYearResults, untaxedReserves, nonDeductibleCosts, nonTaxableIncome);
    }

    /** Auto-calculated tax at 20.6% when fiscal result is positive. */
    public BigDecimal getEstimatedTaxOnYearsProfit() {
        return ChangeOfferCalculator.estimatedTaxOnYearsProfit(getFiscalResultForTheYear());
    }

    /** Resolved offer price: explicit manual value or auto-suggested from equity. */
    public BigDecimal getResolvedOfferPriceSek() {
        if (offerPriceSek != null) {
            return offerPriceSek;
        }
        return ChangeOfferCalculator.suggestedOfferPriceSek(equity);
    }

    /** Fee = equity - offer price (Example 1: 9431.97). */
    public BigDecimal getFee() {
        return ChangeOfferCalculator.fee(equity, getResolvedOfferPriceSek());
    }

    public BigDecimal getTotalAssets() {
        return totalAssets;
    }

    public BigDecimal getTotalLiabilities() {
        return totalLiabilities;
    }

    public BigDecimal getThisYearResults() {
        return thisYearResults;
    }

    public BigDecimal getUntaxedReserves() {
        return untaxedReserves;
    }

    public BigDecimal getNonTaxableIncome() {
        return nonTaxableIncome;
    }

    public BigDecimal getNonDeductibleCosts() {
        return nonDeductibleCosts;
    }

    public BigDecimal getEquity() {
        return equity;
    }

    @Override
    public String toString() {
        return label + " {totalAssets=" + totalAssets + ", equity=" + equity + "}";
    }
}
