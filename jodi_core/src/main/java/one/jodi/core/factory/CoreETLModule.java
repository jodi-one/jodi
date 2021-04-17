package one.jodi.core.factory;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.matcher.Matchers;
import com.google.inject.util.Providers;
import one.jodi.annotations.IncludeVariables;
import one.jodi.base.annotations.Cached;
import one.jodi.base.annotations.DefaultStrategy;
import one.jodi.base.aop.WriteThroughCacheInterceptor;
import one.jodi.base.context.Context;
import one.jodi.base.context.ContextImpl;
import one.jodi.base.service.files.FileCollector;
import one.jodi.base.service.files.FileCollectorImpl;
import one.jodi.bootstrap.EtlRunConfig;
import one.jodi.core.annotations.ExportInDatabaseConstraints;
import one.jodi.core.annotations.IncludeConstraints;
import one.jodi.core.annotations.InterfacePrefix;
import one.jodi.core.annotations.JournalizedData;
import one.jodi.core.annotations.MasterPassword;
import one.jodi.core.annotations.TransactionAttribute;
import one.jodi.core.aop.TransactionInterceptor;
import one.jodi.core.automapping.ColumnMappingContext;
import one.jodi.core.automapping.impl.ColumnMappingContextImpl;
import one.jodi.core.automapping.impl.ColumnMappingDefaultStrategy;
import one.jodi.core.automapping.impl.ColumnMappingIDStrategy;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.config.JodiPropertiesImpl;
import one.jodi.core.config.km.KnowledgeModulePropertiesProvider;
import one.jodi.core.config.km.KnowledgeModulePropertiesProviderImpl;
import one.jodi.core.config.modelproperties.ModelPropertiesProvider;
import one.jodi.core.config.modelproperties.ModelPropertiesProviderImpl;
import one.jodi.core.context.packages.PackageCache;
import one.jodi.core.context.packages.PackageCacheImpl;
import one.jodi.core.datastore.ModelCodeContext;
import one.jodi.core.datastore.impl.ModelCodeContextImpl;
import one.jodi.core.datastore.impl.ModelCodeDefaultStrategy;
import one.jodi.core.datastore.impl.ModelCodeIDStrategy;
import one.jodi.core.executionlocation.ExecutionLocationContext;
import one.jodi.core.executionlocation.impl.ExecutionLocationContextImpl;
import one.jodi.core.executionlocation.impl.ExecutionLocationDefaultStrategy;
import one.jodi.core.executionlocation.impl.ExecutionLocationIDStrategy;
import one.jodi.core.extensions.strategies.ColumnMappingStrategy;
import one.jodi.core.extensions.strategies.ExecutionLocationStrategy;
import one.jodi.core.extensions.strategies.FlagsStrategy;
import one.jodi.core.extensions.strategies.FolderNameStrategy;
import one.jodi.core.extensions.strategies.JournalizingStrategy;
import one.jodi.core.extensions.strategies.KnowledgeModuleStrategy;
import one.jodi.core.extensions.strategies.ModelCodeStrategy;
import one.jodi.core.extensions.strategies.TransformationNameStrategy;
import one.jodi.core.folder.FolderNameContext;
import one.jodi.core.folder.impl.FolderNameContextImpl;
import one.jodi.core.folder.impl.FolderNameDefaultStrategy;
import one.jodi.core.folder.impl.FolderNameIDStrategy;
import one.jodi.core.journalizing.JournalizingContext;
import one.jodi.core.journalizing.impl.JournalizingContextImpl;
import one.jodi.core.journalizing.impl.JournalizingDefaultStrategy;
import one.jodi.core.journalizing.impl.JournalizingIDStrategy;
import one.jodi.core.km.KnowledgeModuleContext;
import one.jodi.core.km.impl.KnowledgeModuleContextImpl;
import one.jodi.core.km.impl.KnowledgeModuleDefaultStrategy;
import one.jodi.core.km.impl.KnowledgeModuleIDStrategy;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.core.metadata.DatabaseMetadataServiceImpl;
import one.jodi.core.metadata.ETLSubsystemService;
import one.jodi.core.metadata.ETLSubsystemServiceImpl;
import one.jodi.core.service.DatastoreService;
import one.jodi.core.service.ExtractionTables;
import one.jodi.core.service.MetadataServiceProvider;
import one.jodi.core.service.ModelValidator;
import one.jodi.core.service.PackageService;
import one.jodi.core.service.ProcedureService;
import one.jodi.core.service.ScenarioService;
import one.jodi.core.service.TableService;
import one.jodi.core.service.TransformationService;
import one.jodi.core.service.impl.DatastoreServiceImpl;
import one.jodi.core.service.impl.ModelValidatorImpl;
import one.jodi.core.service.impl.OneToOneMappingGenerationImpl;
import one.jodi.core.service.impl.PackageServiceImpl;
import one.jodi.core.service.impl.ProcedureServiceImpl;
import one.jodi.core.service.impl.ScenarioServiceImpl;
import one.jodi.core.service.impl.TableServiceImpl;
import one.jodi.core.service.impl.TransformationServiceImpl;
import one.jodi.core.service.impl.XMLMetadataServiceProvider;
import one.jodi.core.targetcolumn.FlagsContext;
import one.jodi.core.targetcolumn.impl.FlagsContextImpl;
import one.jodi.core.targetcolumn.impl.FlagsDefaultStrategy;
import one.jodi.core.targetcolumn.impl.FlagsIDStrategy;
import one.jodi.core.transformation.TransformationNameContext;
import one.jodi.core.transformation.impl.TransformationNameContextImpl;
import one.jodi.core.transformation.impl.TransformationNameDefaultStrategy;
import one.jodi.core.transformation.impl.TransformationNameIDStrategy;
import one.jodi.core.validation.etl.ETLValidator;
import one.jodi.core.validation.etl.ETLValidatorImpl;
import one.jodi.core.validation.packages.PackageValidator;
import one.jodi.core.validation.packages.PackageValidatorImpl;
import one.jodi.etl.builder.EnrichingBuilder;
import one.jodi.etl.builder.PackageBuilder;
import one.jodi.etl.builder.ProcedureTransformationBuilder;
import one.jodi.etl.builder.TransformationBuilder;
import one.jodi.etl.builder.impl.DictionaryModelLogicalSchema;
import one.jodi.etl.builder.impl.DictionaryModelLogicalSchemaImpl;
import one.jodi.etl.builder.impl.EnrichingBuilderImpl;
import one.jodi.etl.builder.impl.PackageBuilderImpl;
import one.jodi.etl.builder.impl.ProcedureTransformationBuilderImpl;
import one.jodi.etl.builder.impl.TransformationBuilderImpl;

