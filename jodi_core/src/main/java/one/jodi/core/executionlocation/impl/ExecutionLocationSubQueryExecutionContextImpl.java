package one.jodi.core.executionlocation.impl;

import one.jodi.base.model.types.DataStore;
import one.jodi.core.config.PropertyValueHolder;
import one.jodi.core.extensions.contexts.ExecutionLocationSubQueryExecutionContext;
import one.jodi.core.extensions.types.DataStoreWithAlias;
import one.jodi.etl.internalmodel.RoleEnum;
import one.jodi.model.extensions.TransformationExtension;

import java.util.Map;

public class ExecutionLocationSubQueryExecutionContextImpl implements
        ExecutionLocationSubQueryExecutionContext {
    private final String condition;
    private final RoleEnum role;
    private final DataStoreWithAlias filterDataStore;
    private final Map<String, PropertyValueHolder> properties;
    private final DataStoreWithAlias sourceDataStore;
    private final DataStore targetDataStore;
    private final TransformationExtension transformationExtension;
    private final boolean sameModelInTransformation;

    public ExecutionLocationSubQueryExecutionContextImpl(String condition,
                                                         DataStoreWithAlias filterDataStore,
                                                         RoleEnum role,
                                                         Map<String, PropertyValueHolder> map,
                                                         DataStoreWithAlias sourceDataStore,
                                                         DataStore targetDataStore,
                                                         TransformationExtension transformationExtension,
                                                         boolean sameModelInTransformation) {
        this.condition = condition;
        this.filterDataStore = filterDataStore;
        this.properties = map;
        this.sourceDataStore = sourceDataStore;
        this.targetDataStore = targetDataStore;
        this.transformationExtension = transformationExtension;
        this.sameModelInTransformation = sameModelInTransformation;
        this.role = role;
    }


    @Override
    public Map<String, PropertyValueHolder> getProperties() {
        return properties;
    }

    @Override
    public DataStoreWithAlias getSourceDataStore() {
        return sourceDataStore;
    }


    @Override
    public DataStore getTargetDataStore() {
        return targetDataStore;
    }

    @Override
    public TransformationExtension getTransformationExtension() {
        return transformationExtension;
    }


    @Override
    public boolean isSameModelInTransformation() {
        return sameModelInTransformation;
    }


    @Override
    public DataStoreWithAlias getFilterDataStore() {
        return filterDataStore;
    }


    @Override
    public String getCondition() {
        return condition;
    }


    @Override
    public RoleEnum getRole() {
        return role;
    }
}
