package com.qa.bolags.constants;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Valid offer-sent accounting rows for ChangeQuote (QA Manage order form).
 * A random row is chosen each run; pin with {@code -DofferSent.dataSetIndex=0..4}.
 */
public final class OfferSentDataProvider {

    public static final String DATA_SET_INDEX_PROPERTY = "offerSent.dataSetIndex";

    private static final List<OfferSentAccountingData> VALID_SETS = Collections.unmodifiableList(Arrays.asList(
            new OfferSentAccountingData(
                    "Set A — standard QA (original)",
                    "246435.09",
                    "7843.26",
                    "4145.86",
                    "0",
                    "0",
                    "14105.91",
                    "234831.97"),
            new OfferSentAccountingData(
                    "Set B — small active company",
                    "98500.50",
                    "4200.00",
                    "1850.25",
                    "0",
                    "0",
                    "5200.00",
                    "90550.50"),
            new OfferSentAccountingData(
                    "Set C — medium with untaxed reserves",
                    "156200.00",
                    "11200.00",
                    "6200.00",
                    "1500.00",
                    "0",
                    "9800.00",
                    "141500.00"),
            new OfferSentAccountingData(
                    "Set D — higher deductible costs",
                    "198750.25",
                    "9600.75",
                    "5100.40",
                    "0",
                    "0",
                    "11200.00",
                    "178949.50"),
            new OfferSentAccountingData(
                    "Set E — minimal liabilities",
                    "45000.00",
                    "850.00",
                    "1200.00",
                    "0",
                    "0",
                    "800.00",
                    "42350.00")));

    private OfferSentDataProvider() {
    }

    public static List<OfferSentAccountingData> allValidSets() {
        return Collections.unmodifiableList(VALID_SETS);
    }

    /**
     * Picks a data set for this test run (random unless {@link #DATA_SET_INDEX_PROPERTY} is set).
     */
    public static OfferSentAccountingData pickForExecution() {
        String indexProp = System.getProperty(DATA_SET_INDEX_PROPERTY, "").trim();
        if (!indexProp.isEmpty()) {
            int index = Integer.parseInt(indexProp);
            if (index < 0 || index >= VALID_SETS.size()) {
                throw new IllegalArgumentException(
                        DATA_SET_INDEX_PROPERTY + "=" + index + " out of range 0.." + (VALID_SETS.size() - 1));
            }
            return VALID_SETS.get(index);
        }
        int randomIndex = ThreadLocalRandom.current().nextInt(VALID_SETS.size());
        return VALID_SETS.get(randomIndex);
    }
}
