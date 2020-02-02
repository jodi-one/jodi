package one.jodi.odi.interfaces;

import one.jodi.etl.internalmodel.Lookup;
import one.jodi.etl.internalmodel.Source;
import one.jodi.etl.internalmodel.SubQuery;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.etl.service.ResourceFoundAmbiguouslyException;
import one.jodi.etl.service.ResourceNotFoundException;
import one.jodi.etl.service.interfaces.TransformationAccessStrategyException;
import oracle.odi.domain.IOdiEntity;
import oracle.odi.domain.IRepositoryEntity;
import oracle.odi.domain.mapping.MapRootContainer;
import oracle.odi.domain.model.OdiDataStore;
import oracle.odi.domain.model.OdiModel;
import oracle.odi.domain.project.OdiFolder;
import oracle.odi.domain.project.OdiProject;
import oracle.odi.domain.runtime.loadplan.OdiLoadPlan;
import oracle.odi.domain.topology.OdiTechnology;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @param <T> interface or mapping
 * @param <U> DataSet of Dataset
 * @param <V> SourceDataStore or DataStoreComponent
 * @param <W> Object or ReusableMappingComponent
 * @param <X> Object or IMapComponent
 * @param <Y> Object or OdiContext
 * @param <Z> Object or  ILogicalSchema
 */
public interface OdiTransformationAccessStrategy<T extends IOdiEntity, U extends IRepositoryEntity,
        V extends IRepositoryEntity, W extends Object,
        X extends Object, Y extends Object, Z extends Object> {
    final static String UD1 = "UD1";
    final static String UD2 = "UD2";
    final static String UD3 = "UD3";
    final static String UD4 = "UD4";
    final static String UD5 = "UD5";
    final static String UD6 = "UD6";
    final static String UD7 = "UD7";
    final static String UD8 = "UD8";
    final static String UD9 = "UD9";
    final static String UD10 = "UD10";
    final static String INSERT = "INSERT";
    final static String UPDATE = "UPDATE";
    final static String KEY = "KEY";
    final static String MANDATORY = "MANDATORY";


    @Deprecated
    V findSourceDataStore(T mapping,
                          Transformation transformation, int dataSetIndex, Source source)
            throws TransformationAccessStrategyException;

    @Deprecated
    V findSourceLookupDataStore(T odiInterface, Transformation transformation,
                                int dataSetIndex, Lookup lookup,
                                int dataSetNumberOfLoookup)
            throws TransformationAccessStrategyException;

    Collection<T> findMappingsByProject(String projectCode)
            throws ResourceNotFoundException, ResourceFoundAmbiguouslyException;

    T findMappingsByName(String name, String folder, String projectCode)
            throws ResourceNotFoundException, ResourceFoundAmbiguouslyException;

    // below method is quicker than above method
    T findMappingsByName(String lookupDataStore, String projectCode)
            throws ResourceNotFoundException, ResourceFoundAmbiguouslyException;

    Collection<U> findAllDataSets(T mapping) throws TransformationAccessStrategyException;

    boolean isOneOfTheSourcesDerived(T mapping) throws TransformationAccessStrategyException;

    boolean areAllDatastoresJoinedNaturally(T mapping)
            throws TransformationAccessStrategyException;

    Set<String> findStagingAreas(T mapping, String contextCode)
            throws TransformationAccessStrategyException;

    boolean validateDataSetRelation(T mapping) throws TransformationAccessStrategyException;

    X findSourceComponent(T mapping,
                          Transformation transformation, Source source)
            throws ResourceNotFoundException, TransformationAccessStrategyException;

    X findSourceComponent(final MapRootContainer mapping,
                          final SubQuery subquery) throws TransformationAccessStrategyException;

    X findLookupComponent(T mapping,
                          Transformation transformation, Lookup secondSource)
            throws ResourceNotFoundException, TransformationAccessStrategyException;

    int findSequenceNumberFromDataSet(U dataSet);

    OdiDataStore findDataStore(String tableName, String model)
            throws ResourceNotFoundException;

    OdiProject findProject(String projeccode) throws ResourceNotFoundException;

    List<U> findDatasets(T map) throws TransformationAccessStrategyException;

    int findGlobalVersion();

    OdiFolder findFolderByName(String name, String project)
            throws ResourceFoundAmbiguouslyException, ResourceNotFoundException;

    OdiModel findModel(String code) throws ResourceNotFoundException;

    String getComponentName(X sds);

    int getJoinNumber(String name);

    int getDataSetNumberFromComponentName(X value);

    X getComponentByName(T mapping, String string)
            throws ResourceNotFoundException, TransformationAccessStrategyException;

    boolean checkThatAllTargetsHaveCKMName(T mapping, String ckmName)
            throws TransformationAccessStrategyException;

    boolean checkThatAllTargetsHaveIKMName(T mapping, String ckmName)
            throws TransformationAccessStrategyException;

    boolean checkThatAllTargetsHaveLKMName(T mapping, String ckmName)
            throws TransformationAccessStrategyException;

    void deleteMappingsByName(String name, String folder, String projectCode)
            throws ResourceNotFoundException, ResourceFoundAmbiguouslyException;

    OdiFolder addFolder(String folderProp) throws ResourceCreationException;

    Y findOdiContext(String property) throws ResourceNotFoundException;

    boolean isDistinctMapping(T mapping) throws Exception;

    Map<String, String> getFilterExecutionLocations(T mapping)
            throws TransformationAccessStrategyException;

    Map<String, String> getJoinExecutionLocations(T mapping)
            throws TransformationAccessStrategyException;

    Map<String, String> getSubQueryExecutionLocation(T mapping)
            throws TransformationAccessStrategyException;

    /**
     * @param projectCode code of ODI project
     * @param mappingName name of mapping (ODI12c)or interface (ODI11g)
     * @param columnName  name of column in mapping
     * @return Map that contains Boolean values for flags associated with column
     * the following key names are uses (see static values above)
     * UD1, ..., UD10  - user-defined flags
     * INSERT - insert flag
     * UPDATE - update flag
     * MANDATORY - mandatory flag
     * KEY - update key flag
     * @throws ResourceNotFoundException
     * @throws ResourceFoundAmbiguouslyException
     * @throws TransformationAccessStrategyException
     */
    Map<String, Boolean> getFlags(String projectCode, String mappingName,
                                  String columnName) throws ResourceNotFoundException, ResourceFoundAmbiguouslyException, TransformationAccessStrategyException;

    Collection<OdiLoadPlan> findAllLoadPlans();

    Collection<T> findMappingsByFolder(String projectCode, String folder);

    boolean areAllSourcesNotJournalised(T mapping);

    boolean areAllSourcesJournalised(T mapping);

    String getBeginOrEndMappingText(T mapping, String beginOrEnd);

    String getBeginOrEndMappingLocationCode(T mapping, String beginOrEnd);

    String getBeginOrEndMappingTechnologyCode(T mapping, String beginOrEnd);

    OdiTechnology findTechnologyByCode(String technology);

    Object findLogicalSchemaByName(String model);

}