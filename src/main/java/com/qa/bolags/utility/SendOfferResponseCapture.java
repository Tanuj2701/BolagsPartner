package com.qa.bolags.utility;

import com.qa.bolags.constants.AcceptOfferContext;
import org.json.JSONObject;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Captures {@code orderId} and {@code md5Token} from {@code PUT .../companyLiquidationOrders/{id}/sendOffer}
 * via Chrome performance logs and CDP {@code Network.getResponseBody}.
 */
public final class SendOfferResponseCapture {

    private static final Logger LOG = LoggerFactory.getLogger(SendOfferResponseCapture.class);

    private static final String SEND_OFFER_PATH_FRAGMENT = "/sendOffer";

    private static final Set<String> consumedRequestIds = new HashSet<>();

    private static final Map<String, String> cumulativeRequestIdToMethod = new ConcurrentHashMap<>();
    private static final Map<String, String> cumulativeRequestIdToUrl = new ConcurrentHashMap<>();

    private SendOfferResponseCapture() {
    }

    public static void resetForNewBrowserSession() {
        cumulativeRequestIdToMethod.clear();
        cumulativeRequestIdToUrl.clear();
        consumedRequestIds.clear();
    }

    public static int debugCumulativeRequestMapSize() {
        return cumulativeRequestIdToMethod.size();
    }

