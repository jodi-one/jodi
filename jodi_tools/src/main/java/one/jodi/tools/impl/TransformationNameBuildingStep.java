package one.jodi.tools.impl;

import com.google.inject.Inject;
import one.jodi.core.annotations.InterfacePrefix;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.model.impl.TransformationImpl;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.tools.ModelBuildingStep;
import one.jodi.tools.dependency.MappingHolder;
import oracle.odi.domain.mapping.Mapping;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TransformationNameBuildingStep implements ModelBuildingStep {

    final static String option = "tools.use_mapping_for_transformation_name";
    private final String prefix;
    private final JodiProperties properties;
    private final Logger logger = LogManager.getLogger(TransformationNameBuildingStep.class);

    @Inject
    public TransformationNameBuildingStep(final @InterfacePrefix String prefix,
                                          final JodiProperties properties) {
        this.prefix = prefix;
        this.properties = properties;
    }


    @Override
    public void processPreEnrichment(one.jodi.core.model.Transformation transformation,
                                     Mapping mapping, MappingHolder mappingHolder) {

        if (getBooleanProperty(option, false)) {
            ((TransformationImpl) transformation).setName(getTransformationName(mapping));
        }

    }

    private String getTransformationName(Mapping mapping) {
        String name = mapping.getName();
        boolean p = prefix != null;
        boolean l = prefix.length() > 0;
        boolean s = name.startsWith(prefix);
        boolean nlpl = name.length() > prefix.length();

        if (prefix != null && prefix.length() > 0 && mapping.getName().startsWith(prefix) && name.length() > prefix.length()) {
            return name.substring(prefix.length(), name.length());
        } else {
            logger.warn("Mapping '" + mapping.getName() + "' cannot have prefix removed, using as is.");
            return name;
        }
    }


    @Override
    public void processPostEnrichment(
            one.jodi.core.model.Transformation transformation,
            Transformation enrichedTransformation, Mapping mapping, MappingHolder mappingHolders) {
        if (transformation.getName() == null || transformation.getName().length() < 1) {
            ((TransformationImpl) transformation).setName(enrichedTransformation.getName());
        }

    }


    private Boolean getBooleanProperty(String propertyName, Boolean defaultValue) {
        Boolean value = defaultValue;
        if (properties.getPropertyKeys().contains(propertyName)) {
            value = Boolean.parseBoolean(properties.getProperty(propertyName));
        }

        return value;
    }


}
