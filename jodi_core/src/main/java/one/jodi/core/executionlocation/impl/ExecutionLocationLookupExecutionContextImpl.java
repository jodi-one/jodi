package one.jodi.core.executionlocation.impl;

import one.jodi.base.model.types.DataStore;
import one.jodi.core.config.PropertyValueHolder;
import one.jodi.core.extensions.contexts.ExecutionLocationLookupExecutionContext;
import one.jodi.core.extensions.types.DataStoreWithAlias;
import one.jodi.etl.internalmodel.LookupTypeEnum;
import one.jodi.model.extensions.TransformationExtension;

import java.util.Map;

public class ExecutionLocationLookupExecutionContextImpl implements
        ExecutionLocationLookupExecutionContext {
    private final String joinCondition;
    private final DataStoreWithAlias lookupDataStore;
    private final LookupTypeEnum lookupType;
    private final Map<String, PropertyValueHolder> properties;
    private final DataStoreWithAlias sourceDataStore;
    //private final SourceExtension sourceExtension;
    private final DataStore targetDataStore;
    private final TransformationExtension transformationExtension;
    private final boolean sameModelInTransformation;

    public ExecutionLocationLookupExecutionContextImpl(String joinCondition,
                                                       DataStoreWithAlias lookupDataStore, LookupTypeEnum lookupType,
                                                       Map<String, PropertyValueHolder> map, DataStoreWithAlias sourceDataStore,
            /*SourceExtension sourceExtension, */DataStore targetDataStore,
                                                       TransformationExtension transformationExtension,
                                                       boolean sameModelInTransformation) {
        super();
        this.joinCondition = joinCondition;
        this.lookupDataStore = lookupDataStore;
        this.lookupType = lookupType;
        this.properties = map;
        this.sourceDataStore = sourceDataStore;
        //this.sourceExtension = sourceExtension;
        this.targetDataStore = targetDataStore;
        this.transformationExtension = transformationExtension;
        this.sameModelInTransformation = sameModelInTransformation;
    }

    @Override
    public String getJoinCondition() {
        return joinCondition;
    }

    @Override
    public DataStoreWithAlias getLookupDataStore() {
        return lookupDataStore;
    }

    @Override
    public LookupTypeEnum getLookupType() {
        return lookupType;
    }

    @Override
    public Map<String, PropertyValueHolder> getProperties() {
        return properties;
    }

    @Override
    public DataStoreWithAlias getSourceDataStore() {
        return sourceDataStore;
    }
	
	/*
	@Override
	public SourceExtension getSourceExtension() {
		return sourceExtension;
	}
	*/

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
}
