package one.jodi.core.service;

import one.jodi.etl.internalmodel.ETLPackageHeader;
import one.jodi.etl.internalmodel.Transformation;

import java.util.List;


/**
 * Service that provides Scenario management functionality.
 */
public interface ScenarioService {

    /**
     * Delete the scenario that matches the provided name
     *
     * @param scenarioName
     */
    void deleteScenario(String scenarioName);

    /**
     * Delete all scenarios that are indicated by the list of scenario names.
     *
     * @param scenarioName List of names of scenarios to be deleted
     */
    void deleteScenario(List<ETLPackageHeader> scenarioName);

    /**
     * Generates all scenarios
     *
     * @param list         of package names to generate scenarios for.
     * @param packageNames
     * @param list
     * @param etlObject
     */
    void generateAllScenarios(List<ETLPackageHeader> packageHeaders,
                              List<Transformation> transformations);

    void generateScenarioForMapping(String mappingName, String mappingFolder);

    /**
     * Delete all scenarios that matches the provided name
     */
    void deleteScenarios();
}
