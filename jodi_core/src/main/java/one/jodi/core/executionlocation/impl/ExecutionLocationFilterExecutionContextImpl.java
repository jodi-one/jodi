package one.jodi.core.executionlocation.impl;

import one.jodi.base.model.types.DataStore;
import one.jodi.core.config.PropertyValueHolder;
import one.jodi.core.extensions.contexts.ExecutionLocationFilterExecutionContext;
import one.jodi.core.extensions.types.DataStoreWithAlias;
import one.jodi.model.extensions.SourceExtension;
import one.jodi.model.extensions.TransformationExtension;

import java.util.List;
import java.util.Map;

public class ExecutionLocationFilterExecutionContextImpl implements ExecutionLocationFilterExecutionContext {

    private final Map<String, PropertyValueHolder> properties;
    private final TransformationExtension transformationExtension;
    private final SourceExtension sourceExtension;
    private final DataStore targetDataStore;
    private final String filterCondition;
    private final List<DataStoreWithAlias> filteredDataStores;
    private boolean sameModelInTransformation;

    public ExecutionLocationFilterExecutionContextImpl(final Map<String, PropertyValueHolder> map,
                                                       TransformationExtension transformationExtension,
                                                       SourceExtension sourceExtension,
                                                       final DataStore targetDataStore, final String filterCondition,
                                                       List<DataStoreWithAlias> filteredDataStores,
                                                       final boolean sameModelInTransformation) {
        this.properties = map;
        this.transformationExtension = transformationExtension;
        this.sourceExtension = sourceExtension;
        this.targetDataStore = targetDataStore;
        this.filterCondition = filterCondition;
        this.filteredDataStores = filteredDataStores;
        this.sameModelInTransformation = sameModelInTransformation;
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
    public String getFilterCondition() {
        return filterCondition;
    }

    @Override
    public List<DataStoreWithAlias> getFilteredDataStores() {
        return filteredDataStores;
    }

    @Override
    public boolean isSameModelInTransformation() {
        return this.sameModelInTransformation;
    }
}
