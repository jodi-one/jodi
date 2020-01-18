package one.jodi.core.executionlocation.impl;

import one.jodi.base.model.types.DataStore;
import one.jodi.core.config.PropertyValueHolder;
import one.jodi.core.extensions.contexts.ExecutionLocationJoinExecutionContext;
import one.jodi.core.extensions.types.DataStoreWithAlias;
import one.jodi.etl.internalmodel.JoinTypeEnum;
import one.jodi.model.extensions.SourceExtension;
import one.jodi.model.extensions.TransformationExtension;

import java.util.List;
import java.util.Map;

public class ExecutionLocationJoinExecutionContextImpl implements ExecutionLocationJoinExecutionContext {

    private final Map<String, PropertyValueHolder> properties;
    private final TransformationExtension transformationExtension;
    private final SourceExtension sourceExtension;
    private final JoinTypeEnum joinType;
    private final DataStore targetDataStore;
    private final String joinCondition;
    private final List<DataStoreWithAlias> joinedDataStores;
    // whether or not an the data is from the same server,
    // true is indicating no LKM is used.
    private boolean sameModelInTransformation;

    public ExecutionLocationJoinExecutionContextImpl(final Map<String, PropertyValueHolder> map,
                                                     final TransformationExtension transformationExtension,
                                                     final SourceExtension sourceExtension,
                                                     final JoinTypeEnum joinType,
                                                     final DataStore targetDataStore,
                                                     final String joinCondition,
                                                     final List<DataStoreWithAlias> dataStores,
                                                     final boolean sameModelInTransformation) {
        this.properties = map;
        this.transformationExtension = transformationExtension;
        this.sourceExtension = sourceExtension;
        this.joinType = joinType;
        this.targetDataStore = targetDataStore;
        this.joinCondition = joinCondition;
        this.joinedDataStores = dataStores;
        this.sameModelInTransformation = sameModelInTransformation;
    }

    @Override
    public JoinTypeEnum getJoinType() {
        return joinType;
    }

    @Override
    public TransformationExtension getTransformationExtension() {
        return transformationExtension;
    }

    @Override
    public SourceExtension getSourceExtension() {
        return sourceExtension;
    }

    @Override
    public Map<String, PropertyValueHolder> getProperties() {
        return properties;
    }

    @Override
    public DataStore getTargetDataStore() {
        return targetDataStore;
    }

    @Override
    public String getJoinCondition() {
        return joinCondition;
    }

    @Override
    public List<DataStoreWithAlias> getJoinedDataStores() {
        return joinedDataStores;
    }

    @Override
    public boolean isSameModelInTransformation() {
        return this.sameModelInTransformation;
    }
}
