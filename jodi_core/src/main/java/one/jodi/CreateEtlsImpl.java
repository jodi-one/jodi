package one.jodi;

import com.google.inject.Inject;
import one.jodi.annotations.IncludeVariables;
import one.jodi.base.annotations.Nullable;
import one.jodi.base.annotations.XmlFolderName;
import one.jodi.base.bootstrap.ActionRunner;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.bootstrap.UsageException;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.exception.UnRecoverableException;
import one.jodi.base.model.types.DataStore;
import one.jodi.base.util.StringUtils;
import one.jodi.bootstrap.EtlRunConfig;
import one.jodi.core.annotations.IncludeConstraints;
import one.jodi.core.annotations.InterfacePrefix;
import one.jodi.core.annotations.JournalizedData;
import one.jodi.core.journalizing.JournalizingContext;
import one.jodi.core.service.ConstraintService;
import one.jodi.core.service.MetadataServiceProvider;
import one.jodi.core.service.ModelValidator;
import one.jodi.core.service.PackageService;
import one.jodi.core.service.ProcedureService;
import one.jodi.core.service.ScenarioService;
import one.jodi.core.service.TableService;
import one.jodi.core.service.TransformationService;
import one.jodi.core.service.VariableService;
import one.jodi.core.validation.etl.ETLValidator;
import one.jodi.etl.internalmodel.ETLPackage;
import one.jodi.etl.internalmodel.ETLPackageHeader;
import one.jodi.etl.journalizng.JournalizingConfiguration;
import one.jodi.etl.service.table.TableDefaultBehaviors;
import one.jodi.etl.service.table.TableServiceProvider;
import one.jodi.logging.ErrorReport;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Singleton;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class CreateEtlsImpl implements CreateEtls, ActionRunner {
   public static final String ERROR_MESSAGE_00250 = "Parse error in file 0.xml %s";
   public static final String ERROR_MESSAGE_00260 = "Unknown error processing file 0.xml %s";
   private static final Logger logger = LogManager.getLogger(CreateEtlsImpl.class);
   private static final String ERROR_MESSAGE_00270 = "The input file/folder doesn't exist: %s";
   private static final String ERROR_MESSAGE_00280 = "The prefix configuration is required to run ETL creation";
   private static final String ERROR_MESSAGE_00290 = "The metadata directory is required to run ETL creation";
   private static final String ERROR_MESSAGE_00300 =
           "Generating Scenarios failed, this might happen if there are two or more packages with the same name. %s ";
   private static final String ERROR_MESSAGE_00320 = "The configuration property file is required to run ETL creation";
   private static final String ERROR_MESSAGE_00330 = "Validation for Journalizing failed.";

   protected final TableServiceProvider tableService;
   protected final PackageService odiPackageService;
   protected final ScenarioService odiScenarioService;

   protected final String prefix;
   protected final String mappingFolder;
   protected final boolean journalized;
   private final TransformationService transformationService;
   private final MetadataServiceProvider metadataProvider;
   private final JournalizingContext journalizingContext;
   private final TableService tableServiceCore;
   private final ModelValidator modelValidator;
   private final VariableService variablesService;
   private final ConstraintService constraintService;
   private final ProcedureService procedureService;
   private final ErrorWarningMessageJodi errorWarningMessages;
   private final Boolean includeVariable;
   private final ETLValidator etlValidator;
   private final Boolean includeConstraints;

   @Inject
   protected CreateEtlsImpl(final TransformationService transformationService, final TableServiceProvider tableService,
                            final PackageService odiPackageService, final ScenarioService odiScenarioService,
                            final ProcedureService procedureService, final MetadataServiceProvider metadataProvider,
                            final @InterfacePrefix String prefix, final @Nullable @XmlFolderName String mappingFolder,
                            final @JournalizedData String journalized, final JournalizingContext journalizingContext,
                            final TableService tableServiceCore, final ModelValidator modelValidator,
                            final ErrorWarningMessageJodi errorWarningMessages, final VariableService variablesService,
                            final @Nullable @IncludeVariables Boolean includeVariables,
                            final @Nullable @IncludeConstraints Boolean includeConstraints,
                            final ConstraintService constraintService, final ETLValidator etlValidator) {
      super();
      this.tableService = tableService;
      this.odiPackageService = odiPackageService;
      this.odiScenarioService = odiScenarioService;
      this.procedureService = procedureService;
      this.prefix = prefix;
      this.mappingFolder = mappingFolder;
      this.journalized = Boolean.valueOf(journalized);
      this.metadataProvider = metadataProvider;
      this.transformationService = transformationService;
      this.journalizingContext = journalizingContext;
      this.tableServiceCore = tableServiceCore;
      this.modelValidator = modelValidator;
      this.variablesService = variablesService;
      this.errorWarningMessages = errorWarningMessages;
      this.includeVariable = includeVariables;
      this.includeConstraints = includeConstraints;
      this.constraintService = constraintService;
      this.etlValidator = etlValidator;
      ErrorReport.reset();
   }

   @Override
   public void regenerateProject() {
      List<ETLPackage> etlObject;
      List<ETLPackageHeader> packageHeaders;
      try {
         packageHeaders = metadataProvider.getPackageHeaders(journalized);
      } catch (RuntimeException re) {
         String msg = "";
         if ((re.getCause() != null) && (re.getCause() instanceof JAXBException)) {
            msg = errorWarningMessages.formatMessage(250, ERROR_MESSAGE_00250, this.getClass(), re.getCause());
            logger.error(msg, re);
         } else {
            msg = errorWarningMessages.formatMessage(260, ERROR_MESSAGE_00260, this.getClass(), re);
            logger.error(msg, re);
         }
         ErrorReport.addErrorLine(0, msg);
         throw new UnRecoverableException(msg, re);
      }
      //
      // String[] args = {"odi.properties","Inf ","../xml/interfaces","false"};
      //
      if (journalized) {
         if (!etlValidator.validateJournalizing()) {
            String msg = errorWarningMessages.formatMessage(330, ERROR_MESSAGE_00330, this.getClass());
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);
            logger.error(msg);
            throw new RuntimeException(msg);
         }

         tableService.resetJKMs();
         List<JournalizingConfiguration> jkmOptions = journalizingContext.getJournalizingConfiguration();
         for (JournalizingConfiguration ops : jkmOptions) {
            tableService.setJKM(ops.getModelCode(), ops.getName());
            logger.info("Journalizing Knowledge Module: " + ops.getName());
         }
         logger.info("------- correct CDC Descriptors");

         List<JournalizingConfiguration> cdcDs = journalizingContext.getJournalizingConfiguration();
         tableService.resetCDCDescriptor();
         int i = 0;
         for (JournalizingConfiguration config : cdcDs) {
            for (DataStore ds : config.getDataStores()) {
               i = i + 10;
               tableService.setCDCDescriptor(ds.getDataStoreName(), ds.getDataModel()
                                                                      .getModelCode(), i);
            }
         }
         logger.info("------- correct models");
         for (JournalizingConfiguration ops : jkmOptions) {
            tableService.setJKMOptions(ops.getModelCode(), ops.getJkmOptions());
            logger.info("Journalizing Knowledge Module: " + ops.getName());
         }
         logger.info("------- correct tables");
      }

      List<TableDefaultBehaviors> tableDefaults = tableServiceCore.assembleDefaultBehaviors();
      tableService.alterTables(tableDefaults);
      tableService.alterSCDTables(tableDefaults);

      List<DataStore> dataStores = tableServiceCore.getDataStores();
      dataStores.forEach(modelValidator::doCheck);
      tableService.checkTables();

      logger.info("------- remove all packages");
      boolean throwErrorOnFailure = false;

      // clone before reversing header list
      // delete root packages before child folder packages
      List<ETLPackageHeader> revPackageHeaders = new ArrayList<>(packageHeaders);
      Collections.reverse(revPackageHeaders);
      odiPackageService.deletePackages(revPackageHeaders, throwErrorOnFailure);

      logger.info("------- remove all scenarios");
      odiScenarioService.deleteScenario(revPackageHeaders);

      logger.info("------- regenerating all variables");
      regenerateVariables(mappingFolder);

      logger.info("------- regenerating all procedures");
      regenerateProcedures(mappingFolder);

      logger.info("------- regenerating all constraints");
      regenerateConstraints(mappingFolder);

      logger.info("------- create or replace all interfaces");
      File file = new File(mappingFolder);
      if (!file.exists()) {
         String msg = errorWarningMessages.formatMessage(270, ERROR_MESSAGE_00270, this.getClass(), mappingFolder);
         ErrorReport.addErrorLine(0, msg);
         logger.error(msg);
         throw new UnRecoverableException(msg);
      }
      transformationService.createOrReplaceTransformations(journalized);

      logger.info("------- create all packages");
      boolean raiseErrorOnFailureWhileCreating = false;

      try {
         etlObject = metadataProvider.getPackages(journalized);
      } catch (RuntimeException re) {
         String msg = "";
         if ((re.getCause() != null) && (re.getCause() instanceof JAXBException)) {
            msg = errorWarningMessages.formatMessage(250, ERROR_MESSAGE_00250, this.getClass(), re.getCause());
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);
            logger.error(msg, re);
         } else {
            msg = errorWarningMessages.formatMessage(260, ERROR_MESSAGE_00260, this.getClass(), re);
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);
            logger.error(msg, re);
         }
         ErrorReport.addErrorLine(0, msg);
         throw new UnRecoverableException(msg, re);
      }


      odiPackageService.createPackages(etlObject, raiseErrorOnFailureWhileCreating);

      logger.info("------- regenerate scenarios");
      try {
         odiScenarioService.generateAllScenarios(packageHeaders, metadataProvider.getInternaTransformations());
      } catch (RuntimeException rte) {
         String message = rte.getMessage() != null ? rte.getMessage() : "Unknown error occurred.";
         logger.debug(message);
         String msg = errorWarningMessages.formatMessage(300, ERROR_MESSAGE_00300, this.getClass(), rte.getMessage());
         errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);
         logger.error(msg, rte);
         throw new RuntimeException(msg, rte);
      }
      logger.info("------- Complete");
      // transformationService.writeToDisk();
   }

   private void regenerateVariables(String metaDataDirectory) {
      if (!this.includeVariable) {
         return;
      }
      //this.variablesService.delete(metaDataDirectory);
      this.variablesService.create(metaDataDirectory);
   }

   private void regenerateConstraints(String metadataDirectory) {
      if (includeConstraints) {
         constraintService.delete(metadataDirectory);
         constraintService.create(metadataDirectory);
      }
   }

   private void regenerateProcedures(final String metadataDirectory) {
      this.procedureService.delete(metadataDirectory);
      this.procedureService.create(metadataDirectory, false);
   }

   protected List<String> getPackageNames(Collection<ETLPackage> etlObject) {
      List<String> packages = etlObject.stream()
                                       .map(ETLPackage::getPackageName)
                                       .collect(Collectors.toCollection(LinkedList::new));

      return packages;
   }

   @Override
   public void run(RunConfig config) {
      StopWatch sw = new StopWatch();
      sw.start();
      regenerateProject();
      sw.stop();
      logger.info("ETL Regenerated in " + sw.toString());
   }

   @Override
   public void validateRunConfig(RunConfig config) throws UsageException {
      final EtlRunConfig etlConfig = (EtlRunConfig) config;

      if (!StringUtils.hasLength(etlConfig.getPrefix())) {
         String msg = errorWarningMessages.formatMessage(280, ERROR_MESSAGE_00280, this.getClass());
         errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);
         logger.error(msg);
         throw new UsageException(msg);
      }

      if (!StringUtils.hasLength(config.getMetadataDirectory())) {
         String msg = errorWarningMessages.formatMessage(290, ERROR_MESSAGE_00290, this.getClass());
         errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);
         logger.error(msg);
         throw new UsageException(msg);
      }

      if (!StringUtils.hasLength(config.getPropertyFile())) {
         String msg = errorWarningMessages.formatMessage(320, ERROR_MESSAGE_00320, this.getClass());
         errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);
         logger.error(msg);
         throw new UsageException(msg);
      }
   }
}
