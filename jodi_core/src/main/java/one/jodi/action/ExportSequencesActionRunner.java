package one.jodi.action;

import com.google.inject.Inject;
import one.jodi.base.bootstrap.ActionRunner;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.bootstrap.UsageException;
import one.jodi.base.util.StringUtils;
import one.jodi.core.service.SequenceService;

public class ExportSequencesActionRunner implements ActionRunner {

    private final SequenceService service;

    @Inject
    protected ExportSequencesActionRunner(final SequenceService service) {
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