    /**
     * Polls until the PUT sendOffer response is found and parsed, then {@link AcceptOfferContext#setOfferAcceptanceData}.
     *
     * @return true if stored
     */
    public static boolean pollAndStoreFromSendOfferChrome(WebDriver driver, int maxWaitSeconds) {
        if (!(driver instanceof ChromeDriver)) {
            LOG.warn("sendOffer capture skipped: driver is not ChromeDriver");
            return false;
        }
        ChromeDriver chrome = (ChromeDriver) driver;
        long deadline = System.currentTimeMillis() + maxWaitSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            List<LogEntry> batch = drainPerformanceLog(chrome);
            mergeRequestWillBeSentIntoCumulative(batch);
            Optional<OfferAcceptanceData> data = tryExtractFromResponseBatch(chrome, batch);
            if (!data.isPresent()) {
                data = retryBodiesForKnownSendOfferRequests(chrome);
            }
            if (data.isPresent()) {
                AcceptOfferContext.setOfferAcceptanceData(data.get().orderId, data.get().md5Token);
                return true;
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        LOG.warn("Timed out after {}s waiting for PUT {} with orderId/md5Token (cumulative request map size={})",
                maxWaitSeconds, SEND_OFFER_PATH_FRAGMENT, cumulativeRequestIdToMethod.size());
        return false;
    }

    private static List<LogEntry> drainPerformanceLog(ChromeDriver chrome) {
        List<LogEntry> list = new ArrayList<>();
        for (LogEntry entry : chrome.manage().logs().get(LogType.PERFORMANCE)) {
            list.add(entry);
        }
        return list;
    }

    private static void mergeRequestWillBeSentIntoCumulative(List<LogEntry> batch) {
        for (LogEntry entry : batch) {
            JSONObject message = parseDevToolsMessage(entry);
            if (message == null || !"Network.requestWillBeSent".equals(message.optString("method", ""))) {
                continue;
            }
            JSONObject params = message.optJSONObject("params");
            if (params == null) {
                continue;
            }
            String rid = params.optString("requestId", "");
            JSONObject req = params.optJSONObject("request");
            if (rid.isEmpty() || req == null) {
                continue;
            }
            cumulativeRequestIdToMethod.put(rid, req.optString("method", "GET").toUpperCase());
            String url = req.optString("url", "");
            if (!url.isEmpty()) {
                cumulativeRequestIdToUrl.put(rid, url);
            }
        }
    }

    private static Optional<OfferAcceptanceData> tryExtractFromResponseBatch(ChromeDriver chrome, List<LogEntry> batch) {
        for (LogEntry entry : batch) {
            JSONObject message = parseDevToolsMessage(entry);
            if (message == null || !"Network.responseReceived".equals(message.optString("method", ""))) {
                continue;
            }
            JSONObject params = message.optJSONObject("params");
            if (params == null) {
                continue;
            }
            String requestId = params.optString("requestId", "");
            if (requestId.isEmpty() || consumedRequestIds.contains(requestId)) {
                continue;
            }
            JSONObject response = params.optJSONObject("response");
            if (response == null) {
                continue;
            }
            String url = response.optString("url", "");
            if (url.isEmpty()) {
                url = cumulativeRequestIdToUrl.getOrDefault(requestId, "");
            }
            if (!url.contains(SEND_OFFER_PATH_FRAGMENT)) {
                continue;
            }
            int status = response.optInt("status", 0);
            if (status < 200 || status >= 300) {
                continue;
            }
            String method = cumulativeRequestIdToMethod.getOrDefault(requestId, "").toUpperCase();
            if (!method.isEmpty() && !"PUT".equals(method)) {
                continue;
            }
            Optional<String> body = fetchResponseBodyWithRetry(chrome, requestId);
            if (!body.isPresent()) {
                continue;
            }
            Optional<OfferAcceptanceData> parsed = parseOfferAcceptanceFromJsonBody(body.get());
            if (parsed.isPresent()) {
                consumedRequestIds.add(requestId);
                return parsed;
            }
        }
        return Optional.empty();
    }

    /**
     * Chrome clears the performance buffer on each read. If {@code responseReceived} was seen but
     * {@code Network.getResponseBody} was not ready yet, retry from the cumulative sendOffer request map.
     */
    private static Optional<OfferAcceptanceData> retryBodiesForKnownSendOfferRequests(ChromeDriver chrome) {
        for (Map.Entry<String, String> entry : cumulativeRequestIdToUrl.entrySet()) {
            String requestId = entry.getKey();
            if (consumedRequestIds.contains(requestId) || !entry.getValue().contains(SEND_OFFER_PATH_FRAGMENT)) {
                continue;
            }
            String method = cumulativeRequestIdToMethod.getOrDefault(requestId, "PUT").toUpperCase();
            if (!method.isEmpty() && !"PUT".equals(method)) {
                continue;
            }
            Optional<String> body = fetchResponseBodyOnce(chrome, requestId);
            if (!body.isPresent()) {
                continue;
            }
            Optional<OfferAcceptanceData> parsed = parseOfferAcceptanceFromJsonBody(body.get());
            if (parsed.isPresent()) {
                consumedRequestIds.add(requestId);
                return parsed;
            }
        }
        return Optional.empty();
    }

    private static JSONObject parseDevToolsMessage(LogEntry entry) {
        try {
            JSONObject root = new JSONObject(entry.getMessage());
            JSONObject message = root.optJSONObject("message");
            if (message != null) {
                return message;
            }
            if (root.has("method")) {
                return root;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static Optional<String> fetchResponseBodyWithRetry(ChromeDriver chrome, String requestId) {
        for (int attempt = 0; attempt < 12; attempt++) {
            Optional<String> body = fetchResponseBodyOnce(chrome, requestId);
            if (body.isPresent()) {
                return body;
            }
            try {
                Thread.sleep(120);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private static Optional<String> fetchResponseBodyOnce(ChromeDriver chrome, String requestId) {
        try {
            Map<String, Object> cmd = new HashMap<>();
            cmd.put("requestId", requestId);
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = chrome.executeCdpCommand("Network.getResponseBody", cmd);
            if (raw == null) {
                return Optional.empty();
            }
            Object bodyObj = raw.get("body");
            if (!(bodyObj instanceof String)) {
                return Optional.empty();
            }
            String body = (String) bodyObj;
            if (Boolean.TRUE.equals(raw.get("base64Encoded"))) {
                body = new String(Base64.getDecoder().decode(body), java.nio.charset.StandardCharsets.UTF_8);
            }
            return Optional.of(body);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    static Optional<OfferAcceptanceData> parseOfferAcceptanceFromJsonBody(String json) {
        if (json == null || json.isEmpty()) {
            return Optional.empty();
        }
        try {
            JSONObject root = new JSONObject(json);
            OfferAcceptanceData fromRoot = readOrderIdAndToken(root);
            if (fromRoot != null) {
                return Optional.of(fromRoot);
            }
            JSONObject nested = root.optJSONObject("data");
            if (nested != null) {
                OfferAcceptanceData fromData = readOrderIdAndToken(nested);
                if (fromData != null) {
                    return Optional.of(fromData);
                }
            }
        } catch (Exception ignored) {
            // fall through to regex
        }
        return parseOfferAcceptanceWithRegex(json);
    }

    private static OfferAcceptanceData readOrderIdAndToken(JSONObject obj) {
        String token = obj.optString("md5Token", "").trim();
        if (token.isEmpty()) {
            token = obj.optString("token", "").trim();
        }
        long orderIdLong = obj.optLong("orderId", 0L);
        if (token.isEmpty() || orderIdLong <= 0L) {
            return null;
        }
        return new OfferAcceptanceData(String.valueOf(orderIdLong), token);
    }

    private static Optional<OfferAcceptanceData> parseOfferAcceptanceWithRegex(String json) {
        java.util.regex.Matcher tokenMatcher = java.util.regex.Pattern.compile("\"md5Token\"\\s*:\\s*\"([^\"]+)\"")
                .matcher(json);
        java.util.regex.Matcher orderMatcher = java.util.regex.Pattern.compile("\"orderId\"\\s*:\\s*(\\d+)")
                .matcher(json);
        if (tokenMatcher.find() && orderMatcher.find()) {
            return Optional.of(new OfferAcceptanceData(orderMatcher.group(1), tokenMatcher.group(1)));
        }
        return Optional.empty();
    }

    static final class OfferAcceptanceData {
        final String orderId;
        final String md5Token;

        OfferAcceptanceData(String orderId, String md5Token) {
            this.orderId = orderId;
            this.md5Token = md5Token;
        }
    }
}
