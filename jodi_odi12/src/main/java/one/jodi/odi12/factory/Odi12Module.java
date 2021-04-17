package one.jodi.odi12.factory;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.matcher.Matchers;
import one.jodi.base.annotations.Cached;
import one.jodi.base.aop.WriteThroughCacheInterceptor;
import one.jodi.core.annotations.TransactionAttribute;
import one.jodi.core.aop.TransactionInterceptor;
import one.jodi.core.service.ConstraintService;
import one.jodi.core.service.ProjectService;
import one.jodi.core.service.SequenceService;
import one.jodi.core.service.ValidationService;
import one.jodi.core.service.VariableService;
import one.jodi.core.service.impl.ConstraintServiceImpl;
import one.jodi.core.service.impl.ProjectServiceImpl;
import one.jodi.core.service.impl.SequenceServiceImpl;
import one.jodi.core.service.impl.ValidationServiceImpl;
import one.jodi.core.service.impl.VariableServiceImpl;
import one.jodi.core.validation.sequences.SequenceValidator;
import one.jodi.core.validation.sequences.SequenceValidatorImpl;
import one.jodi.core.validation.variables.VariableValidator;
import one.jodi.core.validation.variables.VariableValidatorImp;
import one.jodi.etl.builder.SequenceEnrichmentBuilder;
import one.jodi.etl.builder.SequenceTransformationBuilder;
import one.jodi.etl.builder.VariableEnrichmentBuilder;
import one.jodi.etl.builder.VariableTransformationBuilder;
import one.jodi.etl.builder.impl.SequenceEnrichmentBuilderImpl;
import one.jodi.etl.builder.impl.SequenceTransformationBuilderImpl;
import one.jodi.etl.builder.impl.VariableEnrichmentBuilderImpl;
import one.jodi.etl.builder.impl.VariableTransformationBuilderImpl;
import one.jodi.etl.common.EtlSubSystemVersion;
import one.jodi.etl.service.EtlDataStoreBuildService;
import one.jodi.etl.service.constraints.ConstraintEnrichmentService;
import one.jodi.etl.service.constraints.ConstraintEnrichmentServiceImpl;
import one.jodi.etl.service.constraints.ConstraintServiceProvider;
import one.jodi.etl.service.constraints.ConstraintTransformationService;
import one.jodi.etl.service.constraints.ConstraintTransformationServiceImpl;
import one.jodi.etl.service.constraints.ConstraintValidationService;
import one.jodi.etl.service.interfaces.TransformationPrintServiceProvider;
import one.jodi.etl.service.interfaces.TransformationServiceProvider;
import one.jodi.etl.service.loadplan.LoadPlanExportService;
import one.jodi.etl.service.loadplan.LoadPlanService;
import one.jodi.etl.service.packages.PackageServiceProvider;
import one.jodi.etl.service.procedure.ProcedureServiceProvider;
import one.jodi.etl.service.project.ProjectServiceProvider;
import one.jodi.etl.service.repository.OdiRepositoryExportService;
import one.jodi.etl.service.repository.OdiRepositoryImportService;
import one.jodi.etl.service.sequences.SequenceServiceProvider;
import one.jodi.etl.service.variables.VariableServiceProvider;
import one.jodi.odi.common.OdiVersion;
import one.jodi.odi.constraints.ConstraintValidationServiceImpl;
import one.jodi.odi.constraints.OdiConstraintAccessStrategy;
import one.jodi.odi.interfaces.OdiTransformationAccessStrategy;
import one.jodi.odi.loadplan.OdiLoadPlanAccessStrategy;
import one.jodi.odi.loadplan.OdiLoadPlanTransformationService;
import one.jodi.odi.loadplan.OdiLoadPlanValidationService;
import one.jodi.odi.loadplan.OdiLoadPlanValidator;
import one.jodi.odi.loadplan.service.OdiLoadPlanValidationServiceImpl;
import one.jodi.odi.packages.OdiPackageAccessStrategy;
import one.jodi.odi.sequences.OdiSequenceAccessStrategy;
import one.jodi.odi.variables.OdiVariableAccessStrategy;
import one.jodi.odi12.constraints.Odi12ConstraintServiceProvider;
import one.jodi.odi12.constraints.Odi12ConstraintsAccessStrategy;
import one.jodi.odi12.etl.AggregateBuilder;
import one.jodi.odi12.etl.DatastoreBuilder;
import one.jodi.odi12.etl.DistinctBuilder;
import one.jodi.odi12.etl.ExpressionsBuilder;
import one.jodi.odi12.etl.FilterBuilder;
import one.jodi.odi12.etl.FlagsBuilder;
import one.jodi.odi12.etl.FlowsBuilder;
import one.jodi.odi12.etl.JoinBuilder;
import one.jodi.odi12.etl.KMBuilder;
import one.jodi.odi12.etl.LookupBuilder;
import one.jodi.odi12.etl.SetBuilder;
import one.jodi.odi12.etl.impl.AggregateBuilderImpl;
import one.jodi.odi12.etl.impl.DatastoreBuilderImpl;
import one.jodi.odi12.etl.impl.DistinctBuilderImpl;
import one.jodi.odi12.etl.impl.ExpressionsBuilderImpl;
import one.jodi.odi12.etl.impl.FilterBuilderImpl;
import one.jodi.odi12.etl.impl.FlagsBuilderImpl;
import one.jodi.odi12.etl.impl.FlowsBuilderImpl;
import one.jodi.odi12.etl.impl.JoinBuilderImpl;
import one.jodi.odi12.etl.impl.KMBuilderImpl;
import one.jodi.odi12.etl.impl.LookupBuilderImpl;
import one.jodi.odi12.etl.impl.SetBuilderImpl;
import one.jodi.odi12.flow.FlowStrategy;
import one.jodi.odi12.flow.FlowStrategyImpl;
import one.jodi.odi12.loadplan.Odi12LoadPlanAccessStrategy;
import one.jodi.odi12.loadplan.Odi12LoadPlanValidator;
import one.jodi.odi12.mappings.Odi12PrintServiceProvider;
import one.jodi.odi12.mappings.Odi12TransformationAccessStrategy;
import one.jodi.odi12.mappings.Odi12TransformationServiceProvider;
import one.jodi.odi12.packages.Odi12PackageAccessStrategyImpl;
import one.jodi.odi12.packages.Odi12PackageServiceProviderImpl;
import one.jodi.odi12.procedure.Odi12ProcedureServiceProvider;
import one.jodi.odi12.project.Odi12ProjectServiceProvider;
import one.jodi.odi12.sequences.Odi12SequenceAccessStrategy;
import one.jodi.odi12.sequences.Odi12SequenceServiceProvider;
import one.jodi.odi12.service.Odi12LoadPlanExportService;
import one.jodi.odi12.service.Odi12LoadPlanService;
import one.jodi.odi12.service.Odi12LoadPlanTransformationService;
import one.jodi.odi12.service.Odi12RepositoryExportService;
import one.jodi.odi12.service.Odi12RepositoryImportService;
import one.jodi.odi12.service.OdiDatastoreBuildService;
import one.jodi.odi12.variables.Odi12VariableAccessStrategy;
import one.jodi.odi12.variables.Odi12VariableServiceProvider;
import oracle.odi.domain.adapter.topology.ILogicalSchema;
import oracle.odi.domain.mapping.IMapComponent;
import oracle.odi.domain.mapping.MapRootContainer;
import oracle.odi.domain.mapping.Mapping;
import oracle.odi.domain.mapping.component.Dataset;
import oracle.odi.domain.mapping.component.DatastoreComponent;
import oracle.odi.domain.mapping.component.ReusableMappingComponent;
import oracle.odi.domain.project.StepMapping;
import oracle.odi.domain.runtime.loadplan.OdiLoadPlan;
import oracle.odi.domain.topology.OdiContext;

