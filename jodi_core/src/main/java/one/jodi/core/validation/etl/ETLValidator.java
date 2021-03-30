package one.jodi.core.validation.etl;

import one.jodi.core.extensions.strategies.ExecutionLocationStrategy;
import one.jodi.core.extensions.strategies.FolderNameStrategy;
import one.jodi.core.extensions.strategies.KnowledgeModuleStrategy;
import one.jodi.etl.internalmodel.Dataset;
import one.jodi.etl.internalmodel.Flow;
import one.jodi.etl.internalmodel.Lookup;
import one.jodi.etl.internalmodel.Mappings;
import one.jodi.etl.internalmodel.Source;
import one.jodi.etl.internalmodel.SubQuery;
import one.jodi.etl.internalmodel.Targetcolumn;
import one.jodi.etl.internalmodel.Transformation;

import java.util.List;
import java.util.Map;

/**
 * The Interface that defines the ETL Transformation validator API.
 * <p>
 * This interface should collect information between successive calls from any methods {@link #validateTransformationName(Transformation)}
 * in order to ensure conditions are met on a warehouse-wide basis (e.g. uniqueness for {@link Transformation#getPackageSequence()})
 */
public interface ETLValidator {

    /**
     * Clear all errors and warnings.  This should also clear out history used for validating transformation name uniqueness.
     */
    void reset();

    /**
     * Validate package association is correctly specified.
     *
     * @param transformation {@link Transformation}
     * @return true if valid
     */
    boolean validatePackageAssociations(Transformation transformation);

    /**
     * Ensure that transformation name is unique.  Results are dumped to the ETLValidationResult instance consumed.
     * <p>
     * Note that successive calls to this method will fail; call ETLValidator#clearTransformationSet().
     *
     * @param transformation {@link Transformation}
     */
    boolean validateTransformationName(Transformation transformation);


    /**
     * Ensure that folder name is correct - non-empty.
     *
     * @param transformation {@link Transformation}
     * @param strategy       {@link FolderNameStrategy}
     * @return indicating valid or not
     */
    boolean validateFolderName(Transformation transformation, FolderNameStrategy strategy);

    /**
     * @param packageSequence as int
     * @param fileName        is what is says it is
     */
    boolean validatePackageSequence(int packageSequence, String fileName);

    /**
     * Ensure the first dataset has no set operator defined.
     *
     * @param datasets of transformation
     * @return true if valid else false
     */
    boolean validateDataset(List<Dataset> datasets);

    /**
     * @param source {@link Source}
     * @return true if valid else false
     */
    boolean validateSubselect(Source source);


    /**
     * @param source {@link Source}
     * @return indicating valid or not
     */
    boolean validateSourceDataStore(Source source);


    /**
     * Ensure that filter uses appropriate names and aliases  and
     * that filter conditions.
     *
     * @param source {@link Source}
     * @return true if valid else false
     */
    boolean validateFilter(Source source);

    /**
     * Ensure that filter uses DataStores and DataStoreColumns defined in ODI.
     *
     * @param source {@link Source}
     * @return indicating valid or not
     */
    boolean validateFilterEnriched(Source source);


    /**
     * Validate explicit filter execution location values.
     *
     * @param source {@link Source}
     * @return true if valid
     */
    boolean validateFilterExecutionLocation(Source source);

    /**
     * Validate enriched value of filter execution location.
     *
     * @param source   {@link Source}
     * @param strategy strategy used to compute value
     * @return true if valid
     */
    boolean validateFilterExecutionLocation(Source source, ExecutionLocationStrategy strategy);

    /**
     * Validate Join execution location.
     *
     * @param source {@link Source}
     * @return true if valid
     */
    boolean validateJoinExecutionLocation(Source source);

    /**
     * Validate enriched value of filter execution location
     *
     * @param source   {@link Source}
     * @param strategy plugin used
     * @return true if valid
     */
    boolean validateJoinExecutionLocation(Source source, ExecutionLocationStrategy strategy);


    /**
     * Validate basic source name, alias and join type information.
     *
     * @param source {@link Source}
     * @return true if valid else false
     */
    boolean validateJoin(Source source);

    /**
     * Validate the join to ensure that each DataSource and DataSourceColumn exists in Jodi and
     * ensure that joins are performed across same types.
     *
     * @param source {@link Source}
     * @return indicating valid or not
     */
    boolean validateJoinEnriched(Source source);


    /**
     * Validate explicit LKM information
     *
     * @param source {@link Source}
     * @return valid
     */
    boolean validateLKM(Source source);

