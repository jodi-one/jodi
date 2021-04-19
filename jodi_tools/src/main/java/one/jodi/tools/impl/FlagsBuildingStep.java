package one.jodi.tools.impl;

import com.google.inject.Inject;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.model.Transformation;
import one.jodi.core.model.impl.TargetcolumnImpl;
import one.jodi.tools.ModelBuildingStep;
import one.jodi.tools.dependency.MappingHolder;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.mapping.IMapComponent;
import oracle.odi.domain.mapping.MapAttribute;
import oracle.odi.domain.mapping.Mapping;
import oracle.odi.domain.mapping.exception.MappingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class FlagsBuildingStep implements ModelBuildingStep {
    public static final String INSERT = "INSERT";
    public static final String UPDATE = "UPDATE";
    public static final String KEY = "KEY";
    public static final String MANDATORY = "MANDATORY";
    public static final String UD1 = "UD1";
    public static final String UD2 = "UD2";
    public static final String UD3 = "UD3";
    public static final String UD4 = "UD4";
    public static final String UD5 = "UD5";
    public static final String UD6 = "UD6";
    public static final String UD7 = "UD7";
    public static final String UD8 = "UD8";
    public static final String UD9 = "UD9";
    public static final String UD10 = "UD10";


    private final Logger logger = LogManager.getLogger(FlagsBuildingStep.class);
    @SuppressWarnings("unused")
    private final JodiProperties properties;

    @Inject
    public FlagsBuildingStep(JodiProperties properties) {
        this.properties = properties;
    }

    @Override
    public void processPreEnrichment(one.jodi.core.model.Transformation transformation, Mapping mapping, MappingHolder mappingHolder) {
        try {
            pre(transformation, mapping, mappingHolder);
        } catch (MappingException me) {
            handleError(me, mapping);
        }
    }


    private void pre(one.jodi.core.model.Transformation transformation, Mapping mapping, MappingHolder mappingHolder) throws MappingException {
        for (IMapComponent mo : mapping.getTargets()) {
            for (MapAttribute ma : mo.getAttributes()) {

                TargetcolumnImpl targetcolumn = getTargetColumn(transformation, ma.getName());
                extendedPreProcess(targetcolumn, ma.isUpdateIndicator(), ma.isInsertIndicator(), ma.isKeyIndicator(), ma.isCheckNotNullIndicator());
            }
        }
    }

    private TargetcolumnImpl getTargetColumn(one.jodi.core.model.Transformation transformation, String name) {
        for (one.jodi.core.model.Targetcolumn targetcolumn : transformation.getMappings().getTargetColumn()) {
            if (name.equalsIgnoreCase(targetcolumn.getName())) {
                return (TargetcolumnImpl) targetcolumn;
            }
        }

        return null;
    }

    @Override
    public void processPostEnrichment(
            one.jodi.core.model.Transformation transformation,
            one.jodi.etl.internalmodel.Transformation enrichedTransformation,
            Mapping mapping,
            MappingHolder mappingHolder) {

        try {
            generate(mapping, enrichedTransformation, transformation);
        } catch (AdapterException ae) {
            ae.printStackTrace();
            handleError(ae, mapping);
        } catch (MappingException me) {
            me.printStackTrace();
            handleError(me, mapping);
        }

    }

    private void handleError(Exception e, Mapping mapping) {
        String error = "Cannot set target columns for mapping '" + mapping.getName() + "' due to ODI exception";
        logger.error(e.getMessage());
        logger.error(error, e);
        throw new RuntimeException(error, e);
    }


    private void generate(Mapping mapping, one.jodi.etl.internalmodel.Transformation enrichedTransformation, Transformation transformation) throws AdapterException, MappingException {

        for (IMapComponent mo : mapping.getTargets()) {
            if (mo.getAttributes() == null) {
                logger.info("No attributes for " + mapping.getName());
            }
            for (MapAttribute ma : mo.getAttributes()) {
                for (one.jodi.etl.internalmodel.Targetcolumn targetcolumn : enrichedTransformation.getMappings().getTargetColumns()) {
                    if (targetcolumn.getName().equalsIgnoreCase(ma.getName())) {
                        if (ma.isInsertIndicator() != targetcolumn.isInsert()) {
                            logger.warn("Flags issue. Mapping '" + mapping.getName() + "' and target column '" + ma.getName() + "' specifies insert '" + ma.isInsertIndicator() + "' but strategy set '" + targetcolumn.isInsert() + "'.");
                        }

                        if (ma.isUpdateIndicator() != targetcolumn.isUpdate()) {
                            logger.warn("Flags issue. Mapping '" + mapping.getName() + "' and target column '" + ma.getName() + "' specifies update '" + ma.isUpdateIndicator() + "' but strategy set '" + targetcolumn.isUpdate() + "'.");
                        }

                        // If there is a warning the extension must be added
                        TargetcolumnImpl tc = getTargetColumn(transformation, ma.getName());

                        if (tc != null) {
                            HashMap<String, Boolean> flags = new HashMap<String, Boolean>();
                            flags.put(INSERT, ma.isInsertIndicator());
                            flags.put(UPDATE, ma.isUpdateIndicator());
                            flags.put(KEY, ma.isKeyIndicator());
                            flags.put(MANDATORY, ma.isCheckNotNullIndicator());
                            for (int i = 1; i <= 10; i++) {
                                Boolean flag = (Boolean) ma.getPropertyValue("UD_" + i);
                                flags.put("UD" + i, flag);
                            }

                            extendedPostProcess(getTargetColumn(transformation, ma.getName()), targetcolumn, flags);
                        }

                    }
                }
            }
        }
    }

    protected void extendedPostProcess(TargetcolumnImpl targetcolumn, one.jodi.etl.internalmodel.Targetcolumn enrichedTargetcolumn, Map<String, Boolean> flags) {

    }

    protected void extendedPreProcess(TargetcolumnImpl targetcolumn, boolean isUpdate, boolean isInsert, boolean isUpdateKey, boolean isMandatory) {

    }


}
