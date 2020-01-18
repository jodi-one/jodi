package one.jodi.tools.impl;

import com.google.inject.Inject;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.model.SetOperatorTypeEnum;
import one.jodi.core.model.Transformation;
import one.jodi.core.model.impl.DatasetImpl;
import one.jodi.core.model.impl.DatasetsImpl;
import one.jodi.core.model.impl.SourceImpl;
import one.jodi.tools.ModelBuildingStep;
import one.jodi.tools.dependency.MappingHolder;
import one.jodi.tools.dependency.MappingType;
import oracle.odi.domain.adapter.relational.IDataStore;
import oracle.odi.domain.mapping.IMapComponent;
import oracle.odi.domain.mapping.Mapping;
import oracle.odi.domain.mapping.component.DatastoreComponent;
import oracle.odi.domain.mapping.component.SetComponent;
import oracle.odi.domain.mapping.exception.MapComponentException;
import oracle.odi.domain.mapping.exception.MappingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SourceBuildingStep implements ModelBuildingStep {

    @SuppressWarnings("unused")
    private final JodiProperties properties;

    private final Logger logger = LogManager.getLogger(SourceBuildingStep.class);

    @Inject
    public SourceBuildingStep(final JodiProperties properties) {
        this.properties = properties;
    }

    @Override
    public void processPreEnrichment(Transformation transformation,
                                     Mapping mapping,
                                     MappingHolder mappingHolder) {

        try {
            pre(transformation, mapping, mappingHolder);
        } catch (MapComponentException mce) {
            handleError(mce, mapping);
        } catch (MappingException me) {
            handleError(me, mapping);
        }
    }


    private void handleError(Exception e, Mapping mapping) {
        String error = "Cannot set sources for for mapping '" + mapping.getName() + "' due to ODI exception";
        logger.equals(error);
        e.printStackTrace();
        throw new RuntimeException(error);
    }

    /*
     * NOT_DEFINED("NOT_DEFINED"),
    MINUS("MINUS"),
    UNION("UNION"),
    UNION_ALL("UNION ALL"),
    INTERSECT("INTERSECT");
     */
    private void pre(Transformation transformation, Mapping mapping, MappingHolder mappingHolder) throws MappingException {

        DatasetsImpl datasets = new DatasetsImpl();
        transformation.setDatasets(datasets);

        if (MappingType.SourceToSetToExpressionToTarget == mappingHolder.getType()) {
            SetComponent setComponent = (SetComponent) mapping.getAllComponentsOfType(SetComponent.COMPONENT_TYPE_NAME).get(0);
            int index = 0;
            for (IMapComponent mo : mapping.getSources()) {
                DatasetImpl dataset = new DatasetImpl();
                datasets.getDataset().add(dataset);
                DatastoreComponent dsc = (DatastoreComponent) mo;
                IDataStore ids = dsc.getBoundDataStore();
                SourceImpl s = new SourceImpl();
                s.setName(ids.getName());
                s.setAlias(dsc.getName());
                dataset.getSource().add(s);
                if (index > 0) {
                    String setOperationString = setComponent.getSetOperationType(dsc.getQualifiedName());
                    try {
                        dataset.setSetOperator(SetOperatorTypeEnum.fromValue(setOperationString));
                    } catch (IllegalArgumentException iae) {
                        logger.error("Mapping '" + mapping.getName() + "' contains set operator '" + setOperationString + "' that is not supported by Jodi");
                    }
                }
                index++;
            }
        } else {
            DatasetImpl dataset = new DatasetImpl();
            datasets.getDataset().add(dataset);
            for (IMapComponent mo : mapping.getSources()) {
                DatastoreComponent dsc = (DatastoreComponent) mo;
                IDataStore ids = dsc.getBoundDataStore();
                SourceImpl s = new SourceImpl();
                s.setName(ids.getName());
                s.setAlias(dsc.getName());
                dataset.getSource().add(s);
            }
        }

    }

    @Override
    public void processPostEnrichment(Transformation externalTransformation,
                                      one.jodi.etl.internalmodel.Transformation transformation,
                                      Mapping mapping,
                                      MappingHolder mappingHolder) {
        // TODO Auto-generated method stub

    }

}
