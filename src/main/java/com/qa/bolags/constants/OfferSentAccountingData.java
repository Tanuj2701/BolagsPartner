package com.qa.bolags.constants;

import java.math.BigDecimal;
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

    public OfferSentAccountingData(
            String label,
            String totalAssets,
            String totalLiabilities,
            String thisYearResults,
            String untaxedReserves,
            String nonTaxableIncome,
            String nonDeductibleCosts,
            String equity) {
        this.label = Objects.requireNonNull(label, "label");
        this.totalAssets = new BigDecimal(totalAssets);
        this.totalLiabilities = new BigDecimal(totalLiabilities);
        this.thisYearResults = new BigDecimal(thisYearResults);
        this.untaxedReserves = new BigDecimal(untaxedReserves);
        this.nonTaxableIncome = new BigDecimal(nonTaxableIncome);
        this.nonDeductibleCosts = new BigDecimal(nonDeductibleCosts);
        this.equity = new BigDecimal(equity);
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
