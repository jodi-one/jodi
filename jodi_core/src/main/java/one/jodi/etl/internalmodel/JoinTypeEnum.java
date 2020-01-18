package one.jodi.etl.internalmodel;

/**
 * Describes a the suite of Join Types available in SQL.
 *
 */
public enum JoinTypeEnum {

    NOT_DEFINED("NOT_DEFINED"),
    INNER("INNER"),
    LEFT_OUTER("LEFT OUTER"),
    FULL("FULL"),
    CROSS("CROSS"),
    NATURAL("NATURAL");
    private final String value;

    JoinTypeEnum(String v) {
        value = v;
    }

    public static JoinTypeEnum fromValue(String v) {
        for (JoinTypeEnum c : JoinTypeEnum.values()) {
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
