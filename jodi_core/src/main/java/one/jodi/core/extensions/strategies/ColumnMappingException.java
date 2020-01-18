package one.jodi.core.extensions.strategies;


/**
 * This exception is thrown in cases where either a column mapping cannot be determined
 * or where the mapping is ambiguous.
 *
 */
public class ColumnMappingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ColumnMappingException(String message) {
        super(message);
    }

}

