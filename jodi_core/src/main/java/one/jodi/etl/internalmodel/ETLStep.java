package one.jodi.etl.internalmodel;

public interface ETLStep {
    String getName();

    String getLabel();

    ETLStep getNextStepOnSuccess();

    ETLStep getNextStepOnFailure();

    boolean executeAsynchronously();

    // use scenario instead of mapping
    boolean useScenario();
}
