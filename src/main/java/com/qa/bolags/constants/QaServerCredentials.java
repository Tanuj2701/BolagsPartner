package com.qa.bolags.constants;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Loads QA HTTP Basic credentials from {@link Constants#CONFIGPROP} (and optional JVM overrides)
 * for embedding in navigation URLs so the browser server prompt is not shown.
 */
public final class QaServerCredentials {

    private static final String QA_HOST = "qa.bolagspartner.se";
    private static final String PROP_USER = "qa.server.basicAuth.user";
    private static final String PROP_PASSWORD = "qa.server.basicAuth.password";

    private static final Properties CACHED = new Properties();

    static {
        try (InputStream fromClasspath = QaServerCredentials.class.getClassLoader()
                .getResourceAsStream("config/config.properties")) {
            if (fromClasspath != null) {
                CACHED.load(fromClasspath);
            }
        } catch (IOException ignored) {
            // ignore
        }
        try (FileInputStream fromProject = new FileInputStream(Constants.CONFIGPROP)) {
            CACHED.load(fromProject);
        } catch (IOException ignored) {
            // ignore — JVM system properties and defaults still apply
        }
    }

    private QaServerCredentials() {
    }

    public static String qaHttpBasicUser() {
        String v = System.getProperty(PROP_USER);
        if (v != null && !v.trim().isEmpty()) {
            return v.trim();
        }
        return CACHED.getProperty(PROP_USER, "dev-bolagspartner");
    }

    public static String property(String key, String defaultValue) {
        String v = System.getProperty(key);
        if (v != null && !v.trim().isEmpty()) {
            return v.trim();
        }
        return CACHED.getProperty(key, defaultValue);
    }

    public static String genericOrderSuperAdminEmail() {
        return property("qa.app.genericOrder.superAdminEmail", "superAdmin@bolagspartner.se");
    }

    public static String genericOrderSuperAdminPassword() {
        return property("qa.app.genericOrder.superAdminPassword", "admin123");
    }

    public static String qaHttpBasicPassword() {
        String v = System.getProperty(PROP_PASSWORD);
        if (v != null && !v.trim().isEmpty()) {
            return v.trim();
        }
        return CACHED.getProperty(PROP_PASSWORD, "Bolags@123");
    }

    /**
     * If the URL targets the QA host, returns the same URL with {@code user:password@} after the scheme.
     * If credentials are already embedded, returns the URL unchanged.
     */
    public static String urlWithHttpBasicAuth(String url) {
        if (url == null || !url.contains(QA_HOST) || urlAlreadyHasUserInfo(url)) {
            return url;
        }
        try {
            String u = URLEncoder.encode(qaHttpBasicUser(), StandardCharsets.UTF_8.name());
            String p = URLEncoder.encode(qaHttpBasicPassword(), StandardCharsets.UTF_8.name());
            return url.replaceFirst("^(https?://)", "$1" + u + ":" + p + "@");
        } catch (Exception e) {
            return url;
        }
    }

    private static boolean urlAlreadyHasUserInfo(String url) {
        int scheme = url.indexOf("://");
        if (scheme < 0) {
            return false;
        }
        int from = scheme + 3;
        int at = url.indexOf('@', from);
        int slash = url.indexOf('/', from);
        return at > 0 && (slash < 0 || at < slash);
    }

    /**
     * Origin without userinfo, for Chrome flags that must not include embedded credentials.
     */
    public static String bareHttpsOriginForQa() {
        try {
            return URI.create("https://" + QA_HOST).toString().replaceAll("/$", "");
        } catch (Exception e) {
            return "https://" + QA_HOST;
        }
    }
}
