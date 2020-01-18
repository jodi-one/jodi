package one.jodi.core.targetcolumn.impl;

import one.jodi.base.model.types.SCDType;
import one.jodi.core.executionlocation.ExecutionLocationType;
import one.jodi.core.extensions.contexts.UDFlagsTargetColumnExecutionContext;
import one.jodi.core.extensions.types.TargetColumnFlags;
import one.jodi.core.extensions.types.UserDefinedFlag;
import one.jodi.model.extensions.TargetColumnExtension;

import java.util.Collections;
import java.util.Set;

public class FlagsTargetColumnExecutionContextImpl implements
        UDFlagsTargetColumnExecutionContext {
    private final String columnDataType;
    private final String targetColumnName;
    private final SCDType scdType;
    private final boolean notNullFlag;
    private final TargetColumnExtension targetColumnExtension;
    private final TargetColumnFlags targetColumnFlags;
    private final Set<UserDefinedFlag> userDefinedFlags;
    private final boolean explicitlyMapped;
    private final boolean useExpression;
    private final Boolean explicitUpdateKey;
    private final Boolean explicitMandatory;
    private final boolean isAnalyticalFunction;
    private final ExecutionLocationType explicitExecutionLocationType;

    public FlagsTargetColumnExecutionContextImpl(
            final String columnDataType,
            final String columnName,
            final SCDType scdType,
            final boolean notNullFlag,
            final TargetColumnExtension targetColumnExtension,
            final TargetColumnFlags targetColumnFlags,
            final Set<UserDefinedFlag> userDefinedFlags,
            boolean explicitlyMapped,
            Boolean explicitUpdateKey,
            Boolean explicitMandatory,
            final boolean useExpression,
            final boolean isAnalyticalFunction,
            ExecutionLocationType explicitExecutionLocationType
    ) {
        this.columnDataType = columnDataType;
        this.targetColumnName = columnName;
        this.scdType = scdType;
        this.notNullFlag = notNullFlag;
        this.targetColumnExtension = targetColumnExtension;
        this.targetColumnFlags = targetColumnFlags;
        this.userDefinedFlags = userDefinedFlags;
        this.explicitlyMapped = explicitlyMapped;
        this.explicitUpdateKey = explicitUpdateKey;
        this.explicitMandatory = explicitMandatory;
        this.useExpression = useExpression;
        this.isAnalyticalFunction = isAnalyticalFunction;
        this.explicitExecutionLocationType = explicitExecutionLocationType;
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
    public String getColumnDataType() {
        return columnDataType;
    }

    @Override
    public SCDType getColumnSCDType() {
        return scdType;
    }

    @Override
    public TargetColumnExtension getTargetColumnExtension() {
        return targetColumnExtension;
    }

    @Override
    public TargetColumnFlags getTargetColumnFlags() {
        return targetColumnFlags;
    }


    @Override
    public Set<UserDefinedFlag> getUserDefinedFlags() {
        return Collections.unmodifiableSet(this.userDefinedFlags);
    }

    @Override
    public boolean hasNotNullConstraint() {
        return notNullFlag;
    }

    @Override
    public boolean useExpression() {
        return useExpression;
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
        return explicitExecutionLocationType;
    }

}
