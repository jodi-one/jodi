package one.jodi.action;

import com.google.inject.Inject;
import one.jodi.base.bootstrap.ActionRunner;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.bootstrap.UsageException;
import one.jodi.base.util.StringUtils;
import one.jodi.core.service.VariableService;

public class ExportVariableActionrunner implements ActionRunner {

    private final VariableService service;

    @Inject
    protected ExportVariableActionrunner(final VariableService service) {
        this.service = service;
    }

    @Override
    public void run(final RunConfig config) {
        service.export(config.getMetadataDirectory());
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