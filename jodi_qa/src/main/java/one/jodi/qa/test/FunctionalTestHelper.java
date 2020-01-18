package one.jodi.qa.test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import one.jodi.action.CreateScenariosActionRunner;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.config.PasswordConfigImpl;
import one.jodi.base.factory.BaseModule;
import one.jodi.bootstrap.EtlRunConfig;
import one.jodi.bootstrap.JodiController;
import one.jodi.core.factory.CoreETLModule;
import one.jodi.odi.common.OdiVersion;
import one.jodi.odi.constraints.OdiConstraintAccessStrategy;
import one.jodi.odi.factory.OdiModule;
import one.jodi.odi.interfaces.OdiTransformationAccessStrategy;
import one.jodi.odi.loadplan.OdiLoadPlanAccessStrategy;
import one.jodi.odi.packages.OdiPackageAccessStrategy;
import one.jodi.odi.sequences.OdiSequenceAccessStrategy;
import one.jodi.odi.variables.OdiVariableAccessStrategy;
import one.jodi.odi12.factory.Odi12Module;
import one.jodi.odi12.procedure.Odi12ProcedureServiceProvider;
import oracle.odi.core.OdiInstance;
import oracle.odi.domain.adapter.relational.IColumn;
import oracle.odi.domain.adapter.relational.IKey;
import oracle.odi.domain.adapter.topology.ILogicalSchema;
import oracle.odi.domain.mapping.IMapComponent;
import oracle.odi.domain.mapping.MapAttribute;
import oracle.odi.domain.mapping.MapRootContainer;
import oracle.odi.domain.mapping.Mapping;
import oracle.odi.domain.mapping.component.Dataset;
import oracle.odi.domain.mapping.component.DatastoreComponent;
import oracle.odi.domain.mapping.component.ReusableMappingComponent;
import oracle.odi.domain.mapping.finder.IMappingFinder;
import oracle.odi.domain.project.Step;
import oracle.odi.domain.project.StepMapping;
import oracle.odi.domain.runtime.loadplan.OdiLoadPlan;
import oracle.odi.domain.topology.OdiContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.assertTrue;

public class FunctionalTestHelper {

    private final static Logger logger =
            LogManager.getLogger(CreateScenariosActionRunner.class);

    static OdiSequenceAccessStrategy odiSequenceAccessStrategy;
    static OdiVariableAccessStrategy odiVariableAccessStrategy;
    static OdiConstraintAccessStrategy odiConstraintAccessStrategy;
    static OdiTransformationAccessStrategy<MapRootContainer, Dataset, DatastoreComponent,
            ReusableMappingComponent, IMapComponent,
            OdiContext, ILogicalSchema>
            odiTransformationAccessStrategy;
    static OdiPackageAccessStrategy<Mapping, StepMapping> odiPackageAccessStrategy;
    static OdiLoadPlanAccessStrategy<OdiLoadPlan, Mapping> odiLoadPlanAccessStrategy;
    static Odi12ProcedureServiceProvider odi12ProcedureServiceProvider;

    public static String getPropertiesDir() {
        String propertiesDir = "Properties_12c";
        if (new OdiVersion().isVersion11()) {
            propertiesDir = "Properties_11g";
        }
        return propertiesDir;
    }

    public static String getDefaultAgent(String properties) {
        logger.info(properties);
        String hostName = "localhost";
        try {
            hostName = Files.lines(Paths.get(properties)).filter(l -> l.contains("jodi:1521")).count() > 0 ? "jodi" : "localhost";
        } catch (IOException e) {
            hostName = "localhost";
        }
        return "http://" + hostName + ":20914/oraclediagent";
    }

    public static String getOdiPass() {
        return new PasswordConfigImpl().getOdiUserPassword();
    }

    public static OdiTransformationAccessStrategy<MapRootContainer, Dataset,
            DatastoreComponent, ReusableMappingComponent, IMapComponent, OdiContext, ILogicalSchema>
    getOdiAccessStrategy(RunConfig runConfig, JodiController jodiController) {
        if (odiTransformationAccessStrategy == null) {
            EtlRunConfig etlRunConfig = (EtlRunConfig) runConfig;
            Injector injector = Guice.createInjector(new Odi12Module(),
                    new OdiModule(runConfig),
                    new CoreETLModule(etlRunConfig),
                    new BaseModule(runConfig, jodiController));
            odiTransformationAccessStrategy = injector.getInstance(Key.get(
                    new TypeLiteral<OdiTransformationAccessStrategy<MapRootContainer,
                            Dataset, DatastoreComponent, ReusableMappingComponent,
                            IMapComponent, OdiContext, ILogicalSchema>>() {
                    }));
        }
        return odiTransformationAccessStrategy;
    }

    public static OdiPackageAccessStrategy<Mapping, StepMapping>
    getOdiPackageAccessStrategy(final RunConfig runConfig,
                                final JodiController jodiController) {
        if (odiPackageAccessStrategy == null) {
            EtlRunConfig etlRunConfig = (EtlRunConfig) runConfig;
            Injector injector = Guice.createInjector(new Odi12Module(),
                    new OdiModule(runConfig),
                    new CoreETLModule(etlRunConfig),
                    new BaseModule(runConfig, jodiController));
            odiPackageAccessStrategy = injector.getInstance(Key.get(
                    new TypeLiteral<OdiPackageAccessStrategy<Mapping, StepMapping>>() {
                    }));
        }
        return odiPackageAccessStrategy;
    }

