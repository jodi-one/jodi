package one.jodi.core.extensions.contexts;

import one.jodi.core.extensions.strategies.ExecutionLocationStrategy;
import one.jodi.core.extensions.types.DataStoreWithAlias;
import one.jodi.model.extensions.SourceExtension;

import java.util.List;

/**
 * This interface is used to provide contextual information for
 * {@link ExecutionLocationStrategy
 * ExecutionLocationStrategy} that is used to determine the execution locations
 * for filters.
 */
public interface ExecutionLocationFilterExecutionContext extends ExecutionLocationExecutionContext {

    /**
     * Gets the filter condition.
     *
     * @return the filter condition
     */
    String getFilterCondition();

    /**
     * Gets the data stores with distinct aliases associated with the filter
     * condition. Entries in the list may represent the same data store but in
     * that case the respective aliases will different.
     * <p>
     * The main reason that identical data stores can be returned multiple times
     * is related to the definition of multiple Sources referring to the same
     * data store that have different aliases. This can occur when the same
     * underlying table needs to be joined with itself.
     *
     * @return data stores specified in separate sources in the XML
     * specification and references with their aliases in a filter
     * condition
     */
    public List<DataStoreWithAlias> getFilteredDataStores();

    /**
     * Gets source extension that is associated with the XML specification
     * source element the filter is associated.
     *
     * @return Extension element that may be customized as a consequence of XSD
     * customizations.
     */
    SourceExtension getSourceExtension();
}
