package one.jodi.etl.service.scenarios;

import one.jodi.etl.internalmodel.ETLPackageHeader;
import one.jodi.etl.internalmodel.Transformation;

import java.util.List;

public interface ScenarioServiceProvider {

    void generateAllScenarios(List<ETLPackageHeader> packagesNames, List<Transformation> transformations);

    void deleteScenarios(List<ETLPackageHeader> scenarioNames);

    void deleteScenario(String scenarioName);

    void generateScenarioForMapping(String mappingName, String mappingFolder);

    void deleteScenarios();

}
