package one.jodi.base.service.bi.finder;

import one.jodi.base.service.bi.ast.ColumnWrapper;

import java.util.Optional;


/**
 * Represents the result of a call to the ColumnFinder. It represents the
 * identified column or the error code and error message.
 *
 */
public interface FinderResult {

    /**
     * @return column if one is found; otherwise empty() and getError()
     * returns value
     */
    Optional<ColumnWrapper> getColumn();

    /**
     * @return error message and code; otherwise empty()
     */
    Optional<FinderError> getError();
}
