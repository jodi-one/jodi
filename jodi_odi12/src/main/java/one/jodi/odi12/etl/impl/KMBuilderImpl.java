package one.jodi.odi12.etl.impl;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.core.config.JodiProperties;
import one.jodi.etl.internalmodel.ComponentPrefixType;
import one.jodi.etl.internalmodel.Dataset;
import one.jodi.etl.internalmodel.ExecutionLocationtypeEnum;
import one.jodi.etl.internalmodel.Source;
import one.jodi.etl.internalmodel.Targetcolumn;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.etl.service.interfaces.TransformationException;
import one.jodi.odi.etl.OdiCommon;
import one.jodi.odi12.etl.KMBuilder;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.adapter.project.IKnowledgeModule.KMType;
import oracle.odi.domain.adapter.project.IOptionValue;
import oracle.odi.domain.adapter.relational.IColumn;
import oracle.odi.domain.adapter.relational.IKey;
import oracle.odi.domain.mapping.IMapComponent;
import oracle.odi.domain.mapping.MapAttribute;
import oracle.odi.domain.mapping.MapRootContainer;
import oracle.odi.domain.mapping.Mapping;
import oracle.odi.domain.mapping.component.DatastoreComponent;
import oracle.odi.domain.mapping.exception.MappingException;
import oracle.odi.domain.mapping.physical.ExecutionUnit;
import oracle.odi.domain.mapping.physical.MapPhysicalDesign;
import oracle.odi.domain.mapping.physical.MapPhysicalNode;
import oracle.odi.domain.project.OdiKM;
import oracle.odi.mapping.generation.GenerationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class KMBuilderImpl implements KMBuilder {
    private static final Logger logger = LogManager.getLogger(KMBuilderImpl.class);
    private static final String ERROR_MESSAGE_09997 =
            "%3$s: Couldn't set update key for mapping '%1$s' of datastore '%2$s'.";

    private final OdiCommon odiCommon;
    private final JodiProperties properties;
    private final ErrorWarningMessageJodi errorWarningMessages;


    @Inject
    protected KMBuilderImpl(final OdiCommon odiCommon, final JodiProperties properties,
                            ErrorWarningMessageJodi errorWarningMessages) {
        this.properties = properties;
        this.odiCommon = odiCommon;
        this.errorWarningMessages = errorWarningMessages;
    }

    /* (non-Javadoc)
     * @see one.jodi.odi12.mappings.KMBuilder#setLKM(oracle.odi.domain.mapping.MapRootContainer, one.jodi.etl.internalmodel.Transformation)
     */
    @Override
    public void setLKM(final MapRootContainer mapping, final Transformation transformation) throws MappingException,
            AdapterException, GenerationException, TransformationException {

        if (!(mapping instanceof Mapping)) {
            return;
        }
        String IKMname = properties.getProperty(transformation.getMappings()
                                                              .getIkm()
                                                              .getName() + ".name");
        boolean globalIKM = properties.getPropertyKeys()
                                      .contains(transformation.getMappings()
                                                              .getIkm()
                                                              .getName() + ".global") ? Boolean.valueOf(
                properties.getProperty(transformation.getMappings()
                                                     .getIkm()
                                                     .getName() + ".global")) : false;

        OdiKM<?> ikm = !globalIKM ? odiCommon.findIKMByName(IKMname, properties.getProjectCode())
                                  : odiCommon.findIKMByName(IKMname);
        assert ikm != null : "Can't find ikm with name " + IKMname + " globalIkm " + globalIKM;
        logger.debug("Found IKM " + ikm.getName() + " global is " + globalIKM);
        for (Dataset dataset : transformation.getDatasets()) {
            for (Source source : dataset.getSources()) {
                String name = properties.getProperty(source.getLkm()
                                                           .getName() + ".name");
                String globalProperty = source.getLkm()
                                              .getName() + ".global";
                boolean global = properties.getPropertyKeys()
                                           .contains(globalProperty) ? Boolean.valueOf(
                        properties.getProperty(globalProperty)) : false;
                OdiKM<?> lkm = !global ? odiCommon.findLKMByName(name, properties.getProjectCode())
                                       : odiCommon.findLKMByName(name);
                assert lkm != null : "LKM not found " + name;
                logger.debug("LKM found  " + lkm.getName() + " global is " + global);
                for (MapPhysicalDesign physicalDesign : ((Mapping) mapping).getPhysicalDesigns()) {
                    for (MapPhysicalNode node : physicalDesign.getAllAPNodes()) {
                        //
                        logger.debug(node.getName());
                        String alias = source.getAlias() != null ? source.getAlias() : source.getName();
                        if (source.getFilter() != null && source.getFilter()
                                                                .length() > 2 &&
                                source.getFilterExecutionLocation() != null && source.getFilterExecutionLocation()
                                                                                     .equals(ExecutionLocationtypeEnum.SOURCE)) {
                            alias = ComponentPrefixType.FILTER.getAbbreviation() + "_" +
                                    ComponentPrefixType.DATASET.getAbbreviation() + source.getParent()
                                                                                          .getDataSetNumber() + alias +
                                    "_AP";
                        } else {
                            alias = ComponentPrefixType.DATASET.getAbbreviation() + source.getParent()
                                                                                          .getDataSetNumber() + alias +
                                    "_AP";
                        }
                        if (alias.length() > 30) {
                            alias = alias.substring(0, 30);
                        }
                        if (!alias.equalsIgnoreCase(node.getName())) {
                            logger.debug(String.format("alias %s doesn't equal %s", alias, node.getName()));
                            continue;
                        }
                        if (ikm.isMultiConnectionSupported()) {
                            if (!lkm.isMultiConnectionSupported()) {
                                throw new TransformationException(String.format(
                                        "This LKM %1$s is not multi connection supported, and the IKM %2$s is multiconnection supported, please choose a LKM which is multiconnection supported by placing tags in your xml.",
                                        lkm.getName(), ikm.getName()));
                            } else {
                                logger.debug(String.format("LKM set to '%1$s' and multiconnect supported is %2$s.",
                                                           lkm.getName(), lkm.isMultiConnectionSupported() + ""));
                                node.setLKMByName(lkm.getName());
                            }
                        } else {
                            logger.debug(String.format(
                                    "LKM set to '%1$s' and multiconnect supported is %2$s for source %3$s.",
                                    lkm.getName(), lkm.isMultiConnectionSupported() + "", node.getName()));
                            node.setLKMByName(lkm.getName());
                        }
                        applyKnowledgeModuleConfigurationSettings(mapping,
                                                                  oracle.odi.domain.adapter.project.IKnowledgeModule.KMType.LKM,
                                                                  node.getLKMOptionValues(), node, source, lkm);
                        assert node.getLKM()
                                   .getName()
                                   .equalsIgnoreCase(name) : "Source LKM not set to " + name + " but to name " +
                                node.getLKM()
                                    .getName();
                    }
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see one.jodi.odi12.mappings.KMBuilder#setIKM(oracle.odi.domain.mapping.MapRootContainer, one.jodi.etl.internalmodel.Transformation, java.util.List)
     */
    @Override
    public void setIKM(final MapRootContainer mapping, final Transformation transformation,
                       final List<IMapComponent> targetComponents) throws AdapterException, MappingException,
            GenerationException, TransformationException {
        String name = properties.getProperty(transformation.getMappings()
                                                           .getIkm()
                                                           .getName() + ".name");
        boolean global = properties.getPropertyKeys()
                                   .contains(transformation.getMappings()
                                                           .getIkm()
                                                           .getName() + ".global") ? Boolean.valueOf(
                properties.getProperty(transformation.getMappings()
                                                     .getIkm()
                                                     .getName() + ".global")) : false;
        OdiKM<?> ikm =
                !global ? odiCommon.findIKMByName(name, properties.getProjectCode()) : odiCommon.findIKMByName(name);
        assert ikm != null : "Can't find ikm " + name + " global set to " + global;
        if (transformation.isTemporary()) {
            return;
        }
        boolean foundAtLeastOneUpldateKey = false;
        if (targetComponents.get(0) instanceof DatastoreComponent) {
            for (IKey key : ((DatastoreComponent) targetComponents.get(0)).getBoundDataStore()
                                                                          .getKeys()) {
                if (key != null && key.isAK()) {
                    // there are no update keys in xml
                    // but we do use an IKM that supports updating
                    // so we use AK
                    ((DatastoreComponent) targetComponents.get(0)).setUpdateKey(key);
                    foundAtLeastOneUpldateKey = true;
                    setUpdateKeyToUpdateTrue(key, transformation, targetComponents);
                }
            }
            if (transformation.getMappings()
                              .hasUpdateKeys()) {
                // there are update keys specified in xml so we use that
                List<String> columnNames = transformation.getMappings()
                                                         .getTargetColumns()
                                                         .stream()
                                                         .filter(Targetcolumn::isUpdateKey)
                                                         .map(Targetcolumn::getName)
                                                         .collect(Collectors.toList());
                @SuppressWarnings("unchecked") IKey key = getKeyFromExplicitlySpecifiedKeys(
                        (Collection<IKey>) ((DatastoreComponent) targetComponents.get(0)).getBoundDataStore()
                                                                                         .getKeys(), columnNames);
                if (key != null) {
                    ((DatastoreComponent) targetComponents.get(0)).setUpdateKey(key);
                    foundAtLeastOneUpldateKey = true;
                    setUpdateKeyToUpdateTrue(key, transformation, targetComponents);
                } else {
                    String message = errorWarningMessages.formatMessage(9997, ERROR_MESSAGE_09997, this.getClass(),
                                                                        transformation.getName(),
                                                                        targetComponents.get(0)
                                                                                        .getName(),
                                                                        transformation.getPackageSequence());
                    errorWarningMessages.addMessage(transformation.getPackageSequence(), message,
                                                    MESSAGE_TYPE.WARNINGS);
                }
            }
            ((DatastoreComponent) targetComponents.get(0)).setIntegrationType(ikm.getIntegrationType());
            logger.debug(ikm.getIntegrationType());
        }
        if (!foundAtLeastOneUpldateKey && transformation.getMappings()
                                                        .hasUpdateKeys()) {
            String message = errorWarningMessages.formatMessage(9997, ERROR_MESSAGE_09997, this.getClass(),
                                                                transformation.getName(), targetComponents.get(0)
                                                                                                          .getName(),
                                                                transformation.getPackageSequence());
            errorWarningMessages.addMessage(transformation.getPackageSequence(), message, MESSAGE_TYPE.WARNINGS);

        }
        for (MapPhysicalDesign pd : ((Mapping) mapping).getPhysicalDesigns()) {
            // here are the target IKMs set
            for (ExecutionUnit teu : pd.getTargetExecutionUnits()) {
                for (MapPhysicalNode target : teu.getTargetNodes()) {
                    assert (target != null);
                    logger.debug("IKM global set to: " + global);
                    target.setIKMByName(ikm.getName());
                    applyKnowledgeModuleConfigurationSettings(mapping,
                                                              oracle.odi.domain.adapter.project.IKnowledgeModule.KMType.IKM,
                                                              transformation.getMappings()
                                                                            .getIkm()
                                                                            .getOptions(), target);
                }
            }
        }
    }

    /**
     * Set the update indicator to true on the mapping on the mapattributes of
     * the key.
     *
     * @param key
     * @param transformation
     * @param targetComponents
     * @throws AdapterException @throws MappingException
     */
    private void setUpdateKeyToUpdateTrue(final IKey key, final Transformation transformation,
                                          final List<IMapComponent> targetComponents) throws AdapterException,
            MappingException {
        // in odi12 it is required to set the columns of the key to update =
        // true;
        for (IColumn col : key.getColumns()) {
            for (MapAttribute ma : targetComponents.get(0)
                                                   .getAttributes()) {
                if (ma.getName()
                      .equals(col.getName())) {
                    ma.setUpdateIndicator(true);
                }
            }
        }
    }

    /**
     * @param keys to choose from @param columnNames of the key @return one of
     *             the keys wich holds all columnNames.
     */
    private IKey getKeyFromExplicitlySpecifiedKeys(Collection<IKey> keys, List<String> columnNames) {
        for (IKey key : keys) {
            Set<String> keyColumnsSet = key.getColumns()
                                           .stream()
                                           .map((Function<IColumn, String>) IColumn::getName)
                                           .collect(Collectors.toSet());
            if (keyColumnsSet.containsAll(columnNames)) {
                return key;
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see one.jodi.odi12.mappings.KMBuilder#setCKM(oracle.odi.domain.mapping.MapRootContainer, one.jodi.etl.internalmodel.Transformation)
     */
    @Override
    public void setCKM(final MapRootContainer mapping, final Transformation transformation) throws AdapterException,
            MappingException, GenerationException, TransformationException {
        if (transformation.getMappings()
                          .getCkm() != null) {
            String name = properties.getProperty(transformation.getMappings()
                                                               .getCkm()
                                                               .getName() + ".name");
            boolean global = properties.getPropertyKeys()
                                       .contains(transformation.getMappings()
                                                               .getCkm()
                                                               .getName() + ".global") ? Boolean.valueOf(
                    properties.getProperty(transformation.getMappings()
                                                         .getCkm()
                                                         .getName() + ".global")) : false;

            OdiKM<?> ckm = !global ? odiCommon.findCKMByName(name, properties.getProjectCode())
                                   : odiCommon.findCKMByName(name);
            if (mapping instanceof Mapping) {
                for (MapPhysicalDesign pd : ((Mapping) mapping).getPhysicalDesigns()) {
                    for (ExecutionUnit teu : pd.getTargetExecutionUnits()) {
                        for (MapPhysicalNode target : teu.getTargetNodes()) {
                            target.setCheckKMByName(ckm.getName());
                            //
                            applyKnowledgeModuleConfigurationSettings(mapping, KMType.CKM, transformation.getMappings()
                                                                                                         .getCkm()
                                                                                                         .getOptions(),
                                                                      target);
                        }
                    }
                }
            }
        }
    }

    /**
     * Apply KM options for CKM and IKM
     *
     * @param mapping
     * @param km
     * @param options
     * @throws AdapterException @throws MappingException @throws
     *                          GenerationException @throws TransformationException
     */
    private void applyKnowledgeModuleConfigurationSettings(final MapRootContainer mapping, final KMType km,
                                                           final Map<String, String> options,
                                                           final MapPhysicalNode target) throws AdapterException,
            MappingException, GenerationException, TransformationException {
        assert (target != null);
        if (mapping instanceof Mapping && km.equals(oracle.odi.domain.adapter.project.IKnowledgeModule.KMType.IKM)) {
            for (Entry<String, String> option : options.entrySet()) {
                if (target.getIKM() != null && target.getIKM()
                                                     .getName() != null) {
                    validateKMOption(target.getIKM()
                                           .getName(), option.getKey(), target.getIKMOptionValues());
                }
                if (option.getValue() != null) {
                    target.getIKMOptionValue(option.getKey())
                          .setValue(option.getValue());
                }
            }
        }
        if (mapping instanceof Mapping && km.equals(oracle.odi.domain.adapter.project.IKnowledgeModule.KMType.CKM)) {
            for (Entry<String, String> option : options.entrySet()) {
                if (target.getCheckKM() != null && target.getCheckKMOptionValues() != null &&
                        target.getCheckKMOptionValues()
                              .size() > 0) {
                    validateKMOption(target.getCheckKM()
                                           .getName(), option.getKey(), target.getCheckKMOptionValues());
                    target.getCheckKMOptionValue(option.getKey())
                          .setValue(option.getValue());
                }
            }
        }
    }

    /***
     *
     * @param KMname
     * @param key
     * @param ikmOptionValues
     *            @throws TransformationException
     */
    private void validateKMOption(final String KMname, final String key,
                                  final List<IOptionValue> ikmOptionValues) throws TransformationException {
        boolean found = false;
        for (IOptionValue optionVal : ikmOptionValues) {
            if (optionVal.getName()
                         .equals(key)) {
                found = true;
            }
        }
        if (!found) {
            throw new TransformationException(String.format("Option %1$s not found for KM %2$s.", key, KMname));
        }
    }

    /**
     * Apply KM options for LKM
     *
     * @param mapping
     * @param lkm
     * @param lkmOptionValues
     * @param node            @param source @param lkmModule @throws
     *                        AdapterException @throws MappingException @throws
     *                        TransformationException
     */
    private void applyKnowledgeModuleConfigurationSettings(final MapRootContainer mapping, final KMType lkm,
                                                           final List<IOptionValue> lkmOptionValues,
                                                           final MapPhysicalNode node, final Source source,
                                                           @SuppressWarnings("rawtypes") final OdiKM lkmModule) throws
            AdapterException, MappingException, TransformationException {
        for (String key : source.getLkm()
                                .getOptions()
                                .keySet()) {
            validateKMOption(lkmModule.getName(), key, node.getLKMOptionValues());
            Object value = source.getLkm()
                                 .getOptions()
                                 .get(key);
            if (value.toString()
                     .toLowerCase()
                     .equals("true") || value.toString()
                                             .toLowerCase()
                                             .equals("false")) {
                Boolean valu = Boolean.valueOf(node.getLKMOptionValue(key)
                                                   .toString());
                valu = Boolean.valueOf(value.toString());
                for (IOptionValue option : node.getLKMOptionValues()) {
                    if (option.getName()
                              .equals(key)) {
                        logger.debug("Set KMOption: " + key + " = " + value);
                        option.setValue(valu);
                    }
                }
            } else {
                String valu = node.getLKMOptionValue(key)
                                  .toString();
                valu = value.toString();
                for (IOptionValue option : node.getLKMOptionValues()) {
                    if (option.getName()
                              .equals(key)) {
                        logger.debug("Set KMOption: " + key + " = " + value);
                        option.setValue(valu);
                    }
                }
            }
        }
    }

}
