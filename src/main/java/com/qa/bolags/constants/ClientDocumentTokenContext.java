package com.qa.bolags.constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds {@code uploadDocumentToken} for client document checklist URLs.
 */
public final class ClientDocumentTokenContext {

    private static final Logger LOG = LoggerFactory.getLogger(ClientDocumentTokenContext.class);
    private static final String QA_BASE = "https://qa.bolagspartner.se";
    public static final String TOKEN_PROPERTY_KEY = "liquidation.uploadDocumentToken";

    private static volatile String uploadDocumentToken;

    private ClientDocumentTokenContext() {
    }

    public static void setUploadDocumentToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return;
        }
        uploadDocumentToken = token.trim();
        System.setProperty(TOKEN_PROPERTY_KEY, uploadDocumentToken);
        LOG.info("Stored uploadDocumentToken (prefix {}...)", uploadDocumentToken.substring(0, Math.min(8, uploadDocumentToken.length())));
    }

    public static String getUploadDocumentTokenOrNull() {
        if (uploadDocumentToken != null && !uploadDocumentToken.isEmpty()) {
            return uploadDocumentToken;
        }
        String fromSys = System.getProperty(TOKEN_PROPERTY_KEY, "").trim();
        return fromSys.isEmpty() ? null : fromSys;
    }

    public static String buildClientChecklistUrl() {
        String orderId = LiquidationOrderIdContext.getCapturedOrderIdOrNull();
        String token = getUploadDocumentTokenOrNull();
        if (orderId == null || token == null) {
            throw new IllegalStateException(
                    "uploadDocumentToken or orderId missing — complete accept-offer flow first or set "
                            + TOKEN_PROPERTY_KEY + " and liquidation order id");
        }
        return QA_BASE + "/app/companyLiquidationOrder/show/" + orderId + "?token=" + token;
    }

    public static void clear() {
        uploadDocumentToken = null;
        System.clearProperty(TOKEN_PROPERTY_KEY);
    }
}
