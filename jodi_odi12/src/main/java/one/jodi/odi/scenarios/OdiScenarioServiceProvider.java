package one.jodi.odi.scenarios;

import com.google.inject.Inject;
import one.jodi.base.annotations.Cached;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.core.annotations.TransactionAttribute;
import one.jodi.core.annotations.TransactionAttributeType;
import one.jodi.core.config.JodiConstants;
import one.jodi.core.config.JodiProperties;
import one.jodi.etl.common.EtlSubSystemVersion;
import one.jodi.etl.internalmodel.ETLPackageHeader;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.etl.service.ResourceFoundAmbiguouslyException;
import one.jodi.etl.service.ResourceNotFoundException;
import one.jodi.etl.service.scenarios.ScenarioServiceProvider;
import one.jodi.logging.OdiLogHandler;
import one.jodi.odi.interfaces.OdiTransformationAccessStrategy;
import one.jodi.odi.packages.OdiPackageAccessStrategy;
import one.jodi.odi.runtime.OdiConnection;
import oracle.odi.core.OdiInstance;
import oracle.odi.core.persistence.IOdiEntityManager;
import oracle.odi.core.persistence.transaction.ITransactionDefinition;
import oracle.odi.core.persistence.transaction.ITransactionManager;
import oracle.odi.core.persistence.transaction.ITransactionStatus;
import oracle.odi.core.persistence.transaction.support.DefaultTransactionDefinition;
import oracle.odi.domain.mapping.Mapping;
import oracle.odi.domain.mapping.finder.IMappingFinder;
import oracle.odi.domain.project.*;
import oracle.odi.domain.project.finder.IOdiPackageFinder;
import oracle.odi.domain.project.finder.IOdiProjectFinder;
import oracle.odi.domain.project.finder.IOdiUserProcedureFinder;
import oracle.odi.domain.project.finder.IOdiVariableFinder;
import oracle.odi.domain.runtime.scenario.OdiScenario;
import oracle.odi.domain.runtime.scenario.OdiScenarioFolder;
import oracle.odi.domain.runtime.scenario.finder.IOdiScenarioFinder;
import oracle.odi.domain.runtime.scenario.finder.IOdiScenarioFolderFinder;
import oracle.odi.generation.GenerationOptions;
import oracle.odi.generation.IOdiScenarioGenerator;
import oracle.odi.generation.OdiScenarioGeneratorException;
import oracle.odi.generation.support.OdiScenarioGeneratorImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.logging.Level;

public class OdiScenarioServiceProvider implements ScenarioServiceProvider {
    private final static Logger logger =
            LogManager.getLogger(OdiScenarioServiceProvider.class);


    private final static String ERROR_MESSAGE_80720 = "RuntimeException: %s";

    private final OdiInstance odiInstance;
    @SuppressWarnings("rawtypes")
    private final OdiTransformationAccessStrategy odiTransfromationStrategy;
    private final OdiPackageAccessStrategy<Mapping, StepMapping> odiPackageStrategy;
    private final EtlSubSystemVersion etlSubSystemVersion;
    private final JodiProperties properties;
    private final ErrorWarningMessageJodi errorWarningMessages;

    @Inject
    protected OdiScenarioServiceProvider(
            final OdiInstance odiInstance,
            final ErrorWarningMessageJodi errorWarningMessages,
            @SuppressWarnings("rawtypes") final OdiTransformationAccessStrategy odiTransfromationStrategy,
            final OdiPackageAccessStrategy<Mapping, StepMapping> odiPackageStrategy,
            final JodiProperties properties,
            final EtlSubSystemVersion etlSubSystemVersion) {
        this.odiInstance = odiInstance;
        this.odiTransfromationStrategy = odiTransfromationStrategy;
        this.odiPackageStrategy = odiPackageStrategy;
        this.properties = properties;
        this.etlSubSystemVersion = etlSubSystemVersion;
        this.errorWarningMessages = errorWarningMessages;
    }

    @Cached
    public OdiProject getProject() {
        return ((IOdiProjectFinder) odiInstance.getTransactionalEntityManager()
                .getFinder(OdiProject.class))
                .findByCode(properties.getProjectCode());
    }

