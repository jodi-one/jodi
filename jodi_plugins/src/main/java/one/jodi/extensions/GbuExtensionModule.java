package one.jodi.extensions;

import com.google.inject.AbstractModule;
import one.jodi.core.extensions.strategies.*;

/**
 * The Class GenericExtensionModule provide a Guice Module implementation that
 * overrides core configurations with GBU specific customizations.
 *
 */
public class GbuExtensionModule extends AbstractModule {

    /* (non-Javadoc)
     * @see com.google.inject.AbstractModule#configure()
     */
    @Override
    protected void configure() {
        bind(ExecutionLocationStrategy.class)
                .to(GenericExecutionLocationStrategy.class);
        bind(FlagsStrategy.class).to(GenericFlagsStrategy.class);
        bind(FolderNameStrategy.class).to(GenericFolderNameStrategy.class);
    }

}
