package one.jodi.core.extensions.types;


/**
 * Represents a named user-defined flag value.
 *
 */
public interface UserDefinedFlag {

    /**
     * Gets the name of user-defined flag .
     *
     * @return the name
     */
    public String getName();

    /**
     * Gets value associated with user-defined flag.
     *
     * @return 'true' indicates that the flag is set; 'false' indicates that the
     * flag is not set
     */
    public boolean getValue();
}
