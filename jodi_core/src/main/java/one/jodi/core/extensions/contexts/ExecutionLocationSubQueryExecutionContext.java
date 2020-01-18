package one.jodi.core.extensions.contexts;

import one.jodi.core.extensions.strategies.ExecutionLocationStrategy;
import one.jodi.core.extensions.types.DataStoreWithAlias;
import one.jodi.etl.internalmodel.RoleEnum;

/**
 * This interface is used to provide contextual information for
 * {@link ExecutionLocationStrategy
 * ExecutionLocationStrategy} that is used to determine the execution locations
 * for lookups.
 *
 */
public interface ExecutionLocationSubQueryExecutionContext extends ExecutionLocationExecutionContext {

    /**
     * Gets the filter data store.
     *
     * @return the filter data store
     */
    DataStoreWithAlias getFilterDataStore();

    /**
     * Gets the source data stores to which the subquery filter is attached.
     *
     * @return the source data store
     */
    DataStoreWithAlias getSourceDataStore();


    /**
     * Gets the join condition of between either the source or previous flow item and filter table.
     *
     * @return the join condition
     */
    String getCondition();

    /**
     * Gets the role type of the subquery
     *
     * @return
     */
    RoleEnum getRole();

}
