package one.jodi.etl.internalmodel;


/**
 * Enumeration used to specify where ODI should perform work.
 *
 */
public enum ExecutionLocationtypeEnum {

    SOURCE,
    WORK,
    TARGET;

    public static ExecutionLocationtypeEnum fromValue(String v)
            throws IllegalArgumentException {
        return valueOf(v);
    }

    public String value() {
        return name();
    }

}
