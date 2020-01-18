package one.jodi.base.service.bi.finder;

/**
 * Represents all errors that were detected while parsing the expression.
 *
 */
public interface FinderError {
    String getErrorMessage();

    ErrorCode getErrorCode();

    enum ErrorCode {
        TABLE_NOT_FOUND, COLUMN_NOT_FOUND, MULTIPLE_COLUMNS_FOUND
    }
}
