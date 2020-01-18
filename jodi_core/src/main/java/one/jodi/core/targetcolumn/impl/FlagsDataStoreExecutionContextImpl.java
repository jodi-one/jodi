package one.jodi.core.targetcolumn.impl;

import one.jodi.base.model.types.DataStore;
import one.jodi.core.config.PropertyValueHolder;
import one.jodi.core.extensions.contexts.FlagsDataStoreExecutionContext;
import one.jodi.model.extensions.MappingsExtension;
import one.jodi.model.extensions.TransformationExtension;

import java.util.Map;

public class FlagsDataStoreExecutionContextImpl implements
        FlagsDataStoreExecutionContext {
    //	private final String dataSourceName;
    private final MappingsExtension mappingsExtension;
    private final Map<String, PropertyValueHolder> properties;
    //	private final ModelSolutionLayer solutionLayer;
//	private final Map<String, Object> tableFlexFields;
    private final String ikmName;
    //	private final String tableModel;
//	private final DataStoreType tableType;
    private final DataStore targetDataStore;
    private final TransformationExtension transformationExtension;

    public FlagsDataStoreExecutionContextImpl(MappingsExtension mappingsExtension,
                                              Map<String, PropertyValueHolder> map, String tableIKMName,
                                              DataStore targetDataStore,
                                              TransformationExtension transformationExtension) {
        this.mappingsExtension = mappingsExtension;
        this.properties = map;
        this.ikmName = tableIKMName;
        this.targetDataStore = targetDataStore;
        this.transformationExtension = transformationExtension;
    }

    @Override
    public DataStore getTargetDataStore() {
        return targetDataStore;
    }

    @Override
    public String getIKMCode() {
        return ikmName;
    }

    @Override
    public TransformationExtension getTransformationExtension() {
        return transformationExtension;
    }

    @Override
    public MappingsExtension getMappingsExtension() {
        return mappingsExtension;
    }

    @Override
    public Map<String, PropertyValueHolder> getProperties() {
        return properties;
    }

}
