package one.jodi.action;

import com.google.inject.Inject;
import one.jodi.base.bootstrap.ActionRunner;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.bootstrap.UsageException;
import one.jodi.base.util.StringUtils;
import one.jodi.bootstrap.EtlRunConfig;
import one.jodi.core.service.ConstraintService;

public class ExportConstraintsActionRunner implements ActionRunner {

    private final ConstraintService service;

    @Inject
    protected ExportConstraintsActionRunner(final ConstraintService service) {
        this.service = service;
    }

    @Override
    public void run(final RunConfig rc) {
        final EtlRunConfig config = (EtlRunConfig) rc;

        service.export(config.getMetadataDirectory(), config.isExportingDBConstraints());
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
