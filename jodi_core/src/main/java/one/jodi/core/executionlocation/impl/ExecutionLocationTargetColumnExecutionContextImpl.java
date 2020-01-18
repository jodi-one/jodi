package one.jodi.core.executionlocation.impl;

import one.jodi.core.executionlocation.ExecutionLocationType;
import one.jodi.core.extensions.contexts.ExecutionLocationTargetColumnExecutionContext;
import one.jodi.model.extensions.TargetColumnExtension;

import java.util.List;

public class ExecutionLocationTargetColumnExecutionContextImpl implements
        ExecutionLocationTargetColumnExecutionContext {
    private final boolean explicitlyMapped;
    private final Boolean explicitUpdateKey;
    private final Boolean explicitMandatory;
    private final String targetColumnName;
    private final List<String> sqlExpressions;
    private final TargetColumnExtension targetColumnExtension;
    private final boolean isAnalyticalFunction;
    private final ExecutionLocationType explicitTargetColumnExecutionLocation;

    public ExecutionLocationTargetColumnExecutionContextImpl(
            boolean explicitlyMapped, Boolean explicitUpdateKey, Boolean explicitMandatory, String targetColumnName,
            List<String> sqlExpressions,
            TargetColumnExtension targetColumnExtension,
            final boolean isAnalyticalFunction,
            final ExecutionLocationType executionLocationType) {
        super();
        this.explicitlyMapped = explicitlyMapped;
        this.explicitUpdateKey = explicitUpdateKey;
        this.explicitMandatory = explicitMandatory;
        this.targetColumnName = targetColumnName;
        this.sqlExpressions = sqlExpressions;
        this.targetColumnExtension = targetColumnExtension;
        this.isAnalyticalFunction = isAnalyticalFunction;
        this.explicitTargetColumnExecutionLocation = executionLocationType;
    }

    @Override
    public boolean isExplicitlyMapped() {
        return explicitlyMapped;
    }

    @Override
    public String getTargetColumnName() {
        return targetColumnName;
    }

    @Override
    public List<String> getSqlExpressions() {
        return sqlExpressions;
    }

    @Override
    public TargetColumnExtension getTargetColumnExtension() {
        return targetColumnExtension;
    }

    @Override
    public Boolean isExplicitMandatory() {
        return explicitMandatory;
    }

    @Override
    public Boolean isExplicitUpdateKey() {
        return explicitUpdateKey;
    }

    @Override
    public boolean isAnalyticalFunction() {
        return isAnalyticalFunction;
    }

    @Override
    public ExecutionLocationType getExplicitTargetColumnExecutionLocation() {
        return explicitTargetColumnExecutionLocation;
    }
}
