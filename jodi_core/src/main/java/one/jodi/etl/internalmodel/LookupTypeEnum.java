package one.jodi.etl.internalmodel;

/**
 * Describe the lookup types that may be specified as part of a {@link Lookup}
 */
public enum LookupTypeEnum {

    LEFT_OUTER("LEFT OUTER"),
    SCALAR("SCALAR"),
    /* ODI 12 specific */
    ALL_ROWS("ALL_ROWS"),
    ANY_ROW("ANY_ROW"),
    ERROR_WHEN_MULTIPLE_ROW("ERROR_WHEN_MULTIPLE_ROW"),
    FIRST_ROW("FIRST_ROW"),
    LAST_ROW("LAST_ROW"),
    NTH_ROW("NTH_ROW");

    private final String value;

    LookupTypeEnum(String v) {
        value = v;
    }

    public static LookupTypeEnum fromValue(String v) {
        for (LookupTypeEnum c : LookupTypeEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

    public String getValue() {
        return value;
    }

}
