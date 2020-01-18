package one.jodi.core.executionlocation.impl;

import one.jodi.base.model.types.DataStore;
import one.jodi.core.config.PropertyValueHolder;
import one.jodi.core.extensions.contexts.ExecutionLocationDataStoreExecutionContext;
import one.jodi.model.extensions.MappingsExtension;
import one.jodi.model.extensions.TransformationExtension;

import java.util.Map;

public class ExecutionLocationDataStoreExecutionContextImpl implements
        ExecutionLocationDataStoreExecutionContext {

    private final boolean sameModelInTransformation;
    private final DataStore targetDataStore;
    private final TransformationExtension transformationExtension;
    private final Map<String, PropertyValueHolder> properties;
    private final int dataSetIndex;
    private final String dataSetName;
    private final MappingsExtension mappingsExtension;

    public ExecutionLocationDataStoreExecutionContextImpl(
            boolean sameModelInTransformation, DataStore targetDataStore,
            TransformationExtension transformationExtension,
            Map<String, PropertyValueHolder> map, int dataSetIndex,
            String dataSetName, MappingsExtension mappingsExtension) {
        super();
        this.sameModelInTransformation = sameModelInTransformation;
        this.targetDataStore = targetDataStore;
        this.transformationExtension = transformationExtension;
        this.properties = map;
        this.dataSetIndex = dataSetIndex;
        this.dataSetName = dataSetName;
        this.mappingsExtension = mappingsExtension;
    }

    @Override
    public DataStore getTargetDataStore() {
        return targetDataStore;
    }

    @Override
    public boolean isSameModelInTransformation() {
        return sameModelInTransformation;
    }

    @Override
    public TransformationExtension getTransformationExtension() {
        return transformationExtension;
    }

    @Override
    public Map<String, PropertyValueHolder> getProperties() {
        return properties;
    }

    @Override
    public int getDataSetIndex() {
        return dataSetIndex;
    }

    @Override
    public String getDataSetName() {
        return dataSetName;
    }

    @Override
    public MappingsExtension getMappingsExtension() {
        return mappingsExtension;
    }

}
