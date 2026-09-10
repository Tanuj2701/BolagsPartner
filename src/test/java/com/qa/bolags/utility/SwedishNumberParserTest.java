package com.qa.bolags.utility;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.math.BigDecimal;

public class SwedishNumberParserTest {

    @Test
    public void parsesUnicodeMinusAndSwedishDecimal() {
        Assert.assertEquals(
                SwedishNumberParser.parse("\u2212105\u00a0423,52"),
                new BigDecimal("-105423.52"));
    }

    @Test
    public void parsesEnglishThousandsWithDecimal() {
        Assert.assertEquals(
                SwedishNumberParser.parse("246,435.09"),
                new BigDecimal("246435.09"));
    }

    @Test
    public void parsesSwedishSpaceThousandsWithCommaDecimal() {
        Assert.assertEquals(
                SwedishNumberParser.parse("81\u00a0100,00"),
                new BigDecimal("81100.00"));
    }

    @Test
    public void parsesExample1Equity() {
        Assert.assertEquals(
                SwedishNumberParser.parse("234,831.97 kr"),
                new BigDecimal("234831.97"));
    }

    @Test
    public void parsesIntlSwedishFormattedPositive() {
        Assert.assertEquals(
                SwedishNumberParser.parse("234\u00a0831,97"),
                new BigDecimal("234831.97"));
    }

    @Test
    public void parsesIntlSwedishFormattedNegative() {
        Assert.assertEquals(
                SwedishNumberParser.parse("\u2212105\u00a0423,52"),
                new BigDecimal("-105423.52"));
    }

    @Test
    public void parseSafeNeverThrows() {
        Assert.assertEquals(SwedishNumberParser.parseSafe("\u2212"), BigDecimal.ZERO);
    }
}
