package com.qa.bolags.constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds {@code orderId} and {@code md5Token} from the {@code PUT .../companyLiquidationOrders/{id}/sendOffer}
 * response for building the client accept-offer link ({@code /app/liqTok/acceptOffer}).
 */
public final class AcceptOfferContext {

    private static final Logger LOG = LoggerFactory.getLogger(AcceptOfferContext.class);

    private static final String QA_BASE = "https://qa.bolagspartner.se";
    private static final String UTM_QUERY =
            "utm_source=internal-reminder&utm_medium=email&utm_campaign=offer_email_accepted";

    public static final String ORDER_ID_PROPERTY_KEY = "liquidation.sendOffer.orderId";
    public static final String MD5_TOKEN_PROPERTY_KEY = "liquidation.sendOffer.md5Token";

    private static volatile String orderId;
    private static volatile String md5Token;

    private AcceptOfferContext() {
    }

    public static void setOfferAcceptanceData(String capturedOrderId, String capturedMd5Token) {
        if (capturedOrderId == null || capturedOrderId.trim().isEmpty()
                || capturedMd5Token == null || capturedMd5Token.trim().isEmpty()) {
            return;
        }
        orderId = capturedOrderId.trim();
        md5Token = capturedMd5Token.trim();
        System.setProperty(ORDER_ID_PROPERTY_KEY, orderId);
        System.setProperty(MD5_TOKEN_PROPERTY_KEY, md5Token);
        LOG.info("Stored sendOffer acceptance data — orderId={}, md5Token={}...", orderId, md5Token.substring(0, 8));
    }

    public static String getOrderIdOrNull() {
        if (orderId != null && !orderId.isEmpty()) {
            return orderId;
        }
        String fromSys = System.getProperty(ORDER_ID_PROPERTY_KEY, "").trim();
        return fromSys.isEmpty() ? null : fromSys;
    }

    public static String getMd5TokenOrNull() {
        if (md5Token != null && !md5Token.isEmpty()) {
            return md5Token;
        }
        String fromSys = System.getProperty(MD5_TOKEN_PROPERTY_KEY, "").trim();
        return fromSys.isEmpty() ? null : fromSys;
    }

    /**
     * {@code /app/liqTok/acceptOffer?orderId=...&token=...&utm_*} — same shape as offer email links.
     */
    public static String buildAcceptOfferUrl() {
        return buildTokenUrl("/app/liqTok/acceptOffer");
    }

    /** Client decline-offer link ({@code /app/liqTok/declineOffer}) — same token as sendOffer. */
    public static String buildDeclineOfferUrl() {
        return buildTokenUrl("/app/liqTok/declineOffer");
    }

    private static String buildTokenUrl(String path) {
        String oid = getOrderIdOrNull();
        String token = getMd5TokenOrNull();
        if (oid == null || token == null) {
            throw new IllegalStateException(
                    "sendOffer orderId/md5Token not captured — run send quote first or set "
                            + ORDER_ID_PROPERTY_KEY + " / " + MD5_TOKEN_PROPERTY_KEY);
        }
        return QA_BASE + path + "?orderId=" + oid + "&token=" + token + "&" + UTM_QUERY;
    }

    public static void clear() {
        orderId = null;
        md5Token = null;
        System.clearProperty(ORDER_ID_PROPERTY_KEY);
        System.clearProperty(MD5_TOKEN_PROPERTY_KEY);
    }
}
