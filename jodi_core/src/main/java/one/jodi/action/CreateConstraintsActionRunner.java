package one.jodi.action;

import com.google.inject.Inject;
import one.jodi.base.bootstrap.ActionRunner;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.bootstrap.UsageException;
import one.jodi.base.util.StringUtils;
import one.jodi.core.service.ConstraintService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CreateConstraintsActionRunner implements ActionRunner {

    private final ConstraintService service;
    private final Logger logger =
            LogManager.getLogger(CreateConstraintsActionRunner.class);


    @Inject
    protected CreateConstraintsActionRunner(final ConstraintService service) {
        this.service = service;
    }


    @Override
    public void run(final RunConfig config) {
        logger.info("Creating constraints");
        service.create(config.getMetadataDirectory());
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