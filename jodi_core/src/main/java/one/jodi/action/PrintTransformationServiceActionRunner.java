package one.jodi.action;

import com.google.inject.Inject;
import one.jodi.base.bootstrap.ActionRunner;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.bootstrap.UsageException;
import one.jodi.bootstrap.EtlRunConfig;
import one.jodi.etl.service.interfaces.TransformationPrintServiceProvider;

import java.io.File;

public class PrintTransformationServiceActionRunner implements ActionRunner {

    private final TransformationPrintServiceProvider printServiceProvider;

    @Inject
    public PrintTransformationServiceActionRunner(final TransformationPrintServiceProvider printServiceProvider) {
        this.printServiceProvider = printServiceProvider;
    }

    @Override
    public void run(RunConfig config) {
        validateRunConfig(config);
        this.printServiceProvider.print(((EtlRunConfig) config).getFolder(), ((EtlRunConfig) config).getPrefix());
    }

    @Override
    public void validateRunConfig(RunConfig config) throws UsageException {
        if (config.getPropertyFile() == null ||
                !new File(config.getPropertyFile()).exists()) {
            String msg = "Properties file should be specified and exist.";
            throw new UsageException(msg);
        }
        if (config.getMetadataDirectory() == null ||
                !new File(config.getMetadataDirectory()).exists()) {
            String msg = "Metadata directory should be specified and exist.";
            throw new UsageException(msg);
        }
    }
}
