package one.jodi.action;

import com.google.inject.Inject;
import one.jodi.base.bootstrap.ActionRunner;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.bootstrap.UsageException;
import one.jodi.base.util.StringUtils;
import one.jodi.core.service.VariableService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DeleteVariableActionRunner implements ActionRunner {
    private static final Logger logger =
            LogManager.getLogger(DeleteVariableActionRunner.class);

    private final VariableService service;

    @Inject
    protected DeleteVariableActionRunner(final VariableService service) {
        this.service = service;
    }

    @Override
    public void run(final RunConfig config) {
        service.delete(config.getMetadataDirectory());
    }

    @Override
    public void validateRunConfig(RunConfig config)
            throws UsageException {
        if (!StringUtils.hasLength(config.getPropertyFile())) {
            String msg = "Properties file not found.";
            logger.fatal(msg);
            throw new UsageException(msg);
        }
    }

}