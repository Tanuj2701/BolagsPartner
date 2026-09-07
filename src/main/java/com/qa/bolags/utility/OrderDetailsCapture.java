package com.qa.bolags.utility;

import com.qa.bolags.constants.ClientDocumentTokenContext;
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
 * Captures {@code uploadDocumentToken} from {@code GET .../companyLiquidationOrders/orderDetails/{id}} responses.
 */
public final class OrderDetailsCapture {

    private static final Logger LOG = LoggerFactory.getLogger(OrderDetailsCapture.class);
    private static final String ORDER_DETAILS_FRAGMENT = "/orderDetails/";

    private static final Set<String> consumedRequestIds = new HashSet<>();
    private static final Map<String, String> cumulativeRequestIdToMethod = new ConcurrentHashMap<>();
    private static final Map<String, String> cumulativeRequestIdToUrl = new ConcurrentHashMap<>();

    private OrderDetailsCapture() {
    }

    public static void resetForNewBrowserSession() {
        cumulativeRequestIdToMethod.clear();
        cumulativeRequestIdToUrl.clear();
        consumedRequestIds.clear();
    }

    public static boolean pollAndStoreUploadDocumentToken(WebDriver driver, int maxWaitSeconds) {
        if (!(driver instanceof ChromeDriver)) {
            LOG.warn("orderDetails capture skipped: driver is not ChromeDriver");
            return false;
        }
        ChromeDriver chrome = (ChromeDriver) driver;
        long deadline = System.currentTimeMillis() + maxWaitSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            List<LogEntry> batch = drainPerformanceLog(chrome);
            mergeRequestWillBeSentIntoCumulative(batch);
            Optional<String> token = tryExtractUploadTokenFromResponseBatch(chrome, batch);
            if (token.isPresent()) {
                ClientDocumentTokenContext.setUploadDocumentToken(token.get());
                return true;
            }
            try {
                Thread.sleep(350);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        LOG.warn("Timed out after {}s waiting for orderDetails uploadDocumentToken", maxWaitSeconds);
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

    private static Optional<String> tryExtractUploadTokenFromResponseBatch(ChromeDriver chrome, List<LogEntry> batch) {
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
            if (!url.contains(ORDER_DETAILS_FRAGMENT)) {
                continue;
            }
            int status = response.optInt("status", 0);
            if (status < 200 || status >= 300) {
                continue;
            }
            Optional<String> body = fetchResponseBodyWithRetry(chrome, requestId);
            if (!body.isPresent()) {
                continue;
            }
            Optional<String> token = parseUploadTokenFromJsonBody(body.get());
            if (token.isPresent()) {
                consumedRequestIds.add(requestId);
                return token;
            }
        }
        return Optional.empty();
    }

    private static Optional<String> parseUploadTokenFromJsonBody(String json) {
        try {
            JSONObject root = new JSONObject(json);
            String token = root.optString("uploadDocumentToken", "").trim();
            if (!token.isEmpty()) {
                return Optional.of(token);
            }
        } catch (Exception e) {
            LOG.debug("Could not parse uploadDocumentToken: {}", e.getMessage());
        }
        return Optional.empty();
    }

    private static JSONObject parseDevToolsMessage(LogEntry entry) {
        try {
            JSONObject root = new JSONObject(entry.getMessage());
            JSONObject message = root.optJSONObject("message");
            return message != null ? message : (root.has("method") ? root : null);
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
}
