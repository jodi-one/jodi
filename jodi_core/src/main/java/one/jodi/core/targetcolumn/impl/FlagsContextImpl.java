package one.jodi.core.targetcolumn.impl;

import com.google.inject.Inject;
import one.jodi.base.annotations.DefaultStrategy;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.model.types.DataStore;
import one.jodi.base.model.types.DataStoreColumn;
import one.jodi.core.common.ClonerUtil;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.extensions.contexts.FlagsDataStoreExecutionContext;
import one.jodi.core.extensions.contexts.FlagsTargetColumnExecutionContext;
import one.jodi.core.extensions.contexts.UDFlagsTargetColumnExecutionContext;
import one.jodi.core.extensions.strategies.FlagsStrategy;
import one.jodi.core.extensions.strategies.IncorrectCustomStrategyException;
import one.jodi.core.extensions.types.TargetColumnFlags;
import one.jodi.core.extensions.types.UserDefinedFlag;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.core.targetcolumn.FlagsContext;
import one.jodi.etl.internalmodel.Mappings;
import one.jodi.etl.internalmodel.Targetcolumn;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.etl.service.interfaces.TransformationException;
import one.jodi.model.extensions.MappingsExtension;
import one.jodi.model.extensions.TargetColumnExtension;
import one.jodi.model.extensions.TransformationExtension;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This FlagsContext implementation uses the specified strategy
 * implementations to determine the user defined flags for a given column.
 */
public class FlagsContextImpl implements FlagsContext {

    private static final String ERROR_MESSAGE_02032 =
            "An unknown exception was raised in flags strategy %s while determining flags for data store";
    private static final String ERROR_MESSAGE_02034 = "TargetColumn '%s' does not exist.";

    private static final String ERROR_MESSAGE_03093 =
            "Custom flags strategy %1$s must return non-empty " + TargetColumnFlags.class + " object";
    private static final Logger logger = LogManager.getLogger(FlagsContextImpl.class);
    private final DatabaseMetadataService databaseMetadataService;
    private final ErrorWarningMessageJodi errorWarningMessages;
    /**
     * The default udf strategy.
     */
    private final FlagsStrategy defaultStrategy;
    /**
     * The custom udf strategy.
     */
    private final FlagsStrategy customStrategy;
    private final JodiProperties properties;

    /**
     * Instantiates a new user defined flag context impl.
     *
     * @param databaseMetadataService reference to a builder interface used for execution context
     *                                creation
     * @param defaultStrategy         the default user defined flag strategy
     * @param customStrategy          the custom user defined flag strategy
     * @param errorWarningMessages    reference to the error and warning subsystem
     * @param properties              reference to the property file
     */
    @Inject
    public FlagsContextImpl(final DatabaseMetadataService databaseMetadataService,
                            @DefaultStrategy final FlagsStrategy defaultStrategy, final FlagsStrategy customStrategy,
                            final ErrorWarningMessageJodi errorWarningMessages, final JodiProperties properties) {
        this.databaseMetadataService = databaseMetadataService;
        this.defaultStrategy = defaultStrategy;
        this.customStrategy = customStrategy;
        this.errorWarningMessages = errorWarningMessages;
        this.properties = properties;
    }

