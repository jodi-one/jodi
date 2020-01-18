package one.jodi.bootstrap;

import com.google.inject.Inject;
import one.jodi.base.bootstrap.ActionRunner;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.bootstrap.UsageException;
import one.jodi.base.exception.UnRecoverableException;
import one.jodi.base.util.StringUtils;
import one.jodi.tools.ReverseGenerator;

/**
 * An {@link ActionRunner} implementation that invokes the {@link
 * one.jodi.base.util.Resource} method.
 *
 */
public class ToolsActionRunner implements ActionRunner {

    private final ReverseGenerator generator;

    @Inject
    public ToolsActionRunner(final ReverseGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void run(RunConfig config) {
        try {
            generator.generate();

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new UnRecoverableException("Exception thrown during generation of XML.", ex);
        }
    }

    @Override
    public void validateRunConfig(RunConfig config) throws UsageException {
        if (!StringUtils.hasLength(config.getPropertyFile())) {
            throw new UsageException("The configuration property property file is required to run Cache.");
        }
    }
}