    @Override // only used from scenario action runner
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deleteScenario(String scenarioName) {
        final DefaultTransactionDefinition txnDef =
                new DefaultTransactionDefinition(ITransactionDefinition.PROPAGATION_MANDATORY);
        final ITransactionManager tm = odiInstance.getTransactionManager();
        final ITransactionStatus txnStatus = tm.getTransaction(txnDef);
        final IOdiEntityManager tme = odiInstance.getTransactionalEntityManager();

        String name = scenarioName.toUpperCase();
        logger.info(String.format("Removing scenario: '%s'", name));
        // there may be more scenarios than shown in the gui.
        @SuppressWarnings("unchecked")
        Collection<OdiScenario> odiScenarios =
                ((IOdiScenarioFinder) tme.getFinder(OdiScenario.class)).findAll();
        for (OdiScenario odiScenario : odiScenarios) {
            if (odiScenario != null && name.equals(odiScenario.getName())) {
                tme.remove(odiScenario);
                logger.info(String.format("Removed scenario: '%s'.", name));
            }
        }
        tm.commit(txnStatus);
    }

    @Override
    public void deleteScenarios(final List<ETLPackageHeader> headers) {
        headers.forEach(s -> deleteScenario(s.getPackageName()));
    }

    private OdiPackage getPackagesByName(final ETLPackageHeader header) {
        OdiPackage odiPackage =
                this.odiPackageStrategy.findPackage(header.getPackageName().toUpperCase(),
                        header.getFolderCode(),
                        properties.getProjectCode());
        return odiPackage;
    }

