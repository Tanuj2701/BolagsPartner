package com.qa.bolags.constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Test-scoped holder for the order id returned by {@code POST .../companyLiquidationOrders/saveInitial}.
 * Scenario 1 (liquidation UI) populates it; Scenario 2 (admin) reads it for
 * {@code /app/genericOrder/list/{orderId}}. Same JVM as {@code mvn test} with features ordered liquidation → admin.
 */
public final class LiquidationOrderIdContext {

    private static final Logger LOG = LoggerFactory.getLogger(LiquidationOrderIdContext.class);

    private static volatile String capturedOrderId;

    /** Mirrored to {@link System#setProperty(String, String)} for tooling / debugging. */
    public static final String SYSTEM_PROPERTY_KEY = "liquidation.saveInitial.orderId";

    private static final Path PERSIST_PATH =
            Paths.get(System.getProperty("user.dir"), "target", "e2e-liquidation-order-id.properties");

    private LiquidationOrderIdContext() {
    }

    public static void setCapturedOrderId(String orderId) {
        if (orderId == null || orderId.trim().isEmpty()) {
            return;
        }
        String trimmed = orderId.trim();
        capturedOrderId = trimmed;
        System.setProperty(SYSTEM_PROPERTY_KEY, trimmed);
        persistToFile(trimmed);
        LOG.info("Stored liquidation saveInitial orderId for downstream scenarios: {}", trimmed);
    }

    /** In-memory or JVM system property from this run (not loaded from file — avoids stale ids). */
    public static String getCapturedOrderIdOrNull() {
        if (capturedOrderId != null && !capturedOrderId.isEmpty()) {
            return capturedOrderId;
        }
        String fromSys = System.getProperty(SYSTEM_PROPERTY_KEY, "").trim();
        return fromSys.isEmpty() ? null : fromSys;
    }

    public static void clear() {
        capturedOrderId = null;
        System.clearProperty(SYSTEM_PROPERTY_KEY);
        try {
            Files.deleteIfExists(PERSIST_PATH);
        } catch (IOException ignored) {
            // best-effort
        }
    }

    private static void persistToFile(String orderId) {
        try {
            Files.createDirectories(PERSIST_PATH.getParent());
            Properties p = new Properties();
            p.setProperty(SYSTEM_PROPERTY_KEY, orderId);
            try (OutputStream out = Files.newOutputStream(PERSIST_PATH)) {
                p.store(out, "Last POST saveInitial orderId (for manual inspection only)");
            }
        } catch (IOException e) {
            LOG.warn("Could not persist order id to {}: {}", PERSIST_PATH, e.getMessage());
        }
    }
}
