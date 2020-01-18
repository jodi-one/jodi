package one.jodi.core.validation.etl;

import one.jodi.core.extensions.strategies.ExecutionLocationStrategy;
import one.jodi.core.extensions.strategies.FolderNameStrategy;
import one.jodi.core.extensions.strategies.KnowledgeModuleStrategy;
import one.jodi.etl.internalmodel.*;

import java.util.List;
import java.util.Map;

/**
 * The Interface that defines the ETL Transformation validator API.
 * <p>
 * This interface should collect information between successive calls from any methods {@link #validateTransformationName(Transformation)}
 * in order to ensure conditions are met on a warehouse-wide basis (e.g. uniqueness for {@link Transformation#getPackageSequence()})
 *
 */
public interface ETLValidator {


    /**
     * Clear all errors and warnings.  This should also clear out history used for validating transformation name uniqueness.
     */
    public void reset();


    /**
     * Validate package association is correctly specified.
     *
     * @param transformation
     * @return true if valid
     */
    public boolean validatePackageAssociations(Transformation transformation);

    /**
     * Ensure that transformation name is unique.  Results are dumped to the ETLValidationResult instance consumed.
     * <p>
     * Note that successive calls to this method will fail; call {@link ETLValidator#clearTransformationSet()}.
     *
     * @param transformation
     */
    public boolean validateTransformationName(Transformation transformation);


    /**
     * Ensure that folder name is correct - non-empty.
     *
     * @param transformation
     * @return indicating valid or not
     */
    public boolean validateFolderName(Transformation transformation, FolderNameStrategy strategy);

    /**
     * @param packageSequence
     * @param fileName
     */
    public boolean validatePackageSequence(int packageSequence, String fileName);

    /**
     * Ensure the first dataset has no set operator defined.
     *
     * @param datasets of transformation
     * @param result
     */
    public boolean validateDataset(List<Dataset> datasets);

    /**
     * @param source
     * @param result
     */
    public boolean validateSubselect(Source source);


    /**
     * @param source
     * @param result
     * @return indicating valid or not
     */
    public boolean validateSourceDataStore(Source source);


    /**
     * Ensure that filter uses appropriate names and aliases  and
     * that filter conditions.
     *
     * @param source
     * @param result
     */
    public boolean validateFilter(Source source);

    /**
     * Ensure that filter uses DataStores and DataStoreColumns defined in ODI.
     *
     * @param source
     * @return indicating valid or not
     */
    public boolean validateFilterEnriched(Source source);


    /**
     * Validate explicit filter execution location values.
     *
     * @param source
     * @return true if valid
     */
    public boolean validateFilterExecutionLocation(Source source);

    /**
     * Validate enriched value of filter execution location.
     *
     * @param source
     * @param strategy strategy used to compute value
     * @return true if valid
     */
    public boolean validateFilterExecutionLocation(Source source, ExecutionLocationStrategy strategy);

    /**
     * Validate Join execution location.
     *
     * @param source
     * @return true if valid
     */
    public boolean validateJoinExecutionLocation(Source source);

    /**
     * Validate enriched value of filter execution location
     *
     * @param source
     * @param strategy plugin used
     * @return true if valid
     */
    public boolean validateJoinExecutionLocation(Source source, ExecutionLocationStrategy strategy);


    /**
     * Validate basic source name, alias and join type information.
     *
     * @param source
     * @param result
     */
    public boolean validateJoin(Source source);

    /**
     * Validate the join to ensure that each DataSource and DataSourceColumn exists in Jodi and
     * ensure that joins are performed across same types.
     *
     * @param source
     * @return indicating valid or not
     */
    public boolean validateJoinEnriched(Source source);


    /**
     * Validate explicit LKM information
     *
     * @param source
     * @return valid
     */
    public boolean validateLKM(Source source);

    /**
     * POST validate LKM specific information
     *
     * @param source
     * @param strategy the plugin used to compute LKM name option and values
     * @return valid
     */
    public boolean validateLKM(Source source, KnowledgeModuleStrategy strategy);


    /**
     * @param lookup
     * @param result
     */
    public boolean validateLookup(Lookup lookup);


