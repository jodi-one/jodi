package one.jodi.etl.internalmodel;

public enum RoleEnum {
    EQUAL("EQUAL"),
    GREATER_OR_EQUAL("EQUAL_OR_GREATER"),
    LESS_OR_EQUAL("EQUAL_OR_LESS"),
    EXISTS("EXISTS"),
    GREATER("GREATER"),
    IN("IN"),
    LESS("LESS"),
    LESS_OR_GREATER("LESS_OR_GREATER"),
    NOT_EXISTS("NOT_EXISTS"),
    NOT_IN("NOT_IN"),
    UNEQUAL("UNEQUAL"),
    XOR("XOR"),
    XOR_BEFORE_EQUAL("XOR_BEFORE_EQUAL");
     
     /*
	EXISTS("Exists"),
	NOT_EXISTS("Not Exists"),
	IN("In"),
	NOT_IN("Not In"),
	GT(">"),
	LT("<"),
	GE(">="),
	LE("<="),
	NE_NE("!="),
	NE_LTGT("<>"),
	NE_CE("^=");*/

    private final String value;

    RoleEnum(String v) {
        value = v;
    }

    public static RoleEnum fromValue(String v) {
        for (RoleEnum c : RoleEnum.values()) {
            if (c.name().equals(v.replace(" ", "_"))) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

    public String getValue() {
        return value;
    }
}
