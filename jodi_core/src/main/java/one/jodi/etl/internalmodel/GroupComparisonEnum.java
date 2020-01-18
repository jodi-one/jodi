package one.jodi.etl.internalmodel;

public enum GroupComparisonEnum {
    NONE("NONE"),
    Any("ANY"),
    Some("SOME"),
    All("ALL");


    private final String value;

    GroupComparisonEnum(String v) {
        value = v;
    }

    public static GroupComparisonEnum fromValue(String v) {
        for (GroupComparisonEnum c : GroupComparisonEnum.values()) {
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
