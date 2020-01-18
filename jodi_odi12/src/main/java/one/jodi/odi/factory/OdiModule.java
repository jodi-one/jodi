package one.jodi.odi.factory;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.matcher.Matchers;
import one.jodi.annotations.DeploymentArchivePassword;
import one.jodi.base.annotations.Cached;
import one.jodi.base.aop.WriteThroughCacheInterceptor;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.service.metadata.SchemaMetaDataProvider;
import one.jodi.bootstrap.EtlRunConfig;
import one.jodi.core.annotations.MasterPassword;
import one.jodi.core.annotations.TransactionAttribute;
import one.jodi.core.aop.TransactionInterceptor;
import one.jodi.etl.loadplan.enrichment.EnrichmentBuilder;
import one.jodi.etl.loadplan.enrichment.EnrichmentBuilderImpl;
import one.jodi.etl.loadplan.enrichment.EnrichmentVisitor;
import one.jodi.etl.loadplan.enrichment.EnrichmentVisitorImpl;
import one.jodi.etl.service.SubsystemServiceProvider;
import one.jodi.etl.service.datastore.DatastoreServiceProvider;
import one.jodi.etl.service.scenarios.ScenarioServiceProvider;
import one.jodi.etl.service.table.TableServiceProvider;
import one.jodi.etl.service.transaction.TransactionServiceProvider;
import one.jodi.odi.common.FlexfieldUtil;
import one.jodi.odi.common.FlexfieldUtilImpl;
import one.jodi.odi.common.OdiInstanceManager;
import one.jodi.odi.datastore.OdiDatastoreServiceProvider;
import one.jodi.odi.etl.OdiCommon;
import one.jodi.odi.etl.OdiETLProvider;
import one.jodi.odi.etl.OdiSubsystemServiceProvider;
import one.jodi.odi.scenarios.OdiScenarioServiceProvider;
import one.jodi.odi.table.OdiTableServiceImpl;
import one.jodi.odi.transaction.OdiTransactionServiceProvider;
import oracle.odi.core.OdiInstance;
import oracle.odi.domain.model.OdiDataStore;
import oracle.odi.domain.model.OdiModel;
import oracle.odi.domain.project.OdiInterface;
import oracle.odi.domain.project.ProcedureOptionBuilder;
import oracle.odi.domain.project.ProcedureOptionBuilderImpl;

import java.lang.reflect.Method;

@SuppressWarnings("deprecation")
public class OdiModule extends AbstractModule {

    private final RunConfig config;

    public OdiModule(final RunConfig config) {
        super();
        this.config = config;
    }

    @Override
    protected void configure() {
        String masterPwd = (config.getMasterPassword() == null) ? "" :
                config.getMasterPassword();
        bind(String.class).annotatedWith(MasterPassword.class)
                .toInstance(masterPwd);
        if (config instanceof EtlRunConfig) {
            String deploymentArchivePassword = (((EtlRunConfig) config).getDeployementArchivePassword() == null) ? "" :
                    ((EtlRunConfig) config).getDeployementArchivePassword();
            bind(String.class).annotatedWith(DeploymentArchivePassword.class)
                    .toInstance(deploymentArchivePassword);
        }
        //use provide to generate ODIInstance, which follows the singleton pattern
        //this avoids the need to close and open new ODI instances for every task
        //or pass the instance around through method calls
        bind(OdiInstance.class).toProvider(OdiInstanceManager.class)
                .in(Singleton.class);
        // used for running within ODI.
        bind(OdiCommon.class).to(OdiETLProvider.class);
        bind(SchemaMetaDataProvider.class).to(OdiETLProvider.class);
        bind(SubsystemServiceProvider.class).to(OdiSubsystemServiceProvider.class);
        bind(TransactionServiceProvider.class).to(OdiTransactionServiceProvider.class);

        //more complex logic required for binding an instance with a generic type
        bind(new TypeLiteral<FlexfieldUtil<OdiInterface>>() {
        })
                .to(new TypeLiteral<FlexfieldUtilImpl<OdiInterface>>() {
                })
                .in(Singleton.class);
        bind(new TypeLiteral<FlexfieldUtil<OdiModel>>() {
        })
                .to(new TypeLiteral<FlexfieldUtilImpl<OdiModel>>() {
                })
                .in(Singleton.class);
        bind(new TypeLiteral<FlexfieldUtil<OdiDataStore>>() {
        })
                .to(new TypeLiteral<FlexfieldUtilImpl<OdiDataStore>>() {
                })
                .in(Singleton.class);

        bind(TableServiceProvider.class).to(OdiTableServiceImpl.class);
        bind(DatastoreServiceProvider.class).to(OdiDatastoreServiceProvider.class);
        bind(ScenarioServiceProvider.class).to(OdiScenarioServiceProvider.class);

        //

        bind(EnrichmentBuilder.class).to(EnrichmentBuilderImpl.class);
        bind(EnrichmentVisitor.class).to(EnrichmentVisitorImpl.class);

//		bind(LoadPlanImportService.class).to(OdiLoadPlanImportServiceImpl.class);
//		bind(LoadPlanValidationService.class).to(one.jodi.odi.loadplan.service.OdiLoadPlanValidationService.class);
//		bind(LoadPlanService.class).to(OdiLoadPlanServiceImpl.class);

        bind(ProcedureOptionBuilder.class).to(ProcedureOptionBuilderImpl.class);

        //
        // AOP
        //

        // add transactions to classes annotated with @TransactionAttribute
        TransactionInterceptor ti = new TransactionInterceptor();
        // TransactionMethodMatcher includes logic to
        // prevent annotation of synthetic methods
        bindInterceptor(Matchers.inSubpackage(packageScopeForAOP()),
                new TransactionMethodMatcher(), ti);
        requestInjection(ti);

        WriteThroughCacheInterceptor ci = new WriteThroughCacheInterceptor();
        // CacheMethodMatcher includes logic to prevent annotation of synthetic methods
        bindInterceptor(Matchers.inSubpackage(packageScopeForAOP()),
                new CacheMethodMatcher(), ci);
        requestInjection(ci);
    }

    private String packageScopeForAOP() {
        String packageName = this.getClass().getPackage().toString();
        int first = packageName.lastIndexOf(' ') + 1;
        int last = packageName.lastIndexOf('.');

        return packageName.substring(first, last);
    }

    /**
     * Handling of synthetic methods
     *
     * @see <a href="https://groups.google.com/forum/#!topic/google-guice/-DH5fBD7M30">
     * https://groups.google.com/forum/#!topic/google-guice/-DH5fBD7M30</a>
     */
    private static final class CacheMethodMatcher extends AbstractMatcher<Method> {
        @Override
        public boolean matches(final Method method) {
            return method.isAnnotationPresent(Cached.class) && !method.isSynthetic();
        }
    }

    /**
     * Handling of synthetic methods
     *
     * @see <a href="https://groups.google.com/forum/#!topic/google-guice/-DH5fBD7M30">
     * https://groups.google.com/forum/#!topic/google-guice/-DH5fBD7M30</a>
     */
    private static final class TransactionMethodMatcher extends AbstractMatcher<Method> {
        @Override
        public boolean matches(final Method method) {
            return method.isAnnotationPresent(TransactionAttribute.class) &&
                    !method.isSynthetic();
        }
    }

}
