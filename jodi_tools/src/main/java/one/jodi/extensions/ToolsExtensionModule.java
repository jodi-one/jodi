package one.jodi.extensions;


import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import one.jodi.base.bootstrap.ActionRunner;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.bootstrap.EtlRunConfig.ActionType;
import one.jodi.bootstrap.ToolsActionRunner;
import one.jodi.tools.*;
import one.jodi.tools.dependency.MappingProvider;
import one.jodi.tools.dependency.impl.MappingProviderImpl;
import one.jodi.tools.impl.*;
import one.jodi.tools.service.impl.RepositoryAnalyzerImpl;
import one.jodi.tools.service.impl.ReverseGeneratorImpl;

/**
 * The Class CustomExtensionModule provide a Guice Module implementation that
 * overrides core configurations to allow to run the Reverse Generator to produce XML.
 *
 */
public class ToolsExtensionModule extends AbstractModule {

    private final static String EXTENSION_MODULE = "tools";

    @SuppressWarnings("unused")
    private final RunConfig config;

    ToolsExtensionModule(final RunConfig config) {
        this.config = config;
    }

    /* (non-Javadoc)
     * @see com.google.inject.AbstractModule#configure()
     */
    @Override
    protected void configure() {
        bind(ActionRunner.class).annotatedWith(
                Names.named(ActionType.EXTENSION_POINT.getCode() +
                        EXTENSION_MODULE))
                .to(ToolsActionRunner.class);
        bind(ReverseGenerator.class).to(ReverseGeneratorImpl.class);
        bind(RepositoryAnalyzer.class).to(RepositoryAnalyzerImpl.class);
        bind(Renderer.class).to(RendererImpl.class);
        bind(RenderingWriter.class).to(RenderingWriterImpl.class);
        bind(TransformationCache.class).to(TransformationCacheImpl.class).asEagerSingleton();
        bind(MappingProvider.class).to(MappingProviderImpl.class);
        bind(ModelBuildingStep.class).annotatedWith(Names.named("TransformationNameBuildingStep")).to(TransformationNameBuildingStep.class);
        bind(ModelBuildingStep.class).annotatedWith(Names.named("KMBuildingStep")).to(KMBuildingStep.class);
        bind(ModelBuildingStep.class).annotatedWith(Names.named("ExecutionLocationBuildingStep")).to(ExecutionLocationBuildingStep.class);
        bind(ModelBuildingStep.class).annotatedWith(Names.named("FlagsBuildingStep")).to(FlagsBuildingStep.class);
        bind(ModelBuildingStep.class).annotatedWith(Names.named("TargetColumnBuildingStep")).to(TargetColumnBuildingStep.class);
        bind(ModelBuildingStep.class).annotatedWith(Names.named("SourceBuildingStep")).to(SourceBuildingStep.class);
    }
}