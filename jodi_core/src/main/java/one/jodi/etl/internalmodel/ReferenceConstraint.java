package one.jodi.etl.internalmodel;

import java.util.List;

public interface ReferenceConstraint extends Constraint {
    public Type getReferenceType();

    public String getPrimaryModel();

    public String getPrimaryTable();

    public List<ReferenceAttribute> getReferenceAttributes();

    public String getExpression();

    public Action getDeleteBehavior();

    public Action getUpdateBehavior();

    public enum Type {
        DATABASE_REFERENCE("DB_REFERENCE"),
        USER_REFERENCE("ODI_REFERENCE"),
        COMPLEX_USER_REFERENCE("COMPLEX_REFERENCE");

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

    public enum Action {
        RESTRICT,
        CASCADE,
        SET_NULL,
        NO_ACTION;
    }

}
