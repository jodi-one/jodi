package one.jodi.core.automapping.impl;

import com.google.inject.Inject;
import one.jodi.base.annotations.DefaultStrategy;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.model.types.DataStore;
import one.jodi.core.automapping.ColumnMappingContext;
import one.jodi.core.common.ClonerUtil;
import one.jodi.core.executionlocation.ExecutionLocationType;
import one.jodi.core.extensions.contexts.ColumnMappingExecutionContext;
import one.jodi.core.extensions.contexts.TargetColumnExecutionContext;
import one.jodi.core.extensions.strategies.ColumnMappingStrategy;
import one.jodi.core.extensions.strategies.IncorrectCustomStrategyException;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.core.validation.etl.ETLValidator;
import one.jodi.etl.internalmodel.Dataset;
import one.jodi.etl.internalmodel.Targetcolumn;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.etl.internalmodel.impl.MappingsImpl;
import one.jodi.etl.internalmodel.impl.TargetcolumnImpl;
import one.jodi.model.extensions.TargetColumnExtension;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Implementation of the context object for the strategy to define the target
 * mapping expressions.
 * <p>
 * The {@link #getMappings(Transformation)} method collects all source and lookup
 * {@link DataStore}s and delegates to the wired
 * strategies.
 * <p>
 * A default strategy must be wired into this class if using Guice.  Optionally a
 * custom strategy ({@link #customStrategy}) may be wired in to modify the
 * decision of the default strategy.
 */
public class ColumnMappingContextImpl implements ColumnMappingContext {
    private final static Logger logger =
            LogManager.getLogger(ColumnMappingContextImpl.class);
    private final static String ERROR_MESSAGE_01070 =
            "An unknown exception was raised in column mapping strategy '%1$s' " +
                    "while determining mapping expression for target data store '%2$s' " +
                    "dataset index %3$s with package sequence %4$s.";
    private final DatabaseMetadataService databaseMetadataService;
    private final ColumnMappingStrategy defaultStrategy;
    private final ColumnMappingStrategy customStrategy;
    private final ETLValidator validator;
    private final ErrorWarningMessageJodi errorWarningMessages;

    @Inject
    public ColumnMappingContextImpl(
            final DatabaseMetadataService databaseMetadataService,
            final @DefaultStrategy ColumnMappingStrategy defaultStrategy,
            final ColumnMappingStrategy customStrategy,
            final ETLValidator validator,
            final ErrorWarningMessageJodi errorWarningMessages) {
        this.databaseMetadataService = databaseMetadataService;
        this.defaultStrategy = defaultStrategy;
        this.customStrategy = customStrategy;
        this.validator = validator;
        this.errorWarningMessages = errorWarningMessages;
    }

    @Override
    public Map<String, List<String>> getMappings(final Transformation transformation) {

        final DataStore targetDataStore = databaseMetadataService
                .getTargetDataStoreInModel(transformation.getMappings());
        Map<String, List<String>> map =
                new LinkedHashMap<>(targetDataStore.getColumns().size());

        for (String columnName : targetDataStore.getColumns().keySet()) {
            map.put(columnName, new ArrayList<>());
            int index = 0;
            for (Dataset dataset : transformation.getDatasets()) {
                String expression = null;
                ColumnMappingExecutionContext columnMappingExecutionContext =
                        createColumnMappingExecutionContext(dataset);
                TargetColumnExecutionContext targetColumnExecutionContext =
                        createTargetColumnExecutionContext(columnName, transformation);

                expression = defaultStrategy.getMappingExpression(expression,
                        columnMappingExecutionContext,
                        targetColumnExecutionContext);
                try {
                    if (customStrategy != null) {
                        expression = customStrategy.getMappingExpression(expression,
                                columnMappingExecutionContext,
                                targetColumnExecutionContext);
                    }
                } catch (RuntimeException re) {
                    validator.handleColumnMapping(re, transformation, columnName);
                    String msg = errorWarningMessages.formatMessage(1070,
                            ERROR_MESSAGE_01070, this.getClass(),
                            customStrategy.getClass().getName(),
                            targetDataStore.getDataStoreName() + "." + columnName,
                            index, transformation.getPackageSequence());
                    errorWarningMessages.addMessage(
                            transformation.getPackageSequence(), msg,
                            MESSAGE_TYPE.ERRORS);
                    logger.error(msg, re);
                    throw new IncorrectCustomStrategyException(msg);
                }

                if (expression != null) {
                    map.get(columnName).add(expression);
                } else {
                    logger.debug(transformation.getPackageSequence() +
                            " Cannot find mapping expression for target " +
                            targetDataStore.getDataStoreName() + "." + columnName +
                            " and Dataset with index (" + index + ").");
                }
                index++;
            }
        }

        for (String targetColumnName : map.keySet()) {
            boolean found = false;
            for (Targetcolumn targetColumn : transformation.getMappings()
                    .getTargetColumns()) {
                if (targetColumn.getName().equals(targetColumnName)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                TargetcolumnImpl targetColumn = new TargetcolumnImpl();
                targetColumn.setName(targetColumnName);
                targetColumn.addMappingExpressions(map.get(targetColumnName));
                targetColumn.setParent(transformation.getMappings());
                ((MappingsImpl) transformation.getMappings())
                        .addTargetcolumns(targetColumn);
            }
        }

        try {
            Collections.sort(transformation.getMappings().getTargetColumns(),
                    (t1, t2) -> Integer.compare(
                            targetDataStore.getColumns().get(t1.getName()).getPosition(),
                            targetDataStore.getColumns().get(t2.getName()).getPosition()));
        } catch (NullPointerException npe) {
            // user defined non-existent column, cannot sort.
        }

        transformation.getMappings().getTargetColumns()
                .forEach(validator::validateTargetColumn);

        return map;
    }

    private TargetColumnExecutionContext createTargetColumnExecutionContext(
            final String column,
            final Transformation transformation) {
        Targetcolumn tc = null;
        for (Targetcolumn c : transformation.getMappings().getTargetColumns()) {
            if (c.getName().equals(column)) {
                tc = c;
                break;
            }
        }

        //final Targetcolumn innertc = tc;
        final boolean explicitlyMapped = tc != null;
        final Boolean explicitMandatory = explicitlyMapped ? tc.isMandatory() : null;
        final Boolean explicitUpdateKey = explicitlyMapped ? tc.isUpdateKey() : null;
        final TargetColumnExtension extension = explicitlyMapped ? tc.getExtension() : null;
        final Boolean isAnalyticalFunction = explicitlyMapped ? getIsAnalyticalFunction(tc) : null;
        final ExecutionLocationType executionLocationType = tc == null || tc.getTargetcolumnExplicitExecutionLocation() == null ? null : tc.getTargetcolumnExplicitExecutionLocation();
        //= explicitlyMapped ? tc.isAnalyticalFunction() : null ;
        TargetColumnExecutionContext context = new TargetColumnExecutionContext() {
            @Override
            public boolean isExplicitlyMapped() {
                //return innertc != null;
                return explicitlyMapped;
            }

            @Override
            public String getTargetColumnName() {
                return column;
            }

            @Override
            public TargetColumnExtension getTargetColumnExtension() {
                ClonerUtil<TargetColumnExtension> cloner =
                        new ClonerUtil<>(errorWarningMessages);
                //return innertc != null ? cloner.clone(innertc.getExtension()) : null;
                return extension != null ? cloner.clone(extension) : null;
            }

            @Override
            public Boolean isExplicitMandatory() {
                return explicitMandatory;
                //return innertc.isMandatory();
            }

            @Override
            public Boolean isExplicitUpdateKey() {
                //return innertc.isUpdateKey();
                return explicitUpdateKey;
            }

            @Override
            public boolean isAnalyticalFunction() {
                return isAnalyticalFunction;
            }

            @Override
            public ExecutionLocationType getExplicitTargetColumnExecutionLocation() {
                return executionLocationType;
            }
        };

        return context;
    }

    private boolean getIsAnalyticalFunction(final Targetcolumn tc) {
        boolean isAnalyticalFunction = false;
        for (int dsIndex = 0; dsIndex < tc.getMappingExpressions().size(); dsIndex++) {
            if (tc.isAnalyticalFunction(dsIndex + 1)) {
                isAnalyticalFunction = true;
                break;
            }
        }
        return isAnalyticalFunction;
    }

    private ColumnMappingExecutionContext createColumnMappingExecutionContext(
            final Dataset dataset) {
        ColumnMappingExecutionContext context =
                new ColumnMappingExecutionContextImpl(dataset,
                        this.databaseMetadataService,
                        this.errorWarningMessages);

        return context;
    }


}
