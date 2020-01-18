package one.jodi.etl.internalmodel;

public enum AggregateFunctionEnum {
    MIN("MIN"),
    SUM("SUM"),
    AVG("AVG"),
    COUNT("COUNT"),
    MAX("MAX");

    private final String value;

    AggregateFunctionEnum(String v) {
        value = v;
    }

    public static AggregateFunctionEnum fromValue(String v) {
        for (AggregateFunctionEnum c : AggregateFunctionEnum.values()) {
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
