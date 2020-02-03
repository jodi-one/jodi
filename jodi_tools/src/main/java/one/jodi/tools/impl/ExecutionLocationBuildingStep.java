package one.jodi.tools.impl;

import com.google.inject.Inject;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.model.impl.TargetcolumnImpl;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.tools.ModelBuildingStep;
import one.jodi.tools.ModelMethods;
import one.jodi.tools.dependency.MappingHolder;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.mapping.IMapComponent;
import oracle.odi.domain.mapping.MapAttribute;
import oracle.odi.domain.mapping.Mapping;
import oracle.odi.domain.mapping.exception.MappingException;
import oracle.odi.domain.mapping.expression.MapExpression.ExecuteOnLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExecutionLocationBuildingStep implements ModelBuildingStep {

    private static final Logger logger = LogManager.getLogger(ExecutionLocationBuildingStep.class);
    protected final JodiProperties properties;

    @Inject
    public ExecutionLocationBuildingStep(JodiProperties properties) {
        this.properties = properties;
    }

    @Override
    public void processPreEnrichment(one.jodi.core.model.Transformation transformation, Mapping mapping, MappingHolder mappingHolder) {

    }

    @Override
    public void processPostEnrichment(
            one.jodi.core.model.Transformation externalTransformation,
            Transformation transformation,
            Mapping mapping,
            MappingHolder mappingHolder) {

        try {
            generateWarnings(mapping, transformation);
            processPostEnrichment(mapping, transformation, externalTransformation);
        } catch (AdapterException ae) {
            handleError(ae, mapping);
        } catch (MappingException me) {
            handleError(me, mapping);
        }

    }

    private void generateWarnings(Mapping mapping, one.jodi.etl.internalmodel.Transformation transformation) throws AdapterException, MappingException {

        for (IMapComponent mo : mapping.getTargets()) {
            for (MapAttribute ma : mo.getAttributes()) {
                for (one.jodi.etl.internalmodel.Targetcolumn targetcolumn : transformation.getMappings().getTargetColumns()) {
                    if (targetcolumn.getName().equalsIgnoreCase(ma.getName())) {
                        if (!isEqual(ma.getExecuteOnHint(), targetcolumn.getExecutionLocations().get(0))) {
                            logger.warn("ExecutionLocation issue. Mapping '" + mapping.getName() + "' and target column '" + ma.getName() + "' specifies '" + ma.getExecuteOnHint() + "' but strategy set '" + targetcolumn.getExecutionLocations().get(0) + "'.");
                        }

                        if (ma.isInsertIndicator() != targetcolumn.isInsert()) {
                            logger.warn("Flags issue. Mapping '" + mapping.getName() + "' and target column '" + ma.getName() + "' specifies insert '" + ma.isInsertIndicator() + "' but strategy set '" + targetcolumn.isInsert() + "'.");
                        }

                        if (ma.isUpdateIndicator() != targetcolumn.isUpdate()) {
                            logger.warn("Flags issue. Mapping '" + mapping.getName() + "' and target column '" + ma.getName() + "' specifies update '" + ma.isUpdateIndicator() + "' but strategy set '" + targetcolumn.isUpdate() + "'.");

                        }

                        if (ma.isCheckNotNull() != targetcolumn.isMandatory()) {
                            logger.warn("Flags issue. Mapping '" + mapping.getName() + "' and target column '" + ma.getName() + "' specifies mandatory/checkNotNull '" + ma.isCheckNotNull() + "' but strategy set '" + targetcolumn.isMandatory() + "'.");

                        }

                        if (ma.isKeyIndicator() != targetcolumn.isUpdateKey()) {
                            logger.warn("Flags issue. Mapping '" + mapping.getName() + "' and target column '" + ma.getName() + "' specifies update key '" + ma.isKeyIndicator() + "' but strategy set '" + targetcolumn.isUpdateKey() + "'.");

                        }
                    }
                }
            }
        }
    }

    private void handleError(Exception e, Mapping mapping) {
        String error = "Cannot set target columns for mapping '" + mapping.getName() + "' due to ODI exception";
        logger.error(error, e);
        throw new RuntimeException(error, e);
    }


    private void processPostEnrichment(Mapping mapping, one.jodi.etl.internalmodel.Transformation enrichedTransformation, one.jodi.core.model.Transformation transformation) throws MappingException {
        for (IMapComponent mo : mapping.getTargets()) {
            for (MapAttribute mapAttribute : mo.getAttributes()) {
                String name = mapAttribute.getName();
                one.jodi.etl.internalmodel.Targetcolumn enrichedTargetcolumn = ModelMethods.getTargetcolumn(name, enrichedTransformation);
                TargetcolumnImpl targetcolumn = ModelMethods.getTargetcolumn(name, transformation);
                processPostEnrichment(mapping, mapAttribute, enrichedTargetcolumn, targetcolumn);
            }
        }
    }


    protected void processPostEnrichment(Mapping mapping, MapAttribute mapAttribute, one.jodi.etl.internalmodel.Targetcolumn enrichedTargetcolumn, TargetcolumnImpl targetcolumn) throws MappingException {
        if (!isEqual(mapAttribute.getExecuteOnHint(), enrichedTargetcolumn.getExecutionLocations().get(0))) {
            logger.warn("ExecutionLocation issue. Mapping '" + mapping.getName() + "' and target column '" + mapAttribute.getName() + "' specifies '" + mapAttribute.getExecuteOnHint() +
                    "' but strategy set '" + enrichedTargetcolumn.getExecutionLocations().get(0) + "'.");
        }
    }


    protected boolean isEqual(ExecuteOnLocation eol, one.jodi.etl.internalmodel.ExecutionLocationtypeEnum internalEol) {

        if (eol.equals(ExecuteOnLocation.NO_HINT))
            return true;
        else {
            return eol.name().equals(internalEol.value());
        }

    }


}
