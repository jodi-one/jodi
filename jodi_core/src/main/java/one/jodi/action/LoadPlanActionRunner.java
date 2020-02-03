package one.jodi.action;

import com.google.inject.Inject;
import one.jodi.base.bootstrap.ActionRunner;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.bootstrap.UsageException;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.util.XMLParserUtil;
import one.jodi.core.config.JodiConstants;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.lpmodel.Loadplan;
import one.jodi.core.lpmodel.ObjectFactory;
import one.jodi.etl.loadplan.enrichment.EnrichmentBuilder;
import one.jodi.etl.service.loadplan.LoadPlanService;
import one.jodi.etl.service.loadplan.internalmodel.LoadPlanStep;
import one.jodi.etl.service.loadplan.internalmodel.LoadPlanTree;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * Actionrunner to create loadplans from textual specifications.
 */
public class LoadPlanActionRunner implements ActionRunner {

    private final LoadPlanService loadPlanService;
    private final ErrorWarningMessageJodi errorWarningMessages;
    private final Logger logger = LogManager.getLogger(LoadPlanActionRunner.class);
    private final EnrichmentBuilder enrichmentBuilder;
    private final JodiProperties jodiProperties;

    @Inject
    public LoadPlanActionRunner(final LoadPlanService loadPlanService,
                                final ErrorWarningMessageJodi errorWarningMessages,
                                final EnrichmentBuilder enrichmentBuilder,
                                final JodiProperties jodiProperties) {
        this.loadPlanService = loadPlanService;
        this.errorWarningMessages = errorWarningMessages;
        this.enrichmentBuilder = enrichmentBuilder;
        this.jodiProperties = jodiProperties;
    }

    @Override
    public void run(RunConfig config) {
        logger.info("Starting LoadPlanService validation");
        validateRunConfig(config);
        if (config.getMetadataDirectory() == null) {
            return;
        }
        File xmlLoc = new File(config.getMetadataDirectory() + File.separator + JodiConstants.XMLLOADPLANLOC);
        if (xmlLoc == null || !xmlLoc.exists()) {
            return;
        }
        File[] listFiles = xmlLoc.listFiles();
        if (listFiles == null)
            return;
        for (File lpFile : listFiles) {
            if (lpFile == null || !lpFile.exists() || lpFile.getName() == null || !lpFile.getName().endsWith(".xml")) {
                continue;
            }
            logger.info("Building loadplan from file: " + lpFile.getName());
            FileInputStream fis = null;
            Loadplan loadPlan = null;
            XMLParserUtil<Loadplan, ObjectFactory> loadPlanParser =
                    new XMLParserUtil<>(
                            ObjectFactory.class,
                            JodiConstants.getEmbeddedXSDFileNames(), this.errorWarningMessages);
            String xsdLocation = this.jodiProperties.getPropertyKeys()
                    .contains(JodiConstants.XSDLOCPROPERTY)
                    ? this.jodiProperties.getProperty(JodiConstants.XSDLOCPROPERTY)
                    : "";
            if (!new File(xsdLocation).exists())
                xsdLocation = null;
            try {
                fis = new FileInputStream(lpFile);
                loadPlan = loadPlanParser.loadObjectFromXMLAndValidate(fis,
                        JodiConstants.XSD_FILE_LOADPLAN, lpFile.getAbsolutePath());
            } catch (FileNotFoundException e) {
                logger.error(e);
            } finally {
                try {
                    if (fis != null)
                        fis.close();
                } catch (IOException e) {
                    logger.error(e);
                }
            }
            if (loadPlan == null)
                continue;
            List<LoadPlanTree<LoadPlanStep>> internalloadPlan = this.loadPlanService
                    .transform(loadPlan);
            enrichmentBuilder.enrich(internalloadPlan);
            if (this.loadPlanService != null && internalloadPlan != null && internalloadPlan.size() > 0) {
                this.loadPlanService.build(internalloadPlan, internalloadPlan.get(0).getLoadPlanDetails());
            }
        }
        logger.info("Finished building LoadPlans.");
    }

    @Override
    public void validateRunConfig(RunConfig config) throws UsageException {
        if (config.getPropertyFile() == null || !new File(config.getPropertyFile()).exists()) {
            String msg = "Properties file should be specified and exist.";
            throw new UsageException(msg);
        }
        if (config.getMetadataDirectory() == null || !new File(config.getMetadataDirectory()).exists()) {
            String msg = "Metadata directory should be specified and exist.";
            throw new UsageException(msg);
        }
    }
}
