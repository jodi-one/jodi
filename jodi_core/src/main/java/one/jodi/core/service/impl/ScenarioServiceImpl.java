package one.jodi.core.service.impl;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.service.ScenarioService;
import one.jodi.etl.builder.EnrichingBuilder;
import one.jodi.etl.internalmodel.ETLPackageHeader;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.etl.service.project.ProjectServiceProvider;
import one.jodi.etl.service.scenarios.ScenarioServiceProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;


/**
 * Implementation of the {@link ScenarioService} interface.
 * <p>
 * Please change your Generating Option ie INCREMENTAL (or) REGENERATE (or)
 * REPLACE.
 */
public class ScenarioServiceImpl implements ScenarioService {
    private static final Logger logger =
            LogManager.getLogger(ScenarioServiceImpl.class);

    private static final String ERROR_MESSAGE_03640 =
            "Error while generating all scenarios.";

    private final ScenarioServiceProvider scenarioService;
    private final ErrorWarningMessageJodi errorWarningMessages;
    private final EnrichingBuilder enrichingBuilder;

    /**
     * Creates a new ScenarioServiceImpl instance.
     *
     * @param scenarioService
     * @param properties
     * @param errorWarningMessages
     */
    @Inject
    protected ScenarioServiceImpl(final ScenarioServiceProvider scenarioService,
                                  final JodiProperties properties,
                                  final ErrorWarningMessageJodi errorWarningMessages,
                                  final EnrichingBuilder enrichingBuilder) {
        this.scenarioService = scenarioService;
        this.errorWarningMessages = errorWarningMessages;
        this.enrichingBuilder = enrichingBuilder;
    }

    @Override
    public void deleteScenario(final String scenarioName) {
        scenarioService.deleteScenario(scenarioName);
        logger.debug("Exit deleting scenario: " + scenarioName);
    }

    @Override
    public void deleteScenario(final List<ETLPackageHeader> headers) {
        scenarioService.deleteScenarios(headers);
    }

    @Override
    public void generateAllScenarios(final List<ETLPackageHeader> packageHeaders, List<Transformation> transformations) {
        try {
            packageHeaders.forEach(h -> logger.debug("Generate Scenarios for " +
                    h.getPackageName()));

            List<Transformation> newTransformations = new ArrayList<>();
            if (transformations != null) {
                transformations.forEach(t -> newTransformations.add(this.enrichingBuilder.enrich(t, false)));
            }
            scenarioService.generateAllScenarios(packageHeaders, newTransformations);
        } catch (RuntimeException e) {
            String msg = errorWarningMessages.formatMessage(3640, ERROR_MESSAGE_03640,
                    this.getClass());
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            logger.error(msg, e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            String msg = errorWarningMessages.formatMessage(3640, ERROR_MESSAGE_03640,
                    this.getClass());
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Commiting Changes \n Process Completed");
        }
    }

    @Override
    public void generateScenarioForMapping(final String mappingName,
                                           final String mappingFolder) {
        scenarioService.generateScenarioForMapping(mappingName, mappingFolder);
    }


    @Override
    public void deleteScenarios() {
        scenarioService.deleteScenarios();
        logger.debug("Exit deleting scenarios");
    }

}
