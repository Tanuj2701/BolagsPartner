package com.qa.bolags.utility;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.math.BigDecimal;

/** Validates Example 1 / Example 4 calculations from the QA spreadsheet. */
public class ChangeOfferCalculatorTest {

    @Test
    public void example1Calculations() {
        BigDecimal fiscal = ChangeOfferCalculator.fiscalResultForTheYear(
                bd("4145.86"), bd("0"), bd("14105.91"), bd("0"));
        Assert.assertEquals(fiscal, bd("18251.77"));

        BigDecimal tax = ChangeOfferCalculator.estimatedTaxOnYearsProfit(fiscal);
        Assert.assertEquals(tax, bd("3759.86"));

        BigDecimal offer = ChangeOfferCalculator.suggestedOfferPriceSek(bd("234831.97"));
        Assert.assertEquals(offer, bd("225400.00"));

        BigDecimal fee = ChangeOfferCalculator.fee(bd("234831.97"), bd("225400.00"));
        Assert.assertEquals(fee, bd("9431.97"));
    }

    @Test
    public void example4NegativeFiscalYieldsZeroTax() {
        BigDecimal fiscal = ChangeOfferCalculator.fiscalResultForTheYear(
                bd("-105423.52"), bd("0"), bd("54347.19"), bd("0"));
        Assert.assertEquals(fiscal, bd("-51076.33"));
        Assert.assertEquals(ChangeOfferCalculator.estimatedTaxOnYearsProfit(fiscal), bd("0.00"));
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
