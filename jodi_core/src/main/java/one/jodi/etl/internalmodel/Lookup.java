package one.jodi.etl.internalmodel;

import one.jodi.core.extensions.strategies.KnowledgeModuleStrategy;
import one.jodi.core.extensions.strategies.ModelCodeStrategy;

import java.util.List;
import java.util.Map;

/**
 * The Lookup element defines the lookup of one value based on a join condition between the enclosing {@link Source}
 * and a lookup data store. It has optional elements to overwrite the project-wide definition of model for
 * the lookup data store.
 */
public interface Lookup {
    /**
     * Convenience method used to fetch parent object.
     *
     * @return parent
     */
    Source getParent();

    /**
     * Indicates if the SQL expression defined by lookup e is incorporated as subselect query in the SQL expression generated by Transformation.
     *
     * @return uses subselect
     */
    boolean isSubSelect();

    /**
     * Describes the model used for lookup's data store.  This may be specified in <code>Transformation/Datasets/Dataset/Source/Lookup/Model</code>
     * or, when not explicitly specified, derived by Jodi using the {@link ModelCodeStrategy}
     *
     * @return model name
     */
    String getModel();

    /**
     * Fetches the name of the data store used to obtain lookup result value.
     *
     * @return data store name
     */
    String getLookupDataStore();

    /**
     * Fetches the alias of the lookup.  This may be explicitly defined or assumed by Jodi using data store name {@link #getLookupDataStore()}
     *
     * @return data store alias
     */
    String getAlias();

    /**
     * Fetches SQL expression of join used between lookup and parent Source.  The expression uses aliases.
     *
     * @return join SQL expression
     */
    String getJoin();

    /**
     * Fetches lookup type used.
     *
     * @return lookup type
     */
    LookupTypeEnum getLookupType();

    /**
     * Fetches execution location for join operation.
     *
     * @return location
     */
    ExecutionLocationtypeEnum getJoinExecutionLocation();

    /**
     * Fetches the KM used to load data for lookup.  The may be explicitly defined in the input model or otherwise derived by Jodi
     * Extensions using the plug in {@link KnowledgeModuleStrategy}
     * may change that behavior.
     *
     * @return LKM
     */
    KmType getLkm();

    /**
     * The lookup refers to temporary data store.  This is computed by Jodi.
     * <p>
     * returns data store is temporary
     */
    boolean isTemporary();

    /**
     * Boolean value indicating whether the lookup is Journalized.
     */
    boolean isJournalized();

    String getComponentName();

    int getNumberOfLookupsInDataset();

    List<String> getSubscribers();

    String getJournalizedFilters();

    /**
     * Access the default row to be returned when no row matches through join condition.
     *
     * @return map of column name to expression, in column order.
     */
    Map<String, String> getDefaultRowColumns();
}