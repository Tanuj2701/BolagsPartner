package com.qa.bolags.constants;

/**
 * QA test data for accept-offer shareholder variants ({@code addShareHolder.tsx} / {@code displayPage.tsx}).
 */
public final class AcceptOfferShareholderTestData {

    public static final String DEFAULT_EMAIL = "tanujrasane23@gmail.com";

    /** Swedish natural person — {@code PersonForm} + citizenType Swedish. */
    public static final String SV_PERSON_SSN = "19781219-3519";
    public static final String SV_PERSON_FIRST = "Karl";
    public static final String SV_PERSON_LAST = "Tangby";
    public static final String SV_PERSON_STREET = "Main Street";
    public static final String SV_PERSON_CO = "Test Company";
    public static final String SV_PERSON_ZIP = "11129";
    public static final String SV_PERSON_CITY = "Stockholm";

    /** Swedish legal entity — {@code JuridiskForm} + citizenType Swedish (screenshot). */
    public static final String SV_LEGAL_ORG = "5563659948";
    public static final String SV_LEGAL_NAME = "Automation Legal Entity AB";
    public static final String SV_LEGAL_STREET = "Kolviksvagen 34";
    public static final String SV_LEGAL_CO = "Automation Co";
    public static final String SV_LEGAL_ZIP = "13933";
    public static final String SV_LEGAL_CITY = "Stockholm";

    /** Foreign natural person — {@code PersonForm} + citizenType Foreign. */
    public static final String FOREIGN_PERSON_NATIONALITY = "NO";
    public static final String FOREIGN_PERSON_DOB = "19850315";
    public static final String FOREIGN_PERSON_FIRST = "Erik";
    public static final String FOREIGN_PERSON_LAST = "Hansen";
    public static final String FOREIGN_PERSON_STREET = "Karl Johans gate 1";
    public static final String FOREIGN_PERSON_CO = "Nordic Holdings";
    public static final String FOREIGN_PERSON_ZIP = "0154";
    public static final String FOREIGN_PERSON_CITY = "Oslo";

    /** Foreign legal entity — {@code JuridiskForm} + citizenType Foreign. */
    public static final String FOREIGN_LEGAL_ORG = "GB12345678";
    public static final String FOREIGN_LEGAL_NAME = "Automation Foreign Ltd";
    public static final String FOREIGN_LEGAL_STREET = "10 Downing Street";
    public static final String FOREIGN_LEGAL_CO = "UK Rep Office";
    public static final String FOREIGN_LEGAL_ZIP = "SW1A1AA";
    public static final String FOREIGN_LEGAL_CITY = "London";
    public static final String FOREIGN_LEGAL_COUNTRY = "GB";

    /** Multi-shareholder scenario — unique identifiers per owner on same order. */
    public static final String MULTI_SV_PERSON_SSN = "19800101-0017";
    public static final String MULTI_SV_PERSON_FIRST = "Karl";
    public static final String MULTI_SV_PERSON_LAST = "Tangby";
    public static final String MULTI_SV_PERSON_EMAIL = "shareholder1.automation@test.com";

    public static final String MULTI_SV_LEGAL_ORG = "5567037495";
    public static final String MULTI_SV_LEGAL_NAME = "Multi Holder Legal AB";
    public static final String MULTI_SV_LEGAL_EMAIL = "shareholder2.automation@test.com";

    public static final String MULTI_FOREIGN_PERSON_NATIONALITY = "NO";
    public static final String MULTI_FOREIGN_PERSON_DOB = "19901220";
    public static final String MULTI_FOREIGN_PERSON_FIRST = "Anna";
    public static final String MULTI_FOREIGN_PERSON_LAST = "Berg";
    public static final String MULTI_FOREIGN_PERSON_EMAIL = "shareholder3.automation@test.com";

    public static final String MULTI_FOREIGN_LEGAL_ORG = "DE99887766";
    public static final String MULTI_FOREIGN_LEGAL_NAME = "Multi Foreign Holdings GmbH";
    public static final String MULTI_FOREIGN_LEGAL_EMAIL = "shareholder4.automation@test.com";

    private AcceptOfferShareholderTestData() {
    }
}
