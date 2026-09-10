package com.qa.bolags.constants;

/**
 * Add-owner modal shareholder categories ({@code addShareHolder.tsx} radio combinations).
 */
public enum ShareholderVariantType {
    SWEDISH_PERSON,
    SWEDISH_LEGAL_ENTITY,
    FOREIGN_PERSON,
    FOREIGN_LEGAL_ENTITY;

    public static ShareholderVariantType fromFeatureLabel(String label) {
        if (label == null) {
            throw new IllegalArgumentException("shareholderType is required");
        }
        String normalized = label.trim().toLowerCase().replace(' ', '-').replace('_', '-');
        switch (normalized) {
            case "swedish-person":
            case "sv-person":
                return SWEDISH_PERSON;
            case "swedish-legal-entity":
            case "swedish-legal":
            case "sv-legal-entity":
                return SWEDISH_LEGAL_ENTITY;
            case "foreign-person":
            case "foreign-natural-person":
                return FOREIGN_PERSON;
            case "foreign-legal-entity":
            case "foreign-legal":
                return FOREIGN_LEGAL_ENTITY;
            default:
                throw new IllegalArgumentException("Unknown shareholderType: " + label);
        }
    }
}
