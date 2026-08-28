package com.qa.bolags.constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds the offer-sent accounting row selected for the current scenario (for logging and assertions).
 */
public final class OfferSentDataContext {

    private static final Logger LOG = LoggerFactory.getLogger(OfferSentDataContext.class);

    private static volatile OfferSentAccountingData selected;

    private OfferSentDataContext() {
    }

    public static void set(OfferSentAccountingData data) {
        selected = data;
        if (data != null) {
            LOG.info("Offer-sent data set for scenario: {}", data);
        }
    }

    public static OfferSentAccountingData getSelectedOrThrow() {
        if (selected == null) {
            throw new IllegalStateException(
                    "No offer-sent data set selected — call enterOfferSentAccountingData() first.");
        }
        return selected;
    }

    public static void clear() {
        selected = null;
    }
}
