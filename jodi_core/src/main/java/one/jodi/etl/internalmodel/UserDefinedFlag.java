package one.jodi.etl.internalmodel;


/**
 * Represents a named user-defined flag value.
 */
public interface UserDefinedFlag {

    /**
     * Gets the name of user-defined flag .
     *
     * @return the name
     */
    String getName();

    /**
     * Gets value associated with user-defined flag.
     *
     * @return 'true' indicates that the flag is set; 'false' indicates that the
     * flag is not set
     */
    boolean getValue();


    /**
     * @return Gets the UD number required for ODI
     */
    int getNumber();
}
