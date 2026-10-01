package com.qa.bolags.reporting;

import org.json.JSONObject;
import org.json.JSONArray;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.qa.bolags.utility.OrderDetailsCapture;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Captures HTTP 4xx/5xx responses from Chrome performance / CDP logs for execution reports.
 */
public final class HttpErrorCapture {

    private static final Logger LOG = LoggerFactory.getLogger(HttpErrorCapture.class);
    private static final int MAX_BODY_CHARS = 4000;
    private static final int MAX_ERRORS = 50;
        private static final int MAX_CURL_BODY_CHARS = 6000;
        private static final Pattern SENSITIVE_KEY = Pattern.compile(
                "(?i).*(password|passwd|token|secret|authorization|cookie|email|phone|mobile|personnummer|ssn).*");
        private static final Pattern SENSITIVE_QUERY = Pattern.compile(
                "(?i)([?&](?:access_?token|token|auth|authorization|password|secret|api_?key|session|cookie|email|phone|personnummer|ssn)=)[^&#]*");

    private static final List<HttpError> ERRORS = new CopyOnWriteArrayList<>();
    private static final Map<String, String> REQUEST_URLS = new ConcurrentHashMap<>();
    private static final Map<String, String> REQUEST_METHODS = new ConcurrentHashMap<>();
    private static final Map<String, String> REQUEST_CURL_HEADERS = new ConcurrentHashMap<>();
    private static final Map<String, String> REQUEST_CURL_BODIES = new ConcurrentHashMap<>();
    private static final Map<String, Double> REQUEST_START_TIMES = new ConcurrentHashMap<>();
    private static final Set<String> RESPONSES = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private static final AtomicInteger API_TOTAL = new AtomicInteger();
    private static final AtomicInteger API_SUCCESS = new AtomicInteger();
    private static final AtomicInteger API_4XX = new AtomicInteger();
    private static final AtomicInteger API_5XX = new AtomicInteger();

    private HttpErrorCapture() {
    }

    public static void reset() {
        ERRORS.clear();
        REQUEST_URLS.clear();
        REQUEST_METHODS.clear();
        REQUEST_CURL_HEADERS.clear();
        REQUEST_CURL_BODIES.clear();
        REQUEST_START_TIMES.clear();
        RESPONSES.clear();
        API_TOTAL.set(0);
        API_SUCCESS.set(0);
        API_4XX.set(0);
        API_5XX.set(0);
    }

