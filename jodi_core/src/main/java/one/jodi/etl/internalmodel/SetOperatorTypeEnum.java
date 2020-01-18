package one.jodi.etl.internalmodel;

/**
 * The SetOperator defines the list of set operations.
 *
 */
public enum SetOperatorTypeEnum {

    NOT_DEFINED("NOT_DEFINED"),
    MINUS("MINUS"),
    UNION("UNION"),
    UNION_ALL("UNION ALL"),
    INTERSECT("INTERSECT");

    private final String value;

    SetOperatorTypeEnum(String v) {
        value = v;
    }

    public static SetOperatorTypeEnum fromValue(String v) {
        for (SetOperatorTypeEnum c : SetOperatorTypeEnum.values()) {
            if (c.value.equals(v.replace("_", " "))) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

    public String getValue() {
        return value;
    }

}