    private boolean isValidResultFlags(TargetColumnFlags flags) {

        boolean cond =
                (flags != null && flags.isInsert() != null && flags.isUpdate() != null && flags.isUpdateKey() != null &&
                        flags.isMandatory() != null);

        return cond;
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

    private DataStoreColumn getDataStoreColumn(Targetcolumn targetColumn, DataStore datastore) {
        for (DataStoreColumn datastoreColumn : datastore.getColumns()
                                                        .values()) {
            if (datastoreColumn.getName()
                               .equalsIgnoreCase(targetColumn.getName())) {
                return datastoreColumn;
            }
        }

        return null;
    }

    private UserDefinedFlag map(one.jodi.etl.internalmodel.UserDefinedFlag ud) {
        return new UserDefinedFlag() {
            @Override
            public String getName() {
                return ud.getName();
            }

            @Override
            public boolean getValue() {
                return ud.getValue();
            }

        };
    }

    private Set<UserDefinedFlag> getUdFlags(final Targetcolumn column) {
        return column.getExplicitUserDefinedFlags()
                     .stream()
                     .map(u -> map(u))
                     .collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
    }

    private UDFlagsTargetColumnExecutionContext createColumnContext(final DataStoreColumn column,
                                                                    final Targetcolumn targetColumn,
                                                                    final TargetColumnFlags targetColumnFlags) {
        TargetColumnExtension tce = null;
        if (targetColumn != null && targetColumn.getExtension() != null) {
            ClonerUtil<TargetColumnExtension> cloner = new ClonerUtil<>(errorWarningMessages);
            tce = cloner.clone(targetColumn.getExtension());
        }
        boolean analyticalFunction = false;
        if (targetColumn != null) {
            for (int dataSetIndex = 0; dataSetIndex < targetColumn.getMappingExpressions()
                                                                  .size(); dataSetIndex++) {
                if (targetColumn.isAnalyticalFunction((dataSetIndex + 1))) {
                    analyticalFunction = true;
                    break;
                }
            }
        }
        if (column == null) {
            String message = this.errorWarningMessages.formatMessage(2034, ERROR_MESSAGE_02034, this.getClass(),
                                                                     targetColumn.getName());
            this.errorWarningMessages.addMessage(targetColumn.getParent()
                                                             .getParent()
                                                             .getPackageSequence(), message, MESSAGE_TYPE.ERRORS);
            throw new TransformationException(message);
        }

        return new FlagsTargetColumnExecutionContextImpl(column.getColumnDataType(), column.getName(),
                                                         column.getColumnSCDType(), column.hasNotNullConstraint(), tce,
                                                         targetColumnFlags,
                                                         targetColumn != null ? getUdFlags(targetColumn)
                                                                              : Collections.emptySet(),
                                                         (targetColumn != null),
                                                         targetColumn != null ? targetColumn.isExplicitlyUpdateKey()
                                                                              : null,
                                                         targetColumn != null ? targetColumn.isExplicitlyMandatory()
                                                                              : null,
                                                         targetColumnFlags != null ? targetColumnFlags.useExpression()
                                                                                   : true,
                                                         targetColumn != null ? analyticalFunction : false, null);
    }

    private FlagsDataStoreExecutionContext createDataStoreContext(final Transformation transformation,
                                                                  final String targetDataStore) {
        final Mappings mappings = transformation.getMappings();
        final DataStore ds = databaseMetadataService.getTargetDataStoreInModel(mappings);
        TransformationExtension teClone = null;
        if (transformation.getExtension() != null) {
            ClonerUtil<TransformationExtension> cloner = new ClonerUtil<>(errorWarningMessages);
            teClone = cloner.clone(transformation.getExtension());
        }
        MappingsExtension mappingsExtension = transformation.getMappings()
                                                            .getExtension();
        MappingsExtension meClone = null;
        if (mappingsExtension != null) {
            ClonerUtil<MappingsExtension> cloner = new ClonerUtil<>(errorWarningMessages);
            meClone = cloner.clone(mappingsExtension);
        }

        return new FlagsDataStoreExecutionContextImpl(meClone, databaseMetadataService.getCoreProperties(),
                                                      transformation.getMappings()
                                                                    .getIkm()
                                                                    .getName(), ds, teClone);
    }


    @Override
    public Map<String, Set<UserDefinedFlag>> getUserDefinedFlags(Mappings mappings) {
        String targetDataStore = mappings.getTargetDataStore();
        Transformation transformation = mappings.getParent();
        FlagsDataStoreExecutionContext dataStoreContext = createDataStoreContext(transformation, targetDataStore);

        //get the Insert Update flags for this Mapping
        Map<String, TargetColumnFlags> flagsMap = getTargetColumnFlags(mappings);

        DataStore ds = dataStoreContext.getTargetDataStore();
        Map<String, Set<UserDefinedFlag>> result = new HashMap<>(ds.getColumns()
                                                                   .size());

        for (DataStoreColumn md : ds.getColumns()
                                    .values()) {
            Targetcolumn targetColumn = getMappedColumn(mappings, md.getName());
            TargetColumnFlags currentFlags = flagsMap.get(md.getName());
            UDFlagsTargetColumnExecutionContext columnContext = createColumnContext(md, targetColumn, currentFlags);
            Set<UserDefinedFlag> defaultValues =
                    defaultStrategy.getUserDefinedFlags(columnContext.getUserDefinedFlags(), dataStoreContext,
                                                        columnContext);
            assert (defaultValues != null) : "default strategy must not return a null result";
            Set<UserDefinedFlag> customValues;
            try {
                customValues = customStrategy.getUserDefinedFlags(defaultValues, dataStoreContext, columnContext);
                result.put(md.getName(), customValues);
            } catch (RuntimeException ex) {
                String msg = errorWarningMessages.formatMessage(2032, ERROR_MESSAGE_02032, this.getClass(),
                                                                customStrategy.toString());
                errorWarningMessages.addMessage(mappings.getParent()
                                                        .getPackageSequence(), msg, MESSAGE_TYPE.ERRORS);
                logger.error(msg);
                throw new IncorrectCustomStrategyException(msg, ex);
            }
            if (customValues == null) {
                String msg = errorWarningMessages.formatMessage(3093, ERROR_MESSAGE_03093, this.getClass(),
                                                                customStrategy.toString());
                errorWarningMessages.addMessage(mappings.getParent()
                                                        .getPackageSequence(), msg, MESSAGE_TYPE.ERRORS);
                throw new IncorrectCustomStrategyException(msg);
            }
        }

        return result;
    }


    @Override
    public Map<String, TargetColumnFlags> getTargetColumnFlags(Mappings mappings) {
        String targetDataStore = mappings.getTargetDataStore();
        final Transformation transformation = mappings.getParent();
        FlagsDataStoreExecutionContext dataStoreContext = createDataStoreContext(transformation, targetDataStore);

        DataStore ds = dataStoreContext.getTargetDataStore();
        Map<String, TargetColumnFlags> result = new HashMap<>(ds.getColumns()
                                                                .size());

        for (DataStoreColumn md : ds.getColumns()
                                    .values()) {
            TargetColumnFlags explicitValues = null;
            final Targetcolumn targetColumn = getMappedColumn(mappings, md.getName());
            if (targetColumn != null) {
                explicitValues = new TargetColumnFlags() {
                    @Override
                    public Boolean isInsert() {
                        return targetColumn.isInsert();
                    }

                    @Override
                    public Boolean isUpdate() {
                        return targetColumn.isUpdate();
                    }

                    @Override
                    public Boolean isUpdateKey() {
                        return targetColumn.isUpdateKey();
                    }

                    @Override
                    public Boolean isMandatory() {
                        return targetColumn.isMandatory();
                    }

                    @Override
                    public Boolean useExpression() {
                        return transformation.useExpressions();
                    }
                };
            }

            FlagsTargetColumnExecutionContext columnContext = createColumnContext(md, targetColumn, null);
            TargetColumnFlags defaultValues =
                    defaultStrategy.getTargetColumnFlags(explicitValues, dataStoreContext, columnContext);
            assert (isValidResultFlags(defaultValues)) : "default strategy must not contain null results";
            TargetColumnFlags customValues;
            try {
                customValues = customStrategy.getTargetColumnFlags(defaultValues, dataStoreContext, columnContext);
                result.put(md.getName(), customValues);
            } catch (RuntimeException ex) {
                String msg = errorWarningMessages.formatMessage(2032, ERROR_MESSAGE_02032, this.getClass(),
                                                                customStrategy.toString());
                errorWarningMessages.addMessage(mappings.getParent()
                                                        .getPackageSequence(), msg, MESSAGE_TYPE.ERRORS);
                logger.error(msg);
                throw new IncorrectCustomStrategyException(msg, ex);
            }
            if (!isValidResultFlags(customValues)) {
                String msg = errorWarningMessages.formatMessage(3093, ERROR_MESSAGE_03093, this.getClass(),
                                                                customStrategy.toString());
                errorWarningMessages.addMessage(mappings.getParent()
                                                        .getPackageSequence(), msg, MESSAGE_TYPE.ERRORS);
                throw new IncorrectCustomStrategyException(msg);
            }
        }

        return result;
    }


    @Override
    public TargetColumnFlags getTargetColumnFlags(final Targetcolumn targetColumn) {
        Mappings mappings = targetColumn.getParent();
        String targetDataStore = mappings.getTargetDataStore();
        Transformation transformation = mappings.getParent();
        FlagsDataStoreExecutionContext dataStoreContext = createDataStoreContext(transformation, targetDataStore);

        DataStore datastore = dataStoreContext.getTargetDataStore();
        TargetColumnFlags explicitValues = new TargetColumnFlags() {
            @Override
            public Boolean isInsert() {
                return targetColumn.isInsert();
            }

            @Override
            public Boolean isUpdate() {
                return targetColumn.isUpdate();
            }

            @Override
            public Boolean isUpdateKey() {
                return targetColumn.isUpdateKey();
            }

            @Override
            public Boolean isMandatory() {
                return targetColumn.isMandatory();
            }

            @Override
            public Boolean useExpression() {
                if (properties.getProperty("suppressExpression") == null || properties.getProperty("suppressExpression")
                                                                                      .equals("false")) {
                    return true;
                } else {
                    return false;
                }
            }
        };

        DataStoreColumn datastoreColumn = getDataStoreColumn(targetColumn, datastore);
        FlagsTargetColumnExecutionContext columnContext = createColumnContext(datastoreColumn, targetColumn, null);
        TargetColumnFlags defaultValues =
                defaultStrategy.getTargetColumnFlags(explicitValues, dataStoreContext, columnContext);

        assert (isValidResultFlags(defaultValues)) : "default strategy must not contain null results";

        TargetColumnFlags customValues;
        try {
            customValues = customStrategy.getTargetColumnFlags(defaultValues, dataStoreContext, columnContext);
        } catch (RuntimeException ex) {
            String msg = errorWarningMessages.formatMessage(2032, ERROR_MESSAGE_02032, this.getClass(),
                                                            customStrategy.toString() + "");
            logger.error(msg);
            errorWarningMessages.addMessage(mappings.getParent()
                                                    .getPackageSequence(), msg, MESSAGE_TYPE.ERRORS);
            throw new IncorrectCustomStrategyException(msg, ex);
        }
        if (!isValidResultFlags(customValues)) {
            String msg = errorWarningMessages.formatMessage(3093, ERROR_MESSAGE_03093, this.getClass(),
                                                            customStrategy.toString());
            errorWarningMessages.addMessage(mappings.getParent()
                                                    .getPackageSequence(), msg, MESSAGE_TYPE.ERRORS);
            throw new IncorrectCustomStrategyException(msg);
        }

        return customValues;
    }


    @Override
    public Set<UserDefinedFlag> getUserDefinedFlags(Targetcolumn targetColumn) {
        Mappings mappings = targetColumn.getParent();
        String targetDataStore = mappings.getTargetDataStore();
        Transformation transformation = mappings.getParent();
        FlagsDataStoreExecutionContext dataStoreContext = createDataStoreContext(transformation, targetDataStore);

        //get the Insert Update flags for this Mapping
        TargetColumnFlags flags = getTargetColumnFlags(targetColumn);
        DataStore datastore = dataStoreContext.getTargetDataStore();
        //LinkedHashSet<UserDefinedFlag> result = new LinkedHashSet<UserDefinedFlag>();

        DataStoreColumn dataStoreColumn = getDataStoreColumn(targetColumn, datastore);

        UDFlagsTargetColumnExecutionContext columnContext = createColumnContext(dataStoreColumn, targetColumn, flags);

        Set<UserDefinedFlag> defaultValues =
                defaultStrategy.getUserDefinedFlags(columnContext.getUserDefinedFlags(), dataStoreContext,
                                                    columnContext);
        assert (defaultValues != null) : "default strategy must not return a null result";
        Set<UserDefinedFlag> customValues;
        try {
            customValues = customStrategy.getUserDefinedFlags(defaultValues, dataStoreContext, columnContext);
        } catch (RuntimeException ex) {
            String msg = errorWarningMessages.formatMessage(2032, ERROR_MESSAGE_02032, this.getClass(),
                                                            customStrategy.toString());
            logger.error(msg);
            errorWarningMessages.addMessage(mappings.getParent()
                                                    .getPackageSequence(), msg, MESSAGE_TYPE.ERRORS);
            throw new IncorrectCustomStrategyException(msg, ex);
        }
        if (customValues == null) {
            String msg = errorWarningMessages.formatMessage(3093, ERROR_MESSAGE_03093, this.getClass(),
                                                            customStrategy.toString());
            errorWarningMessages.addMessage(mappings.getParent()
                                                    .getPackageSequence(), msg, MESSAGE_TYPE.ERRORS);
            throw new IncorrectCustomStrategyException(msg);
        } else {
            return customValues;
        }
    }

}