    public boolean validateJoinEnriched(Lookup lookup);

    /**
     * Validate the join condition of lookup.  Independent of enrichment.
     *
     * @param lookup
     * @return indicating valid or not
     */
    public boolean validateLookupJoin(Lookup lookup);

    /**
     * @param mappings
     * @param result
     */
    public boolean validateIKM(Mappings mappings);

    /**
     * Perform post-enrichment validation of IKM settings.
     *
     * @param mappings
     * @param strategy custom strategy used to compute KM information
     * @return true, if errors encountered
     */
    public boolean validateIKM(Mappings mappings, KnowledgeModuleStrategy strategy);

    /**
     * Validate Staging Model
     *
     * @param mappings
     */
    public boolean validateStagingModel(Mappings mappings);

    /**
     * Perform pre-enrichment of CKM settings
     *
     * @param mappings
     * @param result
     */
    public boolean validateCKM(Mappings mappings);

    /**
     * Post enrichment validation of CKM settings.
     *
     * @param mappings
     * @param strategy
     * @return indicating valid or not
     */
    public boolean validateCKM(Mappings mappings, KnowledgeModuleStrategy strategy);

    /**
     * @param targetColumn
     * @param result
     */
    public boolean validateTargetColumn(Targetcolumn targetColumn);

    /**
     * Validate explicitly set join execution location.
     *
     * @param lookup
     * @return indicating valid or not
     */
    public boolean validateExecutionLocation(Lookup lookup);

    /**
     * Validate explicitly set subquery execution location.
     *
     * @param lookup
     * @return indicating valid or not
     */
    public boolean validateExecutionLocation(SubQuery subquery);

    /**
     * Validate change to {@link Lookup#getJoinExecutionLocation()} from referenced strategy
     *
     * @param lookup
     * @param strategy
     * @return indicating valid or not
     */
    public boolean validateExecutionLocation(Lookup lookup, ExecutionLocationStrategy strategy);


    public boolean validateJournalized(Transformation transformation);

    /**
     * Validate JKM options specified by model properties.  When strategy is non-null validation is assumed to
     * be for strategy-derived data.
     *
     * @param options
     * @return indicating valid or not
     */
    public boolean validateJournalizingOptions(String modelCode, String jkm, Map<String, String> options);

    public boolean validateJournalizingOptions(String modelCode, String jkm, Map<String, String> options, String strategyClassName);

    public boolean validateFlow(Flow flow);

    /*
     * Methods used to transform exceptions generated from plugin-in use to user-formatted messages.
     */
    public void handleTransformationName(Exception e, Transformation transformation);

    public void handleFolderName(Exception e, Transformation transformation);

    public void handleExecutionLocation(Exception e, Mappings mapping);

    public void handleModelCode(Exception e, Source source);

    public void handleFilterExecutionLocation(Exception e, Source source);

    public void handleJoinExecutionLocation(Exception e, Source source);

    public void handleLKM(Exception e, Source source);

    public void handleModelCode(Exception e, Lookup lookup);

    public void handleModelCode(Exception e, Mappings mappings);

    public void handleIKM(Exception e, Mappings mappings);

    public void handleStagingModel(Exception e, Mappings mappings);

    public void handleCKM(Exception e, Mappings mappings);

    public void handleColumnMapping(Exception e, Transformation transformation, String column);

    public void handleExecutionLocation(Exception e, Targetcolumn targetColumn);

    public void handleTargetColumnFlags(Exception e, Targetcolumn targetColumn);

    public void handleJournalizingDatastores(Exception e);

    public void handleJournalizingOptions(Exception e);

    public void handleJournalizingSubscribers(Exception e);

    public boolean validateLookupName(Lookup lookup);

    public boolean validateSourceDataStoreName(Source source);

    public boolean validateBeginAndEndMapping(Transformation transformation);

    public boolean validateLookupType(Lookup lookup);

    public boolean validateNoMatchRows(Lookup lookup);

    boolean validateJournalizing();

    boolean validateJKMOptions(String modelCode, String jkm, Map<String, Object> options);

    void validateTargetColumns(Mappings mapping);

}
