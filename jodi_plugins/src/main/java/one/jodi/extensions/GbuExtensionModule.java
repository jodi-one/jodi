package one.jodi.extensions;

import com.google.inject.AbstractModule;
import one.jodi.core.extensions.strategies.ColumnMappingStrategy;
import one.jodi.core.extensions.strategies.ExecutionLocationStrategy;
import one.jodi.core.extensions.strategies.FlagsStrategy;
import one.jodi.core.extensions.strategies.FolderNameStrategy;
import one.jodi.core.extensions.strategies.GenericColumnMappingStrategy;
import one.jodi.core.extensions.strategies.GenericExecutionLocationStrategy;
import one.jodi.core.extensions.strategies.GenericFlagsStrategy;
import one.jodi.core.extensions.strategies.GenericFolderNameStrategy;

/**
 * The Class GenericExtensionModule provide a Guice Module implementation that
 * overrides core configurations with GBU specific customizations.
 */
public class GbuExtensionModule extends AbstractModule {

    /* (non-Javadoc)
     * @see com.google.inject.AbstractModule#configure()
     */
    @Override
    protected void configure() {
        bind(ExecutionLocationStrategy.class).to(GenericExecutionLocationStrategy.class);
        bind(FlagsStrategy.class).to(GenericFlagsStrategy.class);
        bind(FolderNameStrategy.class).to(GenericFolderNameStrategy.class);
        bind(ColumnMappingStrategy.class).to(GenericColumnMappingStrategy.class);
    }
}