    public static List<HttpError> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(ERRORS));
    }

    public static int apiTotal() { return API_TOTAL.get(); }
    public static int apiSuccess() { return API_SUCCESS.get(); }
    public static int api4xx() { return API_4XX.get(); }
    public static int api5xx() { return API_5XX.get(); }

    public static void drain(WebDriver driver) {
        if (!(driver instanceof ChromeDriver)) {
            return;
        }
        ChromeDriver chrome = (ChromeDriver) driver;
        List<LogEntry> batch = new ArrayList<>();
        try {
            for (LogEntry entry : chrome.manage().logs().get(LogType.PERFORMANCE)) {
                batch.add(entry);
            }
        } catch (Exception e) {
            LOG.debug("Performance log drain skipped: {}", e.getMessage());
            return;
        }
        OrderDetailsCapture.captureFromPerformanceLog(chrome, batch);
        ingest(chrome, batch);
    }

    public static void ingest(ChromeDriver chrome, List<LogEntry> batch) {
        if (batch == null || batch.isEmpty()) {
            return;
        }
        for (LogEntry entry : batch) {
            JSONObject message = parseMessage(entry);
            if (message == null) {
                continue;
            }
            String method = message.optString("method", "");
            JSONObject params = message.optJSONObject("params");
            if (params == null) {
                continue;
            }
            if ("Network.requestWillBeSent".equals(method)) {
                recordRequest(params);
            } else if ("Network.responseReceived".equals(method)) {
                recordResponse(chrome, params);
            }
        }
    }

    private static void recordRequest(JSONObject params) {
        String rid = params.optString("requestId", "");
        JSONObject req = params.optJSONObject("request");
        if (rid.isEmpty() || req == null) {
            return;
        }
        REQUEST_METHODS.put(rid, req.optString("method", "GET"));
        REQUEST_START_TIMES.put(rid, params.optDouble("timestamp", -1));
        REQUEST_CURL_HEADERS.put(rid, safeCurlHeaders(req.optJSONObject("headers")));
        REQUEST_CURL_BODIES.put(rid, safeCurlBody(req.optString("postData", "")));
        String url = req.optString("url", "");
        if (!url.isEmpty()) {
            REQUEST_URLS.put(rid, url);
        }
    }

    private static void recordResponse(ChromeDriver chrome, JSONObject params) {
        JSONObject response = params.optJSONObject("response");
        if (response == null) {
            return;
        }
        String requestId = params.optString("requestId", "");
        if (requestId.isEmpty() || !RESPONSES.add(requestId)) {
            return;
        }
        int status = response.optInt("status", 0);
        String resourceType = params.optString("type", RunMetadata.NA);
        if ("XHR".equalsIgnoreCase(resourceType) || "Fetch".equalsIgnoreCase(resourceType)) {
            API_TOTAL.incrementAndGet();
            if (status >= 200 && status < 400) {
                API_SUCCESS.incrementAndGet();
            }
            if (status >= 400 && status < 500) {
                API_4XX.incrementAndGet();
            } else if (status >= 500) {
                API_5XX.incrementAndGet();
            }
        }
        if (status < 400 || ERRORS.size() >= MAX_ERRORS) {
            return;
        }
        String url = response.optString("url", "");
        if (url.isEmpty()) {
            url = REQUEST_URLS.getOrDefault(requestId, "");
        }
        String httpMethod = REQUEST_METHODS.getOrDefault(requestId, "");
        String body = fetchBody(chrome, requestId);
        double started = REQUEST_START_TIMES.getOrDefault(requestId, -1.0);
        double responded = params.optDouble("timestamp", -1);
        long responseTimeMs = started < 0 || responded < started ? -1
            : Math.round((responded - started) * 1000);
        String curl = buildCurl(httpMethod, url, REQUEST_CURL_HEADERS.getOrDefault(requestId, ""),
            REQUEST_CURL_BODIES.getOrDefault(requestId, ""));
        HttpError error = new HttpError(httpMethod, sanitizeUrl(url), status, body, resourceType,
            responseTimeMs, curl);
        ERRORS.add(error);
        LOG.warn("HTTP {} {} {}", status, httpMethod, sanitizeUrl(url));
    }

    private static String fetchBody(ChromeDriver chrome, String requestId) {
        if (requestId == null || requestId.isEmpty() || chrome == null) {
            return "";
        }
        try {
            Map<String, Object> cmd = new HashMap<>();
            cmd.put("requestId", requestId);
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = chrome.executeCdpCommand("Network.getResponseBody", cmd);
            if (raw == null || !(raw.get("body") instanceof String)) {
                return "";
            }
            String body = (String) raw.get("body");
            if (Boolean.TRUE.equals(raw.get("base64Encoded"))) {
                body = new String(Base64.getDecoder().decode(body), StandardCharsets.UTF_8);
            }
            if (body.length() > MAX_BODY_CHARS) {
                return body.substring(0, MAX_BODY_CHARS) + "…";
            }
            return body;
        } catch (Exception e) {
            return "";
        }
    }

    static String sanitizeUrl(String url) {
        if (url == null) {
            return "";
        }
        return SENSITIVE_QUERY.matcher(url.replaceAll("://[^/@\\s]+@", "://"))
                .replaceAll("$1[REDACTED]");
    }

    private static String safeCurlHeaders(JSONObject headers) {
        if (headers == null) {
            return "";
        }
        StringBuilder safe = new StringBuilder();
        for (String name : headers.keySet()) {
            String lower = name.toLowerCase();
            if (SENSITIVE_KEY.matcher(lower).matches() || lower.startsWith("sec-")
                    || "host".equals(lower) || "content-length".equals(lower)
                    || "connection".equals(lower) || "origin".equals(lower)
                    || "referer".equals(lower)) {
                continue;
            }
            Object value = headers.opt(name);
            if (value != null && value != JSONObject.NULL) {
                safe.append(name).append(": ").append(String.valueOf(value)).append('\n');
            }
        }
        return safe.toString();
    }

    private static String safeCurlBody(String body) {
        if (body == null || body.isEmpty() || body.length() > MAX_CURL_BODY_CHARS) {
            return "";
        }
        try {
            if (body.trim().startsWith("{")) {
                return redactJson(new JSONObject(body)).toString();
            }
            if (body.trim().startsWith("[")) {
                return redactJson(new JSONArray(body)).toString();
            }
        } catch (Exception e) {
            return "";
        }
        if (body.contains("=") && body.matches("[A-Za-z0-9%._~+&=\\-]*")) {
            return redactFormBody(body);
        }
        return "";
    }

    private static String redactFormBody(String body) {
        StringBuilder safe = new StringBuilder();
        String[] pairs = body.split("&", -1);
        for (int i = 0; i < pairs.length; i++) {
            if (i > 0) {
                safe.append('&');
            }
            String pair = pairs[i];
            int separator = pair.indexOf('=');
            String key = separator < 0 ? pair : pair.substring(0, separator);
            String decodedKey;
            try {
                decodedKey = java.net.URLDecoder.decode(key, "UTF-8");
            } catch (Exception e) {
                decodedKey = key;
            }
            safe.append(key);
            if (separator >= 0) {
                safe.append('=');
                safe.append(SENSITIVE_KEY.matcher(decodedKey).matches()
                        ? "%5BREDACTED%5D" : pair.substring(separator + 1));
            }
        }
        return safe.toString();
    }

    private static JSONObject redactJson(JSONObject source) {
        JSONObject redacted = new JSONObject();
        for (String key : source.keySet()) {
            Object value = source.opt(key);
            redacted.put(key, SENSITIVE_KEY.matcher(key).matches() ? "[REDACTED]" : redactJsonValue(value));
        }
        return redacted;
    }

    private static JSONArray redactJson(JSONArray source) {
        JSONArray redacted = new JSONArray();
        for (int i = 0; i < source.length(); i++) {
            redacted.put(redactJsonValue(source.opt(i)));
        }
        return redacted;
    }

    private static Object redactJsonValue(Object value) {
        if (value instanceof JSONObject) {
            return redactJson((JSONObject) value);
        }
        if (value instanceof JSONArray) {
            return redactJson((JSONArray) value);
        }
        return value;
    }

    private static String buildCurl(String method, String url, String headers, String body) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        StringBuilder curl = new StringBuilder("curl --location --request ")
                .append(shellQuote(method == null || method.isEmpty() ? "GET" : method.toUpperCase()))
                .append(" ").append(shellQuote(sanitizeUrl(url)));
        if (headers != null && !headers.isEmpty()) {
            for (String line : headers.split("\\R")) {
                if (!line.isEmpty()) {
                    curl.append(" --header ").append(shellQuote(line));
                }
            }
        }
        if (body != null && !body.isEmpty()) {
            curl.append(" --data-raw ").append(shellQuote(body));
        }
        return curl.toString();
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private static JSONObject parseMessage(LogEntry entry) {
        try {
            JSONObject root = new JSONObject(entry.getMessage());
            JSONObject message = root.optJSONObject("message");
            return message != null ? message : root;
        } catch (Exception e) {
            return null;
        }
    }

    public static final class HttpError {
        public final String method;
        public final String url;
        public final int status;
        public final String body;
        public final String resourceType;
        public final long responseTimeMs;
        public final String curlCommand;

        HttpError(String method, String url, int status, String body) {
            this(method, url, status, body, RunMetadata.NA, -1, "");
        }

        HttpError(String method, String url, int status, String body, String resourceType,
                  long responseTimeMs, String curlCommand) {
            this.method = method == null ? "" : method;
            this.url = url == null ? "" : url;
            this.status = status;
            this.body = body == null ? "" : body;
            this.resourceType = resourceType == null ? RunMetadata.NA : resourceType;
            this.responseTimeMs = responseTimeMs;
            this.curlCommand = curlCommand == null ? "" : curlCommand;
        }
    }
}
