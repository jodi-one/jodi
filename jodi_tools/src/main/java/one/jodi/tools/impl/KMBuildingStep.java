package one.jodi.tools.impl;

import com.google.inject.Inject;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.config.km.KnowledgeModuleProperties;
import one.jodi.core.config.km.KnowledgeModulePropertiesProvider;
import one.jodi.core.model.impl.KmOptionImpl;
import one.jodi.core.model.impl.KmOptionsImpl;
import one.jodi.core.model.impl.KmTypeImpl;
import one.jodi.core.model.impl.MappingsImpl;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.etl.km.KnowledgeModuleType;
import one.jodi.tools.ModelBuildingStep;
import one.jodi.tools.dependency.MappingHolder;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.adapter.project.IOptionValue;
import oracle.odi.domain.mapping.Mapping;
import oracle.odi.domain.mapping.exception.MappingException;
import oracle.odi.domain.mapping.physical.MapPhysicalDesign;
import oracle.odi.domain.mapping.physical.MapPhysicalNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class KMBuildingStep implements ModelBuildingStep {

    private final static String IKM_OPTION = "tools.explicit_ikm";
    private final KnowledgeModulePropertiesProvider knowledgeModulePropertiesProvider;
    private final JodiProperties properties;
    private final Logger logger = LogManager.getLogger(KMBuildingStep.class);

    @Inject
    public KMBuildingStep(KnowledgeModulePropertiesProvider knowledgeModulePropertiesProvider, JodiProperties properties) {
        this.knowledgeModulePropertiesProvider = knowledgeModulePropertiesProvider;
        this.properties = properties;
    }


    /* (non-Javadoc)
     * @see one.jodi.core.service.KMModelBuildingStep#processPostEnrichment(one.jodi.core.service.ExternalTransformation, one.jodi.etl.internalmodel.Transformation, oracle.odi.domain.mapping.Mapping)
     */
    @Override
    public void processPostEnrichment(one.jodi.core.model.Transformation externalTransformation, Transformation transformation, Mapping mapping, MappingHolder mappingHolder) {

    }


    /* (non-Javadoc)
     * @see one.jodi.core.service.KMModelBuildingStep#processPreEnrichment(one.jodi.etl.internalmodel.Transformation, oracle.odi.domain.mapping.Mapping)
     */
    @Override
    public void processPreEnrichment(one.jodi.core.model.Transformation transformation, Mapping mapping, MappingHolder mappingHolder) {

        try {
            if (properties.getPropertyKeys().contains(IKM_OPTION) && Boolean.parseBoolean(properties.getProperty(IKM_OPTION))) {
                addIkm(transformation, mapping);
            }

        } catch (AdapterException ae) {
            ae.printStackTrace();
            logger.error("ODI issues received in building IKM");
            throw new RuntimeException("ODI issues received in building IKM");
        } catch (MappingException me) {
            me.printStackTrace();
            logger.error("ODI issues received in building IKM");
            throw new RuntimeException("ODI issues received in building IKM");
        }

    }


    private void addIkm(one.jodi.core.model.Transformation transformation, Mapping mapping) throws AdapterException, MappingException {
        MappingsImpl mappings = (MappingsImpl) transformation.getMappings();

        String targetName = mapping.getTargets().get(0).getName();

        for (MapPhysicalDesign mpd : mapping.getPhysicalDesigns()) {
            for (MapPhysicalNode mpn : mpd.getPhysicalNodes()) {
                if (mpn.isIKMNode() && targetName.equals(mpn.getName())) {

                    KmTypeImpl km = new KmTypeImpl();
                    mappings.setIkm(km);
                    for (KnowledgeModuleProperties kmp : knowledgeModulePropertiesProvider.getProperties(KnowledgeModuleType.Integration)) {
                        if (kmp.getName().get(0).equalsIgnoreCase(mpn.getIKMName())) {
                            logger.warn("setting IKM = " + kmp.getId() + " for " + mpn.getName());
                            km.setCode(kmp.getId());
                        }
                    }
                    if (km.getCode() == null)
                        logger.error("Properties file contains no rule for IKM with name " + mpn.getName());

                    KmOptionsImpl kmOptions = new KmOptionsImpl();
                    km.setKmOptions(kmOptions);
                    try {
                        for (IOptionValue iov : mpn.getIKMOptionValues()) {
                            if (iov.isDefaultValue())
                                continue;
                            KmOptionImpl kmOption = new KmOptionImpl();
                            kmOption.setName(iov.getName());
                            kmOption.setValue(iov.getOptionValueString());
                            kmOptions.getKmOption().add(kmOption);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Cannot set IKM values");
                    }
                }

            }
        }
    }


}
