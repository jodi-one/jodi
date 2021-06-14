package one.jodi.qa.test;

import one.jodi.base.ListAppender;
import one.jodi.base.config.PasswordConfigImpl;
import one.jodi.base.exception.UnRecoverableException;
import one.jodi.base.model.types.DataStoreType;
import one.jodi.base.util.StringUtils;
import one.jodi.bootstrap.EtlRunConfig;
import one.jodi.core.lpmodel.Loadplan;
import one.jodi.core.lpmodel.impl.ObjectFactory;
import one.jodi.odi.common.OdiVersion;
import one.jodi.odi.interfaces.OdiTransformationAccessStrategy;
import one.jodi.odi.loadplan.OdiLoadPlanAccessStrategy;
import one.jodi.odi.runtime.OdiConnection;
import one.jodi.odi.runtime.OdiConnectionFactory;
import oracle.odi.core.OdiInstance;
import oracle.odi.core.persistence.IOdiEntityManager;
import oracle.odi.core.persistence.transaction.ITransactionManager;
import oracle.odi.core.persistence.transaction.ITransactionStatus;
import oracle.odi.core.persistence.transaction.support.DefaultTransactionDefinition;
import oracle.odi.domain.IOdiEntity;
import oracle.odi.domain.IRepositoryEntity;
import oracle.odi.domain.mapping.Mapping;
import oracle.odi.domain.mapping.expression.MapExpression;
import oracle.odi.domain.model.OdiColumn;
import oracle.odi.domain.model.OdiColumn.ScdType;
import oracle.odi.domain.model.OdiDataStore;
import oracle.odi.domain.model.OdiDataStore.CdcDescriptor;
import oracle.odi.domain.model.OdiDataStore.OlapType;
import oracle.odi.domain.model.OdiKey;
import oracle.odi.domain.model.OdiKey.KeyType;
import oracle.odi.domain.model.OdiModel;
import oracle.odi.domain.model.OdiReference;
import oracle.odi.domain.model.OdiReference.ReferenceType;
import oracle.odi.domain.model.finder.IOdiDataStoreFinder;
import oracle.odi.domain.model.finder.IOdiModelFinder;
import oracle.odi.domain.project.IOptionValue;
import oracle.odi.domain.project.OdiProcedureLineCmd;
import oracle.odi.domain.project.OdiUserProcedure;
import oracle.odi.domain.project.ProcedureOption;
import oracle.odi.domain.project.ProcedureOption.OptionType;
import oracle.odi.domain.project.finder.IOdiUserProcedureFinder;
import oracle.odi.domain.runtime.loadplan.OdiLoadPlan;
import oracle.odi.domain.runtime.loadplan.finder.IOdiLoadPlanFinder;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class Sample<T extends IOdiEntity, U extends IRepositoryEntity, V extends IRepositoryEntity, W, X, Y, Z, B extends IOdiEntity>
        extends RegressionTestImpl {
   //
   private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(Sample.class);
   private static final String TEST_BASE_DIRECTORY = "src/test/resources/" + SampleHelper.getFunctionalTestDir();
   private static final String DEFAULT_PROPERTIES_PATH = TEST_BASE_DIRECTORY + "/conf/SampleC.properties";

   private final OdiTransformationAccessStrategy<T, U, V, W, X, Y, Z> odiAccessStrategy;
   private final OdiLoadPlanAccessStrategy<OdiLoadPlan, B> odiLoadPlanAccessStrategy;

   // Chinook SRC DB
   private final String srcUser;
   private final String srcUserJDBC;
   private final String srcUserJDBCDriver;
   // DWH_CON_CHINOOK
   private final String stgUser;
   private final String stgUserJDBC;
   private final String stgUserJDBCDriver;
   // DWH_CON
   private final String conUser;
   private final String conUserJDBC;
   private final String conUserJDBCDriver;
   // DWH_OIL
   private final String oilUser;
   private final String oilUserJDBC;
   private final String oilUserJDBCDriver;
   // DWH_STI
   private final String stiUser;
   private final String stiUserJDBC;
   private final String stiUserJDBCDriver;
   // DWH_STO
   private final String stoUser;
   private final String stoUserJDBC;
   private final String stoUserJDBCDriver;
   // DWH_DMT
   private final String dmtUser;
   private final String dmtUserJDBC;
   private final String dmtUserJDBCDriver;
   private final String smartExport;

   // private  ListAppender listAppender;

   public Sample() throws ConfigurationException {
      super(DEFAULT_PROPERTIES_PATH, new PasswordConfigImpl().getOdiUserPassword(),
            new PasswordConfigImpl().getOdiMasterRepoPassword());
      // Chinook DB
      srcUser = getRegressionConfiguration().getConfig()
                                            .getString("rt.custom.srcUser");
      srcUserJDBC = getRegressionConfiguration().getConfig()
                                                .getString("rt.custom.srcUserJDBC");
      srcUserJDBCDriver = getRegressionConfiguration().getConfig()
                                                      .getString("rt.custom.srcUserJDBCDriver");
      //
      stgUser = getRegressionConfiguration().getConfig()
                                            .getString("rt.custom.stgUser");
      stgUserJDBC = getRegressionConfiguration().getConfig()
                                                .getString("rt.custom.stgUserJDBC");
      stgUserJDBCDriver = getRegressionConfiguration().getConfig()
                                                      .getString("rt.custom.stgUserJDBCDriver");
      //
      conUser = getRegressionConfiguration().getConfig()
                                            .getString("rt.custom.conUser");
      conUserJDBC = getRegressionConfiguration().getConfig()
                                                .getString("rt.custom.conUserJDBC");
      conUserJDBCDriver = getRegressionConfiguration().getConfig()
                                                      .getString("rt.custom.conUserJDBCDriver");
      //
      oilUser = getRegressionConfiguration().getConfig()
                                            .getString("rt.custom.oilUser");
      oilUserJDBC = getRegressionConfiguration().getConfig()
                                                .getString("rt.custom.oilUserJDBC");
      oilUserJDBCDriver = getRegressionConfiguration().getConfig()
                                                      .getString("rt.custom.oilUserJDBCDriver");
      //
      stiUser = getRegressionConfiguration().getConfig()
                                            .getString("rt.custom.stiUser");
      stiUserJDBC = getRegressionConfiguration().getConfig()
                                                .getString("rt.custom.stiUserJDBC");
      stiUserJDBCDriver = getRegressionConfiguration().getConfig()
                                                      .getString("rt.custom.stiUserJDBCDriver");
      //
      stoUser = getRegressionConfiguration().getConfig()
                                            .getString("rt.custom.stoUser");
      stoUserJDBC = getRegressionConfiguration().getConfig()
                                                .getString("rt.custom.stoUserJDBC");
      stoUserJDBCDriver = getRegressionConfiguration().getConfig()
                                                      .getString("rt.custom.stoUserJDBCDriver");
      //
      dmtUser = getRegressionConfiguration().getConfig()
                                            .getString("rt.custom.dmtUser");
      dmtUserJDBC = getRegressionConfiguration().getConfig()
                                                .getString("rt.custom.dmtUserJDBC");
      dmtUserJDBCDriver = getRegressionConfiguration().getConfig()
                                                      .getString("rt.custom.dmtUserJDBCDriver");

      smartExport = getRegressionConfiguration().getConfig()
                                                .getString("rt.file.smartexport");
      Assert.assertNotNull(smartExport);
      //
      Assert.assertNotNull(srcUser);
      Assert.assertNotNull(srcUserJDBC);
      Assert.assertNotNull(srcUserJDBCDriver);
      //
      Assert.assertNotNull(stgUser);
      Assert.assertNotNull(stgUserJDBC);
      Assert.assertNotNull(stgUserJDBCDriver);
      //
      Assert.assertNotNull(conUser);
      Assert.assertNotNull(conUserJDBC);
      Assert.assertNotNull(conUserJDBCDriver);
      //
      Assert.assertNotNull(oilUser);
      Assert.assertNotNull(oilUserJDBC);
      Assert.assertNotNull(oilUserJDBCDriver);
      //
      Assert.assertNotNull(stiUser);
      Assert.assertNotNull(stiUserJDBC);
      Assert.assertNotNull(stiUserJDBCDriver);
      //
      Assert.assertNotNull(stoUser);
      Assert.assertNotNull(stoUserJDBC);
      Assert.assertNotNull(stoUserJDBCDriver);
      //
      Assert.assertNotNull(dmtUser);
      Assert.assertNotNull(dmtUserJDBC);
      Assert.assertNotNull(dmtUserJDBCDriver);

      final EtlRunConfig runConfig = new EtlRunConfig() {
         @Override
         public boolean isJournalized() {
            return false;
         }

         @Override
         public boolean isDevMode() {
            return false;
         }

         @Override
         public String getTargetModel() {
            return null;
         }

         @Override
         public String getSourceModel() {
            return null;
         }

         @Override
         public String getScenario() {
            return null;
         }

         @Override
         public String getPropertyFile() {
            return DEFAULT_PROPERTIES_PATH;
         }

         @Override
         public String getPrefix() {
            return "Init ";
         }

         @Override
         public boolean isIncludeVariables() {
            return false;
         }

         @Override
         public String getPassword() {
            return getRegressionConfiguration().getOdiSupervisorPassword();
         }

         @Override
         public String getPackageSequence() {
            return null;
         }

         @Override
         public String getPackage() {
            return null;
         }

         @Override
         public List<String> getModuleClasses() {
            return Collections.emptyList();
         }

         @Override
         public String getModelCode() {
            return null;
         }

         @Override
         public String getMetadataDirectory() {
            return TEST_BASE_DIRECTORY + "/xml/";
         }

         @Override
         public String getMasterPassword() {
            return getRegressionConfiguration().getMasterRepositoryJdbcPassword();
         }

         @Override
         public String getDeploymentArchiveType() {
            return null;
         }

         @Override
         public boolean isUsingDefaultscenarioNames() {
            return false;
         }

         @Override
         public String getFolder() {
            return "InitialORACLE_DWH_DMT";
         }

         @Override
         public boolean isExportingDBConstraints() {
            return false;
         }

         @Override
         public boolean isIncludingConstraints() {
            return false;
         }

         @Override
         public String getDeployementArchivePassword() {
            return new PasswordConfigImpl().getDeploymentArchivePassword();
         }

      };
      //noinspection unchecked
      this.odiAccessStrategy =
              (OdiTransformationAccessStrategy<T, U, V, W, X, Y, Z>) FunctionalTestHelper.getOdiAccessStrategy(
                      runConfig, getController());
      //noinspection unchecked
      this.odiLoadPlanAccessStrategy =
              (OdiLoadPlanAccessStrategy<OdiLoadPlan, B>) FunctionalTestHelper.getOdiLoadPlanAccessStrategy(runConfig,
                                                                                                            getController());
   }

   private void removeAppender(final ListAppender listAppender) {
      final Logger rootLogger = (Logger) LogManager.getRootLogger();
      rootLogger.removeAppender(listAppender);
   }

   private ListAppender getListAppender() {
      final ListAppender listAppender = new ListAppender(this.getClass()
                                                             .getName());
      final Logger rootLogger = (Logger) LogManager.getRootLogger();
      rootLogger.addAppender(listAppender);
      rootLogger.setLevel(org.apache.logging.log4j.Level.INFO);
      return listAppender;
   }

//	@Before public void initAppender() {
//		ListAppender listAppender = new ListAppender(this.getClass().getName());
//	      Logger rootLogger = (Logger) LogManager.getRootLogger();
//	      rootLogger.addAppender(listAppender);
//	      rootLogger.setLevel(org.apache.logging.log4j.Level.INFO);
//	}
//
//	@After public void verifyLogs() {
//		assert(!listAppender.contains(Level.ERROR, false));
//	}


   @Override
   @After
   public void close() {
      super.close();
   }

   @Override
   @Test
   public void test010Install() {

      super.test010Install();
      //
      // Remove the CDC descriptor on the datastores.
      final OdiInstance odiInstance = getWorkOdiInstance().getOdiInstance();
      final IOdiDataStoreFinder finder = ((IOdiDataStoreFinder) odiInstance.getTransactionalEntityManager()
                                                                           .getFinder(OdiDataStore.class));
      final Collection<OdiDataStore> cdcDataStores = finder.findByModel("ORACLE_CHINOOK");
      int i = 0;
      for (final OdiDataStore cdcDataStore : cdcDataStores) {
         i = i + 10;
         cdcDataStore.setCdcDescriptor(new OdiDataStore.CdcDescriptor(false, i));
         odiInstance.getTransactionalEntityManager()
                    .merge(cdcDataStore);
      }
      odiInstance.getTransactionManager()
                 .commit(getWorkOdiInstance().getTransactionStatus());
   }

   @Test
   // this test fails for java 8, it should be testing with unit tests that;
   // JKM options are set to their values in the propoperties files,
   // other than default.
   public void test011Install() {
      //
      // Remove the CDC descriptor on the datastores.
      final OdiInstance odiInstance = getWorkOdiInstance().getOdiInstance();
      final DefaultTransactionDefinition txnDef = new DefaultTransactionDefinition();
      final ITransactionManager tm = odiInstance.getTransactionManager();
      final ITransactionStatus txnStatus = tm.getTransaction(txnDef);
      // Set the JKMOptions to their default value
      final IOdiModelFinder finder1 = ((IOdiModelFinder) odiInstance.getTransactionalEntityManager()
                                                                    .getFinder(OdiModel.class));
      @SuppressWarnings("unchecked") final Collection<OdiModel> models = finder1.findAll();
      for (final OdiModel cdcModel : models) {
         if (cdcModel.getName()
                     .equalsIgnoreCase("ORACLE_CHINOOK")) {
            if (cdcModel.getJKMOptions() != null) {
               for (int i = 0; i < cdcModel.getJKMOptions()
                                           .size(); i++) {
                  final IOptionValue jkmpoption = cdcModel.getJKMOptions()
                                                          .get(i);
                  //noinspection deprecation
                  LOGGER.info(jkmpoption.getName() + ":" + jkmpoption.getValue());
                  jkmpoption.setValue(jkmpoption.getDefaultOptionValue());
               }
            }
         }
      }
      tm.commit(txnStatus);
   }

   private Collection<OdiDataStore> findOdiDataStores(final String modelCode, final OdiInstance odiInstance) {
      final IOdiDataStoreFinder dataStoreFinder = ((IOdiDataStoreFinder) odiInstance.getTransactionalEntityManager()
                                                                                    .getFinder(OdiDataStore.class));
      return dataStoreFinder.findByModel(modelCode);
   }

   @Test
   public void test015Sequences() {
      LOGGER.info("Create etls started");
      runController("crtseq", DEFAULT_PROPERTIES_PATH, "-p", "Init ", "-m", TEST_BASE_DIRECTORY + "/xml");

   }

   /**
    * Create the interfaces non journalized and check for that
    */
   @Override
   @Test
   public void test020Generation() {
      LOGGER.info("Cleanup packages if necessary");
      final ListAppender listAppender = getListAppender();
      runController("dap", DEFAULT_PROPERTIES_PATH, "-p", "Init ", "-m", TEST_BASE_DIRECTORY + "/xml");

      if (listAppender.contains(Level.ERROR, false)) {
         Assert.fail("Sample threw errors.");
      }

      LOGGER.info("Create etls started");
      runController("ct", DEFAULT_PROPERTIES_PATH, "-p", "Init ", "-m", TEST_BASE_DIRECTORY + "/xml", "-module",
                    "one.jodi.extensions.GbuModuleProvider");
      // the -module one.jodi.extensions.GbuModuleProvider is automapping
      if (listAppender.contains(Level.ERROR, false)) {
         Assert.fail("Sample threw errors.");
      }

      for (final T interf : this.odiAccessStrategy.findMappingsByFolder(getRegressionConfiguration().getProjectCode(),
                                                                        "InitialORACLE_DWH_CON_CHINOOK")) {
         if (!this.odiAccessStrategy.areAllSourcesNotJournalised(interf)) {
            throw new RuntimeException(String.format(
                    "A Sourcedatastore is journalized in interface '%s' it should not be, since the -journalized flag was not used in generation with odiversion '%s'.",
                    interf.getName(), new OdiVersion().getVersion()));
         }

      }
      T mapping;
      try {
         mapping = this.odiAccessStrategy.findMappingsByName("Init W_ALBUM_D", "InitialORACLE_DWH_DMT",
                                                             getRegressionConfiguration().getProjectCode());
      } catch (final Exception e) {
         LOGGER.error(e);
         throw new RuntimeException(e);
      }
      if (mapping == null) {
         throw new RuntimeException();
      }
      if (!new OdiVersion().isVersion11()) {
         final String text =
                 "begin" + System.getProperty("line.separator") + "null;" + System.getProperty("line.separator") +
                         "end;";
         final String locationCode = "ORACLE_DWH_DMT";
         final String technologyCode = "ORACLE";
         final String generatedText = this.odiAccessStrategy.getBeginOrEndMappingText(mapping, "BEGIN");
         final String generatedLocationCode = this.odiAccessStrategy.getBeginOrEndMappingLocationCode(mapping, "BEGIN");
         final String generatedTechnologyCode =
                 this.odiAccessStrategy.getBeginOrEndMappingTechnologyCode(mapping, "BEGIN");
         final Mapping W_ALBUM_D = ((Mapping) mapping);
         boolean found = false;
         try {
            final List<MapExpression> mapExpresions = W_ALBUM_D.getTargets()
                                                               .get(0)
                                                               .getAllExpressions();
            for (final MapExpression me : mapExpresions) {
               final MapExpression e = me.getExpressionMap()
                                         .values()
                                         .iterator()
                                         .next();
               System.out.println(e.getQualifiedName() + ":" + e.getText());
               if (e.getText()
                    .equalsIgnoreCase("trunc(sysdate)")) {
                  // this test automapping functionality
                  // this is set by property
                  // ext.automapping.W_INSERT_DT=trunc(sysdate)
                  // and config ,"-module", "one.jodi.extensions.GbuModuleProvider
                  found = true;
               }
            }
         } catch (final Exception e) {
            throw new RuntimeException(e);
         }
         // automapping works if it is picked up from properties
         assert (found);
         if (!generatedText.replaceAll("\\s+", "")
                           .equalsIgnoreCase(text.replaceAll("\\s+", ""))) {
            throw new RuntimeException("Begin mapping command text set wrong to '" + generatedText + "'.");
         }
         if (!generatedLocationCode.equals(locationCode)) {
            throw new RuntimeException("Begin mapping command location set wrong to '" + generatedLocationCode + "'.");
         }
         if (!generatedTechnologyCode.equals(technologyCode)) {
            throw new RuntimeException(
                    "Begin mapping command technology set wrong to '" + generatedTechnologyCode + "'.");
         }
         removeAppender(listAppender);
      }

      try {
         mapping = this.odiAccessStrategy.findMappingsByName("Init W_INVOICELINE_F",
                                                             getRegressionConfiguration().getProjectCode());
      } catch (final Exception e) {
         throw new RuntimeException(e);
      }
      if (mapping == null) {
         throw new RuntimeException();
      }
      if (!new OdiVersion().isVersion11()) {
         final String text =
                 "begin" + System.getProperty("line.separator") + "null;" + System.getProperty("line.separator") +
                         "end;";
         final String locationCode = "ORACLE_DWH_DMT";
         final String technologyCode = "ORACLE";
         final String generatedText = this.odiAccessStrategy.getBeginOrEndMappingText(mapping, "END");
         final String generatedLocationCode = this.odiAccessStrategy.getBeginOrEndMappingLocationCode(mapping, "END");
         final String generatedTechnologyCode =
                 this.odiAccessStrategy.getBeginOrEndMappingTechnologyCode(mapping, "END");
         if (!generatedText.replaceAll("\\s+", "")
                           .equals(text.replaceAll("\\s+", ""))) {
            throw new RuntimeException("Begin mapping command text set wrong to '" + generatedText + "'.");
         }
         if (!generatedLocationCode.equals(locationCode)) {
            throw new RuntimeException("Begin mapping command location set wrong to '" + generatedLocationCode + "'.");
         }
         if (!generatedTechnologyCode.equals(technologyCode)) {
            throw new RuntimeException(
                    "Begin mapping command technology set wrong to '" + generatedTechnologyCode + "'.");
         }
      }
   }

   /**
    * Create the interfaces journalized and check for that
    */
   @Test
   public void test025Generation() {
      LOGGER.info("Create etls started");
      final ListAppender listAppender = getListAppender();
      runController("etls", DEFAULT_PROPERTIES_PATH, "-p", "Real Time ", "-m", TEST_BASE_DIRECTORY + "/xml",
                    "-journalized");
      if (listAppender.contains(Level.ERROR, false)) {
         Assert.fail("Sample threw errors.");
      }

      for (final T interf : this.odiAccessStrategy.findMappingsByFolder(getRegressionConfiguration().getProjectCode(),
                                                                        "RealTimeORACLE_DWH_CON_CHINOOK")) {
         if (interf.getName()
                   .toLowerCase()
                   .contains("invoiceline")) {
            if (this.odiAccessStrategy.areAllSourcesJournalised(interf) ||
                    this.odiAccessStrategy.areAllSourcesNotJournalised(interf)) {
               throw new RuntimeException();
            }
            // we are ok it needs to be "some" journalized.
            continue;
         }
         if (!this.odiAccessStrategy.areAllSourcesJournalised(interf)) {
            throw new RuntimeException(String.format(
                    "A Sourcedatastore is not journalized in interface '%s$1' it should  be, since the -journalized flag was used in generation.",
                    interf.getName()));

         }
      }
      removeAppender(listAppender);
   }

   /**
    * Tests deletion of packages with action dap (Delete All Packages).
    */
   @Test
   @Override
   public void test030ing() {
      Properties properties;
      FileReader reader = null;
      try {
         reader = new FileReader(DEFAULT_PROPERTIES_PATH);
         properties = new Properties();
         properties.load(reader);
      } catch (final IOException e) {
         e.printStackTrace();
         throw new RuntimeException("Cannot check if this test runs in update mode.");
      } finally {
         if (reader != null) {
            try {
               reader.close();
            } catch (final IOException e) {
               e.printStackTrace();
            }
         }
      }
      final String update = properties.getProperty("jodi.update");
      if (!update.equals("true")) {
         throw new RuntimeException(
                 "this test is meant to run in update mode please update properties to jodi.update = true.");
      }
   }

   /**
    * This test test wheter the CDC descriptors are set during create ETLS.
    */
   @Test
   public void test040setCDCDescritpors() {
      final OdiInstance odiInstance = getWorkOdiInstance().getOdiInstance();
      final IOdiDataStoreFinder finder = ((IOdiDataStoreFinder) odiInstance.getTransactionalEntityManager()
                                                                           .getFinder(OdiDataStore.class));
      final Collection<OdiDataStore> cdcDataStores = finder.findByModel("ORACLE_CHINOOK");
      for (final OdiDataStore cdcDataStore : cdcDataStores) {
         final CdcDescriptor descriptor = cdcDataStore.getCdcDescriptor();
         if (!descriptor.isCdcEnabled()) {
            throw new RuntimeException(String.format("Datastore '%1$s' is not CDC enabled.", cdcDataStore.getName()));
         }
      }
   }

   /**
    * This test test whether JKM options are set to a non default value.
    */
   @SuppressWarnings("deprecation")
   @Test
   public void test050setJKMOptions() {
      final OdiInstance odiInstance = getWorkOdiInstance().getOdiInstance();
      final IOdiModelFinder finder = ((IOdiModelFinder) odiInstance.getTransactionalEntityManager()
                                                                   .getFinder(OdiModel.class));
      final OdiModel cdcModel = finder.findByCode("ORACLE_CHINOOK");
      if (cdcModel != null && cdcModel.getJKMOptions() != null) {
         for (final IOptionValue jkmpoption : cdcModel.getJKMOptions()) {
            if (jkmpoption.getName()
                          .equals("COMPATIBLE") && !jkmpoption.getValue()
                                                              .equals("12")) {
               throw new RuntimeException("JKMOption not set correctly.");
            }
            if (jkmpoption.getName()
                          .equals("VALIDATE") && !jkmpoption.getValue()
                                                            .equals(true)) {
               throw new RuntimeException("JKMOption not set correctly.");
            }
         }
      }
   }

   /**
    * This test test whether Journalized Data Only flag are set on the model.
    */
   @Test
   public void test060ing() {
      for (final T interf : this.odiAccessStrategy.findMappingsByFolder(getRegressionConfiguration().getProjectCode(),
                                                                        "RealTimeORACLE_DWH_CON_CHINOOK")) {
         if (interf.getName()
                   .toLowerCase()
                   .contains("invoiceline")) {
            if (this.odiAccessStrategy.areAllSourcesJournalised(interf) ||
                    this.odiAccessStrategy.areAllSourcesNotJournalised(interf)) {
               throw new RuntimeException();
            }
            // we are ok it needs to be "some" journalized.
            continue;
         }
         if (!this.odiAccessStrategy.areAllSourcesJournalised(interf)) {
            throw new RuntimeException(String.format(
                    "A Sourcedatastore is not journalized in interface '%s' it should  be, since the -journalized flag was used in generation.",
                    interf.getName()));

         }
      }
//		OdiInstance odiInstance = getWorkOdiInstance();
//		IOdiInterfaceFinder finder1 = ((IOdiInterfaceFinder) odiInstance.getTransactionalEntityManager()
//				.getFinder(OdiInterface.class));
//		@SuppressWarnings("unchecked")
//		Collection<OdiInterface> intfs = finder1.findAll();
//		for (OdiInterface odiInterface : intfs) {
//			if (odiInterface.getFolder().getName().equals("RealTimeORACLE_DWH_CON_CHINOOK")) {
//				for (DataSet dataset : odiInterface.getDataSets()) {
//					for (SourceDataStore sds : dataset.getSourceDataStores()) {
//						if (!sds.isJournalized() && !sds.getName().equals("ALBUM")
//								&& !sds.getName().startsWith("INVOICE"))
//							throw new RuntimeException(
//									String.format("Datastore '%1$s' is not journalized in interface '%2$s'.",
//											sds.getName(), odiInterface.getName()));
//					}
//				}
//			}
//		}
   }

   @Test
   public void test070CreatePackages() {
      final ListAppender listAppender = getListAppender();
      runController("cp", DEFAULT_PROPERTIES_PATH, "-p", "Real Time ", "-m", TEST_BASE_DIRECTORY + "/xml",
                    "-journalized");
      if (listAppender.contains(Level.ERROR, false)) {
         Assert.fail("Sample threw errors.");
      }
      removeAppender(listAppender);
   }

   @Test
   public void test080DeletePackages() {
      final ListAppender listAppender = getListAppender();
      runController("dp", DEFAULT_PROPERTIES_PATH, "-p", "Real Time ", "-pkg", "INITIALDWH_STI", "-m",
                    TEST_BASE_DIRECTORY + "/xml", "-f", "InitialORACLE_DWH_STI");

      if (listAppender.contains(Level.ERROR, false)) {
         Assert.fail("Sample threw errors.");
      }
      removeAppender(listAppender);
   }

   @Test
   public void test085DeleteAllPackages() {
      final ListAppender listAppender = getListAppender();
      runController("dap", DEFAULT_PROPERTIES_PATH, "-p", "Real Time ", "-m", TEST_BASE_DIRECTORY + "/xml");

      if (listAppender.contains(Level.ERROR, false)) {
         Assert.fail("Sample threw errors.");
      }
      removeAppender(listAppender);
   }


   @Test
   public void test090CreateTransformations() {
      LOGGER.info("Delete All Packages started");
      final ListAppender listAppender = getListAppender();
      runController("dap", DEFAULT_PROPERTIES_PATH, "-p", "Real Time ", "-m", TEST_BASE_DIRECTORY + "/xml",
                    "-journalized");
      LOGGER.info("Truncate and reccreate Transformations started");
      runController("ct", DEFAULT_PROPERTIES_PATH, "-p", "Real Time ", "-m", TEST_BASE_DIRECTORY + "/xml",
                    "-journalized");
      if (listAppender.contains(Level.ERROR, false)) {
         Assert.fail("Sample threw errors.");
      }
      removeAppender(listAppender);
   }

   @Test
   public void test090DeleteTransformations() {
      LOGGER.info("Delete Transfromations started");
      final ListAppender listAppender = getListAppender();
      runController("dt", DEFAULT_PROPERTIES_PATH, "-p", "Init ", "-m", TEST_BASE_DIRECTORY + "/xml/400_DWH_STI");
      if (listAppender.contains(Level.ERROR, false)) {
         Assert.fail("Sample threw errors.");
      }
      removeAppender(listAppender);
   }

   @Test
   public void test110DeleteScenario() {
      final ListAppender listAppender = getListAppender();
      runController("ds", DEFAULT_PROPERTIES_PATH, "-p", "Init ", "-m", TEST_BASE_DIRECTORY + "/xml", "-scenario",
                    "INITIALDWH_CON");
      if (listAppender.contains(Level.ERROR, false)) {
         Assert.fail("Sample threw errors.");
      }
      removeAppender(listAppender);
   }

   private void cleanModel() {
      final OdiInstance odiInstance = getWorkOdiInstance().getOdiInstance();
      final Collection<OdiDataStore> dataStores = findOdiDataStores("ORACLE_DWH_DMT", odiInstance);
      for (final OdiDataStore ds : dataStores) {
         ds.setOlapType(null);
         for (final OdiColumn column : ds.getColumns()) {
            column.setScdType(null);
            column.setMandatory(false);
            column.setFlowCheckEnabled(false);
            column.setStaticCheckEnabled(false);
            column.setDataServiceAllowUpdate(true);
            column.setDataServiceAllowSelect(true);
            column.setDataServiceAllowInsert(true);
         }
         odiInstance.getTransactionalEntityManager()
                    .merge(ds);
      }
      odiInstance.getTransactionManager()
                 .commit(getWorkOdiInstance().getTransactionStatus());
   }

   private void validateColumnSettings(final OdiDataStore ds) {
      for (final OdiColumn column : ds.getColumns()) {
         final String msg = "incorrect in column " + column.getName();
         assertFalse(msg, column.isMandatory());
         assertFalse(msg, column.isFlowCheckEnabled());
         assertTrue(msg, column.isDataServiceAllowInsert());
         assertFalse(msg, column.isStaticCheckEnabled());
         assertTrue(msg, column.isDataServiceAllowUpdate());
         assertTrue(msg, column.isDataServiceAllowSelect());
      }
   }

   private void validateColumnSettingsAlterTable(final OdiDataStore ds, final boolean isSCD) {
      for (final OdiColumn column : ds.getColumns()) {
         final String msg = "incorrect in column " + column.getName();
         assertFalse(msg, column.isMandatory());
         assertFalse(msg, column.isFlowCheckEnabled());
         assertTrue(msg, column.isDataServiceAllowInsert());
         if (column.getName()
                   .equals("ROW_WID")) {
            if (isSCD) {
               assertFalse(msg, column.isStaticCheckEnabled());
               assertFalse(msg, column.isDataServiceAllowUpdate());
               assertFalse(msg, column.isDataServiceAllowSelect());
            } else {
               //assertTrue(msg, !column.isStaticCheckEnabled());
            }
         } else {
            assertFalse(msg, column.isStaticCheckEnabled());
            // implies that it was not updated
            assertTrue(msg, column.isDataServiceAllowUpdate());
            assertTrue(msg, column.isDataServiceAllowSelect());
         }
      }
   }

   private void validateDataMartForSCD() {
      final OdiInstance odiInstance = getWorkOdiInstance().getOdiInstance();
      final Collection<OdiDataStore> dataStores = findOdiDataStores("ORACLE_DWH_DMT", odiInstance);
      for (final OdiDataStore ds : dataStores) {
         final String name = ds.getName()
                               .toUpperCase();
         if (name.equals("W_EMPLOYEE_D") || name.equals("W_CUSTOMER_D")) {
            // Check Slowly Changing Dimensions
            assertEquals(OlapType.SLOWLY_CHANGING_DIMENSION, ds.getOlapType());
            for (final OdiColumn column : ds.getColumns()) {
               final String msg = "incorrect in column " + column.getName();
               assertFalse(column.isMandatory());
               assertFalse(column.isFlowCheckEnabled());
               assertTrue(column.isDataServiceAllowInsert());

               switch (column.getName()) {
                  case "ROW_WID":
                     assertEquals(msg, ScdType.SURROGATE_KEY, column.getScdType());
                     assertFalse(msg, column.isStaticCheckEnabled());
                     assertTrue(column.isDataServiceAllowUpdate());
                     assertTrue(column.isDataServiceAllowSelect());
                     break;
                  case "EFFECTIVE_DATE":
                     assertEquals(msg, ScdType.START_TIMESTAMP, column.getScdType());
                     assertTrue(msg, column.isStaticCheckEnabled());
                     assertTrue(column.isDataServiceAllowUpdate());
                     assertTrue(column.isDataServiceAllowSelect());
                     break;
                  case "EXPIRATION_DATE":
                     assertEquals(msg, ScdType.END_TIMESTAMP, column.getScdType());
                     assertTrue(msg, column.isStaticCheckEnabled());
                     assertTrue(column.isDataServiceAllowUpdate());
                     assertTrue(column.isDataServiceAllowSelect());
                     break;
                  case "CURRENT_FLG":
                     assertEquals(msg, ScdType.CURRENT_RECORD_FLAG, column.getScdType());
                     assertFalse(msg, column.isStaticCheckEnabled());
                     assertTrue(column.isDataServiceAllowUpdate());
                     assertTrue(column.isDataServiceAllowSelect());
                     break;
                  case "ETL_PROC_WID":
                  case "W_UPDATE_DT":
                  case "W_INSERT_DT":
                     assertEquals(msg, ScdType.OVERWRITE_ON_CHANGE, column.getScdType());
                     assertFalse(msg, column.isStaticCheckEnabled());
                     assertTrue(column.isDataServiceAllowUpdate());
                     assertTrue(column.isDataServiceAllowSelect());
                     break;
                  case "CUST_CODE":
                  case "EMPL_CODE":
                     assertEquals(msg, ScdType.NATURAL_KEY, column.getScdType());
                     assertFalse(msg, column.isStaticCheckEnabled());
                     assertTrue(column.isDataServiceAllowUpdate());
                     assertTrue(column.isDataServiceAllowSelect());
                     break;
                  default:
                     assertEquals(msg, ScdType.ADD_ROW_ON_CHANGE, column.getScdType());
                     assertFalse(msg, column.isStaticCheckEnabled());
                     assertTrue(column.isDataServiceAllowUpdate());
                     assertTrue(column.isDataServiceAllowSelect());
                     break;
               }
            }
         } else {
            assertNull(ds.getOlapType());
            validateColumnSettings(ds);
         }
      }
   }

   private void validateDataMartForAlterTable(final boolean isScd) {
      final OdiInstance odiInstance = getWorkOdiInstance().getOdiInstance();
      final Collection<OdiDataStore> dataStores = findOdiDataStores("ORACLE_DWH_DMT", odiInstance);
      for (final OdiDataStore ds : dataStores) {
         final String name = ds.getName()
                               .toUpperCase();
         if (name.equals("W_INVOICELINE_F") || name.equals("W_FA_F")) {
            assertEquals(OlapType.FACT_TABLE, ds.getOlapType());
            validateColumnSettingsAlterTable(ds, false);
         } else if ((name.equals("W_EMPLOYEE_D") || name.equals("W_CUSTOMER_D") || name.equals("W_DA_D"))) {
            if (isScd) {
               // Check Slowly Changing Dimensions
               assertEquals(OlapType.SLOWLY_CHANGING_DIMENSION, ds.getOlapType());
               validateColumnSettingsAlterTable(ds, true);
               for (final OdiColumn column : ds.getColumns()) {
                  final String msg = "incorrect in column " + column.getName();
                  assertEquals(msg, column.getScdType());
               }
            }
         } else if (name.endsWith("_D") && (name != "W_DA_D" && name != "W_DB_D" && name != "W_DC_D")) {
            System.out.println(String.format("Validating datastore %s has olap type %s in model %s.", ds.getName(),
                                             ds.getOlapType(), ds.getModel()
                                                                 .getName()));
            assertEquals(OlapType.DIMENSION, ds.getOlapType());
            validateColumnSettingsAlterTable(ds, false);
         } else {
            System.out.println(String.format("Validating datastore %s has olap type %s in model %s.", ds.getName(),
                                             ds.getOlapType(), ds.getModel()
                                                                 .getName()));
            assertNull(ds.getOlapType());
         }
      }
   }

   @Test
   public void test120AlterSCDTables() {
      cleanModel();
      final ListAppender listAppender = getListAppender();
      LOGGER.info("atbs started");
      runController("atbs", DEFAULT_PROPERTIES_PATH, "-m", TEST_BASE_DIRECTORY + "/xml");
      if (listAppender.contains(Level.ERROR, false)) {
         Assert.fail("Sample threw errors.");
      }
      removeAppender(listAppender);
      validateDataMartForSCD();
   }

   // destroys model
   @Test
   @Ignore
   public void test121AlterSCDTables_NullOdiKeyColumn() {

      OdiColumn c1 = null, c2 = null, c3 = null, c4 = null, c5 = null;
      final ListAppender listAppender = getListAppender();
      try {
         c1 = removeOdiKeyColumn("W_ALBUM_D", "ALBM_CODE");
         c2 = removeOdiKeyColumn("W_CUSTOMER_D", "CUST_CODE");
         c3 = removeOdiKeyColumn("W_EMPLOYEE_D", "EMPL_CODE");
         c4 = removeOdiKeyColumn("W_INVOICELINE_F", "INVL_LINE_CODE");
         c5 = removeOdiKeyColumn("W_TRACK_D", "TRCK_CODE");
         runController("atbs", DEFAULT_PROPERTIES_PATH, "-m", TEST_BASE_DIRECTORY + "/xml");
      } catch (final Exception e) {
         LOGGER.info(e.getMessage());

      } finally {
         returnOdiKeyColumn("W_ALBUM_D", c1);
         returnOdiKeyColumn("W_CUSTOMER_D", c2);
         returnOdiKeyColumn("W_EMPLOYEE_D", c3);
         returnOdiKeyColumn("W_INVOICELINE_F", c4);
         returnOdiKeyColumn("W_TRACK_D", c5);
      }

      if (listAppender.contains(Level.ERROR, false)) {
         Assert.fail("Sample threw errors.");
      }
   }

   private void returnOdiKeyColumn(final String dataStore, final OdiColumn column) {
      assert (column != null);
      final OdiInstance odiInstance = getWorkOdiInstance().getOdiInstance();
      final IOdiDataStoreFinder finder = ((IOdiDataStoreFinder) odiInstance.getTransactionalEntityManager()
                                                                           .getFinder(OdiDataStore.class));
      @SuppressWarnings("unchecked") final Collection<OdiDataStore> dataStores = finder.findAll();
      for (final OdiDataStore ds : dataStores) {
         if (ds.getName()
               .equalsIgnoreCase(dataStore)) {
            for (final OdiKey key : ds.getKeys()) {
               key.addColumn(column);
               LOGGER.info(column + " added back to " + ds.getName());
            }
         }
      }
      odiInstance.getTransactionManager()
                 .commit(getWorkOdiInstance().getTransactionStatus());
   }

   private OdiColumn removeOdiKeyColumn(final String dataStore, final String column) {
      final OdiInstance odiInstance = getWorkOdiInstance().getOdiInstance();
      new DefaultTransactionDefinition();
      odiInstance.getTransactionManager();

      OdiColumn removedOdiColumn = null;

      final IOdiDataStoreFinder finder = ((IOdiDataStoreFinder) odiInstance.getTransactionalEntityManager()
                                                                           .getFinder(OdiDataStore.class));
      @SuppressWarnings("unchecked") final Collection<OdiDataStore> dataStores = finder.findAll();
      for (final OdiDataStore ds : dataStores) {
         final OdiColumn t;
         if (ds.getName()
               .equalsIgnoreCase(dataStore)) {
            t = ds.getColumn(column);
            LOGGER.info("t: " + t);
            for (final OdiKey key : ds.getKeys()) {
               for (final OdiColumn o : key.getColumns()) {
                  LOGGER.info("column: " + column);
                  LOGGER.info("o: " + o.getName());
                  if (o.getName()
                       .equals(column)) {
                     removedOdiColumn = o;
                     key.removeColumn(o);
                     LOGGER.info(t + " removed from " + ds.getName());
                  }
               }

            }
         }
      }

      return removedOdiColumn;
   }

   @Test
   public void test130AlterTables() {
      cleanModel();
      final ListAppender listAppender = getListAppender();
      runController("atb", DEFAULT_PROPERTIES_PATH, "-m", TEST_BASE_DIRECTORY + "/xml");
      if (listAppender.contains(Level.ERROR, false)) {
         Assert.fail("Sample threw errors.");
      }
      removeAppender(listAppender);
      final boolean isScd = false;
      validateDataMartForAlterTable(isScd);
   }

   @Test
   @Ignore
   public void test131AlterTables_ColumnNotUpdated() {
      final ListAppender listAppender = getListAppender();
      try {
         forcedChange("W_CUSTOMER_D");
         forcedChange("W_EMPLOYEE_D");

         runController("atb", DEFAULT_PROPERTIES_PATH, "-m", TEST_BASE_DIRECTORY + "/xml");
      } catch (final Exception e) {
         LOGGER.info(e.getMessage());
      } finally {
         reverseChange("W_CUSTOMER_D");
         reverseChange("W_EMPLOYEE_D");
      }
      if (listAppender.contains(Level.ERROR, false)) {
         Assert.fail("Sample threw errors.");
      }
      removeAppender(listAppender);
   }

   @SuppressWarnings({"unchecked"})
   private void forcedChange(final String dataStoreName) {
      final OdiInstance odiInstance = getWorkOdiInstance().getOdiInstance();
      final ITransactionManager tm = odiInstance.getTransactionManager();
      final IOdiDataStoreFinder finder = ((IOdiDataStoreFinder) odiInstance.getTransactionalEntityManager()
                                                                           .getFinder(OdiDataStore.class));
      final Collection<OdiDataStore> dataStores = finder.findAll();
      for (final OdiDataStore ds : dataStores) {
         if (ds.getName()
               .equalsIgnoreCase(dataStoreName)) {
            final OdiColumn t = ds.getColumn("EFFECTIVE_DATE");
            LOGGER.info(t);
            for (final OdiKey key : ds.getKeys()) {
               if (key.getColumns()
                      .contains(t)) {
                  key.removeColumn(t);
                  odiInstance.getTransactionalEntityManager()
                             .merge(ds);
                  LOGGER.info(t + " removed from " + ds.getName());
               }
            }
         }
      }
      tm.commit(getWorkOdiInstance().getTransactionStatus());
   }

   private void reverseChange(final String dataStoreName) {
      final OdiInstance odiInstance = getWorkOdiInstance().getOdiInstance();
      final ITransactionManager tm = odiInstance.getTransactionManager();
      final IOdiDataStoreFinder finder = ((IOdiDataStoreFinder) odiInstance.getTransactionalEntityManager()
                                                                           .getFinder(OdiDataStore.class));
      @SuppressWarnings("unchecked") final Collection<OdiDataStore> dataStores = finder.findAll();
      for (final OdiDataStore ds : dataStores) {
         if (ds.getName()
               .equalsIgnoreCase(dataStoreName)) {
            final OdiColumn t = ds.getColumn("EFFECTIVE_DATE");
            for (final OdiKey key : ds.getKeys()) {
               key.addColumn(t);
               odiInstance.getTransactionalEntityManager()
                          .merge(ds);
               LOGGER.info(t + " added back " + ds.getName());
            }
         }
      }
      tm.commit(getWorkOdiInstance().getTransactionStatus());
   }

   @Test
   public void test140CheckTables() {
      final ListAppender listAppender = getListAppender();
      runController("cktb", DEFAULT_PROPERTIES_PATH, "-m", TEST_BASE_DIRECTORY + "/xml");
      if (listAppender.contains(Level.ERROR, false)) {
         Assert.fail("Sample threw errors.");
      }
      removeAppender(listAppender);
   }

   @Test
   public void test141CheckTables_70000() {
      final ListAppender listAppender = getListAppender();
      try {
         forcingFailuresForCheckTables("oracle.odi.domain.model.OdiReference W_INVOICELINE_F_W_CUSTOMER_D",
                                       "oracle.odi.domain.model.OdiColumn INVL_CUST_WID");
         forcingFailuresForCheckTables("oracle.odi.domain.model.OdiReference W_INVOICELINE_F_W_ALBUM_D",
                                       "oracle.odi.domain.model.OdiColumn INVL_ALBM_WID");
         forcingFailuresForCheckTables("oracle.odi.domain.model.OdiReference W_INVOICELINE_F_W_EMPLOYEE_D",
                                       "oracle.odi.domain.model.OdiColumn INVL_SUPERVISOR_WID");
         forcingFailuresForCheckTables("oracle.odi.domain.model.OdiReference W_INVOICELINE_F_W_TRACK_D",
                                       "oracle.odi.domain.model.OdiColumn INVL_TRCK_WID");
         forcingFailuresForCheckTables("oracle.odi.domain.model.OdiReference W_INVOICELINE_F_W_REPRESENTATIVE_D",
                                       "oracle.odi.domain.model.OdiColumn INVL_SUPPORTREP_WID");

         runController("cktb", DEFAULT_PROPERTIES_PATH, "-m", TEST_BASE_DIRECTORY + "/xml");
      } catch (final Exception e) {
         LOGGER.info(e.getMessage());
      } finally {
         reverseForcedChangesForCheckTables("oracle.odi.domain.model.OdiReference W_INVOICELINE_F_W_CUSTOMER_D",
                                            "oracle.odi.domain.model.OdiColumn INVL_CUST_WID");
         reverseForcedChangesForCheckTables("oracle.odi.domain.model.OdiReference W_INVOICELINE_F_W_ALBUM_D",
                                            "oracle.odi.domain.model.OdiColumn INVL_ALBM_WID");
         reverseForcedChangesForCheckTables("oracle.odi.domain.model.OdiReference W_INVOICELINE_F_W_EMPLOYEE_D",
                                            "oracle.odi.domain.model.OdiColumn INVL_SUPERVISOR_WID");
         reverseForcedChangesForCheckTables("oracle.odi.domain.model.OdiReference W_INVOICELINE_F_W_TRACK_D",
                                            "oracle.odi.domain.model.OdiColumn INVL_TRCK_WID");
         reverseForcedChangesForCheckTables("oracle.odi.domain.model.OdiReference W_INVOICELINE_F_W_REPRESENTATIVE_D",
                                            "oracle.odi.domain.model.OdiColumn INVL_SUPPORTREP_WID");
      }
      if (listAppender.contains(Level.ERROR, false)) {
         Assert.fail("Sample threw errors.");
      }
      removeAppender(listAppender);
   }

   @Test
   public void test142CheckTables_70001() {
      final ListAppender listAppender = getListAppender();
      try {
         changeReferenceType("oracle.odi.domain.model.OdiReference W_INVOICELINE_F_W_CUSTOMER_D");
         changeReferenceType("oracle.odi.domain.model.OdiReference W_INVOICELINE_F_W_ALBUM_D");
         changeReferenceType("oracle.odi.domain.model.OdiReference W_INVOICELINE_F_W_EMPLOYEE_D");
         changeReferenceType("oracle.odi.domain.model.OdiReference W_INVOICELINE_F_W_TRACK_D");
         changeReferenceType("oracle.odi.domain.model.OdiReference W_INVOICELINE_F_W_REPRESENTATIVE_D");

         runController("cktb", DEFAULT_PROPERTIES_PATH, "-m", TEST_BASE_DIRECTORY + "/xml");
      } catch (final Exception e) {
         LOGGER.info(e.getMessage());
      } finally {
         reverseChangeReferenceType("oracle.odi.domain.model.OdiReference W_INVOICELINE_F_W_CUSTOMER_D");
         reverseChangeReferenceType("oracle.odi.domain.model.OdiReference W_INVOICELINE_F_W_ALBUM_D");
         reverseChangeReferenceType("oracle.odi.domain.model.OdiReference W_INVOICELINE_F_W_EMPLOYEE_D");
         reverseChangeReferenceType("oracle.odi.domain.model.OdiReference W_INVOICELINE_F_W_TRACK_D");
         reverseChangeReferenceType("oracle.odi.domain.model.OdiReference W_INVOICELINE_F_W_REPRESENTATIVE_D");
      }
      if (listAppender.contains(Level.ERROR, false)) {
         Assert.fail("Sample threw errors.");
      }
      removeAppender(listAppender);
   }

   @SuppressWarnings("unchecked")
   private void reverseChangeReferenceType(final String testOdiReference) {
      final OdiInstance odiInstance = getWorkOdiInstance().getOdiInstance();
      final ITransactionManager tm = odiInstance.getTransactionManager();
      final ITransactionStatus trans = tm.getTransaction(new DefaultTransactionDefinition());
      final IOdiEntityManager tem = odiInstance.getTransactionalEntityManager();
      final IOdiDataStoreFinder finder = ((IOdiDataStoreFinder) tem.getFinder(OdiDataStore.class));
      final Collection<OdiDataStore> dataStores = finder.findAll();
      OdiDataStore temp = null;
      for (final OdiDataStore ds : dataStores) {
         if (ds.getName()
               .equalsIgnoreCase("W_INVOICELINE_F")) {
            temp = ds;
         }
      }
      final Collection<OdiReference> odiReferences = tem.findAll(OdiReference.class);
      for (final OdiReference odiReference : odiReferences) {
         if (StringUtils.equalsIgnoreCase(testOdiReference, // +"_U1",
                                          odiReference.toString())) {
            odiReference.setReferenceType(ReferenceType.DB_REFERENCE);
            LOGGER.info("ReferenceType changed back to: " + odiReference.getReferenceType());
         }
      }
      tem.merge(temp);
      tm.commit(trans);
   }

   @SuppressWarnings("unchecked")
   private void changeReferenceType(final String testOdiReference) {
      final OdiInstance odiInstance = getWorkOdiInstance().getOdiInstance();
      final ITransactionManager tm = odiInstance.getTransactionManager();
      tm.getTransaction(new DefaultTransactionDefinition());
      final IOdiDataStoreFinder finder = ((IOdiDataStoreFinder) odiInstance.getTransactionalEntityManager()
                                                                           .getFinder(OdiDataStore.class));
      final Collection<OdiDataStore> dataStores = finder.findAll();
      OdiDataStore temp = null;
      for (final OdiDataStore ds : dataStores) {
         if (ds.getName()
               .equalsIgnoreCase("W_INVOICELINE_F")) {
            temp = ds;
         }
      }
      final Collection<OdiReference> odiReferences = odiInstance.getTransactionalEntityManager()
                                                                .findAll(OdiReference.class);
      for (final OdiReference odiReference : odiReferences) {
         if (StringUtils.equalsIgnoreCase(testOdiReference, // +"_U1",
                                          odiReference.toString())) {
            final String tempValue = odiReference.getReferenceType()
                                                 .toString();
            odiReference.setReferenceType(ReferenceType.ODI_REFERENCE);
            LOGGER.info("ReferenceType changed from " + tempValue + " to " + odiReference.getReferenceType());
         }
      }
      odiInstance.getTransactionalEntityManager()
                 .merge(temp);
      tm.commit(getWorkOdiInstance().getTransactionStatus());
   }

   @SuppressWarnings("unchecked")
   private void reverseForcedChangesForCheckTables(final String testOdiReference, final String testOdiColumn) {
      final OdiInstance odiInstance = getWorkOdiInstance().getOdiInstance();
      final IOdiEntityManager tem = odiInstance.getTransactionalEntityManager();
      final IOdiDataStoreFinder finder = ((IOdiDataStoreFinder) tem.getFinder(OdiDataStore.class));
      final Collection<OdiDataStore> dataStores = finder.findAll();
      OdiDataStore temp = null;
      for (final OdiDataStore ds : dataStores) {
         if (ds.getName()
               .equalsIgnoreCase("W_INVOICELINE_F")) {
            temp = ds;
         }
      }
      final Collection<OdiReference> odiReferences = tem.findAll(OdiReference.class);
      for (final OdiReference odiReference : odiReferences) {
         if (StringUtils.equalsIgnoreCase(testOdiReference, odiReference.toString())) {
            for (final OdiKey odiKey : odiReference.getForeignDataStore()
                                                   .getKeys()) {
               if (odiKey.getKeyType()
                         .equals(OdiKey.KeyType.ALTERNATE_KEY)) {
                  for (final OdiColumn indexColun : odiKey.getColumns()) {
                     if (StringUtils.equalsIgnoreCase(indexColun.getName(), testOdiColumn.split(" ")[1])) {
                        odiKey.setKeyType(KeyType.INDEX);
                     }
                  }
               }
            }
         }
      }
      tem.merge(temp);
   }

   @SuppressWarnings("unchecked")
   private void forcingFailuresForCheckTables(final String testOdiReference, final String testOdiColumn) {
      final OdiInstance odiInstance = getWorkOdiInstance().getOdiInstance();
      final IOdiEntityManager tem = odiInstance.getTransactionalEntityManager();
      final ITransactionManager tm = odiInstance.getTransactionManager();
      tm.getTransaction(new DefaultTransactionDefinition());
      final IOdiDataStoreFinder finder = ((IOdiDataStoreFinder) tem.getFinder(OdiDataStore.class));
      final Collection<OdiDataStore> dataStores = finder.findAll();
      OdiDataStore temp = null;
      for (final OdiDataStore ds : dataStores) {
         if (ds.getName()
               .equalsIgnoreCase("W_INVOICELINE_F")) {
            temp = ds;
         }
      }
      final Collection<OdiReference> odiReferences = tem.findAll(OdiReference.class);
      for (final OdiReference odiReference : odiReferences) {
         if (StringUtils.equalsIgnoreCase(testOdiReference, odiReference.toString())) {
            for (final OdiKey odiKey : odiReference.getForeignDataStore()
                                                   .getKeys()) {
               if (odiKey.getKeyType()
                         .equals(OdiKey.KeyType.INDEX)) {
                  for (final OdiColumn indexColun : odiKey.getColumns()) {
                     if (StringUtils.equalsIgnoreCase(indexColun.getName(), testOdiColumn.split(" ")[1])) {
                        odiKey.setKeyType(KeyType.ALTERNATE_KEY);
                     }
                  }
               }
            }
         }
      }
      tem.merge(temp);
   }

   // Model is populated with value not code
   // off by 10
   @Test
   public void test150ExtractTables() {
      final ListAppender listAppender = getListAppender();
      runController("etb", DEFAULT_PROPERTIES_PATH, "-ps", "10", "-m", TEST_BASE_DIRECTORY + "/jodi_dist", "-srcmdl",
                    "ORACLE_CHINOOK");
      if (listAppender.contains(Level.ERROR, false)) {
         Assert.fail("Sample threw errors.");
      }
      removeAppender(listAppender);
   }

   @Test
   public void test160DeleteReferences() {
      LOGGER.info("Create etls started");
      final ListAppender listAppender = getListAppender();
      runController("dr", DEFAULT_PROPERTIES_PATH, "-p", "Init ", "-model", "ORACLE_CHINOOK", "-m",
                    TEST_BASE_DIRECTORY + "/xml", "-journalized");
      if (listAppender.contains(Level.ERROR, false)) {
         Assert.fail("Sample threw errors.");
      }
      removeAppender(listAppender);
   }

   @Test
   public void test170DeleteAllPackages() {
      final ListAppender listAppender = getListAppender();
      runController("dap", DEFAULT_PROPERTIES_PATH, "-p", "Init ", "-m", TEST_BASE_DIRECTORY + "/xml");
      if (listAppender.contains(Level.ERROR, false)) {
         Assert.fail("Sample threw errors.");
      }
      removeAppender(listAppender);
   }

   /**
    * Testing JodiPropertiesImpl-- The ODI Property file found.
    */
   @Test
   public void test180PropertyFileFound() {
      try {
         runController("etls", DEFAULT_PROPERTIES_PATH, "-p", "Init ", "-m", TEST_BASE_DIRECTORY + "/xml");
      } catch (final Exception e) {
         Assert.fail("Sample threw unexpected exception.");
      }
   }

   /**
    * Testing JodiPropertiesImpl-- The ODI Property file is not found.
    */
   @Test
   public void test181PropertyFileNotFound() {
      try {
         runController("etls", TEST_BASE_DIRECTORY + "/conf/Sample.propertiesNotFound", "-p", "Init ", "-m",
                       TEST_BASE_DIRECTORY + "/xml");
         Assert.fail("Sample did not throw exception on missing properties file.");
      } catch (final UnRecoverableException ea) {
         // expected that exception is thrown
      } catch (final Exception e) {
         Assert.fail("Sample throw unexception exception.");
      }
   }

   @Test
   public void test200printColumnsDetails() {
      runController("prnt", DEFAULT_PROPERTIES_PATH, "-p", "Init ", "-m", TEST_BASE_DIRECTORY + "/xml", "-folder",
                    "InitialORACLE_CHINOOK");
   }

   @Test
   public void test200AlterTables() {
      try {
         runController("atb", DEFAULT_PROPERTIES_PATH, "-m", TEST_BASE_DIRECTORY + "/xml");
      } catch (final Exception e) {
         Assert.fail("Sample threw unexpected exception.");
      }
      final IOdiDataStoreFinder finder = (IOdiDataStoreFinder) getWorkOdiInstance().getOdiInstance()
                                                                                   .getTransactionalEntityManager()
                                                                                   .getFinder(OdiDataStore.class);
      // OdiDataStore album = finder.findByName("W_ALBUM_D",
      // "ORACLE_DWH_DMT");
      // if(!album.getDataStoreType().equals(DataStoreType.DIMENSION)){
      // throw new RuntimeException("Datatype of datastore album should be
      // dimesion.");
      // }
      final OdiDataStore customer = finder.findByName("W_CUSTOMER_D", "ORACLE_DWH_DMT");
      if (customer.getOlapType() == null || !customer.getOlapType()
                                                     .name()
                                                     .equals(DataStoreType.SLOWLY_CHANGING_DIMENSION.name())) {
         throw new RuntimeException(
                 "Datatype of customer should be " + DataStoreType.SLOWLY_CHANGING_DIMENSION.name() + " and is: " +
                         (customer.getOlapType() != null ? customer.getOlapType()
                                                                   .name() : " null "));
      }
      final OdiDataStore employee = finder.findByName("W_EMPLOYEE_D", "ORACLE_DWH_DMT");
      if (!employee.getOlapType()
                   .name()
                   .equals(DataStoreType.SLOWLY_CHANGING_DIMENSION.name())) {
         throw new RuntimeException(
                 "Datatype of employee should be " + DataStoreType.SLOWLY_CHANGING_DIMENSION.name() + " and is: " +
                         employee.getOlapType()
                                 .name());
      }
   }

   /**
    * Test 300 and above are tests for loadplans. Import a loadplan, test that
    * file exists, and it can be unmarshalled.
    *
    * @throws JAXBException On XML marshalling issues
    * @throws IOException   On issues with any file
    */

   @Test
   public void test300LoadPlanTestAll() throws JAXBException, IOException {
      deleteCreatedLoadPlans();
      if (!new OdiVersion().isVersion11()) {
         LOGGER.info("testing;");
         // import loadplans
         final File generatedFile = new File(TEST_BASE_DIRECTORY + "/xml/loadPlans/ETL.xml");
         if (generatedFile.exists()) {
            if (!generatedFile.delete()) {
               throw new RuntimeException("can't delete file.");
            }
         }
         final File generatedFileTestsForNullpointers =
                 new File(TEST_BASE_DIRECTORY + "/xml/loadPlans/CREATED_TEST_ETL.xml");
         if (generatedFileTestsForNullpointers.exists()) {
            if (!generatedFileTestsForNullpointers.delete()) {
               throw new RuntimeException("can't delete file.");
            }
         }
         runController("lpe", DEFAULT_PROPERTIES_PATH, "-p", "Init ", "-m", TEST_BASE_DIRECTORY + "/xml");
         if (!generatedFile.exists()) {
            throw new RuntimeException("Loadplan import did not import loadplan into xml file.");
         }
         final File orignal = new File("src/test/resources/CREATED_TEST_ETL.xml");
         final File testNullPointer = new File(TEST_BASE_DIRECTORY + "/xml/loadPlans/CREATED_TEST_ETL.xml");
         Files.copy(Paths.get(orignal.getAbsolutePath()), Paths.get(testNullPointer.getAbsolutePath()),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
         final JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class);
         final Unmarshaller u = jc.createUnmarshaller();
         final Loadplan loadplan = (Loadplan) u.unmarshal(generatedFile);
         LOGGER.info("Successfully unmarshalled from file loadplan : " + loadplan.getName());
//
         final Scanner generatedScanner = new Scanner(generatedFile);
         final StringBuilder generatedContent = new StringBuilder();
         while (generatedScanner.hasNext()) {
            generatedContent.append(generatedScanner.next());
         }
         generatedScanner.close();
//
         final File controlFile = new File(TEST_BASE_DIRECTORY + "/controlETL.xml");
         final Scanner controlScanner = new Scanner(controlFile);
         final StringBuilder controlContent = new StringBuilder();
         while (controlScanner.hasNext()) {
            controlContent.append(controlScanner.next());
         }
         controlScanner.close();
         if (!controlContent.toString()
                            .equals(generatedContent.toString())) {
            throw new RuntimeException(
                    "content from xml files are technically different compare them manually in eclipse.");
         }
      }
      // So 2 loadplans will get created;
      // CREATED_FROM_FILE ( full specs ) & CREATED_TEST_ETL (nullpointer test)
      // print
      runController("lpp", DEFAULT_PROPERTIES_PATH, "-p", "Init ", "-m", TEST_BASE_DIRECTORY + "/xml");
      // build loadplans
      final File previousCreation = new File(TEST_BASE_DIRECTORY + "/xml/loadPlans", "CREATED_FROM_FILE.xml");
      if (previousCreation.exists() && !previousCreation.delete()) {
         LOGGER.error("Couldn't deleted CREATED_FROM_FILE");
      }
      final File file = new File(TEST_BASE_DIRECTORY + "/xml/loadPlans", "ETL.xml");
      StringBuilder content = new StringBuilder();
      final Scanner scanner = new Scanner(file);
      scanner.useDelimiter("\n");
      while (scanner.hasNext()) {
         content.append(scanner.next())
                .append("\n");
      }
      scanner.close();
      content = new StringBuilder(content.toString()
                                         .replace("ETL", "CREATED_FROM_FILE"));
      write(file, content.toString());
      runController("lp", DEFAULT_PROPERTIES_PATH, "-p", "Init ", "-m", TEST_BASE_DIRECTORY + "/xml");
      final OdiLoadPlan odiLoadplan = odiLoadPlanAccessStrategy.findLoadPlanByName("CREATED_FROM_FILE");
      odiLoadPlanAccessStrategy.verifyOdiLoadPlanPathExists(odiLoadplan, "/Start/DROP_TEMP_INTERFACE_DATASTORES");
      odiLoadPlanAccessStrategy.verifyOdiLoadPlanPathExists(odiLoadplan,
                                                            "/Start/Case Variable: SAMPLEC.LOAD_UP_TO_DATE/Value Is Null/TEST_MKDIR");
      odiLoadPlanAccessStrategy.verifyOdiLoadPlanPathExists(odiLoadplan,
                                                            "/Start/Case Variable: SAMPLEC.LOAD_UP_TO_DATE/When Value > 0/DISABLE_CONSTRAINTS2");
      odiLoadPlanAccessStrategy.verifyOdiLoadPlanPathExists(odiLoadplan, "/Start/Parallel/DISABLE_BITMAP_INDEX");
      odiLoadPlanAccessStrategy.verifyOdiLoadPlanPathExists(odiLoadplan, "/Start/INITIALDWH_DMT");
      odiLoadPlanAccessStrategy.verifyOdiLoadPlanPathExists(odiLoadplan, "/Start/INITIALDWH_DMT_FACT");

      if (!new OdiVersion().isVersion11()) {
         final OdiLoadPlan odiLoadplanMininmal = odiLoadPlanAccessStrategy.findLoadPlanByName("CREATED_TEST_FILE");
         odiLoadPlanAccessStrategy.verifyOdiLoadPlanPathExists(odiLoadplanMininmal,
                                                               "/14) Serial/15) DROP_TEMP_INTERFACE_DATASTORES");
         odiLoadPlanAccessStrategy.verifyOdiLoadPlanPathExists(odiLoadplanMininmal,
                                                               "/14) Serial/16) Case with variable: SAMPLEC.LOAD_UP_TO_DATE/17) When: IS_NULL/18) TEST_MKDIR");
         odiLoadPlanAccessStrategy.verifyOdiLoadPlanPathExists(odiLoadplanMininmal,
                                                               "/14) Serial/16) Case with variable: SAMPLEC.LOAD_UP_TO_DATE/20) When: GREATER_THAN/DISABLE_CONSTRAINTS2");
         odiLoadPlanAccessStrategy.verifyOdiLoadPlanPathExists(odiLoadplanMininmal,
                                                               "/14) Serial/27) Parallel/28) DISABLE_BITMAP_INDEX");
         odiLoadPlanAccessStrategy.verifyOdiLoadPlanPathExists(odiLoadplanMininmal, "/14) Serial/29) INITIALDWH_DMT");
         odiLoadPlanAccessStrategy.verifyOdiLoadPlanPathExists(odiLoadplanMininmal,
                                                               "/14) Serial/30) INITIALDWH_DMT_FACT");
         // steps for validating OdiLoadPlanStepVariables -> Variables attributes, name, value, refresh

         if (!odiLoadPlanAccessStrategy.verifyOdiLoadPlanStepVariableValues(odiLoadplanMininmal,
                                                                            "GLOBAL_VAR_PARTITION_NAME",
                                                                            "2) INITIALDWH_CON_CHINOOK", true, null)) {
            throw new RuntimeException("Loadplanstepvariables not valid");
         }
         if (!odiLoadPlanAccessStrategy.verifyOdiLoadPlanStepVariableValues(odiLoadplanMininmal,
                                                                            "SAMPLEC.VAR_PARTITION_NAME", "4) Serial",
                                                                            false, "TEST VAR Exception")) {
            throw new RuntimeException("Loadplanstepvariables not valid");
         }
         if (!odiLoadPlanAccessStrategy.verifyOdiLoadPlanStepVariableValues(odiLoadplanMininmal,
                                                                            "SAMPLEC.VAR_PARTITION_NAME",
                                                                            "15) DROP_TEMP_INTERFACE_DATASTORES", false,
                                                                            "TEST VAR RUN SCENARIO")) {
            throw new RuntimeException("Loadplanstepvariables not valid");
         }
         if (!odiLoadPlanAccessStrategy.verifyOdiLoadPlanStepVariableValues(odiLoadplanMininmal,
                                                                            "SAMPLEC.VAR_PARTITION_NAME",
                                                                            "16) Case with variable: SAMPLEC.LOAD_UP_TO_DATE",
                                                                            false, "TEST VAR RUN CASE")) {
            throw new RuntimeException("Loadplanstepvariables not valid");
         }
      }
   }

   @Test
   public void test400CreateScenarios() {
      runController("cs", DEFAULT_PROPERTIES_PATH, "-p", "Init ", "-m", TEST_BASE_DIRECTORY + "/xml");
   }

   //@After
   public void deleteCreatedLoadPlans() {
      final ITransactionManager tm = getWorkOdiInstance().getOdiInstance()
                                                         .getTransactionManager();
      final IOdiLoadPlanFinder loadPlanFinder = (IOdiLoadPlanFinder) getWorkOdiInstance().getOdiInstance()
                                                                                         .getTransactionalEntityManager()
                                                                                         .getFinder(OdiLoadPlan.class);
      //noinspection unchecked
      for (final OdiLoadPlan loadPlan : (Collection<OdiLoadPlan>) loadPlanFinder.findAll()) {
         if (loadPlan.getName()
                     .toLowerCase()
                     .startsWith("created")) {
            getWorkOdiInstance().getOdiInstance()
                                .getTransactionalEntityManager()
                                .remove(loadPlan);
         }
      }
      tm.commit(getWorkOdiInstance().getTransactionStatus());
   }

   /**
    * Testing JodiPropertiesImpl-- The ODI Property file is not found.
    */
   @Test
   public void test190Export() {
      assert new PasswordConfigImpl().getDeploymentArchivePassword() != null;
      runController("oex", DEFAULT_PROPERTIES_PATH, "-p", "Init ", "-m", TEST_BASE_DIRECTORY + "/xml", "-dapwd",
                    new PasswordConfigImpl().getDeploymentArchivePassword(), "-da_type", "DA_PATCH_EXEC_REPOS");
   }

   @Test
   @Ignore
   public void test602CreateConstraints() {
      runController("expcon", DEFAULT_PROPERTIES_PATH, "-p", "Init ", "-m", TEST_BASE_DIRECTORY + "/xml",
                    "-exportDBConstraints", "true");
   }

   @Test
   public void test700Procedures() {
      runController("delproc", DEFAULT_PROPERTIES_PATH, "-p", "Init ", "-m", TEST_BASE_DIRECTORY + "/xml",
                    "-exportDBConstraints", "true");

      runController("crtproc", DEFAULT_PROPERTIES_PATH, "-p", "Init ", "-m", TEST_BASE_DIRECTORY + "/xml",
                    "-exportDBConstraints", "true");


      Properties properties;
      FileReader reader = null;
      try {
         reader = new FileReader(DEFAULT_PROPERTIES_PATH);
         properties = new Properties();
         properties.load(reader);
      } catch (final IOException e) {
         e.printStackTrace();
         throw new RuntimeException("Cannot check if this test runs in update mode.");
      } finally {
         if (reader != null) {
            try {
               reader.close();
            } catch (final IOException e) {
               e.printStackTrace();
            }
         }
      }
      final OdiConnection odiConnection =
              OdiConnectionFactory.getOdiConnection(properties.getProperty("odi.master.repo.url"),
                                                    properties.getProperty("odi.master.repo.username"),
                                                    new PasswordConfigImpl().getOdiMasterRepoPassword(),
                                                    properties.getProperty("odi.login.username"),
                                                    new PasswordConfigImpl().getOdiUserPassword(),
                                                    properties.getProperty("odi.repo.db.driver"),
                                                    properties.getProperty("odi.work.repo"));

      final IOdiUserProcedureFinder finder = (IOdiUserProcedureFinder) odiConnection.getOdiInstance()
                                                                                    .getFinder(OdiUserProcedure.class);
      final Collection<OdiUserProcedure> testProcs = finder.findByName("Test Proc");
      if (testProcs.size() != 1) {
         throw new RuntimeException("Couldn't find 1 instanc of procedure Test Proc");
      }
      final OdiUserProcedure testProc = testProcs.iterator()
                                                 .next();
      final OdiProcedureLineCmd targetCommand = testProc.getLines()
                                                        .get(0)
                                                        .getOnTargetCommand();
      if (!targetCommand.getExecutionContext()
                        .getCode()
                        .equals("GLOBAL")) {
         throw new RuntimeException("Wrong context set");
      }
      if (!targetCommand.getExpression()
                        .getAsString()
                        .replace("\n", "")
                        .replace("\r\n", "")
                        .equalsIgnoreCase("beginnull;end;")) {
         throw new RuntimeException("Wrong code set");
      }
      if (!targetCommand.getLogicalSchema()
                        .getName()
                        .equalsIgnoreCase("ORACLE_DWH_CON_CHINOOK")) {
         throw new RuntimeException("Wrong logicalschema set");
      }
      if (!targetCommand.getTechnology()
                        .getName()
                        .equalsIgnoreCase("ORACLE")) {
         throw new RuntimeException("Wrong technology set");
      }
      final List<ProcedureOption> options = testProc.getOptions();
      if (options.size() != 4) {
         throw new RuntimeException("There should be 4 options set.");
      }
      final Optional<ProcedureOption> testBoolean = options.stream()
                                                           .filter(o -> o.getName()
                                                                         .equalsIgnoreCase("TestBoolean"))
                                                           .findFirst();
      testOption(testBoolean, OptionType.CHECKBOX, true, "1=1", "A Description for boolean.",
                 "A help message for boolean.");
      final Optional<ProcedureOption> TestText = options.stream()
                                                        .filter(o -> o.getName()
                                                                      .equalsIgnoreCase("TestText"))
                                                        .findFirst();
      testOption(TestText, OptionType.LONG_TEXT, "testString", "1=1", "A Description for text.",
                 "A help message for text.");
      final Optional<ProcedureOption> TestValue = options.stream()
                                                         .filter(o -> o.getName()
                                                                       .equalsIgnoreCase("TestValue"))
                                                         .findFirst();
      testOption(TestValue, OptionType.CHOICE, "testValue", "1=1", "A Description for value.",
                 "A help message for value.");
      final Optional<ProcedureOption> TestChoice = options.stream()
                                                          .filter(o -> o.getName()
                                                                        .equalsIgnoreCase("TestChoice"))
                                                          .findFirst();
      testOption(TestChoice, OptionType.CHOICE, "testChoice", "1=1", "A Description for choice.",
                 "A help message for choice.");
   }

   @Test
   public void test800validate() {
      runController("vldt", DEFAULT_PROPERTIES_PATH, "-p", "Init ", "-m", TEST_BASE_DIRECTORY + "/xml");
   }


   @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
   private void testOption(final Optional<ProcedureOption> option, final OptionType type, final Object defaultValue,
                           final String condition, final String description, final String help) {
      if (!option.isPresent()) {
         throw new RuntimeException("There should be an option set.");
      }
      if (!option.get()
                 .getOptionType()
                 .equals(type)) {
         throw new RuntimeException(String.format("There should be an option of type %s set.", option.get()
                                                                                                     .getOptionType()
                                                                                                     .name()));
      }
      //noinspection deprecation
      if (!option.get()
                 .getDefaultValue()
                 .equals(defaultValue)) {
         //noinspection deprecation
         throw new RuntimeException(String.format("There should be an option with defaultvalue %s set.", option.get()
                                                                                                               .getDefaultValue()));
      }
      if (!option.get()
                 .getConditionExpression()
                 .equalsIgnoreCase(condition)) {
         throw new RuntimeException(String.format("There should be an option with condition %s set.", option.get()
                                                                                                            .getConditionExpression()));
      }
      if (!option.get()
                 .getDescription()
                 .equalsIgnoreCase(description)) {
         throw new RuntimeException(String.format("There should be an option with description %s set.", option.get()
                                                                                                              .getDescription()));
      }
      if (!option.get()
                 .getHelp()
                 .equalsIgnoreCase(help)) {
         throw new RuntimeException(String.format("There should be an option with description %s set.", option.get()
                                                                                                              .getHelp()));
      }
   }

   private void write(final File file, final String input) {
      FileOutputStream fos = null;
      Writer out = null;
      try {
         fos = new FileOutputStream(file.getAbsolutePath());
         out = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
         out.write(input);
      } catch (final IOException e) {
         e.printStackTrace();
      } finally {
         try {
            if (out != null) {
               out.close();
            }
         } catch (final IOException e) {
            e.printStackTrace();
         }
         if (fos != null) {
            try {
               fos.close();
            } catch (final IOException e) {
               e.printStackTrace();
            }
         }
      }
   }
}
