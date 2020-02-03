package one.jodi.core.executionlocation.impl;

import com.google.inject.Inject;
import one.jodi.core.config.JodiConstants;
import one.jodi.core.config.PropertyValueHolder;
import one.jodi.core.extensions.contexts.*;
import one.jodi.core.extensions.strategies.ExecutionLocationStrategy;
import one.jodi.core.extensions.types.DataStoreWithAlias;
import one.jodi.core.extensions.types.ExecutionLocationType;
import one.jodi.etl.common.EtlSubSystemVersion;
import one.jodi.etl.internalmodel.JoinTypeEnum;

import java.util.Optional;

/**
 * The default ExecutionLocationStrategy implementation that implements the
 * baseline behavior to determine ExecutionLocation for a given column.
 */
public class ExecutionLocationDefaultStrategy implements
        ExecutionLocationStrategy {

    private final EtlSubSystemVersion etlSubSystemVersion;

    @Inject
    public ExecutionLocationDefaultStrategy(final EtlSubSystemVersion etlSubSystemVersion) {
        this.etlSubSystemVersion = etlSubSystemVersion;
    }

    /**
     * @see ExecutionLocationStrategy#getTargetColumnExecutionLocation(ExecutionLocationType, ExecutionLocationDataStoreExecutionContext, ExecutionLocationTargetColumnExecutionContext)
     */
    @Override
    public ExecutionLocationType getTargetColumnExecutionLocation(ExecutionLocationType defaultExecutionLocation,
                                                                  ExecutionLocationDataStoreExecutionContext dsContext,
                                                                  ExecutionLocationTargetColumnExecutionContext tcContext) {
        ExecutionLocationType result = null;

        PropertyValueHolder valueHolder = dsContext.getProperties().get(JodiConstants.ROW_WID);
        String rowIdProperty = valueHolder != null ? valueHolder.getString() : null;
        Optional<String> isSequenceColumn = tcContext.getSqlExpressions().stream().filter(e -> e.contains("_NEXTVAL")).findFirst();
        if (tcContext.getSqlExpressions().size() == 0) {
            result = ExecutionLocationType.WORK;// for expression 'null'
        } else if (defaultExecutionLocation != null) {
            result = defaultExecutionLocation;
        } else if (isRowidColumn(tcContext.getTargetColumnName(), rowIdProperty,
                tcContext.getSqlExpressions().get(0)) || isSequenceColumn.isPresent()) {
            result = ExecutionLocationType.TARGET;
        } else if (!tcContext.isExplicitlyMapped()) {
            result = ExecutionLocationType.TARGET;
        } else if (tcContext.isAnalyticalFunction() && !this.etlSubSystemVersion.isVersion11()) {
            // set analytical functions to target
            result = ExecutionLocationType.WORK;
        } else {
            String targetSql = tcContext.getSqlExpressions().get(dsContext.getDataSetIndex());
            result = (isExecutedOnSource(targetSql) ? ExecutionLocationType.SOURCE
                    : ExecutionLocationType.WORK);
        }

        if (tcContext.getExplicitTargetColumnExecutionLocation() != null) {
            result = ExecutionLocationType.valueOf(tcContext.getExplicitTargetColumnExecutionLocation().name());
        }

        return result;
    }

    /**
     * Checks if the column SQL should be executed on the source data store.
     *
     * @param sql the column SQL expression
     * @return true, if the column SQL should be executed on the source data
     * store
     */
    private boolean isExecutedOnSource(String sql) {
        boolean isExecOnSource = true;
        // int dataSetNumber = getDataSetIndex(dataSet, transformation,
        // interfaceName, packageSequence);
        // String sql =
        // targetcolumn.getMappingExpressions().getExpression().get(dataSetNumber);
        int positionOfFirstDot = sql.indexOf(".") + 1;
        int positionOfSecondDot = sql.indexOf(".", positionOfFirstDot);
        if (sql.contains(")") || sql.contains("(") || sql.trim().contains(" ")
                || sql.trim().contains(":") || sql.trim().contains(">")
                || sql.trim().contains("<") || sql.trim().contains("-")
                || sql.trim().contains("+") || sql.trim().contains("*")
                || sql.trim().contains("/") || sql.trim().contains("=")) {
            isExecOnSource = false;
        } else if (positionOfSecondDot > 0) {
            isExecOnSource = false;
        } else if (positionOfFirstDot == 0) {
            isExecOnSource = false;
        } else if (sql.contains("||") || sql.toLowerCase().contains("row_loc")) {
            /* for unpivot */
            isExecOnSource = false;
        }
        return isExecOnSource;
    }

    /**
     * Checks a given column name to determine if it is a rowid column.
     *
     * @param columnName  the column name to be tested
     * @param rowidColumn the configured rowid column name
     * @param firstExpr   the sql expression associated with the first datastore
     * @return true, if the given column name to determine if it is a rowid
     * column
     */
    private boolean isRowidColumn(String columnName, String rowidColumn,
                                  String firstExpr) {
        return (columnName.compareToIgnoreCase(rowidColumn) == 0 && (firstExpr
                .contains("SEQ") || firstExpr.contains("NEXTVAL")));
    }


    /**
     * @see ExecutionLocationStrategy#getFilterExecutionLocation(ExecutionLocationType, ExecutionLocationFilterExecutionContext)
     */
    @Override
    public ExecutionLocationType getFilterExecutionLocation(
            ExecutionLocationType defaultExecutionLocation,
            ExecutionLocationFilterExecutionContext context) {
        ExecutionLocationType result = null;
        if (defaultExecutionLocation != null) {
            result = defaultExecutionLocation;
        } else if (context.isSameModelInTransformation()) {
            result = ExecutionLocationType.SOURCE;
        } else {
            result = ExecutionLocationType.WORK;
        }
        return result;
    }


    /**
     * @see ExecutionLocationStrategy#getJoinExecutionLocation(ExecutionLocationType, ExecutionLocationJoinExecutionContext)
     */
    @Override
    public ExecutionLocationType getJoinExecutionLocation(
            ExecutionLocationType defaultExecutionLocation,
            ExecutionLocationJoinExecutionContext context) {
        ExecutionLocationType result = null;
        if (defaultExecutionLocation != null) {
            result = defaultExecutionLocation;
        } else if (context.getJoinType() != null && (context.getJoinType().equals(JoinTypeEnum.NATURAL) || context.getJoinType().equals(JoinTypeEnum.CROSS))) {
            result = ExecutionLocationType.SOURCE;
        } else if (context.isSameModelInTransformation()) {
            result = ExecutionLocationType.SOURCE;
        } else {
            result = ExecutionLocationType.WORK;
        }
        return result;
    }

    /**
     * @see ExecutionLocationStrategy#getLookupExecutionLocation(ExecutionLocationType, ExecutionLocationLookupExecutionContext)
     */
    @Override
    public ExecutionLocationType getLookupExecutionLocation(
            ExecutionLocationType defaultExecutionLocation,
            ExecutionLocationLookupExecutionContext context) {
        ExecutionLocationType result = null;
        if (defaultExecutionLocation != null) {
            result = defaultExecutionLocation;
        } else {
            DataStoreWithAlias lookupDs = context.getLookupDataStore();
            if (!context.isSameModelInTransformation())
                result = ExecutionLocationType.SOURCE;
            else if (lookupDs.getDataStore().isTemporary())
                result = ExecutionLocationType.SOURCE;
            else
                result = ExecutionLocationType.WORK;
        }
        return result;
    }

    @Override
    public ExecutionLocationType getSubQueryExecutionLocation(
            ExecutionLocationType defaultExecutionLocation,
            ExecutionLocationSubQueryExecutionContext context) {
        ExecutionLocationType result = null;
        if (defaultExecutionLocation != null) {
            result = defaultExecutionLocation;
        } else {
            DataStoreWithAlias filterDSA = context.getFilterDataStore();
            if (!context.isSameModelInTransformation())
                result = ExecutionLocationType.SOURCE;
            else if (filterDSA.getDataStore().isTemporary())
                result = ExecutionLocationType.SOURCE;
            else
                result = ExecutionLocationType.WORK;
        }
        return result;
    }
}
