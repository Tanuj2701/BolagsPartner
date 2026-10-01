package com.qa.bolags.reporting;

/**
 * Evidence-based failure category. Application/UI is used only when the error is an assertion
 * on expected UI/state without locator, timeout, API, or environment signals.
 */
public final class FailureClassifier {

    private FailureClassifier() {
    }

    public static String classify(String exceptionType, String message, boolean has5xx, boolean has4xx) {
        String hay = ((exceptionType == null ? "" : exceptionType) + " " + (message == null ? "" : message))
                .toLowerCase();
        if (hay.contains("nosuchelement") || hay.contains("elementclickintercepted")
                || hay.contains("staleelement") || hay.contains("invalidselector")
                || hay.contains("locator")) {
            return "Application/UI";
        }
        if (hay.contains("timeout") || hay.contains("timeoutexception") || hay.contains("timed out")
                || hay.contains("waiting for")) {
            return "Environment";
        }
        if (hay.contains("unhandledalert") || hay.contains("session") || hay.contains("invalid session")
                || hay.contains("login") && hay.contains("401") || hay.contains("403")) {
            return "Application/Auth";
        }
        if (hay.contains("token") || hay.contains("testdata") || hay.contains("test data")
                || hay.contains("not captured") || hay.contains("missing data")
                || hay.contains("organisationsnummer") || hay.contains("fixture")) {
            return "Test Data";
        }
        if (has5xx || hay.contains("http 5") || hay.contains("status 5")) {
            return "API";
        }
        if (has4xx || hay.contains("http 4") || hay.contains("status 4")) {
            return "API";
        }
        if (hay.contains("unknownhost") || hay.contains("network") || hay.contains("connection refused")
                || hay.contains("err_connection") || hay.contains("dns")) {
            return "Environment";
        }
        if (hay.contains("chrome") && hay.contains("crashed") || hay.contains("webdriver")
                && hay.contains("unable to") || hay.contains("session deleted")) {
            return "Browser";
        }
        if (hay.contains("cucumber") || hay.contains("undefined step") || hay.contains("ambiguous")) {
            return "Automation Framework";
        }
        if (hay.contains("assertionerror") || hay.contains("assert") || hay.contains("expected")) {
            return "Application";
        }
        if (hay.contains("environment") || hay.contains("502") || hay.contains("503") || hay.contains("504")) {
            return "Environment";
        }
        return "Unknown";
    }

    public static String exceptionType(String stack) {
        if (stack == null || stack.isEmpty()) {
            return RunMetadata.NA;
        }
        String first = stack.split("\\R", 2)[0].trim();
        int colon = first.indexOf(':');
        String type = colon > 0 ? first.substring(0, colon) : first;
        return type.isEmpty() ? RunMetadata.NA : type;
    }
}
