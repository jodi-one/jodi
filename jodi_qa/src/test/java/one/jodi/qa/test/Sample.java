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
    private static final String DEFAULT_PROPERTIES = "SampleC.properties";

    private final OdiTransformationAccessStrategy<T, U, V, W, X, Y, Z> odiAccessStrategy;
    private final OdiLoadPlanAccessStrategy<OdiLoadPlan, B> odiLoadPlanAccessStrategy;
    private final String TEST_XML_BASE_DIRECTORY;
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
        super("src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/conf/" + DEFAULT_PROPERTIES,
                new PasswordConfigImpl().getOdiUserPassword(), new PasswordConfigImpl().getOdiMasterRepoPassword());
        // Chinook DB
        srcUser = getRegressionConfiguration().getConfig().getString("rt.custom.srcUser");
        srcUserJDBC = getRegressionConfiguration().getConfig().getString("rt.custom.srcUserJDBC");
        srcUserJDBCDriver = getRegressionConfiguration().getConfig().getString("rt.custom.srcUserJDBCDriver");
        //
        stgUser = getRegressionConfiguration().getConfig().getString("rt.custom.stgUser");
        stgUserJDBC = getRegressionConfiguration().getConfig().getString("rt.custom.stgUserJDBC");
        stgUserJDBCDriver = getRegressionConfiguration().getConfig().getString("rt.custom.stgUserJDBCDriver");
        //
        conUser = getRegressionConfiguration().getConfig().getString("rt.custom.conUser");
        conUserJDBC = getRegressionConfiguration().getConfig().getString("rt.custom.conUserJDBC");
        conUserJDBCDriver = getRegressionConfiguration().getConfig().getString("rt.custom.conUserJDBCDriver");
        //
        oilUser = getRegressionConfiguration().getConfig().getString("rt.custom.oilUser");
        oilUserJDBC = getRegressionConfiguration().getConfig().getString("rt.custom.oilUserJDBC");
        oilUserJDBCDriver = getRegressionConfiguration().getConfig().getString("rt.custom.oilUserJDBCDriver");
        //
        stiUser = getRegressionConfiguration().getConfig().getString("rt.custom.stiUser");
        stiUserJDBC = getRegressionConfiguration().getConfig().getString("rt.custom.stiUserJDBC");
        stiUserJDBCDriver = getRegressionConfiguration().getConfig().getString("rt.custom.stiUserJDBCDriver");
        //
        stoUser = getRegressionConfiguration().getConfig().getString("rt.custom.stoUser");
        stoUserJDBC = getRegressionConfiguration().getConfig().getString("rt.custom.stoUserJDBC");
        stoUserJDBCDriver = getRegressionConfiguration().getConfig().getString("rt.custom.stoUserJDBCDriver");
        //
        dmtUser = getRegressionConfiguration().getConfig().getString("rt.custom.dmtUser");
        dmtUserJDBC = getRegressionConfiguration().getConfig().getString("rt.custom.dmtUserJDBC");
        dmtUserJDBCDriver = getRegressionConfiguration().getConfig().getString("rt.custom.dmtUserJDBCDriver");

        smartExport = getRegressionConfiguration().getConfig().getString("rt.file.smartexport");
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

        TEST_XML_BASE_DIRECTORY = "src/test/resources/" + SampleHelper.getFunctionalTestDir();
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
                return "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/conf/" + DEFAULT_PROPERTIES;
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
                return TEST_XML_BASE_DIRECTORY + "/xml/";
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
        this.odiAccessStrategy = (OdiTransformationAccessStrategy<T, U, V, W, X, Y, Z>) FunctionalTestHelper
                .getOdiAccessStrategy(runConfig, getController());
        //noinspection unchecked
        this.odiLoadPlanAccessStrategy = (OdiLoadPlanAccessStrategy<OdiLoadPlan, B>) FunctionalTestHelper
                .getOdiLoadPlanAccessStrategy(runConfig, getController());
    }

    private void removeAppender(ListAppender listAppender) {
        Logger rootLogger = (Logger) LogManager.getRootLogger();
        rootLogger.removeAppender(listAppender);
    }

    private ListAppender getListAppender() {
        ListAppender listAppender = new ListAppender(this.getClass().getName());
        Logger rootLogger = (Logger) LogManager.getRootLogger();
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
        OdiInstance odiInstance = getWorkOdiInstance().getOdiInstance();
        IOdiDataStoreFinder finder = ((IOdiDataStoreFinder) odiInstance.getTransactionalEntityManager()
                .getFinder(OdiDataStore.class));
        Collection<OdiDataStore> cdcDataStores = finder.findByModel("ORACLE_CHINOOK");
        int i = 0;
        for (OdiDataStore cdcDataStore : cdcDataStores) {
            i = i + 10;
            cdcDataStore.setCdcDescriptor(new OdiDataStore.CdcDescriptor(false, i));
            odiInstance.getTransactionalEntityManager().merge(cdcDataStore);
        }
        odiInstance.getTransactionManager().commit(getWorkOdiInstance().getTransactionStatus());
    }

    @Test
    // this test fails for java 8, it should be testing with unit tests that;
    // JKM options are set to their values in the propoperties files,
    // other than default.
    public void test011Install() {
        //
        // Remove the CDC descriptor on the datastores.
        OdiInstance odiInstance = getWorkOdiInstance().getOdiInstance();
        DefaultTransactionDefinition txnDef = new DefaultTransactionDefinition();
        ITransactionManager tm = odiInstance.getTransactionManager();
        ITransactionStatus txnStatus = tm.getTransaction(txnDef);
        // Set the JKMOptions to their default value
        IOdiModelFinder finder1 = ((IOdiModelFinder) odiInstance.getTransactionalEntityManager()
                .getFinder(OdiModel.class));
        @SuppressWarnings("unchecked") Collection<OdiModel> models = finder1.findAll();
        for (OdiModel cdcModel : models) {
            if (cdcModel.getName().equalsIgnoreCase("ORACLE_CHINOOK")) {
                if (cdcModel.getJKMOptions() != null) {
                    for (int i = 0; i < cdcModel.getJKMOptions().size(); i++) {
                        IOptionValue jkmpoption = cdcModel.getJKMOptions().get(i);
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
        IOdiDataStoreFinder dataStoreFinder = ((IOdiDataStoreFinder) odiInstance.getTransactionalEntityManager()
                .getFinder(OdiDataStore.class));
        return dataStoreFinder.findByModel(modelCode);
    }


    @Test
    public void test015Sequences() {
        LOGGER.info("Create etls started");
        runController("crtseq",
                "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/conf/" + DEFAULT_PROPERTIES, "-p",
                "Init ", "-m", "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/xml");

    }

    /**
     * Create the interfaces non journalized and check for that
     */
    @Override
    @Test
    public void test020Generation() {
        LOGGER.info("Cleanup packages if necessary");
        ListAppender listAppender = getListAppender();
        runController("dap", "src/test/resources/" + SampleHelper.getFunctionalTestDir() +
                        "/conf/" + DEFAULT_PROPERTIES,
                "-p", "Init ", "-m",
                "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/xml");

        if (listAppender.contains(Level.ERROR, false)) {
            Assert.fail("Sample threw errors.");
        }

        LOGGER.info("Create etls started");
        runController("ct",
                "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/conf/" + DEFAULT_PROPERTIES, "-p",
                "Init ", "-m", "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/xml");
        if (listAppender.contains(Level.ERROR, false)) {
            Assert.fail("Sample threw errors.");
        }

        for (T interf : this.odiAccessStrategy.findMappingsByFolder(getRegressionConfiguration().getProjectCode(), "InitialORACLE_DWH_CON_CHINOOK")) {
            if (!this.odiAccessStrategy.areAllSourcesNotJournalised(interf)) {
                throw new RuntimeException(String.format(
                        "A Sourcedatastore is journalized in interface '%s' it should not be, since the -journalized flag was not used in generation with odiversion '%s'.",
                        interf.getName(), new OdiVersion().getVersion()));
            }

        }
        T mapping;
        try {
            mapping = this.odiAccessStrategy.findMappingsByName("Init W_ALBUM_D", "InitialORACLE_DWH_DMT", getRegressionConfiguration().getProjectCode());
        } catch (Exception e) {
            LOGGER.error(e);
            throw new RuntimeException(e);
        }
        if (mapping == null) {
            throw new RuntimeException();
        }
        if (!new OdiVersion().isVersion11()) {
            String text = "begin" + System.getProperty("line.separator") +
                    "null;" + System.getProperty("line.separator") +
                    "end;";
            String locationCode = "ORACLE_DWH_DMT";
            String technologyCode = "ORACLE";
            String generatedText = this.odiAccessStrategy.getBeginOrEndMappingText(mapping, "BEGIN");
            String generatedLocationCode = this.odiAccessStrategy.getBeginOrEndMappingLocationCode(mapping, "BEGIN");
            String generatedTechnologyCode = this.odiAccessStrategy.getBeginOrEndMappingTechnologyCode(mapping, "BEGIN");
            if (!generatedText.replaceAll("\\s+", "").equalsIgnoreCase(text.replaceAll("\\s+", ""))) {
                throw new RuntimeException("Begin mapping command text set wrong to '" + generatedText + "'.");
            }
            if (!generatedLocationCode.equals(locationCode)) {
                throw new RuntimeException("Begin mapping command location set wrong to '" + generatedLocationCode + "'.");
            }
            if (!generatedTechnologyCode.equals(technologyCode)) {
                throw new RuntimeException("Begin mapping command technology set wrong to '" + generatedTechnologyCode + "'.");
            }
            removeAppender(listAppender);
        }

        try {
            mapping = this.odiAccessStrategy.findMappingsByName("Init W_INVOICELINE_F", getRegressionConfiguration().getProjectCode());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (mapping == null) {
            throw new RuntimeException();
        }
        if (!new OdiVersion().isVersion11()) {
            String text = "begin" + System.getProperty("line.separator") +
                    "null;" + System.getProperty("line.separator") +
                    "end;";
            String locationCode = "ORACLE_DWH_DMT";
            String technologyCode = "ORACLE";
            String generatedText = this.odiAccessStrategy.getBeginOrEndMappingText(mapping, "END");
            String generatedLocationCode = this.odiAccessStrategy.getBeginOrEndMappingLocationCode(mapping, "END");
            String generatedTechnologyCode = this.odiAccessStrategy.getBeginOrEndMappingTechnologyCode(mapping, "END");
            if (!generatedText.replaceAll("\\s+", "").equals(text.replaceAll("\\s+", ""))) {
                throw new RuntimeException("Begin mapping command text set wrong to '" + generatedText + "'.");
            }
            if (!generatedLocationCode.equals(locationCode)) {
                throw new RuntimeException("Begin mapping command location set wrong to '" + generatedLocationCode + "'.");
            }
            if (!generatedTechnologyCode.equals(technologyCode)) {
                throw new RuntimeException("Begin mapping command technology set wrong to '" + generatedTechnologyCode + "'.");
            }
        }
    }

    /**
     * Create the interfaces journalized and check for that
     */
    @Test
    public void test025Generation() {
        LOGGER.info("Create etls started");
        ListAppender listAppender = getListAppender();
        runController("etls",
                "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/conf/" + DEFAULT_PROPERTIES, "-p",
                "Real Time ", "-m", "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/xml",
                "-journalized");
        if (listAppender.contains(Level.ERROR, false)) {
            Assert.fail("Sample threw errors.");
        }

        for (T interf : this.odiAccessStrategy.findMappingsByFolder(getRegressionConfiguration().getProjectCode(), "RealTimeORACLE_DWH_CON_CHINOOK")) {
            if (interf.getName().toLowerCase().contains("invoiceline")) {
                if (this.odiAccessStrategy.areAllSourcesJournalised(interf)
                        || this.odiAccessStrategy.areAllSourcesNotJournalised(interf)) {
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
            reader = new FileReader(
                    "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/conf/" + DEFAULT_PROPERTIES);
            properties = new Properties();
            properties.load(reader);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot check if this test runs in update mode.");
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        String update = properties.getProperty("jodi.update");
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
        OdiInstance odiInstance = getWorkOdiInstance().getOdiInstance();
        IOdiDataStoreFinder finder = ((IOdiDataStoreFinder) odiInstance.getTransactionalEntityManager()
                .getFinder(OdiDataStore.class));
        Collection<OdiDataStore> cdcDataStores = finder.findByModel("ORACLE_CHINOOK");
        for (OdiDataStore cdcDataStore : cdcDataStores) {
            CdcDescriptor descriptor = cdcDataStore.getCdcDescriptor();
            if (!descriptor.isCdcEnabled()) {
                throw new RuntimeException(
                        String.format("Datastore '%1$s' is not CDC enabled.", cdcDataStore.getName()));
            }
        }
    }

    /**
     * This test test whether JKM options are set to a non default value.
     */
    @SuppressWarnings("deprecation")
    @Test
    public void test050setJKMOptions() {
        OdiInstance odiInstance = getWorkOdiInstance().getOdiInstance();
        IOdiModelFinder finder = ((IOdiModelFinder) odiInstance.getTransactionalEntityManager()
                .getFinder(OdiModel.class));
        OdiModel cdcModel = finder.findByCode("ORACLE_CHINOOK");
        if (cdcModel != null && cdcModel.getJKMOptions() != null) {
            for (IOptionValue jkmpoption : cdcModel.getJKMOptions()) {
                if (jkmpoption.getName().equals("COMPATIBLE") && !jkmpoption.getValue().equals("12")) {
                    throw new RuntimeException("JKMOption not set correctly.");
                }
                if (jkmpoption.getName().equals("VALIDATE") && !jkmpoption.getValue().equals(true)) {
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
        for (T interf : this.odiAccessStrategy.findMappingsByFolder(getRegressionConfiguration().getProjectCode(), "RealTimeORACLE_DWH_CON_CHINOOK")) {
            if (interf.getName().toLowerCase().contains("invoiceline")) {
                if (this.odiAccessStrategy.areAllSourcesJournalised(interf)
                        || this.odiAccessStrategy.areAllSourcesNotJournalised(interf)) {
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
        ListAppender listAppender = getListAppender();
        runController("cp", "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/conf/" + DEFAULT_PROPERTIES,
                "-p", "Real Time ", "-m", "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/xml",
                "-journalized");
        if (listAppender.contains(Level.ERROR, false)) {
            Assert.fail("Sample threw errors.");
        }
        removeAppender(listAppender);
    }

    @Test
    public void test080DeletePackages() {
        ListAppender listAppender = getListAppender();
        runController("dp", "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/conf/" + DEFAULT_PROPERTIES,
                "-p", "Real Time ", "-pkg", "INITIALDWH_STI", "-m",
                "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/xml", "-f", "InitialORACLE_DWH_STI");

        if (listAppender.contains(Level.ERROR, false)) {
            Assert.fail("Sample threw errors.");
        }
        removeAppender(listAppender);
    }

    @Test
    public void test085DeleteAllPackages() {
        ListAppender listAppender = getListAppender();
        runController("dap", "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/conf/" + DEFAULT_PROPERTIES,
                "-p", "Real Time ",
                "-m", "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/xml");

        if (listAppender.contains(Level.ERROR, false)) {
            Assert.fail("Sample threw errors.");
        }
        removeAppender(listAppender);
    }


    @Test
    public void test090CreateTransformations() {
        LOGGER.info("Delete All Packages started");
        ListAppender listAppender = getListAppender();
        runController("dap", "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/conf/" + DEFAULT_PROPERTIES,
                "-p", "Real Time ", "-m", "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/xml",
                "-journalized");
        LOGGER.info("Truncate and reccreate Transformations started");
        runController("ct", "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/conf/" + DEFAULT_PROPERTIES,
                "-p", "Real Time ", "-m", "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/xml",
                "-journalized");
        if (listAppender.contains(Level.ERROR, false)) {
            Assert.fail("Sample threw errors.");
        }
        removeAppender(listAppender);
    }

    @Test
    public void test090DeleteTransformations() {
        LOGGER.info("Delete Transfromations started");
        ListAppender listAppender = getListAppender();
        runController("dt", "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/conf/" + DEFAULT_PROPERTIES,
                "-p", "Init ", "-m",
                "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/xml/400_DWH_STI");
        if (listAppender.contains(Level.ERROR, false)) {
            Assert.fail("Sample threw errors.");
        }
        removeAppender(listAppender);
    }

    @Test
    public void test110DeleteScenario() {
        ListAppender listAppender = getListAppender();
        runController("ds", "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/conf/" + DEFAULT_PROPERTIES,
                "-p", "Init ", "-m", "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/xml", "-scenario",
                "INITIALDWH_CON");
        if (listAppender.contains(Level.ERROR, false)) {
            Assert.fail("Sample threw errors.");
        }
        removeAppender(listAppender);
    }

    private void cleanModel() {
        OdiInstance odiInstance = getWorkOdiInstance().getOdiInstance();
        Collection<OdiDataStore> dataStores = findOdiDataStores("ORACLE_DWH_DMT", odiInstance);
        for (OdiDataStore ds : dataStores) {
            ds.setOlapType(null);
            for (OdiColumn column : ds.getColumns()) {
                column.setScdType(null);
                column.setMandatory(false);
                column.setFlowCheckEnabled(false);
                column.setStaticCheckEnabled(false);
                column.setDataServiceAllowUpdate(true);
                column.setDataServiceAllowSelect(true);
                column.setDataServiceAllowInsert(true);
            }
            odiInstance.getTransactionalEntityManager().merge(ds);
        }
        odiInstance.getTransactionManager().commit(getWorkOdiInstance().getTransactionStatus());
    }

    private void validateColumnSettings(OdiDataStore ds) {
        for (OdiColumn column : ds.getColumns()) {
            String msg = "incorrect in column " + column.getName();
            assertFalse(msg, column.isMandatory());
            assertFalse(msg, column.isFlowCheckEnabled());
            assertTrue(msg, column.isDataServiceAllowInsert());
            assertFalse(msg, column.isStaticCheckEnabled());
            assertTrue(msg, column.isDataServiceAllowUpdate());
            assertTrue(msg, column.isDataServiceAllowSelect());
        }
    }

    private void validateColumnSettingsAlterTable(OdiDataStore ds, boolean isSCD) {
        for (OdiColumn column : ds.getColumns()) {
            String msg = "incorrect in column " + column.getName();
            assertFalse(msg, column.isMandatory());
            assertFalse(msg, column.isFlowCheckEnabled());
            assertTrue(msg, column.isDataServiceAllowInsert());
            if (column.getName().equals("ROW_WID")) {
                if (isSCD) {
                    assertFalse(msg, column.isStaticCheckEnabled());
                } else {
                    assertTrue(msg, column.isStaticCheckEnabled());
                }
                assertFalse(msg, column.isDataServiceAllowUpdate());
                assertFalse(msg, column.isDataServiceAllowSelect());
            } else {
                assertFalse(msg, column.isStaticCheckEnabled());
                // implies that it was not updated
                assertTrue(msg, column.isDataServiceAllowUpdate());
                assertTrue(msg, column.isDataServiceAllowSelect());
            }
        }
    }

    private void validateDataMartForSCD() {
        OdiInstance odiInstance = getWorkOdiInstance().getOdiInstance();
        Collection<OdiDataStore> dataStores = findOdiDataStores("ORACLE_DWH_DMT", odiInstance);
        for (OdiDataStore ds : dataStores) {
            String name = ds.getName().toUpperCase();
            if (name.equals("W_EMPLOYEE_D") || name.equals("W_CUSTOMER_D")) {
                // Check Slowly Changing Dimensions
                assertEquals(OlapType.SLOWLY_CHANGING_DIMENSION, ds.getOlapType());
                for (OdiColumn column : ds.getColumns()) {
                    String msg = "incorrect in column " + column.getName();
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

    private void validateDataMartForAlterTable() {
        OdiInstance odiInstance = getWorkOdiInstance().getOdiInstance();
        Collection<OdiDataStore> dataStores = findOdiDataStores("ORACLE_DWH_DMT", odiInstance);
        for (OdiDataStore ds : dataStores) {
            String name = ds.getName().toUpperCase();
            if (name.equals("W_INVOICELINE_F")) {
                assertEquals(OlapType.FACT_TABLE, ds.getOlapType());
                validateColumnSettingsAlterTable(ds, false);
            } else if (name.equals("W_EMPLOYEE_D") || name.equals("W_CUSTOMER_D")) {
                // Check Slowly Changing Dimensions
                assertEquals(OlapType.SLOWLY_CHANGING_DIMENSION, ds.getOlapType());
                validateColumnSettingsAlterTable(ds, true);
                for (OdiColumn column : ds.getColumns()) {
                    String msg = "incorrect in column " + column.getName();
                    assertNull(msg, column.getScdType());
                }
            } else if (name.endsWith("_D")) {
                assertEquals(OlapType.DIMENSION, ds.getOlapType());
                validateColumnSettingsAlterTable(ds, false);
            } else {
                assertNull(ds.getOlapType());
            }
        }
    }

    @Test
    public void test120AlterSCDTables() {
        cleanModel();
        ListAppender listAppender = getListAppender();
        LOGGER.info("Create etls started");
        runController("atbs",
                "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/conf/" + DEFAULT_PROPERTIES, "-m",
                "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/xml");
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
        ListAppender listAppender = getListAppender();
        try {
            c1 = removeOdiKeyColumn("W_ALBUM_D", "ALBM_CODE");
            c2 = removeOdiKeyColumn("W_CUSTOMER_D", "CUST_CODE");
            c3 = removeOdiKeyColumn("W_EMPLOYEE_D", "EMPL_CODE");
            c4 = removeOdiKeyColumn("W_INVOICELINE_F", "INVL_LINE_CODE");
            c5 = removeOdiKeyColumn("W_TRACK_D", "TRCK_CODE");
            runController("atbs",
                    "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/conf/" + DEFAULT_PROPERTIES, "-m",
                    "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/xml");
        } catch (Exception e) {
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

    private void returnOdiKeyColumn(String dataStore, OdiColumn column) {
        assert (column != null);
        OdiInstance odiInstance = getWorkOdiInstance().getOdiInstance();
        IOdiDataStoreFinder finder = ((IOdiDataStoreFinder) odiInstance.getTransactionalEntityManager()
                .getFinder(OdiDataStore.class));
        @SuppressWarnings("unchecked")
        Collection<OdiDataStore> dataStores = finder.findAll();
        for (OdiDataStore ds : dataStores) {
            if (ds.getName().equalsIgnoreCase(dataStore)) {
                for (OdiKey key : ds.getKeys()) {
                    key.addColumn(column);
                    LOGGER.info(column + " added back to " + ds.getName());
                }
            }
        }
        odiInstance.getTransactionManager().commit(getWorkOdiInstance().getTransactionStatus());
    }

    private OdiColumn removeOdiKeyColumn(String dataStore, String column) {
        OdiInstance odiInstance = getWorkOdiInstance().getOdiInstance();
        new DefaultTransactionDefinition();
        odiInstance.getTransactionManager();

        OdiColumn removedOdiColumn = null;

        IOdiDataStoreFinder finder = ((IOdiDataStoreFinder) odiInstance.getTransactionalEntityManager()
                .getFinder(OdiDataStore.class));
        @SuppressWarnings("unchecked")
        Collection<OdiDataStore> dataStores = finder.findAll();
        for (OdiDataStore ds : dataStores) {
            OdiColumn t;
            if (ds.getName().equalsIgnoreCase(dataStore)) {
                t = ds.getColumn(column);
                LOGGER.info("t: " + t);
                for (OdiKey key : ds.getKeys()) {
                    for (OdiColumn o : key.getColumns()) {
                        LOGGER.info("column: " + column);
                        LOGGER.info("o: " + o.getName());
                        if (o.getName().equals(column)) {
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
        ListAppender listAppender = getListAppender();
        runController("atb", "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/conf/" + DEFAULT_PROPERTIES,
                "-m", "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/xml");
        if (listAppender.contains(Level.ERROR, false)) {
            Assert.fail("Sample threw errors.");
        }
        removeAppender(listAppender);
        validateDataMartForAlterTable();
    }

    @Test
    @Ignore
    public void test131AlterTables_ColumnNotUpdated() {
        ListAppender listAppender = getListAppender();
        try {
            forcedChange("W_CUSTOMER_D"
            );
            forcedChange("W_EMPLOYEE_D"
            );

            runController("atb",
                    "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/conf/" + DEFAULT_PROPERTIES, "-m",
                    "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/xml");
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        } finally {
            reverseChange("W_CUSTOMER_D"
            );
            reverseChange("W_EMPLOYEE_D"
            );
        }
        if (listAppender.contains(Level.ERROR, false)) {
            Assert.fail("Sample threw errors.");
        }
        removeAppender(listAppender);
    }

    @SuppressWarnings({"unchecked"})
    private void forcedChange(String dataStoreName) {
        OdiInstance odiInstance = getWorkOdiInstance().getOdiInstance();
        ITransactionManager tm = odiInstance.getTransactionManager();
        IOdiDataStoreFinder finder = ((IOdiDataStoreFinder) odiInstance.getTransactionalEntityManager()
                .getFinder(OdiDataStore.class));
        Collection<OdiDataStore> dataStores = finder.findAll();
        for (OdiDataStore ds : dataStores) {
            if (ds.getName().equalsIgnoreCase(dataStoreName)) {
                OdiColumn t = ds.getColumn("EFFECTIVE_DATE");
                LOGGER.info(t);
                for (OdiKey key : ds.getKeys()) {
                    if (key.getColumns().contains(t)) {
                        key.removeColumn(t);
                        odiInstance.getTransactionalEntityManager().merge(ds);
                        LOGGER.info(t + " removed from " + ds.getName());
                    }
                }
            }
        }
        tm.commit(getWorkOdiInstance().getTransactionStatus());
    }

    private void reverseChange(String dataStoreName) {
        OdiInstance odiInstance = getWorkOdiInstance().getOdiInstance();
        ITransactionManager tm = odiInstance.getTransactionManager();
        IOdiDataStoreFinder finder = ((IOdiDataStoreFinder) odiInstance.getTransactionalEntityManager()
                .getFinder(OdiDataStore.class));
        @SuppressWarnings("unchecked") Collection<OdiDataStore> dataStores = finder.findAll();
        for (OdiDataStore ds : dataStores) {
            if (ds.getName().equalsIgnoreCase(dataStoreName)) {
                OdiColumn t = ds.getColumn("EFFECTIVE_DATE");
                for (OdiKey key : ds.getKeys()) {
                    key.addColumn(t);
                    odiInstance.getTransactionalEntityManager().merge(ds);
                    LOGGER.info(t + " added back " + ds.getName());
                }
            }
        }
        tm.commit(getWorkOdiInstance().getTransactionStatus());
    }

    @Test
    public void test140CheckTables() {
        ListAppender listAppender = getListAppender();
        runController("cktb",
                "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/conf/" + DEFAULT_PROPERTIES, "-m",
                "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/xml");
        if (listAppender.contains(Level.ERROR, false)) {
            Assert.fail("Sample threw errors.");
        }
        removeAppender(listAppender);
    }

    @Test
    public void test141CheckTables_70000() {
        ListAppender listAppender = getListAppender();
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

            runController("cktb",
                    "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/conf/" + DEFAULT_PROPERTIES, "-m",
                    "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/xml");
        } catch (Exception e) {
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
            reverseForcedChangesForCheckTables(
                    "oracle.odi.domain.model.OdiReference W_INVOICELINE_F_W_REPRESENTATIVE_D",
                    "oracle.odi.domain.model.OdiColumn INVL_SUPPORTREP_WID");
        }
        if (listAppender.contains(Level.ERROR, false)) {
            Assert.fail("Sample threw errors.");
        }
        removeAppender(listAppender);
    }

    @Test
    public void test142CheckTables_70001() {
        ListAppender listAppender = getListAppender();
        try {
            changeReferenceType("oracle.odi.domain.model.OdiReference W_INVOICELINE_F_W_CUSTOMER_D");
            changeReferenceType("oracle.odi.domain.model.OdiReference W_INVOICELINE_F_W_ALBUM_D");
            changeReferenceType("oracle.odi.domain.model.OdiReference W_INVOICELINE_F_W_EMPLOYEE_D");
            changeReferenceType("oracle.odi.domain.model.OdiReference W_INVOICELINE_F_W_TRACK_D");
            changeReferenceType("oracle.odi.domain.model.OdiReference W_INVOICELINE_F_W_REPRESENTATIVE_D");

            runController("cktb",
                    "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/conf/" + DEFAULT_PROPERTIES, "-m",
                    "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/xml");
        } catch (Exception e) {
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
    private void reverseChangeReferenceType(String testOdiReference) {
        OdiInstance odiInstance = getWorkOdiInstance().getOdiInstance();
        ITransactionManager tm = odiInstance.getTransactionManager();
        ITransactionStatus trans = tm.getTransaction(new DefaultTransactionDefinition());
        IOdiEntityManager tem = odiInstance.getTransactionalEntityManager();
        IOdiDataStoreFinder finder = ((IOdiDataStoreFinder) tem
                .getFinder(OdiDataStore.class));
        Collection<OdiDataStore> dataStores = finder.findAll();
        OdiDataStore temp = null;
        for (OdiDataStore ds : dataStores) {
            if (ds.getName().equalsIgnoreCase("W_INVOICELINE_F")) {
                temp = ds;
            }
        }
        Collection<OdiReference> odiReferences = tem
                .findAll(OdiReference.class);
        for (OdiReference odiReference : odiReferences) {
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
    private void changeReferenceType(String testOdiReference) {
        OdiInstance odiInstance = getWorkOdiInstance().getOdiInstance();
        ITransactionManager tm = odiInstance.getTransactionManager();
        tm.getTransaction(new DefaultTransactionDefinition());
        IOdiDataStoreFinder finder = ((IOdiDataStoreFinder) odiInstance.getTransactionalEntityManager()
                .getFinder(OdiDataStore.class));
        Collection<OdiDataStore> dataStores = finder.findAll();
        OdiDataStore temp = null;
        for (OdiDataStore ds : dataStores) {
            if (ds.getName().equalsIgnoreCase("W_INVOICELINE_F")) {
                temp = ds;
            }
        }
        Collection<OdiReference> odiReferences = odiInstance.getTransactionalEntityManager()
                .findAll(OdiReference.class);
        for (OdiReference odiReference : odiReferences) {
            if (StringUtils.equalsIgnoreCase(testOdiReference, // +"_U1",
                    odiReference.toString())) {
                String tempValue = odiReference.getReferenceType().toString();
                odiReference.setReferenceType(ReferenceType.ODI_REFERENCE);
                LOGGER.info("ReferenceType changed from " + tempValue + " to " + odiReference.getReferenceType());
            }
        }
        odiInstance.getTransactionalEntityManager().merge(temp);
        tm.commit(getWorkOdiInstance().getTransactionStatus());
    }

    @SuppressWarnings("unchecked")
    private void reverseForcedChangesForCheckTables(String testOdiReference, String testOdiColumn) {
        OdiInstance odiInstance = getWorkOdiInstance().getOdiInstance();
        IOdiEntityManager tem = odiInstance.getTransactionalEntityManager();
        IOdiDataStoreFinder finder = ((IOdiDataStoreFinder) tem
                .getFinder(OdiDataStore.class));
        Collection<OdiDataStore> dataStores = finder.findAll();
        OdiDataStore temp = null;
        for (OdiDataStore ds : dataStores) {
            if (ds.getName().equalsIgnoreCase("W_INVOICELINE_F")) {
                temp = ds;
            }
        }
        Collection<OdiReference> odiReferences = tem
                .findAll(OdiReference.class);
        for (OdiReference odiReference : odiReferences) {
            if (StringUtils.equalsIgnoreCase(testOdiReference, odiReference.toString())) {
                for (OdiKey odiKey : odiReference.getForeignDataStore().getKeys()) {
                    if (odiKey.getKeyType().equals(OdiKey.KeyType.ALTERNATE_KEY)) {
                        for (OdiColumn indexColun : odiKey.getColumns()) {
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
    private void forcingFailuresForCheckTables(String testOdiReference, String testOdiColumn) {
        OdiInstance odiInstance = getWorkOdiInstance().getOdiInstance();
        IOdiEntityManager tem = odiInstance.getTransactionalEntityManager();
        ITransactionManager tm = odiInstance.getTransactionManager();
        tm.getTransaction(new DefaultTransactionDefinition());
        IOdiDataStoreFinder finder = ((IOdiDataStoreFinder) tem
                .getFinder(OdiDataStore.class));
        Collection<OdiDataStore> dataStores = finder.findAll();
        OdiDataStore temp = null;
        for (OdiDataStore ds : dataStores) {
            if (ds.getName().equalsIgnoreCase("W_INVOICELINE_F")) {
                temp = ds;
            }
        }
        Collection<OdiReference> odiReferences = tem
                .findAll(OdiReference.class);
        for (OdiReference odiReference : odiReferences) {
            if (StringUtils.equalsIgnoreCase(testOdiReference, odiReference.toString())) {
                for (OdiKey odiKey : odiReference.getForeignDataStore().getKeys()) {
                    if (odiKey.getKeyType().equals(OdiKey.KeyType.INDEX)) {
                        for (OdiColumn indexColun : odiKey.getColumns()) {
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
        ListAppender listAppender = getListAppender();
        runController("etb", "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/conf/" + DEFAULT_PROPERTIES,
                "-ps", "10", "-m", "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/jodi_dist", "-srcmdl",
                "ORACLE_CHINOOK");
        if (listAppender.contains(Level.ERROR, false)) {
            Assert.fail("Sample threw errors.");
        }
        removeAppender(listAppender);
    }

    @Test
    public void test160DeleteReferences() {
        LOGGER.info("Create etls started");
        ListAppender listAppender = getListAppender();
        runController("dr", "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/conf/" + DEFAULT_PROPERTIES,
                "-p", "Init ", "-model", "ORACLE_CHINOOK", "-m",
                "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/xml", "-journalized");
        if (listAppender.contains(Level.ERROR, false)) {
            Assert.fail("Sample threw errors.");
        }
        removeAppender(listAppender);
    }

    @Test
    public void test170DeleteAllPackages() {
        ListAppender listAppender = getListAppender();
        runController("dap", "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/conf/" + DEFAULT_PROPERTIES,
                "-p", "Init ", "-m", "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/xml");
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
            runController("etls",
                    "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/conf/" + DEFAULT_PROPERTIES, "-p",
                    "Init ", "-m", "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/xml");
        } catch (Exception e) {
            Assert.fail("Sample threw unexpected exception.");
        }
    }

    /**
     * Testing JodiPropertiesImpl-- The ODI Property file is not found.
     */
    @Test
    public void test181PropertyFileNotFound() {
        try {
            runController("etls",
                    "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/conf/Sample.propertiesNotFound",
                    "-p", "Init ", "-m", "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/xml");
            Assert.fail("Sample did not throw exception on missing properties file.");
        } catch (UnRecoverableException ea) {
            // expected that exception is thrown
        } catch (Exception e) {
            Assert.fail("Sample throw unexception exception.");
        }
    }

    @Test
    public void test200printColumnsDetails() {
        runController("prnt",
                "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/conf/" + DEFAULT_PROPERTIES,
                "-p", "Init ", "-m", "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/xml", "-folder", "InitialORACLE_CHINOOK");
    }

    @Test
    public void test200AlterTables() {
        try {
            runController("atb",
                    "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/conf/" + DEFAULT_PROPERTIES, "-m",
                    "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/xml");
        } catch (Exception e) {
            Assert.fail("Sample threw unexpected exception.");
        }
        IOdiDataStoreFinder finder = (IOdiDataStoreFinder) getWorkOdiInstance().getOdiInstance().getTransactionalEntityManager()
                .getFinder(OdiDataStore.class);
        // OdiDataStore album = finder.findByName("W_ALBUM_D",
        // "ORACLE_DWH_DMT");
        // if(!album.getDataStoreType().equals(DataStoreType.DIMENSION)){
        // throw new RuntimeException("Datatype of datastore album should be
        // dimesion.");
        // }
        OdiDataStore customer = finder.findByName("W_CUSTOMER_D", "ORACLE_DWH_DMT");
        if (!customer.getOlapType().name().equals(DataStoreType.SLOWLY_CHANGING_DIMENSION.name())) {
            throw new RuntimeException("Datatype of customer should be "
                    + DataStoreType.SLOWLY_CHANGING_DIMENSION.name() + " and is: " + customer.getOlapType().name());
        }
        OdiDataStore employee = finder.findByName("W_EMPLOYEE_D", "ORACLE_DWH_DMT");
        if (!employee.getOlapType().name().equals(DataStoreType.SLOWLY_CHANGING_DIMENSION.name())) {
            throw new RuntimeException("Datatype of employee should be "
                    + DataStoreType.SLOWLY_CHANGING_DIMENSION.name() + " and is: " + employee.getOlapType().name());
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
            File generatedFile = new File(
                    "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/xml/loadPlans/ETL.xml");
            if (generatedFile.exists()) {
                if (!generatedFile.delete()) {
                    throw new RuntimeException("can't delete file.");
                }
            }
            File generatedFileTestsForNullpointers = new File(
                    "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/xml/loadPlans/CREATED_TEST_ETL.xml");
            if (generatedFileTestsForNullpointers.exists()) {
                if (!generatedFileTestsForNullpointers.delete()) {
                    throw new RuntimeException("can't delete file.");
                }
            }
            runController("lpe",
                    "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/conf/" + DEFAULT_PROPERTIES, "-p",
                    "Init ", "-m", "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/xml");
            if (!generatedFile.exists()) {
                throw new RuntimeException("Loadplan import did not import loadplan into xml file.");
            }
            File orignal = new File("src/test/resources/CREATED_TEST_ETL.xml");
            File testNullPointer = new File(
                    "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/xml/loadPlans/CREATED_TEST_ETL.xml");
            Files.copy(Paths.get(orignal.getAbsolutePath()), Paths.get(testNullPointer.getAbsolutePath()), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class);
            Unmarshaller u = jc.createUnmarshaller();
            Loadplan loadplan = (Loadplan) u.unmarshal(generatedFile);
            LOGGER.info("Successfully unmarshalled from file loadplan : " + loadplan.getName());
//
            Scanner generatedScanner = new Scanner(generatedFile);
            StringBuilder generatedContent = new StringBuilder();
            while (generatedScanner.hasNext()) {
                generatedContent.append(generatedScanner.next());
            }
            generatedScanner.close();
//
            File controlFile = new File(
                    "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/controlETL.xml");
            Scanner controlScanner = new Scanner(controlFile);
            StringBuilder controlContent = new StringBuilder();
            while (controlScanner.hasNext()) {
                controlContent.append(controlScanner.next());
            }
            controlScanner.close();
            if (!controlContent.toString().equals(generatedContent.toString())) {
                throw new RuntimeException(
                        "content from xml files are technically different compare them manually in eclipse.");
            }
        }
        // So 2 loadplans will get created;
        // CREATED_FROM_FILE ( full specs ) & CREATED_TEST_ETL (nullpointer test)
        // print
        runController("lpp", "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/conf/" + DEFAULT_PROPERTIES,
                "-p", "Init ", "-m", "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/xml");
        // build loadplans
        File previousCreation = new File("src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/xml/loadPlans",
                "CREATED_FROM_FILE.xml");
        if (previousCreation.exists() && !previousCreation.delete()) {
            LOGGER.error("Couldn't deleted CREATED_FROM_FILE");
        }
        File file = new File("src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/xml/loadPlans", "ETL.xml");
        StringBuffer content = new StringBuffer();
        Scanner scanner = new Scanner(file);
        scanner.useDelimiter("\n");
        while (scanner.hasNext()) {
            content.append(scanner.next()).append("\n");
        }
        scanner.close();
        content = new StringBuffer(content.toString().replace("ETL", "CREATED_FROM_FILE"));
        write(file, content.toString());
        runController("lp", "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/conf/" + DEFAULT_PROPERTIES,
                "-p", "Init ", "-m", "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/xml");
        OdiLoadPlan odiLoadplan = odiLoadPlanAccessStrategy.findLoadPlanByName("CREATED_FROM_FILE");
        odiLoadPlanAccessStrategy.verifyOdiLoadPlanPathExists(odiLoadplan, "/Start/DROP_TEMP_INTERFACE_DATASTORES");
        odiLoadPlanAccessStrategy.verifyOdiLoadPlanPathExists(odiLoadplan, "/Start/Case Variable: SAMPLEC.LOAD_UP_TO_DATE/Value Is Null/TEST_MKDIR");
        odiLoadPlanAccessStrategy.verifyOdiLoadPlanPathExists(odiLoadplan, "/Start/Case Variable: SAMPLEC.LOAD_UP_TO_DATE/When Value > 0/DISABLE_CONSTRAINTS2");
        odiLoadPlanAccessStrategy.verifyOdiLoadPlanPathExists(odiLoadplan, "/Start/Parallel/DISABLE_BITMAP_INDEX");
        odiLoadPlanAccessStrategy.verifyOdiLoadPlanPathExists(odiLoadplan, "/Start/INITIALDWH_DMT");
        odiLoadPlanAccessStrategy.verifyOdiLoadPlanPathExists(odiLoadplan, "/Start/INITIALDWH_DMT_FACT");

        if (!new OdiVersion().isVersion11()) {
            OdiLoadPlan odiLoadplanMininmal = odiLoadPlanAccessStrategy.findLoadPlanByName("CREATED_TEST_FILE");
            odiLoadPlanAccessStrategy.verifyOdiLoadPlanPathExists(odiLoadplanMininmal, "/14) Serial/15) DROP_TEMP_INTERFACE_DATASTORES");
            odiLoadPlanAccessStrategy.verifyOdiLoadPlanPathExists(odiLoadplanMininmal, "/14) Serial/16) Case with variable: SAMPLEC.LOAD_UP_TO_DATE/17) When: IS_NULL/18) TEST_MKDIR");
            odiLoadPlanAccessStrategy.verifyOdiLoadPlanPathExists(odiLoadplanMininmal, "/14) Serial/16) Case with variable: SAMPLEC.LOAD_UP_TO_DATE/20) When: GREATER_THAN/DISABLE_CONSTRAINTS2");
            odiLoadPlanAccessStrategy.verifyOdiLoadPlanPathExists(odiLoadplanMininmal, "/14) Serial/27) Parallel/28) DISABLE_BITMAP_INDEX");
            odiLoadPlanAccessStrategy.verifyOdiLoadPlanPathExists(odiLoadplanMininmal, "/14) Serial/29) INITIALDWH_DMT");
            odiLoadPlanAccessStrategy.verifyOdiLoadPlanPathExists(odiLoadplanMininmal, "/14) Serial/30) INITIALDWH_DMT_FACT");
            // steps for validating OdiLoadPlanStepVariables -> Variables attributes, name, value, refresh

            if (!odiLoadPlanAccessStrategy.verifyOdiLoadPlanStepVariableValues(odiLoadplanMininmal, "GLOBAL_VAR_PARTITION_NAME", "2) INITIALDWH_CON_CHINOOK", true, null)) {
                throw new RuntimeException("Loadplanstepvariables not valid");
            }
            if (!odiLoadPlanAccessStrategy.verifyOdiLoadPlanStepVariableValues(odiLoadplanMininmal, "SAMPLEC.VAR_PARTITION_NAME", "4) Serial", false, "TEST VAR Exception")) {
                throw new RuntimeException("Loadplanstepvariables not valid");
            }
            if (!odiLoadPlanAccessStrategy.verifyOdiLoadPlanStepVariableValues(odiLoadplanMininmal, "SAMPLEC.VAR_PARTITION_NAME", "15) DROP_TEMP_INTERFACE_DATASTORES", false, "TEST VAR RUN SCENARIO")) {
                throw new RuntimeException("Loadplanstepvariables not valid");
            }
            if (!odiLoadPlanAccessStrategy.verifyOdiLoadPlanStepVariableValues(odiLoadplanMininmal, "SAMPLEC.VAR_PARTITION_NAME", "16) Case with variable: SAMPLEC.LOAD_UP_TO_DATE", false, "TEST VAR RUN CASE")) {
                throw new RuntimeException("Loadplanstepvariables not valid");
            }
        }
    }

    @Test
    public void test400CreateScenarios() {
        runController("cs",
                "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/conf/" + DEFAULT_PROPERTIES,
                "-p", "Init ", "-m", "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/xml");
    }

    //@After
    public void deleteCreatedLoadPlans() {
        ITransactionManager tm = getWorkOdiInstance().getOdiInstance().getTransactionManager();
        IOdiLoadPlanFinder loadPlanFinder = (IOdiLoadPlanFinder) getWorkOdiInstance().getOdiInstance().getTransactionalEntityManager()
                .getFinder(OdiLoadPlan.class);
        //noinspection unchecked
        for (OdiLoadPlan loadPlan : (Collection<OdiLoadPlan>) loadPlanFinder.findAll()) {
            if (loadPlan.getName().toLowerCase().startsWith("created")) {
                getWorkOdiInstance().getOdiInstance().getTransactionalEntityManager().remove(loadPlan);
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
        runController("oex", "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/conf/SampleC.properties",
                "-p", "Init ", "-m", "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/xml", "-dapwd",
                new PasswordConfigImpl().getDeploymentArchivePassword(), "-da_type", "DA_PATCH_EXEC_REPOS");
    }

    @Test
    @Ignore
    public void test602CreateConstraints() {
        runController("expcon",
                "src/test/resources/" + SampleHelper.getFunctionalTestDir()
                        + "/conf/" + DEFAULT_PROPERTIES, "-p", "Init ", "-m",
                "src/test/resources/" + SampleHelper.getFunctionalTestDir()
                        + "/xml", "-exportDBConstraints", "true");
    }

    @Test
    public void test700Procedures() {
        runController("delproc",
                "src/test/resources/" + SampleHelper.getFunctionalTestDir()
                        + "/conf/" + DEFAULT_PROPERTIES, "-p", "Init ", "-m",
                "src/test/resources/" + SampleHelper.getFunctionalTestDir()
                        + "/xml", "-exportDBConstraints", "true");

        runController("crtproc",
                "src/test/resources/" + SampleHelper.getFunctionalTestDir()
                        + "/conf/" + DEFAULT_PROPERTIES, "-p", "Init ", "-m",
                "src/test/resources/" + SampleHelper.getFunctionalTestDir()
                        + "/xml", "-exportDBConstraints", "true");


        Properties properties;
        FileReader reader = null;
        try {
            reader = new FileReader(
                    "src/test/resources/" + SampleHelper.getFunctionalTestDir() + "/conf/" + DEFAULT_PROPERTIES);
            properties = new Properties();
            properties.load(reader);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot check if this test runs in update mode.");
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        OdiConnection odiConnection = OdiConnectionFactory.getOdiConnection(
                properties.getProperty("odi.master.repo.url"),
                properties.getProperty("odi.master.repo.username"),
                new PasswordConfigImpl().getOdiMasterRepoPassword(),
                properties.getProperty("odi.login.username"),
                new PasswordConfigImpl().getOdiUserPassword(),
                properties.getProperty("odi.repo.db.driver"),
                properties.getProperty("odi.work.repo"));

        IOdiUserProcedureFinder finder = (IOdiUserProcedureFinder) odiConnection.getOdiInstance().getFinder(OdiUserProcedure.class);
        Collection<OdiUserProcedure> testProcs = finder.findByName("Test Proc");
        if (testProcs.size() != 1) {
            throw new RuntimeException("Couldn't find 1 instanc of procedure Test Proc");
        }
        OdiUserProcedure testProc = testProcs.iterator().next();
        OdiProcedureLineCmd targetCommand = testProc.getLines().get(0).getOnTargetCommand();
        if (!targetCommand.getExecutionContext().getCode().equals("GLOBAL")) {
            throw new RuntimeException("Wrong context set");
        }
        if (!targetCommand.getExpression().getAsString()
                .replace("\n", "").replace("\r\n", "").equalsIgnoreCase("beginnull;end;")) {
            throw new RuntimeException("Wrong code set");
        }
        if (!targetCommand.getLogicalSchema().getName().equalsIgnoreCase("ORACLE_DWH_CON_CHINOOK")) {
            throw new RuntimeException("Wrong logicalschema set");
        }
        if (!targetCommand.getTechnology().getName().equalsIgnoreCase("ORACLE")) {
            throw new RuntimeException("Wrong technology set");
        }
        List<ProcedureOption> options = testProc.getOptions();
        if (options.size() != 4) {
            throw new RuntimeException("There should be 4 options set.");
        }
        Optional<ProcedureOption> testBoolean = options.stream().filter(o -> o.getName().equalsIgnoreCase("TestBoolean")).findFirst();
        testOption(testBoolean, OptionType.CHECKBOX, true, "1=1", "A Description for boolean.", "A help message for boolean.");
        Optional<ProcedureOption> TestText = options.stream().filter(o -> o.getName().equalsIgnoreCase("TestText")).findFirst();
        testOption(TestText, OptionType.LONG_TEXT, "testString", "1=1", "A Description for text.", "A help message for text.");
        Optional<ProcedureOption> TestValue = options.stream().filter(o -> o.getName().equalsIgnoreCase("TestValue")).findFirst();
        testOption(TestValue, OptionType.CHOICE, "testValue", "1=1", "A Description for value.", "A help message for value.");
        Optional<ProcedureOption> TestChoice = options.stream().filter(o -> o.getName().equalsIgnoreCase("TestChoice")).findFirst();
        testOption(TestChoice, OptionType.CHOICE, "testChoice", "1=1", "A Description for choice.", "A help message for choice.");
    }

    @Test
    public void test800validate() {
        runController("vldt",
                "src/test/resources/" + SampleHelper.getFunctionalTestDir()
                        + "/conf/" + DEFAULT_PROPERTIES, "-p", "Init ", "-m",
                "src/test/resources/" + SampleHelper.getFunctionalTestDir()
                        + "/xml");
    }


    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void testOption(Optional<ProcedureOption> option, OptionType type, Object defaultValue, String condition,
                            String description, String help) {
        if (!option.isPresent()) {
            throw new RuntimeException("There should be an option set.");
        }
        if (!option.get().getOptionType().equals(type)) {
            throw new RuntimeException(String.format("There should be an option of type %s set.", option.get().getOptionType().name()));
        }
        //noinspection deprecation
        if (!option.get().getDefaultValue().equals(defaultValue)) {
            //noinspection deprecation
            throw new RuntimeException(String.format("There should be an option with defaultvalue %s set.", option.get().getDefaultValue()));
        }
        if (!option.get().getConditionExpression().equalsIgnoreCase(condition)) {
            throw new RuntimeException(String.format("There should be an option with condition %s set.", option.get().getConditionExpression()));
        }
        if (!option.get().getDescription().equalsIgnoreCase(description)) {
            throw new RuntimeException(String.format("There should be an option with description %s set.", option.get().getDescription()));
        }
        if (!option.get().getHelp().equalsIgnoreCase(help)) {
            throw new RuntimeException(String.format("There should be an option with description %s set.", option.get().getHelp()));
        }
    }

    private void write(File file, String input) {
        FileOutputStream fos = null;
        Writer out = null;
        try {
            fos = new FileOutputStream(file.getAbsolutePath());
            out = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
            out.write(input);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
