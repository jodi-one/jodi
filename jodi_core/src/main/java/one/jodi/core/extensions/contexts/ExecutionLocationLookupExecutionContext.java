package one.jodi.core.extensions.contexts;

import one.jodi.core.extensions.strategies.ExecutionLocationStrategy;
import one.jodi.core.extensions.types.DataStoreWithAlias;
import one.jodi.etl.internalmodel.LookupTypeEnum;

/**
 * This interface is used to provide contextual information for
 * {@link ExecutionLocationStrategy
 * ExecutionLocationStrategy} that is used to determine the execution locations
 * for lookups.
 */
public interface ExecutionLocationLookupExecutionContext extends ExecutionLocationExecutionContext {

    /**
     * Gets the lookup data store.
     *
     * @return the lookup data store
     */
    DataStoreWithAlias getLookupDataStore();

    /**
     * Gets the source data stores to which the lookup is attached.
     *
     * @return the source data store
     */
    DataStoreWithAlias getSourceDataStore();


    /**
     * Gets the lookup type
     *
     * @return the lookup type
     */
    LookupTypeEnum getLookupType();

    /**
     * Gets the join condition of between source and lookup table.
     *
     * @return the join condition
     */
    String getJoinCondition();

}