    public static OdiSequenceAccessStrategy getOdiSequenceAccessStrategy(EtlRunConfig runConfig,
                                                                         JodiController controller) {

        if (odiSequenceAccessStrategy == null) {
            EtlRunConfig etlRunConfig = (EtlRunConfig) runConfig;
            Injector injector = Guice.createInjector(new Odi12Module(),
                    new OdiModule(runConfig),
                    new CoreETLModule(etlRunConfig),
                    new BaseModule(runConfig, controller));
            odiSequenceAccessStrategy = injector.getInstance(Key.get(
                    new TypeLiteral<OdiSequenceAccessStrategy>() {
                    }));
        }
        return odiSequenceAccessStrategy;
    }

    public static OdiVariableAccessStrategy getOdiVariableAccessStrategy(EtlRunConfig runConfig,
                                                                         JodiController controller) {
        if (odiVariableAccessStrategy == null) {
            EtlRunConfig etlRunConfig = (EtlRunConfig) runConfig;
            Injector injector = Guice.createInjector(new Odi12Module(),
                    new OdiModule(runConfig),
                    new CoreETLModule(etlRunConfig),
                    new BaseModule(runConfig, controller));
            odiVariableAccessStrategy = injector.getInstance(Key.get(
                    new TypeLiteral<OdiVariableAccessStrategy>() {
                    }));
        }
        return odiVariableAccessStrategy;
    }

    public static OdiConstraintAccessStrategy getOdiConstraintsAccessStrategy(EtlRunConfig runConfig,
                                                                              JodiController controller) {
        if (odiConstraintAccessStrategy == null) {
            EtlRunConfig etlRunConfig = (EtlRunConfig) runConfig;
            Injector injector = Guice.createInjector(new Odi12Module(),
                    new OdiModule(runConfig),
                    new CoreETLModule(etlRunConfig),
                    new BaseModule(runConfig, controller));
            odiConstraintAccessStrategy = injector.getInstance(Key.get(
                    new TypeLiteral<OdiConstraintAccessStrategy>() {

                    }));
        }
        return odiConstraintAccessStrategy;
    }

    public static OdiLoadPlanAccessStrategy<OdiLoadPlan, Mapping> getOdiLoadPlanAccessStrategy(EtlRunConfig runConfig,
                                                                                               JodiController controller) {
        if (odiLoadPlanAccessStrategy == null) {
            EtlRunConfig etlRunConfig = (EtlRunConfig) runConfig;
            Injector injector = Guice.createInjector(new Odi12Module(),
                    new OdiModule(runConfig),
                    new CoreETLModule(etlRunConfig),
                    new BaseModule(runConfig, controller));
            odiLoadPlanAccessStrategy = injector.getInstance(Key.get(
                    new TypeLiteral<OdiLoadPlanAccessStrategy<OdiLoadPlan, Mapping>>() {
                    }));
        }
        return odiLoadPlanAccessStrategy;
    }

    public static Odi12ProcedureServiceProvider getOdiProcedureService(
            final EtlRunConfig runConfig,
            final JodiController controller) {
        if (odi12ProcedureServiceProvider == null) {
            EtlRunConfig etlRunConfig = (EtlRunConfig) runConfig;
            Injector injector =
                    Guice.createInjector(new Odi12Module(), new OdiModule(runConfig),
                            new CoreETLModule(etlRunConfig),
                            new BaseModule(runConfig, controller));
            odi12ProcedureServiceProvider =
                    injector.getInstance(Odi12ProcedureServiceProvider.class);
        }
        return odi12ProcedureServiceProvider;
    }

    public static <T extends Object> void checkThatKeysAreSet(T mapping, String string) throws Exception {
        boolean found = false;
        for (IMapComponent target : ((Mapping) mapping).getTargets()) {
            if (target instanceof DatastoreComponent) {
                IKey key = ((DatastoreComponent) target).getUpdateKey();
                assert (key != null);
                for (IColumn col : key.getColumns()) {
                    if (col.getName().equals(string)) {
                        found = true;
                    }
                }
            }
        }
        if (!found) {
            throw new RuntimeException(String.format("Update Key '%1$s' is not set for mapping '%2$s'.", string,
                    ((Mapping) mapping).getName()));
        }
    }

    public static void printColumnsDetails(OdiInstance odiInstance) throws Exception {
        IMappingFinder mappingsFinder = (IMappingFinder) odiInstance.getTransactionalEntityManager()
                .getFinder(Mapping.class);
        Set<String> messages = new TreeSet<String>();
        @SuppressWarnings("unchecked")
        Collection<Mapping> mappings = mappingsFinder.findAll();
        for (Mapping m : mappings) {
            for (IMapComponent t : m.getTargets()) {
                IKey updateKey = null;
                if (t instanceof DatastoreComponent) {
                    updateKey = ((DatastoreComponent) t).getUpdateKey();
                }

                for (MapAttribute a : t.getAttributes()) {
                    boolean isUpdateKey = false;
                    if (updateKey != null) {
                        for (IColumn col : updateKey.getColumns()) {
                            if (col.getName().equals(a.getName())) {
                                isUpdateKey = true;
                                break;
                            }
                        }
                    }
                    String message = "Map '" + m.getName() + "' tc '" + a.getName() + "' insert '"
                            + a.isInsertIndicator() + "' update '" + a.isUpdateIndicator() + "' active '" + a.isActive()
                            + "' check not null '" + a.isCheckNotNullIndicator() + "' key '" + isUpdateKey + "' exec '"
                            + a.getExecuteOnHint() + "'.";
                    messages.add(message);
                }
            }
        }
        for (String m : messages) {
            logger.info(m);
        }
    }

    public Step validateInterfaceStep(int count, Step currentStep) {
        int stepCnt = 0;
        Step activeStep = currentStep;
        while (stepCnt++ < count) {
            assertTrue("Expected interface step", activeStep instanceof StepMapping);

            activeStep = activeStep.getNextStepAfterSuccess();
        }
        return activeStep;
    }

}
