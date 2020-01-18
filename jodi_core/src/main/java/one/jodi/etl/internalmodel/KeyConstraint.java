package one.jodi.etl.internalmodel;

import java.util.List;

public interface KeyConstraint extends Constraint {

    public List<String> getAttributes();

    public KeyType getKeyType();

    public enum KeyType {
        NOT_UNIQUE_INDEX("INDEX"),
        PRIMARY_KEY("PRIMARY_KEY"),
        ALTERNATE_KEY("ALTERNATE_KEY");

        String value;

        KeyType(String value) {
            this.value = value;
        }

        public static KeyType fromValue(String v) {
            for (KeyType c : KeyType.values()) {
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
