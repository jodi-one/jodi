package one.jodi.base.model.types.impl;

import one.jodi.base.model.types.DataModel;
import one.jodi.base.model.types.ModelSolutionLayer;
import one.jodi.base.model.types.ModelSolutionLayerType;

import java.io.Serializable;
import java.util.Map;

public class DataModelImpl implements DataModel, Serializable {

    private static final long serialVersionUID = -2433661852640930876L;

    private final String modelCode;
    private final String dataServerName;
    private final String physicalDataServerName;
    private final String dataServerTechnology;
    private final String schemaName;
    private final Map<String, Object> modelFlexfields;
    private final ModelSolutionLayer solutionLayer;
    private final boolean modelIgnoredbyHeuristics;
    private final String dataBaseServiceName;
    private final int dataBaseServicePort;


    public DataModelImpl(final String modelCode, final String dataServerName,
                         final String physicalDataServerName,
                         final String dataServerTechnology,
                         final String schemaName,
                         final Map<String, Object> modelFlexfields,
                         final String layerName,
                         final boolean modelIgnoredbyHeuristics,
                         final String dataBaseServiceName,
                         final int dataBaseServicePort) {
        this.modelCode = modelCode;
        this.dataServerName = dataServerName;
        this.physicalDataServerName = physicalDataServerName;
        this.dataServerTechnology = dataServerTechnology;
        this.schemaName = schemaName;
        this.modelFlexfields = modelFlexfields;
        this.solutionLayer = ModelSolutionLayerType.modelSolutionLayerFor(layerName);
        this.modelIgnoredbyHeuristics = modelIgnoredbyHeuristics;
        this.dataBaseServiceName = dataBaseServiceName;
        this.dataBaseServicePort = dataBaseServicePort;
    }

    @Override
    public String getModelCode() {
        return modelCode;
    }

    @Override
    public String getDataServerName() {
        return dataServerName;
    }

    @Override
    public String getPhysicalDataServerName() {
        return physicalDataServerName;
    }

    @Override
    public String getDataServerTechnology() {
        return dataServerTechnology;
    }

    @Override
    public String getSchemaName() {
        return schemaName;
    }

    @Override
    public Map<String, Object> getModelFlexfields() {
        return modelFlexfields;
    }

    @Override
    public ModelSolutionLayer getSolutionLayer() {
        return solutionLayer;
    }

    @Override
    public boolean isModelIgnoredbyHeuristics() {
        return modelIgnoredbyHeuristics;
    }

    @Override
    public String getDataBaseServiceName() {
        return dataBaseServiceName;
    }

    @Override
    public int getDataBaseServicePort() {
        return dataBaseServicePort;
    }
}