    /**
     * POST validate LKM specific information
     *
     * @param source   {@link Source}
     * @param strategy the plugin used to compute LKM name option and values
     * @return valid
     */
    boolean validateLKM(Source source, KnowledgeModuleStrategy strategy);


    /**
     * @param lookup {@link Lookup}
     * @return true if valid else false
     */
    boolean validateLookup(Lookup lookup);


    boolean validateJoinEnriched(Lookup lookup);

    /**
     * Validate the join condition of lookup.  Independent of enrichment.
     *
     * @param lookup {@link Lookup}
     * @return indicating valid or not
     */
    boolean validateLookupJoin(Lookup lookup);

    /**
     * @param mappings {@link Mappings}
     * @return true if valid else false
     */
    boolean validateIKM(Mappings mappings);

    /**
     * Perform post-enrichment validation of IKM settings.
     *
     * @param mappings {@link Mappings}
     * @param strategy custom strategy used to compute KM information
     * @return true, if errors encountered
     */
    boolean validateIKM(Mappings mappings, KnowledgeModuleStrategy strategy);

    /**
     * Validate Staging Model
     *
     * @param mappings {@link Mappings}
     */
    boolean validateStagingModel(Mappings mappings);

    /**
     * Perform pre-enrichment of CKM settings
     *
     * @param mappings {@link Mappings}
     * @return true if valid else false
     */
    boolean validateCKM(Mappings mappings);

    /**
     * Post enrichment validation of CKM settings.
     *
     * @param mappings {@link Mappings}
     * @param strategy {@link KnowledgeModuleStrategy}
     * @return indicating valid or not
     */
    boolean validateCKM(Mappings mappings, KnowledgeModuleStrategy strategy);

    /**
     * @param targetColumn {@link Targetcolumn}
     * @return true if valid else false
     */
    boolean validateTargetColumn(Targetcolumn targetColumn);

    /**
     * Validate explicitly set join execution location.
     *
     * @param lookup {@link Lookup}
     * @return indicating valid or not
     */
    boolean validateExecutionLocation(Lookup lookup);

    /**
     * Validate explicitly set subquery execution location.
     *
     * @param subquery {@link SubQuery}
     * @return indicating valid or not
     */
    boolean validateExecutionLocation(SubQuery subquery);

    /**
     * Validate change to {@link Lookup#getJoinExecutionLocation()} from referenced strategy
     *
     * @param lookup   {@link Lookup}
     * @param strategy {@link ExecutionLocationStrategy}
     * @return indicating valid or not
     */
    boolean validateExecutionLocation(Lookup lookup, ExecutionLocationStrategy strategy);


    boolean validateJournalized(Transformation transformation);

    /**
     * Validate JKM options specified by model properties.  When strategy is non-null validation is assumed to
     * be for strategy-derived data.
     *
     * @param options A Map containing the options
     * @return indicating valid or not
     */
    boolean validateJournalizingOptions(String modelCode, String jkm, Map<String, String> options);

    boolean validateJournalizingOptions(String modelCode, String jkm, Map<String, String> options, String strategyClassName);

    boolean validateFlow(Flow flow);

    /*
     * Methods used to transform exceptions generated from plugin-in use to user-formatted messages.
     */
    void handleTransformationName(Exception e, Transformation transformation);

    void handleFolderName(Exception e, Transformation transformation);

    void handleExecutionLocation(Exception e, Mappings mapping);

    void handleModelCode(Exception e, Source source);

    void handleFilterExecutionLocation(Exception e, Source source);

    void handleJoinExecutionLocation(Exception e, Source source);

    void handleLKM(Exception e, Source source);

    void handleModelCode(Exception e, Lookup lookup);

    void handleModelCode(Exception e, Mappings mappings);

    void handleIKM(Exception e, Mappings mappings);

    void handleStagingModel(Exception e, Mappings mappings);

    void handleCKM(Exception e, Mappings mappings);

    void handleColumnMapping(Exception e, Transformation transformation, String column);

    void handleExecutionLocation(Exception e, Targetcolumn targetColumn);

    void handleTargetColumnFlags(Exception e, Targetcolumn targetColumn);

    void handleJournalizingDatastores(Exception e);

    void handleJournalizingOptions(Exception e);

    void handleJournalizingSubscribers(Exception e);

    boolean validateLookupName(Lookup lookup);

    boolean validateSourceDataStoreName(Source source);

    boolean validateBeginAndEndMapping(Transformation transformation);

    boolean validateLookupType(Lookup lookup);

    boolean validateNoMatchRows(Lookup lookup);

    boolean validateJournalizing();

    boolean validateJKMOptions(String modelCode, String jkm, Map<String, Object> options);

    void validateTargetColumns(Mappings mapping);

}
