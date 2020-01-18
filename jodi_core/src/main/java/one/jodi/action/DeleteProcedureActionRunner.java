package one.jodi.action;

import com.google.inject.Inject;
import one.jodi.base.bootstrap.ActionRunner;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.bootstrap.UsageException;
import one.jodi.base.util.StringUtils;
import one.jodi.core.service.ProcedureService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DeleteProcedureActionRunner implements ActionRunner {

    private final static Logger logger =
            LogManager.getLogger(DeleteProcedureActionRunner.class);

    private final ProcedureService service;

    @Inject
    protected DeleteProcedureActionRunner(final ProcedureService service) {
        this.service = service;
    }

    @Override
    public void run(final RunConfig config) {
        service.delete(config.getMetadataDirectory());
    }

    @Override
    public void validateRunConfig(RunConfig config) throws UsageException {
        if (!StringUtils.hasLength(config.getPropertyFile())) {
            String msg = "Properties file not found.";
            logger.fatal(msg);
            throw new UsageException(msg);
        }
    }

}
