package one.jodi.core.executionlocation.impl;

import one.jodi.base.annotations.DefaultStrategy;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.exception.UnRecoverableException;
import one.jodi.base.model.types.DataStore;
import one.jodi.base.model.types.DataStoreColumn;
import one.jodi.base.util.StringUtils;
import one.jodi.core.common.ClonerUtil;
import one.jodi.core.executionlocation.ExecutionLocationContext;
import one.jodi.core.extensions.contexts.*;
import one.jodi.core.extensions.strategies.ExecutionLocationStrategy;
import one.jodi.core.extensions.strategies.IncorrectCustomStrategyException;
import one.jodi.core.extensions.types.DataStoreWithAlias;
import one.jodi.core.extensions.types.ExecutionLocationType;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.core.validation.etl.ETLValidator;
import one.jodi.etl.internalmodel.*;
import one.jodi.etl.internalmodel.impl.LookupImpl;
import one.jodi.etl.internalmodel.impl.SourceImpl;
import one.jodi.etl.internalmodel.impl.SubQueryImpl;
import one.jodi.etl.internalmodel.impl.TargetcolumnImpl;
import one.jodi.model.extensions.MappingsExtension;
import one.jodi.model.extensions.SourceExtension;
import one.jodi.model.extensions.TargetColumnExtension;
import one.jodi.model.extensions.TransformationExtension;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The ExecutionLocationContext implementation. This class utilizes the defined
 * strategy to determine the ExecutionLocation for a given column.
 *
 */
public class ExecutionLocationContextImpl implements ExecutionLocationContext {

    //private final Logger logger = LogManager.getLogger(ExecutionLocationContextImpl.class);

