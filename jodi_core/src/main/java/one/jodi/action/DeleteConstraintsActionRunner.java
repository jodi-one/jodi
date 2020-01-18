package one.jodi.action;

import com.google.inject.Inject;
import one.jodi.base.bootstrap.ActionRunner;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.bootstrap.UsageException;
import one.jodi.base.util.StringUtils;
import one.jodi.core.service.ConstraintService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DeleteConstraintsActionRunner implements ActionRunner {

    private final ConstraintService service;
    private final Logger logger =
            LogManager.getLogger(DeleteConstraintsActionRunner.class);

    @Inject
    protected DeleteConstraintsActionRunner(final ConstraintService service) {
        this.service = service;
    }

    @Override
    public void run(final RunConfig config) {
        logger.info("deleting constraints");
        service.delete(config.getMetadataDirectory());
    }

    @Override
    public void validateRunConfig(RunConfig config)
            throws UsageException {
        if (!StringUtils.hasLength(config.getPropertyFile())) {
            String msg = "Properties file not found.";
            throw new UsageException(msg);
        }
    }
}