package one.jodi.core.metadata;

import com.google.inject.Inject;
import one.jodi.base.annotations.Cached;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.metadata.types.EtlStrategyDescriptor;
import one.jodi.core.metadata.types.KnowledgeModule;
import one.jodi.etl.km.KnowledgeModuleType;
import oracle.odi.core.OdiInstance;
import oracle.odi.domain.adapter.project.IKnowledgeModule.KMType;
import oracle.odi.domain.project.*;
import oracle.odi.domain.project.finder.IOdiKMFinder;
import oracle.odi.domain.project.finder.IOdiProjectFinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class ETLSubsystemServiceImpl implements ETLSubsystemService {

    private final static Logger log = LogManager.getLogger(ETLSubsystemServiceImpl.class);
    private final OdiInstance odiInstance;
    private final JodiProperties properties;


    @Inject
    public ETLSubsystemServiceImpl(
            final JodiProperties properties,
            final OdiInstance odiInstance) {
        this.properties = properties;
        this.odiInstance = odiInstance;
    }

    private KnowledgeModule.KMOptionType mapType(EtlStrategyDescriptor.OptionType inType) {

        KnowledgeModule.KMOptionType mapped = null;

        switch (inType) {
            case CHECKBOX:
                mapped = KnowledgeModule.KMOptionType.CHECKBOX;
                break;
            case SHORT_TEXT:
                mapped = KnowledgeModule.KMOptionType.SHORT_TEXT;
                break;
            case LONG_TEXT:
                mapped = KnowledgeModule.KMOptionType.LONG_TEXT;
                break;
        }

        return mapped;
    }

    @Override
    public List<KnowledgeModule> getKMs() {

        List<KnowledgeModule> kms = new ArrayList<>();

        for (final EtlStrategyDescriptor km : getKMstrategy()) {
            KnowledgeModule newKM = new KnowledgeModule() {
                @Override
                public KnowledgeModuleType getType() {
                    return km.getType();
                }

                @Override
                public String getName() {
                    return km.getName();
                }

                @Override
                public boolean isMultiTechnology() {
                    return km.isMultiTechnology();
                }

                @Override
                public Map<String, KMOptionType> getOptions() {
                    Map<String, KMOptionType> options = new HashMap<>();
                    for (Map.Entry<String, EtlStrategyDescriptor.OptionType> o : km.getOptions().entrySet()) {
                        options.put(o.getKey(), mapType(o.getValue()));
                    }
                    return options;
                }
            };
            kms.add(newKM);
        }
        return kms;
    }

    @Cached
    @Override
    public List<EtlStrategyDescriptor> getKMstrategy() {
        ArrayList<EtlStrategyDescriptor> list = new ArrayList<>();
        IOdiProjectFinder finder = ((IOdiProjectFinder) odiInstance
                .getTransactionalEntityManager()
                .getFinder(OdiProject.class));
        OdiProject odiProject = finder.findByCode(properties.getProjectCode());
        addOdiKMsToList(odiProject.getCKMs(), list, KnowledgeModuleType.Check);
        addOdiKMsToList(odiProject.getIKMs(), list,
                KnowledgeModuleType.Integration);
        addOdiKMsToList(odiProject.getLKMs(), list, KnowledgeModuleType.Loading);
        addOdiKMsToList(odiProject.getJKMs(), list,
                KnowledgeModuleType.Journalization);
        IOdiKMFinder finderIKMs = ((IOdiKMFinder) odiInstance
                .getTransactionalEntityManager()
                .getFinder(oracle.odi.domain.project.OdiKM.class));

        Collection<? extends OdiKM> globalKMs = finderIKMs.findAllGlobals();
        List<OdiCKM> globalCKMs = (List<OdiCKM>) globalKMs.stream().filter(odiKm -> odiKm.getKMType().equals(KMType.CKM)).collect(Collectors.toList());
        List<OdiIKM> globalIKMs = (List<OdiIKM>) globalKMs.stream().filter(odiKm -> odiKm.getKMType().equals(KMType.IKM)).collect(Collectors.toList());
        List<OdiLKM> globalLKMs = (List<OdiLKM>) globalKMs.stream().filter(odiKm -> odiKm.getKMType().equals(KMType.LKM)).collect(Collectors.toList());
        List<OdiJKM> globalJKMs = (List<OdiJKM>) globalKMs.stream().filter(odiKm -> odiKm.getKMType().equals(KMType.JKM)).collect(Collectors.toList());


//        globalIKMs.forEach(ik -> ik.getOptions().forEach(o -> log.info(String.format("IKM %s has option %s ", ik.getName(), o.getName()))));
//        globalIKMs.stream().filter(i -> i.getBaseComponentKM() != null)
//                .forEach(ik -> log.info(String.format("IKM %s has base %s", ik.getName(), ik.getBaseComponentKM().getName())));
//        globalIKMs.stream().filter(i -> i.getBaseComponentKM() != null)
//                .forEach(ik -> ik.getBaseComponentKM().getOptions()
//                        .forEach(o -> log.info(String.format("Inhertited option %s for km %s ", ((ProcedureOption) o).getName(), ik.getName()))));

        addOdiKMsToList(
                globalCKMs, list, KnowledgeModuleType.Check);
        addOdiKMsToList((Collection<? extends OdiKM<? extends OdiProcedureLine>>)
                globalIKMs, list, KnowledgeModuleType.Integration);
        addOdiKMsToList((Collection<? extends OdiKM<? extends OdiProcedureLine>>)
                globalLKMs, list, KnowledgeModuleType.Loading);
        addOdiKMsToList((Collection<? extends OdiKM<? extends OdiProcedureLine>>)
                globalJKMs, list, KnowledgeModuleType.Journalization);

        return list;
    }

    private void addOdiKMsToList(
            Collection<? extends OdiKM<? extends OdiProcedureLine>> odiList,
            List<EtlStrategyDescriptor> list, final KnowledgeModuleType type) {
        if (odiList != null) {
            for (final OdiKM<? extends OdiProcedureLine> km : odiList) {
                list.add(new EtlStrategyDescriptor() {

                    @Override
                    public KnowledgeModuleType getType() {
                        return type;
                    }

                    @Override
                    public String getName() {
                        return km.getName().trim().replaceAll("\\s{2,}", " ");
                    }

                    @Override
                    public boolean isMultiTechnology() {
                        return km.isMultiConnectionSupported();
                    }

                    @Override
                    public Map<String, EtlStrategyDescriptor.OptionType> getOptions() {
                        HashMap<String, EtlStrategyDescriptor.OptionType> options =
                                new HashMap<>();
                        for (String name : km.getOptionNames()) {
                            switch (km.getOption(name).getOptionType()) {
                                case CHECKBOX:
                                    options.put(name, OptionType.CHECKBOX);
                                    break;
                                case LONG_TEXT:
                                    options.put(name, OptionType.LONG_TEXT);
                                    break;
                                case CHOICE:
                                    options.put(name, OptionType.CHOICE);
                                    break;
                                case SHORT_TEXT:
                                    options.put(name, OptionType.SHORT_TEXT);
                                    break;
                            }
                        }
                        if (type.equals(KnowledgeModuleType.Integration) && km != null) {
                            addBaseOptions(((OdiIKM) km).getBaseComponentKM(), odiList, options, type);
                        }
                        if (type.equals(KnowledgeModuleType.Loading) && km != null) {
                            addBaseOptions(((OdiLKM) km).getBaseComponentKM(), odiList, options, type);
                        }
                        if (type.equals(KnowledgeModuleType.Check) && km != null) {
                            addBaseOptions(((OdiCKM) km).getBaseComponentKM(), odiList, options, type);
                        }
                        return options;
                    }
                });
            }
        }
    }

    private void addBaseOptions(ComponentKM km,
                                Collection<? extends OdiKM<? extends OdiProcedureLine>> odiList,
                                HashMap<String, EtlStrategyDescriptor.OptionType> options,
                                KnowledgeModuleType type) {
        if (type.equals(KnowledgeModuleType.Integration) && km != null) {
            for (Object name : ((OdiIKM) km).getOptionNames()) {
                switch (km.getOption(name.toString()).getOptionType()) {
                    case CHECKBOX:
                        options.put(name.toString(), EtlStrategyDescriptor.OptionType.CHECKBOX);
                        break;
                    case LONG_TEXT:
                        options.put(name.toString(), EtlStrategyDescriptor.OptionType.LONG_TEXT);
                        break;
                    case CHOICE:
                        options.put(name.toString(), EtlStrategyDescriptor.OptionType.CHOICE);
                        break;
                    case SHORT_TEXT:
                        options.put(name.toString(), EtlStrategyDescriptor.OptionType.SHORT_TEXT);
                        break;
                }
            }
            addBaseOptions(((OdiIKM) km).getBaseComponentKM(), odiList, options, type);
        }

        if (type.equals(KnowledgeModuleType.Loading) && km != null) {
            for (Object name : ((OdiLKM) km).getOptionNames()) {
                switch (km.getOption(name.toString()).getOptionType()) {
                    case CHECKBOX:
                        options.put(name.toString(), EtlStrategyDescriptor.OptionType.CHECKBOX);
                        break;
                    case LONG_TEXT:
                        options.put(name.toString(), EtlStrategyDescriptor.OptionType.LONG_TEXT);
                        break;
                    case CHOICE:
                        options.put(name.toString(), EtlStrategyDescriptor.OptionType.CHOICE);
                        break;
                    case SHORT_TEXT:
                        options.put(name.toString(), EtlStrategyDescriptor.OptionType.SHORT_TEXT);
                        break;
                }
            }
            addBaseOptions(((OdiLKM) km).getBaseComponentKM(), odiList, options, type);
        }

        if (type.equals(KnowledgeModuleType.Check) && km != null) {
            for (Object name : ((OdiCKM) km).getOptionNames()) {
                switch (km.getOption(name.toString()).getOptionType()) {
                    case CHECKBOX:
                        options.put(name.toString(), EtlStrategyDescriptor.OptionType.CHECKBOX);
                        break;
                    case LONG_TEXT:
                        options.put(name.toString(), EtlStrategyDescriptor.OptionType.LONG_TEXT);
                        break;
                    case CHOICE:
                        options.put(name.toString(), EtlStrategyDescriptor.OptionType.CHOICE);
                        break;
                    case SHORT_TEXT:
                        options.put(name.toString(), EtlStrategyDescriptor.OptionType.SHORT_TEXT);
                        break;
                }
            }
            addBaseOptions(((OdiCKM) km).getBaseComponentKM(), odiList, options, type);
        }
    }
}