import java.lang.reflect.Method;

/**
 * Defines the injection bindings
 */
public class CoreETLModule extends AbstractModule {

    private final EtlRunConfig config;

    public CoreETLModule(final EtlRunConfig config) {
        super();
        this.config = config;
    }

    @Override
    protected void configure() {

        String masterPwd = (config.getMasterPassword() == null) ? "" :
                config.getMasterPassword();
        bind(String.class).annotatedWith(MasterPassword.class)
                .toInstance(masterPwd);

        bind(String.class).annotatedWith(InterfacePrefix.class)
                .toInstance(config.getPrefix());
        bind(String.class).annotatedWith(JournalizedData.class)
                .toInstance(String.valueOf(config.isJournalized()));

        // Core classes that support metadata service
        bind(JodiProperties.class).to(JodiPropertiesImpl.class);
        // Core classes that support metadata service
        bind(Context.class).to(ContextImpl.class).in(Singleton.class);
        bind(ModelPropertiesProvider.class).to(ModelPropertiesProviderImpl.class)
                .asEagerSingleton();
        bind(DatabaseMetadataService.class).to(DatabaseMetadataServiceImpl.class);


        bind(MetadataServiceProvider.class).to(XMLMetadataServiceProvider.class);
        bind(TransformationService.class).to(TransformationServiceImpl.class);
        bind(EnrichingBuilder.class).to(EnrichingBuilderImpl.class);
        bind(TransformationBuilder.class).to(TransformationBuilderImpl.class);
        bind(ETLValidator.class).to(ETLValidatorImpl.class);

        bind(ETLSubsystemService.class).to(ETLSubsystemServiceImpl.class);
        bind(KnowledgeModulePropertiesProvider.class)
                .to(KnowledgeModulePropertiesProviderImpl.class)
                .asEagerSingleton();

        bind(PackageService.class).to(PackageServiceImpl.class);
        bind(PackageCache.class).to(PackageCacheImpl.class);
        bind(PackageBuilder.class).to(PackageBuilderImpl.class);
        bind(PackageValidator.class).to(PackageValidatorImpl.class);

        bind(TableService.class).to(TableServiceImpl.class);
        bind(ModelValidator.class).to(ModelValidatorImpl.class);
        bind(DatastoreService.class).to(DatastoreServiceImpl.class);
        bind(ScenarioService.class).to(ScenarioServiceImpl.class);
        bind(ExtractionTables.class).to(OneToOneMappingGenerationImpl.class);

        bind(Boolean.class).annotatedWith(IncludeConstraints.class)
                .toProvider(Providers.of(config.isIncludingConstraints()));

        bind(Boolean.class).annotatedWith(ExportInDatabaseConstraints.class)
                .toProvider(Providers.of(config.isExportingDBConstraints()));

        // Dependencies for Procedures
        bind(ProcedureService.class).to(ProcedureServiceImpl.class);
        bind(ProcedureTransformationBuilder.class).to(ProcedureTransformationBuilderImpl.class);

        //
        // Extensions of Core ETL
        //

        bind(TransformationNameContext.class).to(TransformationNameContextImpl.class);
        bind(TransformationNameStrategy.class).annotatedWith(DefaultStrategy.class)
                .to(TransformationNameDefaultStrategy.class);
        bind(TransformationNameStrategy.class).to(TransformationNameIDStrategy.class);

        bind(FolderNameContext.class).to(FolderNameContextImpl.class);
        bind(FolderNameStrategy.class).annotatedWith(DefaultStrategy.class).to(FolderNameDefaultStrategy.class);
        bind(FolderNameStrategy.class).to(FolderNameIDStrategy.class);

        bind(ModelCodeContext.class).to(ModelCodeContextImpl.class);
        bind(ModelCodeStrategy.class).annotatedWith(DefaultStrategy.class).to(ModelCodeDefaultStrategy.class);
        bind(ModelCodeStrategy.class).to(ModelCodeIDStrategy.class);

        bind(KnowledgeModuleContext.class).to(KnowledgeModuleContextImpl.class);
        bind(KnowledgeModuleStrategy.class).to(KnowledgeModuleIDStrategy.class);
        bind(KnowledgeModuleStrategy.class).annotatedWith(DefaultStrategy.class)
                .to(KnowledgeModuleDefaultStrategy.class);

        bind(ExecutionLocationContext.class).to(ExecutionLocationContextImpl.class);
        bind(ExecutionLocationStrategy.class).annotatedWith(DefaultStrategy.class)
                .to(ExecutionLocationDefaultStrategy.class);
        bind(ExecutionLocationStrategy.class).to(ExecutionLocationIDStrategy.class);

        bind(FlagsContext.class).to(FlagsContextImpl.class);
        bind(FlagsStrategy.class).to(FlagsIDStrategy.class);
        bind(FlagsStrategy.class).annotatedWith(DefaultStrategy.class).to(FlagsDefaultStrategy.class);

        bind(JournalizingContext.class).to(JournalizingContextImpl.class);
        bind(JournalizingStrategy.class).to(JournalizingIDStrategy.class);
        bind(JournalizingStrategy.class).annotatedWith(DefaultStrategy.class).to(JournalizingDefaultStrategy.class);

        bind(ColumnMappingContext.class).to(ColumnMappingContextImpl.class);
        bind(ColumnMappingStrategy.class).to(ColumnMappingIDStrategy.class);
        bind(ColumnMappingStrategy.class).annotatedWith(DefaultStrategy.class).to(ColumnMappingDefaultStrategy.class);

        bind(FileCollector.class).to(FileCollectorImpl.class);
        bind(Boolean.class).annotatedWith(IncludeVariables.class)
                .toProvider(Providers.of(config.isIncludeVariables()));

        bind(DictionaryModelLogicalSchema.class).to(DictionaryModelLogicalSchemaImpl.class);
        // AOP
        // add Cache functionality to classes annotated with @Cache
        // in one.jodi packages
        WriteThroughCacheInterceptor ci = new WriteThroughCacheInterceptor();
        // CacheMethodMatcher includes logic to prevent annotation of synthetic methods
        bindInterceptor(Matchers.inSubpackage(packageScopeForAOP()),
                new CacheMethodMatcher(), ci);
        bind(WriteThroughCacheInterceptor.class).toInstance(ci);
        requestInjection(ci);

        TransactionInterceptor ti = new TransactionInterceptor();
        // TransactionMethodMatcher includes logic to
        // prevent annotation of synthetic methods
        bindInterceptor(Matchers.any(), new TransactionMethodMatcher(), ti);
//      bind(TransactionInterceptor.class).toInstance(ti);
        requestInjection(ti);
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
