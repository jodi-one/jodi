package one.jodi.etl.internalmodel;

public interface ConditionConstraint extends Constraint {

    public Type getType();

    public String getWhere();

    public String getMessage();

    public enum Type {
        ORACLE_DATA_INTEGRATION_CONDITION("ODI_CONDITION"),
        DATABASE_CONDITION("DB_CONDITION");

        String value;

        Type(String value) {
            this.value = value;
        }

        public static Type fromValue(String v) {
            for (Type c : Type.values()) {
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
}