    private final static String ERROR_MESSAGE_03140 =
            "Source filter validation failed.  See error report.";
    private final static String ERROR_MESSAGE_03150 =
            "Execution location set to invalid value.  See error report.";
    private final static String ERROR_MESSAGE_03160 =
            "Join execution location errors.  See error report.";
    private final static String ERROR_MESSAGE_03170 =
            "Execution location strategy has set invalid join location.";
    private final static String ERROR_MESSAGE_03180 =
            "The explicitly defined %1$s execution location can only be values SOURCE or WORK.";
    private final static String ERROR_MESSAGE_03190 =
            "The %2$s execution location strategy %1$s must only return values SOURCE or WORK.";
    private final static String ERROR_MESSAGE_03191 =
            "The execution location strategy %1$s returns an unknown value %2$s.";
    private final static String AUTO_MAP_SQL_PREFIX = "mockAlias.";
    private static final Pattern CONDITION_TABLENAME_PATTERN =
            Pattern.compile("([a-z]{1}[a-z1-9\\$#_]*)\\.[a-z]{1}[a-z1-9\\$#_]*(\\.[a-z]{1}[a-z1-9\\$#_]*){0,1}",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private final static Logger logger = LogManager.getLogger(ExecutionLocationContextImpl.class);
    /**
     * The custom strategy.
     */
    private final ExecutionLocationStrategy customStrategy;
    /**
     * The default strategy.
     */
    private final ExecutionLocationStrategy defaultStrategy;
    private final DatabaseMetadataService databaseMetadataService;
    private final ETLValidator validator;
    private final ErrorWarningMessageJodi errorWarningMessages;

    /**
     * Instantiates a new ExecutionLocationContextImpl.
     *
     * @param customStrategy          the custom strategy
     * @param defaultStrategy         the default strategy
     * @param databaseMetadataService the common builder
     * @param validator               the validator of the context
     * @param errorWarningMessages    the error and warning messages
     */
    @Inject
    public ExecutionLocationContextImpl(
            final ExecutionLocationStrategy customStrategy,
            @DefaultStrategy final ExecutionLocationStrategy defaultStrategy,
            final DatabaseMetadataService databaseMetadataService,
            final ETLValidator validator,
            final ErrorWarningMessageJodi errorWarningMessages) {
        this.customStrategy = customStrategy;
        this.defaultStrategy = defaultStrategy;
        this.databaseMetadataService = databaseMetadataService;
        this.validator = validator;
        this.errorWarningMessages = errorWarningMessages;

    }

    private static List<String> getConditionSources(String conditionStr) {
        Set<String> result = new HashSet<>();
        if (conditionStr != null) {
            Matcher regexMatcher = CONDITION_TABLENAME_PATTERN.matcher(conditionStr);
            while (regexMatcher.find()) {
                if (!StringUtils.hasLength(regexMatcher.group(2))) {
                    result.add(regexMatcher.group(1));
                }
            }
        }
        return new ArrayList<>(result);
    }

    private boolean isSameModelInTransformation(Transformation transformation) {
        boolean isExecutedOnTarget = false;

        HashMap<String, String> db = new HashMap<>();
        DataStore targetDataStore = databaseMetadataService.getTargetDataStoreInModel(transformation.getMappings());
        db.put(targetDataStore.getDataModel().getDataServerName(), targetDataStore.getDataModel().getDataServerName());
        ALL:
        for (Dataset dataset : transformation.getDatasets()) {
            for (Source source : dataset.getSources()) {
                DataStore sourceDataStore = databaseMetadataService.getSourceDataStoreInModel(source.getName(), source.getModel());
                if (!db.containsKey(sourceDataStore.getDataModel().getDataServerName())) {
                    isExecutedOnTarget = false;
                    break ALL;
                }
                for (Lookup lookup : source.getLookups()) {
                    DataStore lookupDataStore = databaseMetadataService.getSourceDataStoreInModel(lookup.getLookupDataStore(), lookup.getModel());
                    if (!db.containsKey(lookupDataStore.getDataModel().getDataServerName())) {
                        isExecutedOnTarget = false;
                        break ALL;
                    }
                }
            }
        }
        // if there are temporary datastores then execute them on source not on target.
        // consider temporary datastores a select statement; that can't be moved to target easy.
        ALL:
        for (Dataset dataset : transformation.getDatasets()) {
            for (Source source : dataset.getSources()) {
                if (source.isTemporary() || targetDataStore.isTemporary()) {
                    isExecutedOnTarget = false;
                    break ALL;
                }
                for (Lookup lookup : source.getLookups()) {
                    if (lookup.isTemporary()) {
                        isExecutedOnTarget = false;
                        break ALL;
                    }
                }
            }
        }

        return isExecutedOnTarget;
    }

    @Override
    public List<ExecutionLocationType> getTargetColumnExecutionLocation(
            Targetcolumn targetColumn) {

        boolean isSameModelInTransformation = isSameModelInTransformation(targetColumn.getParent().getParent());

        ClonerUtil<MappingsExtension> mappingsCloner =
                new ClonerUtil<>(errorWarningMessages);
        ClonerUtil<TransformationExtension> transformationCloner =
                new ClonerUtil<>(errorWarningMessages);

        Mappings mappings = targetColumn.getParent();
        Transformation transformation = mappings.getParent();
        MappingsExtension mappingsExtension = mappingsCloner.clone(mappings.getExtension());
        TransformationExtension transformationExtension = transformationCloner.clone(transformation.getExtension());
        DataStore targetDatastore = databaseMetadataService.getTargetDataStoreInModel(mappings);

        //TODO refactor out dataset index and
        ArrayList<ExecutionLocationType> executionLocations = new ArrayList<>();
        ExecutionLocationTargetColumnExecutionContext columnContext = createTargetColumnContext(targetDatastore, targetColumn);
        for (Dataset dataset : targetColumn.getParent().getParent().getDatasets()) {
            int datasetIndex = targetColumn.getParent().getParent().getDatasets().indexOf(dataset);
            ExecutionLocationDataStoreExecutionContext datastoreContext = createDataStoreContext(dataset.getName(), datasetIndex, isSameModelInTransformation, mappingsExtension, transformationExtension, targetDatastore);
            ExecutionLocationType defaultLocation = defaultStrategy.getTargetColumnExecutionLocation(null, datastoreContext, columnContext);
            ExecutionLocationType customLocation = customStrategy.getTargetColumnExecutionLocation(defaultLocation, datastoreContext, columnContext);
            executionLocations.add(customLocation);

        }

        ((TargetcolumnImpl) targetColumn).setExecutionLocations(convert(executionLocations));

        return executionLocations;
    }

    private ExecutionLocationTargetColumnExecutionContext createTargetColumnContext(DataStore targetDatastore, Targetcolumn targetColumn) {
        for (DataStoreColumn cmd : targetDatastore.getColumns().values()) {
            if (cmd.getName().equalsIgnoreCase(targetColumn.getName())) {
                return createTargetColumnContext(cmd, targetColumn);
            }
        }
        return null;
    }

    private ExecutionLocationDataStoreExecutionContext createDataStoreContext(
            final String datasetName,
            final int datasetIndex,
            final boolean sameModelInTransformation,
            final MappingsExtension mappingsExtension,
            final TransformationExtension transformationExtension,
            final DataStore datastore) {
        return new ExecutionLocationDataStoreExecutionContextImpl(
                sameModelInTransformation, datastore, transformationExtension,
                databaseMetadataService.getCoreProperties(), datasetIndex, datasetName,
                mappingsExtension);
    }

    private ExecutionLocationTargetColumnExecutionContext
    createTargetColumnContext(final DataStoreColumn columnMetaData,
                              final Targetcolumn column) {
        boolean explicitlyMapped = column != null;

        TargetColumnExtension columnExtension = null;
        String columnName = columnMetaData.getName();
        List<String> sqlExpression = null;

        if (explicitlyMapped) {
            ClonerUtil<TargetColumnExtension> columnCloner =
                    new ClonerUtil<>(errorWarningMessages);

            columnExtension = columnCloner.clone(column.getExtension());
            sqlExpression = getColumnSqlExpressions(column);
        } else {
            sqlExpression = Collections.singletonList(AUTO_MAP_SQL_PREFIX + columnMetaData.getName());
        }
        boolean isAnalytical = false;
        for (int dataSetIndex = 0; dataSetIndex < column.getMappingExpressions().size(); dataSetIndex++) {
            if (column.isAnalyticalFunction((dataSetIndex + 1))) {
                isAnalytical = column.isAnalyticalFunction((dataSetIndex + 1));
                break;
            }
        }
        return new ExecutionLocationTargetColumnExecutionContextImpl(
                explicitlyMapped, column.isExplicitlyUpdateKey(), column.isExplicitlyMandatory(), columnName, sqlExpression, columnExtension, isAnalytical, column.getTargetcolumnExplicitExecutionLocation());
    }

    private ExecutionLocationFilterExecutionContext createFilterContext(
            final Source source,
            final boolean sameModelInTransformation) {
        Transformation transformation = source.getParent().getParent();
        ClonerUtil<SourceExtension> sourceCloner =
                new ClonerUtil<>(errorWarningMessages);
        ClonerUtil<TransformationExtension> transformationCloner =
                new ClonerUtil<>(errorWarningMessages);
        SourceExtension sourceExtension = sourceCloner.clone(source
                .getExtension());
        TransformationExtension transformationExtension = transformationCloner
                .clone(transformation.getExtension());

        final Mappings mappings = transformation.getMappings();
        final DataStore targetDataStore = databaseMetadataService
                .getTargetDataStoreInModel(mappings);

        List<DataStoreWithAlias> dsList = getDataStoresAssociatedWithCondition(
                source, source.getFilter());

        ExecutionLocationFilterExecutionContext executionContext =
                new ExecutionLocationFilterExecutionContextImpl(
                        databaseMetadataService.getCoreProperties(),
                        transformationExtension,
                        sourceExtension, targetDataStore,
                        source.getFilter(), dsList,
                        sameModelInTransformation);
        return executionContext;
    }

    private ExecutionLocationJoinExecutionContext createJoinContext(
            final Source source,
            final boolean sameModelInTransformation) {
        //Datasets ds = (Datasets) source.getParent().getParent();
        //Transformation transformation = (Transformation) ds.getParent();
        Transformation transformation = source.getParent().getParent();
        ClonerUtil<SourceExtension> sourceCloner =
                new ClonerUtil<>(errorWarningMessages);
        ClonerUtil<TransformationExtension> transformationCloner =
                new ClonerUtil<>(errorWarningMessages);
        SourceExtension sourceExtension = sourceCloner.clone(source
                .getExtension());
        TransformationExtension transformationExtension = transformationCloner
                .clone(transformation.getExtension());

        final Mappings mappings = transformation.getMappings();
        final DataStore dataStore = databaseMetadataService.getTargetDataStoreInModel(mappings);

        List<DataStoreWithAlias> dsList = getDataStoresAssociatedWithCondition(
                source, source.getJoin());

        ExecutionLocationJoinExecutionContext executionContext = new ExecutionLocationJoinExecutionContextImpl(
                databaseMetadataService.getCoreProperties(), transformationExtension,
                sourceExtension, source.getJoinType(), dataStore,
                source.getJoin(), dsList, sameModelInTransformation);
        return executionContext;
    }

    private ExecutionLocationSubQueryExecutionContext createSubQueryContext(
            final SubQuery subquery,
            final boolean sameModelInTransformation) {
        Source source = subquery.getParent();
        Transformation transformation = source.getParent().getParent();

        ClonerUtil<SourceExtension> sourceCloner =
                new ClonerUtil<>(errorWarningMessages);
        ClonerUtil<TransformationExtension> transformationCloner =
                new ClonerUtil<>(errorWarningMessages);
        final SourceExtension sourceExtension = sourceCloner.clone(source.getExtension());
        TransformationExtension transformationExtension = transformationCloner.clone(transformation.getExtension());
        String modelCode = subquery.getFilterSourceModel();
        //final String lookup_alias = (subquery.getAlias() != null) ? subquery.getAlias()
        //: subquery.getLookupDataStore();
        final DataStore filterds = databaseMetadataService.getSourceDataStoreInModel(subquery.getFilterSource(), modelCode);
        DataStoreWithAlias filterDataStore = new DataStoreWithAlias() {
            @Override
            public String getAlias() {
                return subquery.getFilterSource();
            }

            @Override
            public DataStore getDataStore() {
                return filterds;
            }

            @Override
            public Type getType() {
                return DataStoreWithAlias.Type.Filter;
            }

            @Override
            public SourceExtension getSourceExtension() {
                return sourceExtension;
            }
        };

        final Mappings mappings = transformation.getMappings();
        final DataStore targetDataStore = databaseMetadataService.getTargetDataStoreInModel(mappings);

        String sourceModelCode = source.getModel();
        final String source_alias = (source.getAlias() != null) ? source.getAlias() : source.getName();
        final DataStore sourceds = databaseMetadataService.getSourceDataStoreInModel(source.getName(), sourceModelCode);
        DataStoreWithAlias sourceDataStore = new DataStoreWithAlias() {
            @Override
            public String getAlias() {
                return source_alias;
            }

            @Override
            public DataStore getDataStore() {
                return sourceds;
            }

            @Override
            public Type getType() {
                return DataStoreWithAlias.Type.Source;
            }

            @Override
            public SourceExtension getSourceExtension() {
                return sourceExtension;
            }
        };

        ExecutionLocationSubQueryExecutionContext executionContext =
                new ExecutionLocationSubQueryExecutionContextImpl(
                        subquery.getCondition(),
                        filterDataStore,
                        subquery.getRole(),
                        databaseMetadataService.getCoreProperties(),
                        sourceDataStore,
                        targetDataStore,
                        transformationExtension,
                        sameModelInTransformation);
        return executionContext;
    }

    private ExecutionLocationLookupExecutionContext createLookupContext(
            final Lookup lookup,
            final boolean sameModelInTransformation) {
        Source source = lookup.getParent();
        Transformation transformation = source.getParent().getParent();

        ClonerUtil<SourceExtension> sourceCloner =
                new ClonerUtil<>(errorWarningMessages);
        ClonerUtil<TransformationExtension> transformationCloner =
                new ClonerUtil<>(errorWarningMessages);
        final SourceExtension sourceExtension = sourceCloner.clone(source.getExtension());
        TransformationExtension transformationExtension = transformationCloner.clone(transformation.getExtension());
        String modelCode = lookup.getModel();
        final String lookup_alias = (lookup.getAlias() != null) ? lookup.getAlias()
                : lookup.getLookupDataStore();
        final DataStore lookupds = databaseMetadataService.getSourceDataStoreInModel(lookup.getLookupDataStore(), modelCode);
        DataStoreWithAlias lookupDataStore = new DataStoreWithAlias() {
            @Override
            public String getAlias() {
                return lookup_alias;
            }

            @Override
            public DataStore getDataStore() {
                return lookupds;
            }

            @Override
            public Type getType() {
                return DataStoreWithAlias.Type.Lookup;
            }

            @Override
            public SourceExtension getSourceExtension() {
                return sourceExtension;
            }
        };

        final Mappings mappings = transformation.getMappings();
        final DataStore targetDataStore = databaseMetadataService.getTargetDataStoreInModel(mappings);

        String sourceModelCode = source.getModel();//modelCodeContext.getModelCode(source);
        final String source_alias = (source.getAlias() != null) ? source.getAlias() : source.getName();
        final DataStore sourceds = databaseMetadataService.getSourceDataStoreInModel(source.getName(), sourceModelCode);
        DataStoreWithAlias sourceDataStore = new DataStoreWithAlias() {
            @Override
            public String getAlias() {
                return source_alias;
            }

            @Override
            public DataStore getDataStore() {
                return sourceds;
            }

            @Override
            public Type getType() {
                return DataStoreWithAlias.Type.Source;
            }

            @Override
            public SourceExtension getSourceExtension() {
                return sourceExtension;
            }
        };

        ExecutionLocationLookupExecutionContext executionContext =
                new ExecutionLocationLookupExecutionContextImpl(
                        lookup.getJoin(), lookupDataStore,
                        lookup.getLookupType(),
                        databaseMetadataService.getCoreProperties(),
                        sourceDataStore,
                        /*sourceExtension, */targetDataStore,
                        transformationExtension,
                        sameModelInTransformation);
        return executionContext;
    }

    private List<String> getColumnSqlExpressions(Targetcolumn column) {
        List<String> result = new ArrayList<>();
        result.addAll(column.getMappingExpressions());
        return result;
    }

    private List<DataStoreWithAlias> getDataStoreList(List<String> sourceNames, List<Source> sourceList) {
        List<DataStoreWithAlias> dsList = Collections.emptyList();
        if (sourceNames != null && sourceList != null) {
            dsList = new ArrayList<>(sourceNames.size());
            Set<String> processedSourceAliases = new HashSet<>(sourceNames.size());
            for (final Source source : sourceList) {
                final SourceExtension sourceExtension = source.getExtension();
                final String alias = (source.getAlias() != null) ? source.getAlias() : source.getName();
                if (sourceNames.contains(alias) && !processedSourceAliases.contains(alias)) {
                    final String model = source.getModel();
                    //dsList.add(databaseMetadataService.getSourceDataStoreInModel(source.getName(), model, alias));
                    dsList.add(new DataStoreWithAlias() {
                        @Override
                        public String getAlias() {
                            return alias;
                        }

                        @Override
                        public DataStore getDataStore() {
                            return databaseMetadataService.getSourceDataStoreInModel(source.getName(), model);
                        }

                        @Override
                        public Type getType() {
                            return DataStoreWithAlias.Type.Source;
                        }

                        @Override
                        public SourceExtension getSourceExtension() {
                            return sourceExtension;
                        }
                    });
                    processedSourceAliases.add(alias);
                }
            }
        }
        return dsList;
    }

    private List<DataStoreWithAlias> getDataStoresAssociatedWithCondition(Source source, String condition) {
        List<DataStoreWithAlias> dsList = null;
        List<String> sourceAliasesInCondition = getConditionSources(condition);
        if (sourceAliasesInCondition != null && !sourceAliasesInCondition.isEmpty()) {
            dsList = getDataStoreList(sourceAliasesInCondition, source.getParent().getSources());
        } else {
            dsList = Collections.emptyList();
        }
        return dsList;
    }

    private Targetcolumn getMappedColumn(Mappings mappings, String columnName) {
        Targetcolumn result = null;

        for (Targetcolumn column : mappings.getTargetColumns()) {
            if (columnName.compareToIgnoreCase(column.getName()) == 0) {
                result = column;
                break;
            }
        }
        return result;
    }

    private ExecutionLocationType mapToExecutionLocationType(ExecutionLocationtypeEnum sourceType) {
        ExecutionLocationType result = null;
        if (sourceType != null) {
            switch (sourceType) {
                case SOURCE:
                    result = ExecutionLocationType.SOURCE;
                    break;
                case WORK:
                    result = ExecutionLocationType.WORK;
                    break;
                case TARGET:
                    result = ExecutionLocationType.TARGET;
                    break;
            }
        }

        return result;
    }

    @Override
    public Map<String, ExecutionLocationType> getTargetColumnExecutionLocation(
            Mappings mappings,
            String datasetName, int datasetIndex) {
        Map<String, ExecutionLocationType> result = new HashMap<>();

        boolean sameModelInTransformation = isSameModelInTransformation(mappings.getParent());


        ClonerUtil<MappingsExtension> mappingsCloner =
                new ClonerUtil<>(errorWarningMessages);
        ClonerUtil<TransformationExtension> transformationCloner =
                new ClonerUtil<>(errorWarningMessages);

        Transformation transformation = mappings.getParent();
        MappingsExtension mappingsExtension = mappingsCloner.clone(mappings.getExtension());
        TransformationExtension transformationExtension = transformationCloner.clone(transformation.getExtension());
        DataStore ds = databaseMetadataService.getTargetDataStoreInModel(mappings);
        ExecutionLocationDataStoreExecutionContext dsContext = createDataStoreContext(datasetName, datasetIndex, sameModelInTransformation, mappingsExtension, transformationExtension, ds);
        for (DataStoreColumn cmd : ds.getColumns().values()) {
            Targetcolumn targetColumn = getMappedColumn(mappings, cmd.getName());

            ExecutionLocationTargetColumnExecutionContext tcContext = createTargetColumnContext(cmd, targetColumn);
            ExecutionLocationType defaultLocation = defaultStrategy.getTargetColumnExecutionLocation(null, dsContext, tcContext);

            ExecutionLocationType customLocation = customStrategy.getTargetColumnExecutionLocation(defaultLocation, dsContext, tcContext);

            if (customLocation != null) {
                result.put(cmd.getName(), customLocation);
            }
        }
        return result;
    }

    @Override
    public ExecutionLocationType getFilterExecutionLocation(Source source) {
        if (source.getFilter() != null && source.getFilter().length() > 1) {

            if (!validator.validateFilterExecutionLocation(source)) {
                String msg = errorWarningMessages.formatMessage(3140,
                        ERROR_MESSAGE_03140, this.getClass());
                logger.error(msg);
                throw new UnRecoverableException(msg);
            }

            boolean sameModelInTransformation = isSameModelInTransformation(source.getParent().getParent());

            ExecutionLocationFilterExecutionContext executionContext = createFilterContext(source, sameModelInTransformation);
            ExecutionLocationType definedType = mapToExecutionLocationType(source.getFilterExecutionLocation());
            ExecutionLocationType defaultLocation;
            try {
                defaultLocation = defaultStrategy.getFilterExecutionLocation(definedType, executionContext);

                assert (defaultLocation != null && defaultLocation != ExecutionLocationType.TARGET)
                        : "The filter execution location must only return SOURCE or WORK.";

                ExecutionLocationType location = defaultLocation;
                if (this.customStrategy != null) {
                    location = customStrategy.getFilterExecutionLocation(defaultLocation, executionContext);
                }
                ((SourceImpl) source).setFilterExecutionLocation(ExecutionLocationtypeEnum.fromValue(location.name()));

                if (!validator.validateFilterExecutionLocation(source, customStrategy)) {
                    String msg = errorWarningMessages.formatMessage(3150,
                            ERROR_MESSAGE_03150, this.getClass());
                    errorWarningMessages.addMessage(
                            source.getParent().getParent().getPackageSequence(), msg,
                            MESSAGE_TYPE.ERRORS);
                    throw new UnRecoverableException(msg);
                }
                return location;
            } catch (RuntimeException e) {
                validator.handleFilterExecutionLocation(e, source);
                throw e;
            }
        }

        return null;
    }

    @Override
    public ExecutionLocationType getJoinExecutionLocation(Source source) {
        if (!validator.validateJoinExecutionLocation(source)) {
            String msg = errorWarningMessages.formatMessage(3160,
                    ERROR_MESSAGE_03160, this.getClass());
            logger.error(msg);
            throw new UnRecoverableException(msg);
        }

        if (source.getJoin() != null && source.getJoin().length() > 1) {
            boolean sameModelInTransformation = isSameModelInTransformation(source.getParent().getParent());

            ExecutionLocationJoinExecutionContext executionContext = createJoinContext(source, sameModelInTransformation);

            ExecutionLocationType definedType = mapToExecutionLocationType(source.getJoinExecutionLocation());
            ExecutionLocationType defaultLocation = defaultStrategy.getJoinExecutionLocation(definedType, executionContext);

            assert (defaultLocation != null && defaultLocation != ExecutionLocationType.TARGET)
                    : "The join execution location must only return SOURCE or WORK.";

            ExecutionLocationType location = defaultLocation;
            if (this.customStrategy != null) {
                location = customStrategy.getJoinExecutionLocation(defaultLocation, executionContext);
            }

            ((SourceImpl) source).setJoinExecutionLocation(ExecutionLocationtypeEnum.fromValue(location.name()));

            if (!validator.validateJoinExecutionLocation(source, customStrategy)) {
                String msg = errorWarningMessages.formatMessage(3170,
                        ERROR_MESSAGE_03170, this.getClass());
                errorWarningMessages.addMessage(
                        source.getParent().getParent().getPackageSequence(), msg,
                        MESSAGE_TYPE.ERRORS);
                throw new UnRecoverableException(msg);
            }

            return location;


        }

        return null;
    }

    @Override
    public ExecutionLocationType getLookupExecutionLocation(
            Lookup lookup) {

        boolean sameModelInTransformation = isSameModelInTransformation(lookup.getParent().getParent().getParent());


        ExecutionLocationLookupExecutionContext executionContext = createLookupContext(lookup, sameModelInTransformation);
        ExecutionLocationType definedType = mapToExecutionLocationType(lookup.getJoinExecutionLocation());
        if (!validator.validateExecutionLocation(lookup)) {
            String msg = errorWarningMessages.formatMessage(3180, ERROR_MESSAGE_03180, this.getClass(), "lookup");
            errorWarningMessages.addMessage(lookup.getParent().getParent().getParent().getPackageSequence(), msg, MESSAGE_TYPE.ERRORS);
            throw new UnRecoverableException(msg);
        }

        ExecutionLocationType defaultLocation = defaultStrategy
                .getLookupExecutionLocation(definedType, executionContext);
        assert (defaultLocation != null && defaultLocation != ExecutionLocationType.TARGET)
                : "The lookup execution location must only return SOURCE or WORK.";

        ExecutionLocationType location = defaultLocation;
        if (this.customStrategy != null) {
            location = customStrategy.getLookupExecutionLocation(defaultLocation, executionContext);
        }

        ExecutionLocationtypeEnum finalValue;
        try {
            finalValue = ExecutionLocationtypeEnum.fromValue(location.name());
        } catch (IllegalArgumentException e) {
            // incorrect name was used
            String msg = errorWarningMessages.formatMessage(3191,
                    ERROR_MESSAGE_03191, this.getClass(),
                    customStrategy.getClass().getName(), location.name());
            logger.error(msg);
            errorWarningMessages.addMessage(
                    lookup.getParent().getParent().getParent().getPackageSequence(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new IncorrectCustomStrategyException(msg, e);
        }

        ((LookupImpl) lookup).setJoinExecutionLocation(finalValue);
        ExecutionLocationStrategy strategy = customStrategy != null ? customStrategy
                : defaultStrategy;
        if (!validator.validateExecutionLocation(lookup, strategy)) {
            String msg = errorWarningMessages.formatMessage(3190,
                    ERROR_MESSAGE_03190, this.getClass(),
                    strategy.getClass().getName(), "lookup");
            errorWarningMessages.addMessage(
                    lookup.getParent().getParent().getParent().getPackageSequence(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new IncorrectCustomStrategyException(msg);
        }

        return location;
    }

    private List<ExecutionLocationtypeEnum> convert(List<ExecutionLocationType> in) {
        ArrayList<ExecutionLocationtypeEnum> out = in.stream()
                .map(el -> ExecutionLocationtypeEnum
                        .fromValue(el.name()))
                .collect(Collectors
                        .toCollection(ArrayList::new));

        return out;
    }


    @Override
    public ExecutionLocationType getSubQueryExecutionLocation(SubQuery subquery) {
        boolean sameModelInTransformation = isSameModelInTransformation(subquery.getParent().getParent().getParent());

        ExecutionLocationSubQueryExecutionContext executionContext = createSubQueryContext(subquery, sameModelInTransformation);
        ExecutionLocationType definedType = mapToExecutionLocationType(subquery.getExecutionLocation());
//		if(!validator.validateExecutionLocation(lookup)) {
//			String msg = errorWarningMessages.formatMessage(3180, ERROR_MESSAGE_03180, this.getClass(), "lookup");
//			errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);
//			throw new UnRecoverableException(msg);
//		}

        ExecutionLocationType defaultLocation = defaultStrategy
                .getSubQueryExecutionLocation(definedType, executionContext);
        assert (defaultLocation != null && defaultLocation != ExecutionLocationType.TARGET)
                : "The lookup execution location must only return SOURCE or WORK.";

        ExecutionLocationType location = defaultLocation;
        if (this.customStrategy != null) {
            location = customStrategy.getSubQueryExecutionLocation(defaultLocation, executionContext);
        }

        ExecutionLocationtypeEnum finalValue;
        try {
            finalValue = ExecutionLocationtypeEnum.fromValue(location.name());
        } catch (IllegalArgumentException e) {
            // incorrect name was used
            String msg = errorWarningMessages.formatMessage(3191,
                    ERROR_MESSAGE_03191, this.getClass(),
                    customStrategy.getClass().getName(), location.name());
            logger.error(msg);
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new IncorrectCustomStrategyException(msg, e);
        }

        ((SubQueryImpl) subquery).setExecutionLocation(finalValue);
//		ExecutionLocationStrategy strategy = customStrategy != null ? customStrategy 
//		                                                            : defaultStrategy;
//		if (!validator.validateExecutionLocation(lookup, strategy)) {
//			String msg = errorWarningMessages.formatMessage(3190,
//					                          ERROR_MESSAGE_03190, this.getClass(),
//					                          strategy.getClass().getName(), "lookup");
//			errorWarningMessages.addMessage(
//					errorWarningMessages.assignSequenceNumber(), msg,
//					MESSAGE_TYPE.ERRORS);
//			throw new IncorrectCustomStrategyException(msg);
//		}

        return location;
    }

}