    @Override
    public void generateAllScenarios(final List<ETLPackageHeader> headers, List<Transformation> transformations) {
        PrintStream original = null;
        try {
            original = new PrintStream(System.out, true, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.warn(e);
        }
        try (PrintStream ps = new PrintStream(new OutputStream() {
            public void write(int b) {
                // DO NOTHING
            }
        }, true, "UTF-8")) {
            System.setOut(ps);
        } catch (UnsupportedEncodingException e) {
            logger.warn(e);
        }
        Set<String> uniquenessCheck = new HashSet<String>();
        for (ETLPackageHeader header : headers) {
            if (uniquenessCheck.contains(header.getPackageName())) {
                continue;
            } else {
                uniquenessCheck.add(header.getPackageName());
            }
            OdiPackage odiPackage = getPackagesByName(header);
            if (odiPackage == null) {
                String msg = String.format("Package '%1$s' was not found in folder '%2$s'.",
                        header.getPackageName(), header.getFolderCode());
                logger.warn(msg);
                continue;
            }
            boolean pGenerateFromPackage = true;
            if (properties.getPropertyKeys()
                    .contains(JodiConstants.ODI12_GENERATE_SCENARIOS_FOR_PACKAGES)) {
                pGenerateFromPackage = Boolean.valueOf(properties.getProperty(
                        JodiConstants.ODI12_GENERATE_SCENARIOS_FOR_PACKAGES));
            } else {
                pGenerateFromPackage = true;
            }
            if (pGenerateFromPackage) {
                processPackage(odiPackage, header.getPackageName());
            }
        }
        processMappingProcedureVariables(transformations);
        System.setOut(original);
        //moveScenariosToFolders();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void processPackage(OdiPackage pck, String packageName) {
        java.util.logging.Logger javautillogger =
                java.util.logging.Logger.getLogger("oracle.odi.mapping");
        javautillogger.setLevel(Level.SEVERE);
        OdiLogHandler odiHandler = new OdiLogHandler(errorWarningMessages);
        odiHandler.setLevel(Level.SEVERE);
        javautillogger.addHandler(odiHandler);

        java.util.logging.Logger javautillogger2 =
                java.util.logging.Logger.getLogger("oracle.odi.core.persistence");
        javautillogger2.setLevel(Level.WARNING);
        OdiLogHandler odiHandler2 = new OdiLogHandler(errorWarningMessages);
        odiHandler2.setLevel(Level.WARNING);
        javautillogger2.addHandler(odiHandler2);

        java.util.logging.Logger javautillogger3 =
                java.util.logging.Logger.getLogger("oracle.odi.scenario.generation");
        javautillogger3.setLevel(Level.WARNING);
        OdiLogHandler odiHandler3 = new OdiLogHandler(errorWarningMessages);
        odiHandler3.setLevel(Level.WARNING);
        javautillogger3.addHandler(odiHandler3);

        java.util.logging.Logger javautillogger4 =
                java.util.logging.Logger.getLogger("oracle.odi.scripting.odixml.OdiXMLParser");
        javautillogger4.setLevel(Level.WARNING);
        OdiLogHandler odiHandler4 = new OdiLogHandler(errorWarningMessages);
        odiHandler4.setLevel(Level.WARNING);
        javautillogger4.addHandler(odiHandler4);

        java.util.logging.Logger javautillogger5 =
                java.util.logging.Logger.getLogger("oracle.odi.util.serialization.SerializationLibrary");
        javautillogger5.setLevel(Level.WARNING);
        OdiLogHandler odiHandler5 = new OdiLogHandler(errorWarningMessages);
        odiHandler5.setLevel(Level.WARNING);
        javautillogger5.addHandler(odiHandler5);

        java.util.logging.Logger javautillogger6 =
                java.util.logging.Logger.getLogger("oracle.odi.scripting.ETLHelper");
        javautillogger6.setLevel(Level.WARNING);
        OdiLogHandler odiHandler6 = new OdiLogHandler(errorWarningMessages);
        odiHandler6.setLevel(Level.WARNING);
        javautillogger6.addHandler(odiHandler5);

        DefaultTransactionDefinition txnDef =
                new DefaultTransactionDefinition(ITransactionDefinition.PROPAGATION_MANDATORY);
        ITransactionManager tm = odiInstance.getTransactionManager();
        ITransactionStatus txnStatus = tm.getTransaction(txnDef);
        IOdiScenarioGenerator generatescen = new OdiScenarioGeneratorImpl(odiInstance);
        OdiScenario scen = null;
        try {
            logger.info("Processing  Scenario for package " + packageName);
            scen = generatescen.generateScenario(pck, packageName.toUpperCase(), "001",
                    new ArrayList<>());
            // generateAllScenarios(project, options);
            logger.info("Generated Scenario  for package " + scen.getName() + "\t" +
                    scen.getVersion());
        } catch (RuntimeException e) {
            logger.fatal(String.format("Error generating scenario for package %1$s.",
                    packageName.toUpperCase()), e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            logger.fatal(String.format("Error generating scenario for package %1$s.",
                    packageName.toUpperCase()), e);
            String msg = errorWarningMessages.formatMessage(80720, ERROR_MESSAGE_80720,
                    this.getClass(), e);
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                    msg,
                    ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
            throw new RuntimeException(msg);
        }
        scen = null;
        generatescen = null;

        tm.commit(txnStatus);

        javautillogger.removeHandler(odiHandler);
        odiHandler.flush();
        javautillogger2.removeHandler(odiHandler2);
        odiHandler2.flush();
        javautillogger3.removeHandler(odiHandler3);
        odiHandler3.flush();
        javautillogger4.removeHandler(odiHandler4);
        odiHandler4.flush();
        javautillogger5.removeHandler(odiHandler5);
        odiHandler5.flush();
        javautillogger6.removeHandler(odiHandler6);
        odiHandler6.flush();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void processMappingProcedureVariables(List<Transformation> transformations) {
        final int pMode = GenerationMode.REPLACE.getValue();
        DefaultTransactionDefinition txnDef =
                new DefaultTransactionDefinition(ITransactionDefinition.PROPAGATION_MANDATORY);
        ITransactionManager tm = odiInstance.getTransactionManager();
        ITransactionStatus txnStatus = tm.getTransaction(txnDef);

        final boolean pGenerateFromPackage = false;
        boolean pGenerateFromMapping;
        IOdiScenarioGenerator generatescen = new OdiScenarioGeneratorImpl(odiInstance);
        IOdiScenarioSourceContainer paramIOdiScenarioSourceContainer = getProject();
        // if useScenario is true scenario is already generated.
        // or if this.ODI12_GENERATE_SCENARIOS_FOR_MAPPINGS
        // scenarios of mappings are generated if property set to true.
        pGenerateFromMapping = false;
        final boolean pGenerateFromProcedure;
        if (!etlSubSystemVersion.isVersion11()) {
            if (properties.getPropertyKeys()
                    .contains(JodiConstants.ODI12_GENERATE_SCENARIOS_FOR_PROCEDURES)) {
                pGenerateFromProcedure =
                        Boolean.valueOf(properties.getProperty(
                                JodiConstants.ODI12_GENERATE_SCENARIOS_FOR_PROCEDURES));
            } else {
                pGenerateFromProcedure = false;
            }
        } else {
            pGenerateFromProcedure = false;
        }
        final boolean pGenerateFromVariable = false;
        GenerationOptions paramGenerationOptions =
                new GenerationOptions(pMode, pGenerateFromPackage, pGenerateFromMapping,
                        pGenerateFromProcedure, pGenerateFromVariable);
        try {

            if (pGenerateFromProcedure) logger.info("----> scenarios from procedure");
            if (pGenerateFromVariable) logger.info("----> scenarios from variables");
            generatescen.generateAllScenarios(paramIOdiScenarioSourceContainer,
                    paramGenerationOptions);
        } catch (OdiScenarioGeneratorException e) {
            logger.error(e);
            String message = e.getMessage() != null ? e.getMessage()
                    : "Generating scenarios failed.";
            errorWarningMessages.addMessage(message,
                    ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
            throw new OdiScenarioException(message, e);
        }
        tm.commit(txnStatus);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void generateScenarioForMapping(final String name, final String folderPath) {
        IOdiEntityManager tem = odiInstance.getTransactionalEntityManager();
        IOdiScenarioFolderFinder scenFolderFinder = (IOdiScenarioFolderFinder) tem.getFinder(OdiScenarioFolder.class);
        IOdiPackageFinder packFinder = (IOdiPackageFinder) tem.getFinder(OdiPackage.class);
        IMappingFinder mapFinder = (IMappingFinder) tem.getFinder(Mapping.class);
        IOdiUserProcedureFinder procFinder = (IOdiUserProcedureFinder) tem.getFinder(OdiUserProcedure.class);
        IOdiVariableFinder varFinder = (IOdiVariableFinder) tem.getFinder(OdiVariable.class);

        IOdiScenarioGenerator generatescen = new OdiScenarioGeneratorImpl(odiInstance);

        try {
            Mapping mapping = (Mapping) this.odiTransfromationStrategy
                    .findMappingsByName(name, folderPath,
                            properties.getProjectCode());
            logger.info("Generating scenario for mappping : " + name + " from folder: " + folderPath);
            assert (mapping != null) : "Mapping " + name + " in folder " + folderPath + " does not exists.";
            OdiScenario scenario = generatescen.generateScenario(mapping,
                    JodiConstants.getScenarioNameFromObject(name, true),
                    "001");
            OdiScenarioFolder projectFolder = moveScenariosToFoldersCreatProjectFolder(scenario, scenFolderFinder);
            OdiScenarioFolder subFolder = moveScenariosToFoldersCreateSubFolder(scenario, projectFolder, packFinder, mapFinder, procFinder, varFinder);
            moveScenariosToFolders(scenario, projectFolder, subFolder);
        } catch (OdiScenarioGeneratorException | ResourceNotFoundException |
                ResourceFoundAmbiguouslyException e) {
            logger.error(e);
            String message = e.getMessage() != null ? e.getMessage()
                    : "Generating scenarios failed for.";
            message += String.format("Mapping %s in folder: %s. ", name, folderPath);
            errorWarningMessages.addMessage(message, ErrorWarningMessageJodi.MESSAGE_TYPE
                    .ERRORS);
            throw new OdiScenarioException(message, e);
        }
    }

    private ArrayList getParentFolder(OdiFolder folder, ArrayList folderList) {
        OdiFolder parentFolder = (OdiFolder) folder.getParentFolder();
        if (parentFolder != null) {
            folderList.add(parentFolder.getName());
            folderList = getParentFolder(parentFolder, folderList);
        }
        return folderList;
    }

    private ArrayList getFolderList(OdiFolder folder) {
        ArrayList folderList = new ArrayList<>();
        folderList.add(folder.getName());
        folderList = getParentFolder(folder, folderList);
        return folderList;
    }

    private OdiScenarioFolder moveScenariosToFoldersCreatProjectFolder(OdiScenario scenario, IOdiScenarioFolderFinder scenFolderFinder) {
        DefaultTransactionDefinition txnDef = new DefaultTransactionDefinition();
        ITransactionManager tm = odiInstance.getTransactionManager();
        ITransactionStatus txnStatus = tm.getTransaction(txnDef);
        IOdiEntityManager tem = odiInstance.getTransactionalEntityManager();

        OdiProject project = getProject();
        OdiScenarioFolder projectScenarioFolder = null;
        if (project != null) {
            projectScenarioFolder = scenFolderFinder.findByName(project.getName().toUpperCase());
            if (projectScenarioFolder == null) {
                projectScenarioFolder = new OdiScenarioFolder(project.getName().toUpperCase());
                tem.persist(projectScenarioFolder);
                tm.commit(txnStatus);
                logger.info("Created OdiScenarioFolder " + project.getName().toUpperCase());
                return projectScenarioFolder;
            } else {
                return projectScenarioFolder;
            }
        } else {
            throw new RuntimeException("Project not found.");
        }
    }

    private OdiScenarioFolder moveScenariosToFoldersCreateSubFolder(OdiScenario scenario,
                                                                    OdiScenarioFolder projectScenarioFolder,
                                                                    IOdiPackageFinder packFinder,
                                                                    IMappingFinder mapFinder,
                                                                    IOdiUserProcedureFinder procFinder,
                                                                    IOdiVariableFinder varFinder) {
        OdiFolder folder = null;
        String objName;

        if (scenario.getScenarioFolder() == null) {
            if (scenario.getSourceComponentClass().getName().endsWith("Mapping")) {
                Mapping m = (Mapping) mapFinder.findById(scenario.getSourceComponentId());
                objName = m.getName();
                folder = (OdiFolder) m.getParentFolder();
            } else if (scenario.getSourceComponentClass().getName().endsWith("OdiUserProcedure")) {
                OdiUserProcedure up = (OdiUserProcedure) procFinder.findById(scenario.getSourceComponentId());
                objName = up.getName();
                folder = up.getFolder();
            } else if (scenario.getSourceComponentClass().getName().endsWith("OdiPackage")) {
                OdiPackage op = (OdiPackage) packFinder.findById(scenario.getSourceComponentId());
                if (op != null && op.getName() != null) {
                    objName = op.getName();
                    folder = op.getParentFolder();
                }
            } else if (scenario.getSourceComponentClass().getName().endsWith("OdiVariable")) {
                OdiVariable var = (OdiVariable) varFinder.findById(scenario.getSourceComponentId());
                objName = var.getName();
                //  project = var.getProject();
            } else {
                //println(scenario.getSourceComponentClass().getName());
            }
            DefaultTransactionDefinition txnDef = new DefaultTransactionDefinition();
            ITransactionManager tm = odiInstance.getTransactionManager();
            ITransactionStatus txnStatus = tm.getTransaction(txnDef);
            IOdiEntityManager tem = odiInstance.getTransactionalEntityManager();
            if (folder != null) {
                ArrayList<String> sourceFolderList = getFolderList(folder);
                for (String subFolder : sourceFolderList) {
                    Collection<OdiScenarioFolder> subFolders = projectScenarioFolder.getSubFolders();
                    boolean found = false;
                    for (OdiScenarioFolder sub : subFolders) {
                        if (sub.getName().equalsIgnoreCase(subFolder)) {
                            found = true;
                            return sub;
                        }
                    }
                    if (!found) {
                        OdiScenarioFolder subF = new OdiScenarioFolder(projectScenarioFolder, subFolder.toUpperCase());
                        tem.persist(subF);
                        tm.commit(txnStatus);
                        return subF;
                    }
                }
            }
        }
        throw new RuntimeException("Subfolder missing.");
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void moveScenariosToFolders(OdiScenario scenario, OdiScenarioFolder projectScenarioFolder, OdiScenarioFolder subFolder) {
        assert projectScenarioFolder != null;
        assert subFolder != null;
        subFolder.addScenario(scenario);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void deleteScenarios() {
        IOdiEntityManager tem = odiInstance.getTransactionalEntityManager();
        IOdiScenarioFinder scenarioFinder = (IOdiScenarioFinder) tem
                .getFinder(OdiScenario.class);
        Collection<OdiScenario> scenarios = scenarioFinder.findAll();
        logger.info("Found " + scenarios.size() + " scenarios to delete.");
        scenarios.stream().forEach(s -> {
            removeScenario(s.getGlobalId());
            logger.info("Trying to delete: " + ((OdiScenario) s).getName());
        });
    }

    public void removeScenario(String globalId) {
        ITransactionStatus trans = odiInstance.getTransactionManager()
                .getTransaction(new DefaultTransactionDefinition());
        OdiConnection odiConnection = new OdiConnection(odiInstance, trans);
        IOdiScenarioFinder scenarioFinder = (IOdiScenarioFinder) odiConnection.getOdiInstance().getFinder(OdiScenario.class);
        OdiScenario scenario = (OdiScenario) scenarioFinder.findByGlobalId(globalId);
        odiConnection.getOdiInstance().removeEntity(scenario);
        odiConnection.getOdiInstance().getTransactionManager().commit(odiConnection.getTransactionStatus());
    }
}