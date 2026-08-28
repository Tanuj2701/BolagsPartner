package com.qa.bolags.utility;

import com.qa.bolags.constants.LiquidationOrderIdContext;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Captures {@code orderId} from {@code POST .../companyLiquidationOrders/saveInitial} via Chrome performance logs
 * and CDP {@code Network.getResponseBody}.
 * <p>Chrome clears the performance buffer on each read, so {@code requestWillBeSent} and {@code responseReceived}
 * may appear in different batches — we keep a cumulative {@code requestId → HTTP method} map across polls.</p>
 */
public final class SaveInitialOrderCapture {

    private static final Logger LOG = LoggerFactory.getLogger(SaveInitialOrderCapture.class);

    private static final Pattern ORDER_ID_IN_JSON = Pattern.compile("\"orderId\"\\s*:\\s*(\\d+)");

    private static final String SAVE_INITIAL_PATH_FRAGMENT = "companyLiquidationOrders/saveInitial";

    private static final Set<String> consumedRequestIdsSafe = new HashSet<>();

    /** Survives across performance-log reads (buffer clears each read). */
    private static final Map<String, String> cumulativeRequestIdToMethod = new ConcurrentHashMap<>();
    private static final Map<String, String> cumulativeRequestIdToUrl = new ConcurrentHashMap<>();

    private SaveInitialOrderCapture() {
    }

    /** Call when starting a new liquidation browser session (with {@link LiquidationOrderIdContext#clear()}). */
    public static void resetForNewBrowserSession() {
        cumulativeRequestIdToMethod.clear();
        cumulativeRequestIdToUrl.clear();
        consumedRequestIdsSafe.clear();
    }

    /** For assertion messages when capture fails. */
    public static int debugCumulativeRequestMapSize() {
        return cumulativeRequestIdToMethod.size();
    }

    public static void enableNetworkDomain(WebDriver driver) {
        if (!(driver instanceof ChromeDriver)) {
            return;
        }
        try {
            ((ChromeDriver) driver).executeCdpCommand("Network.enable", new HashMap<>());
        } catch (Exception e) {
            LOG.warn("Network.enable failed: {}", e.getMessage());
        }
    }

    /**
     * Polls until the POST saveInitial response is found and parsed, then {@link LiquidationOrderIdContext#setCapturedOrderId(String)}.
     *
     * @return true if stored
     */
    public static boolean pollAndStoreOrderIdFromChrome(WebDriver driver, int maxWaitSeconds) {
        if (!(driver instanceof ChromeDriver)) {
            LOG.warn("saveInitial capture skipped: driver is not ChromeDriver");
            return false;
        }
        ChromeDriver chrome = (ChromeDriver) driver;
        long deadline = System.currentTimeMillis() + maxWaitSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            List<LogEntry> batch = drainPerformanceLog(chrome);
            mergeRequestWillBeSentIntoCumulative(batch);
            Optional<String> id = tryExtractFromResponseBatch(chrome, batch);
            if (id.isPresent()) {
                LiquidationOrderIdContext.setCapturedOrderId(id.get());
                return true;
            }
            try {
                Thread.sleep(350);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        LOG.warn("Timed out after {}s waiting for POST {} with orderId (cumulative request map size={})",
                maxWaitSeconds, SAVE_INITIAL_PATH_FRAGMENT, cumulativeRequestIdToMethod.size());
        return false;
    }

    private static List<LogEntry> drainPerformanceLog(ChromeDriver chrome) {
        List<LogEntry> list = new ArrayList<LogEntry>();
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
            String method = req.optString("method", "GET").toUpperCase();
            cumulativeRequestIdToMethod.put(rid, method);
            String url = req.optString("url", "");
            if (!url.isEmpty()) {
                cumulativeRequestIdToUrl.put(rid, url);
            }
        }
    }

    private static Optional<String> tryExtractFromResponseBatch(ChromeDriver chrome, List<LogEntry> batch) {
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
            if (requestId.isEmpty() || consumedRequestIdsSafe.contains(requestId)) {
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
            if (!url.contains(SAVE_INITIAL_PATH_FRAGMENT)) {
                continue;
            }
            int status = response.optInt("status", 0);
            if (status < 200 || status >= 300) {
                continue;
            }
            String method = cumulativeRequestIdToMethod.getOrDefault(requestId, "").toUpperCase();
            // Initial order creation is POST; if method was lost across log reads, still try body (short JSON map).
            if (!method.isEmpty() && !"POST".equals(method)) {
                continue;
            }
            Optional<String> body = fetchResponseBodyWithRetry(chrome, requestId);
            if (!body.isPresent()) {
                continue;
            }
            Optional<String> orderId = parseOrderIdFromJsonBody(body.get());
            if (orderId.isPresent()) {
                consumedRequestIdsSafe.add(requestId);
                return orderId;
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

    static Optional<String> parseOrderIdFromJsonBody(String json) {
        if (json == null || json.isEmpty()) {
            return Optional.empty();
        }
        Matcher m = ORDER_ID_IN_JSON.matcher(json);
        if (m.find()) {
            return Optional.of(m.group(1));
        }
        return Optional.empty();
    }
}