import java.lang.reflect.Method;

public class Odi12Module extends AbstractModule {

    public Odi12Module() {
    }

    @Override
    protected void configure() {
        // ODI 12C
        bind(FlowStrategy.class).to(FlowStrategyImpl.class);
        bind(TransformationServiceProvider.class).to(Odi12TransformationServiceProvider.class);
        bind(PackageServiceProvider.class).to(Odi12PackageServiceProviderImpl.class);
        bind(new TypeLiteral<OdiPackageAccessStrategy<Mapping, StepMapping>>() {
        }).to(Odi12PackageAccessStrategyImpl.class);
        bind(new TypeLiteral<OdiTransformationAccessStrategy<MapRootContainer,
                Dataset, DatastoreComponent, ReusableMappingComponent,
                IMapComponent, OdiContext, ILogicalSchema>>() {
        }).to(Odi12TransformationAccessStrategy.class);
        bind(OdiRepositoryExportService.class).to(Odi12RepositoryExportService.class);
        bind(OdiRepositoryImportService.class).to(Odi12RepositoryImportService.class);
        bind(new TypeLiteral<OdiLoadPlanAccessStrategy<OdiLoadPlan, Mapping>>() {
        }).to(Odi12LoadPlanAccessStrategy.class);

        bind(LoadPlanExportService.class).to(Odi12LoadPlanExportService.class);
        bind(LoadPlanService.class).to(Odi12LoadPlanService.class);
        bind(OdiLoadPlanValidator.class).to(Odi12LoadPlanValidator.class);
        bind(OdiLoadPlanTransformationService.class)
                .to(Odi12LoadPlanTransformationService.class);
        bind(OdiLoadPlanValidationService.class)
                .to(OdiLoadPlanValidationServiceImpl.class);
        bind(AggregateBuilder.class).to(AggregateBuilderImpl.class);
        bind(DatastoreBuilder.class).to(DatastoreBuilderImpl.class);
        bind(DistinctBuilder.class).to(DistinctBuilderImpl.class);
        bind(ExpressionsBuilder.class).to(ExpressionsBuilderImpl.class);
        bind(FilterBuilder.class).to(FilterBuilderImpl.class);
        bind(FlagsBuilder.class).to(FlagsBuilderImpl.class);
        bind(FlowsBuilder.class).to(FlowsBuilderImpl.class);
        bind(JoinBuilder.class).to(JoinBuilderImpl.class);
        bind(KMBuilder.class).to(KMBuilderImpl.class);
        bind(LookupBuilder.class).to(LookupBuilderImpl.class);
        bind(SetBuilder.class).to(SetBuilderImpl.class);

        bind(ProcedureServiceProvider.class).to(Odi12ProcedureServiceProvider.class);

        bind(SequenceServiceProvider.class).to(Odi12SequenceServiceProvider.class);
        bind(SequenceService.class).to(SequenceServiceImpl.class);
        bind(OdiSequenceAccessStrategy.class).to(Odi12SequenceAccessStrategy.class);
        bind(SequenceEnrichmentBuilder.class).to(SequenceEnrichmentBuilderImpl.class);
        bind(SequenceTransformationBuilder.class)
                .to(SequenceTransformationBuilderImpl.class);
        bind(SequenceValidator.class).to(SequenceValidatorImpl.class);

        bind(VariableServiceProvider.class).to(Odi12VariableServiceProvider.class);
        bind(VariableService.class).to(VariableServiceImpl.class);
        bind(OdiVariableAccessStrategy.class).to(Odi12VariableAccessStrategy.class);
        bind(VariableEnrichmentBuilder.class).to(VariableEnrichmentBuilderImpl.class);
        bind(VariableTransformationBuilder.class)
                .to(VariableTransformationBuilderImpl.class);
        bind(VariableValidator.class).to(VariableValidatorImp.class);

        bind(ConstraintService.class).to(ConstraintServiceImpl.class);
        bind(ConstraintServiceProvider.class).to(Odi12ConstraintServiceProvider.class);
        bind(ConstraintTransformationService.class)
                .to(ConstraintTransformationServiceImpl.class);
        bind(ConstraintEnrichmentService.class).to(ConstraintEnrichmentServiceImpl.class);
        bind(OdiConstraintAccessStrategy.class).to(Odi12ConstraintsAccessStrategy.class);
        bind(ConstraintValidationService.class).to(ConstraintValidationServiceImpl.class);
        bind(OdiTransformationAccessStrategy.class)
                .to(Odi12TransformationAccessStrategy.class);
        bind(ValidationService.class).to(ValidationServiceImpl.class);
        bind(TransformationPrintServiceProvider.class).to(Odi12PrintServiceProvider.class);
        bind(OdiLoadPlanAccessStrategy.class).to(Odi12LoadPlanAccessStrategy.class);
        bind(EtlSubSystemVersion.class).to(OdiVersion.class);
        bind(EtlDataStoreBuildService.class).to(OdiDatastoreBuildService.class);

        bind(ProjectService.class).to(ProjectServiceImpl.class);
        bind(ProjectServiceProvider.class).to(Odi12ProjectServiceProvider.class);


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
