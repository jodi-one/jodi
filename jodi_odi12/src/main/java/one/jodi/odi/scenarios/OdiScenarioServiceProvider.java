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
import oracle.odi.domain.project.IOdiScenarioSourceContainer;
import oracle.odi.domain.project.OdiFolder;
import oracle.odi.domain.project.OdiPackage;
import oracle.odi.domain.project.OdiProject;
import oracle.odi.domain.project.OdiUserProcedure;
import oracle.odi.domain.project.OdiVariable;
import oracle.odi.domain.project.StepMapping;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class OdiScenarioServiceProvider implements ScenarioServiceProvider {
   private static final Logger LOGGER = LogManager.getLogger(OdiScenarioServiceProvider.class);

   private static final String ERROR_MESSAGE_80720 = "RuntimeException: %s";

   private final OdiInstance odiInstance;
   @SuppressWarnings("rawtypes")
   private final OdiTransformationAccessStrategy odiTransfromationStrategy;
   private final OdiPackageAccessStrategy<Mapping, StepMapping> odiPackageStrategy;
   private final EtlSubSystemVersion etlSubSystemVersion;
   private final JodiProperties properties;
   private final ErrorWarningMessageJodi errorWarningMessages;

   @Inject
   protected OdiScenarioServiceProvider(final OdiInstance odiInstance,
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
                                             .getFinder(OdiProject.class)).findByCode(properties.getProjectCode());
   }

   @Override // only used from scenario action runner
   @TransactionAttribute(TransactionAttributeType.REQUIRED)
   public void deleteScenario(final String scenarioName) {
      final DefaultTransactionDefinition txnDef =
              new DefaultTransactionDefinition(ITransactionDefinition.PROPAGATION_MANDATORY);
      final ITransactionManager tm = odiInstance.getTransactionManager();
      final ITransactionStatus txnStatus = tm.getTransaction(txnDef);
      final IOdiEntityManager tme = odiInstance.getTransactionalEntityManager();

      final String name = scenarioName.toUpperCase();
      LOGGER.info(String.format("Removing scenario: '%s'", name));
      // there may be more scenarios than shown in the gui.
      @SuppressWarnings("unchecked") final Collection<OdiScenario> odiScenarios = tme.getFinder(OdiScenario.class)
                                                                                     .findAll();
      for (final OdiScenario odiScenario : odiScenarios) {
         if (odiScenario != null && name.equals(odiScenario.getName())) {
            tme.remove(odiScenario);
            LOGGER.info(String.format("Removed scenario: '%s'.", name));
         }
      }
      tm.commit(txnStatus);
   }

   @Override
   public void deleteScenarios(final List<ETLPackageHeader> headers) {
      headers.forEach(s -> deleteScenario(s.getPackageName()));
   }

   private OdiPackage getPackagesByName(final ETLPackageHeader header) {
      return this.odiPackageStrategy.findPackage(header.getPackageName()
                                                       .toUpperCase(), header.getFolderCode(),
                                                 properties.getProjectCode());
   }

   @Override
   public void generateAllScenarios(final List<ETLPackageHeader> headers, final List<Transformation> transformations) {
      PrintStream original = null;
      try {
         original = new PrintStream(System.out, true, "UTF-8");
      } catch (final UnsupportedEncodingException e) {
         LOGGER.warn(e);
      }
      try (final PrintStream ps = new PrintStream(new OutputStream() {
         @Override
         public void write(final int b) {
            // DO NOTHING
         }
      }, true, "UTF-8")) {
         System.setOut(ps);
      } catch (final UnsupportedEncodingException e) {
         LOGGER.warn(e);
      }
      final Set<String> uniquenessCheck = new HashSet<>();
      for (final ETLPackageHeader header : headers) {
         if (uniquenessCheck.contains(header.getPackageName())) {
            continue;
         } else {
            uniquenessCheck.add(header.getPackageName());
         }
         final OdiPackage odiPackage = getPackagesByName(header);
         if (odiPackage == null) {
            final String msg = String.format("Package '%1$s' was not found in folder '%2$s'.", header.getPackageName(),
                                             header.getFolderCode());
            LOGGER.warn(msg);
            continue;
         }
         final boolean pGenerateFromPackage;
         if (properties.getPropertyKeys()
                       .contains(JodiConstants.ODI12_GENERATE_SCENARIOS_FOR_PACKAGES)) {
            pGenerateFromPackage =
                    Boolean.parseBoolean(properties.getProperty(JodiConstants.ODI12_GENERATE_SCENARIOS_FOR_PACKAGES));
         } else {
            pGenerateFromPackage = true;
         }
         if (pGenerateFromPackage) {
            processPackage(odiPackage, header.getPackageName());
         }
      }
      processMappingProcedureVariables(transformations);
      System.setOut(original);
   }

   @TransactionAttribute(TransactionAttributeType.REQUIRED)
   public void processPackage(final OdiPackage pck, final String packageName) {
      final java.util.logging.Logger javautillogger = java.util.logging.Logger.getLogger("oracle.odi.mapping");
      javautillogger.setLevel(Level.SEVERE);
      final OdiLogHandler odiHandler = new OdiLogHandler(errorWarningMessages);
      odiHandler.setLevel(Level.SEVERE);
      javautillogger.addHandler(odiHandler);

      final java.util.logging.Logger javautillogger2 =
              java.util.logging.Logger.getLogger("oracle.odi.core.persistence");
      javautillogger2.setLevel(Level.WARNING);
      final OdiLogHandler odiHandler2 = new OdiLogHandler(errorWarningMessages);
      odiHandler2.setLevel(Level.WARNING);
      javautillogger2.addHandler(odiHandler2);

      final java.util.logging.Logger javautillogger3 =
              java.util.logging.Logger.getLogger("oracle.odi.scenario.generation");
      javautillogger3.setLevel(Level.WARNING);
      final OdiLogHandler odiHandler3 = new OdiLogHandler(errorWarningMessages);
      odiHandler3.setLevel(Level.WARNING);
      javautillogger3.addHandler(odiHandler3);

      final java.util.logging.Logger javautillogger4 =
              java.util.logging.Logger.getLogger("oracle.odi.scripting.odixml.OdiXMLParser");
      javautillogger4.setLevel(Level.WARNING);
      final OdiLogHandler odiHandler4 = new OdiLogHandler(errorWarningMessages);
      odiHandler4.setLevel(Level.WARNING);
      javautillogger4.addHandler(odiHandler4);

      final java.util.logging.Logger javautillogger5 =
              java.util.logging.Logger.getLogger("oracle.odi.util.serialization.SerializationLibrary");
      javautillogger5.setLevel(Level.WARNING);
      final OdiLogHandler odiHandler5 = new OdiLogHandler(errorWarningMessages);
      odiHandler5.setLevel(Level.WARNING);
      javautillogger5.addHandler(odiHandler5);

      final java.util.logging.Logger javautillogger6 =
              java.util.logging.Logger.getLogger("oracle.odi.scripting.ETLHelper");
      javautillogger6.setLevel(Level.WARNING);
      final OdiLogHandler odiHandler6 = new OdiLogHandler(errorWarningMessages);
      odiHandler6.setLevel(Level.WARNING);
      javautillogger6.addHandler(odiHandler5);

      final DefaultTransactionDefinition txnDef =
              new DefaultTransactionDefinition(ITransactionDefinition.PROPAGATION_MANDATORY);
      final ITransactionManager tm = odiInstance.getTransactionManager();
      final ITransactionStatus txnStatus = tm.getTransaction(txnDef);
      final IOdiScenarioGenerator generatescen = new OdiScenarioGeneratorImpl(odiInstance);
      final OdiScenario scen;
      try {
         LOGGER.info("Processing  Scenario for package " + packageName);
         scen = generatescen.generateScenario(pck, packageName.toUpperCase(), "001", new ArrayList<>());
         // generateAllScenarios(project, options);
         LOGGER.info("Generated Scenario  for package " + scen.getName() + "\t" + scen.getVersion());
      } catch (final RuntimeException e) {
         LOGGER.fatal(String.format("Error generating scenario for package %1$s.", packageName.toUpperCase()), e);
         throw new RuntimeException(e);
      } catch (final Exception e) {
         LOGGER.fatal(String.format("Error generating scenario for package %1$s.", packageName.toUpperCase()), e);
         final String msg = errorWarningMessages.formatMessage(80720, ERROR_MESSAGE_80720, this.getClass(), e);
         errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg,
                                         ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
         throw new RuntimeException(msg);
      }

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
   public void processMappingProcedureVariables(final List<Transformation> transformations) {
      final int pMode = GenerationMode.REPLACE.getValue();
      final DefaultTransactionDefinition txnDef =
              new DefaultTransactionDefinition(ITransactionDefinition.PROPAGATION_MANDATORY);
      final ITransactionManager tm = odiInstance.getTransactionManager();
      final ITransactionStatus txnStatus = tm.getTransaction(txnDef);

      final boolean pGenerateFromPackage = false;
      final IOdiScenarioGenerator generatescen = new OdiScenarioGeneratorImpl(odiInstance);
      final IOdiScenarioSourceContainer paramIOdiScenarioSourceContainer = getProject();
      // if useScenario is true scenario is already generated.
      // or if this.ODI12_GENERATE_SCENARIOS_FOR_MAPPINGS
      // scenarios of mappings are generated if property set to true.
      final boolean pGenerateFromProcedure;
      if (!etlSubSystemVersion.isVersion11()) {
         if (properties.getPropertyKeys()
                       .contains(JodiConstants.ODI12_GENERATE_SCENARIOS_FOR_PROCEDURES)) {
            pGenerateFromProcedure =
                    Boolean.parseBoolean(properties.getProperty(JodiConstants.ODI12_GENERATE_SCENARIOS_FOR_PROCEDURES));
         } else {
            pGenerateFromProcedure = false;
         }
      } else {
         pGenerateFromProcedure = false;
      }
      final GenerationOptions paramGenerationOptions =
              new GenerationOptions(pMode, pGenerateFromPackage, false, pGenerateFromProcedure, false);
      try {
         if (pGenerateFromProcedure) {
            LOGGER.info("----> scenarios from procedure");
         }
         generatescen.generateAllScenarios(paramIOdiScenarioSourceContainer, paramGenerationOptions);
      } catch (final OdiScenarioGeneratorException e) {
         LOGGER.error(e);
         final String message = e.getMessage() != null ? e.getMessage() : "Generating scenarios failed.";
         errorWarningMessages.addMessage(message, ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
         throw new OdiScenarioException(message, e);
      }
      tm.commit(txnStatus);
   }

   @Override
   @TransactionAttribute(TransactionAttributeType.REQUIRED)
   public void generateScenarioForMapping(final String name, final String folderPath) {
      final IOdiEntityManager tem = odiInstance.getTransactionalEntityManager();
      final IOdiScenarioFolderFinder scenFolderFinder =
              (IOdiScenarioFolderFinder) tem.getFinder(OdiScenarioFolder.class);
      final IOdiPackageFinder packFinder = (IOdiPackageFinder) tem.getFinder(OdiPackage.class);
      final IMappingFinder mapFinder = (IMappingFinder) tem.getFinder(Mapping.class);
      final IOdiUserProcedureFinder procFinder = (IOdiUserProcedureFinder) tem.getFinder(OdiUserProcedure.class);
      final IOdiVariableFinder varFinder = (IOdiVariableFinder) tem.getFinder(OdiVariable.class);

      final IOdiScenarioGenerator generatescen = new OdiScenarioGeneratorImpl(odiInstance);

      try {
         final Mapping mapping = (Mapping) this.odiTransfromationStrategy.findMappingsByName(name, folderPath,
                                                                                             properties.getProjectCode());
         LOGGER.info("Generating scenario for mappping : " + name + " from folder: " + folderPath);
         assert (mapping != null) : "Mapping " + name + " in folder " + folderPath + " does not exists.";
         final OdiScenario scenario =
                 generatescen.generateScenario(mapping, JodiConstants.getScenarioNameFromObject(name, true), "001");
         final OdiScenarioFolder projectFolder = moveScenariosToFoldersCreatProjectFolder(scenFolderFinder);
         final OdiScenarioFolder subFolder =
                 moveScenariosToFoldersCreateSubFolder(scenario, projectFolder, packFinder, mapFinder, procFinder,
                                                       varFinder);
         moveScenariosToFolders(scenario, projectFolder, subFolder);
      } catch (final OdiScenarioGeneratorException | ResourceNotFoundException | ResourceFoundAmbiguouslyException e) {
         LOGGER.error(e);
         String message = e.getMessage() != null ? e.getMessage() : "Generating scenarios failed for.";
         message += String.format("Mapping %s in folder: %s. ", name, folderPath);
         errorWarningMessages.addMessage(message, ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
         throw new OdiScenarioException(message, e);
      }
   }

   private List<String> getParentFolder(final OdiFolder folder, List<String> folderList) {
      final OdiFolder parentFolder = folder.getParentFolder();
      if (parentFolder != null) {
         folderList.add(parentFolder.getName());
         folderList = getParentFolder(parentFolder, folderList);
      }
      return folderList;
   }

   private List<String> getFolderList(final OdiFolder folder) {
      List<String> folderList = new ArrayList<>();
      folderList.add(folder.getName());
      folderList = getParentFolder(folder, folderList);
      return folderList;
   }

   private OdiScenarioFolder moveScenariosToFoldersCreatProjectFolder(final IOdiScenarioFolderFinder scenFolderFinder) {
      final DefaultTransactionDefinition txnDef = new DefaultTransactionDefinition();
      final ITransactionManager tm = odiInstance.getTransactionManager();
      final ITransactionStatus txnStatus = tm.getTransaction(txnDef);
      final IOdiEntityManager tem = odiInstance.getTransactionalEntityManager();

      final OdiProject project = getProject();
      OdiScenarioFolder projectScenarioFolder;
      if (project != null) {
         projectScenarioFolder = scenFolderFinder.findByName(project.getName()
                                                                    .toUpperCase());
         if (projectScenarioFolder == null) {
            projectScenarioFolder = new OdiScenarioFolder(project.getName()
                                                                 .toUpperCase());
            tem.persist(projectScenarioFolder);
            tm.commit(txnStatus);
            LOGGER.info("Created OdiScenarioFolder " + project.getName()
                                                              .toUpperCase());
         }
         return projectScenarioFolder;
      } else {
         throw new RuntimeException("Project not found.");
      }
   }

   private OdiScenarioFolder moveScenariosToFoldersCreateSubFolder(final OdiScenario scenario,
                                                                   final OdiScenarioFolder projectScenarioFolder,
                                                                   final IOdiPackageFinder packFinder,
                                                                   final IMappingFinder mapFinder,
                                                                   final IOdiUserProcedureFinder procFinder,
                                                                   final IOdiVariableFinder varFinder) {
      OdiFolder folder = null;
      if (scenario.getScenarioFolder() == null) {
         if (scenario.getSourceComponentClass()
                     .getName()
                     .endsWith("Mapping")) {
            final Mapping m = (Mapping) mapFinder.findById(scenario.getSourceComponentId());
            folder = (OdiFolder) m.getParentFolder();
         } else if (scenario.getSourceComponentClass()
                            .getName()
                            .endsWith("OdiUserProcedure")) {
            final OdiUserProcedure up = (OdiUserProcedure) procFinder.findById(scenario.getSourceComponentId());
            folder = up.getFolder();
         } else if (scenario.getSourceComponentClass()
                            .getName()
                            .endsWith("OdiPackage")) {
            final OdiPackage op = (OdiPackage) packFinder.findById(scenario.getSourceComponentId());
            if (op != null && op.getName() != null) {
               folder = op.getParentFolder();
            }
         } else if (scenario.getSourceComponentClass()
                            .getName()
                            .endsWith("OdiVariable")) {
            varFinder.findById(scenario.getSourceComponentId());
            //  TODO do we need to do something with this?
         }
         final DefaultTransactionDefinition txnDef = new DefaultTransactionDefinition();
         final ITransactionManager tm = odiInstance.getTransactionManager();
         final ITransactionStatus txnStatus = tm.getTransaction(txnDef);
         final IOdiEntityManager tem = odiInstance.getTransactionalEntityManager();
         if (folder != null) {
            final List<String> sourceFolderList = getFolderList(folder);
            for (final String subFolder : sourceFolderList) {
               final Collection<OdiScenarioFolder> subFolders = projectScenarioFolder.getSubFolders();
               for (final OdiScenarioFolder sub : subFolders) {
                  if (sub.getName()
                         .equalsIgnoreCase(subFolder)) {
                     return sub;
                  }
               }
               final OdiScenarioFolder subF = new OdiScenarioFolder(projectScenarioFolder, subFolder.toUpperCase());
               tem.persist(subF);
               tm.commit(txnStatus);
               return subF;
            }
         }
      }
      throw new RuntimeException("Subfolder missing.");
   }

   @TransactionAttribute(TransactionAttributeType.REQUIRED)
   public void moveScenariosToFolders(final OdiScenario scenario, final OdiScenarioFolder projectScenarioFolder,
                                      final OdiScenarioFolder subFolder) {
      assert projectScenarioFolder != null;
      assert subFolder != null;
      subFolder.addScenario(scenario);
   }


   @Override
   public void deleteScenarios() {

      final IOdiEntityManager tem = odiInstance.getTransactionalEntityManager();
      final IOdiScenarioFolderFinder scenarioFolderFinder =
              (IOdiScenarioFolderFinder) tem.getFinder(OdiScenarioFolder.class);
      @SuppressWarnings("unchecked") final Collection<OdiScenarioFolder> scenarioFolders =
              scenarioFolderFinder.findAll();

      scenarioFolders.forEach(sf -> {
         LOGGER.info(String.format("Delete scenarios by folder %s.", sf.getName()));
         deleteScenariosByFolder(sf.getName());
      });

      final IOdiScenarioFinder scenarioFinder = (IOdiScenarioFinder) tem.getFinder(OdiScenario.class);
      @SuppressWarnings("unchecked") final Collection<OdiScenario> scenarios = scenarioFinder.findAll();
      LOGGER.info("Found " + scenarios.size() + " scenarios to delete.");
      scenarios.forEach(s -> {
         removeScenario(s.getGlobalId());
         LOGGER.info("Trying to delete: " + s.getName());
      });

   }

   @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
   public void deleteScenariosByFolder(final String foldername) {
      final IOdiEntityManager tem = odiInstance.getTransactionalEntityManager();
      final IOdiScenarioFolderFinder scenarioFolderFinder =
              (IOdiScenarioFolderFinder) tem.getFinder(OdiScenarioFolder.class);
      @SuppressWarnings("unchecked") final OdiScenarioFolder f = scenarioFolderFinder.findByName(foldername);
      if (f != null) {
         tem.remove(f);
      }
   }


   public void removeScenario(final String globalId) {
      final ITransactionStatus trans = odiInstance.getTransactionManager()
                                                  .getTransaction(new DefaultTransactionDefinition());
      final OdiConnection odiConnection = new OdiConnection(odiInstance, trans);
      final IOdiScenarioFinder scenarioFinder = (IOdiScenarioFinder) odiConnection.getOdiInstance()
                                                                                  .getFinder(OdiScenario.class);
      final OdiScenario scenario = (OdiScenario) scenarioFinder.findByGlobalId(globalId);
      odiConnection.getOdiInstance()
                   .removeEntity(scenario);
      odiConnection.getOdiInstance()
                   .getTransactionManager()
                   .commit(odiConnection.getTransactionStatus());
   }
}