package one.jodi.etl.service.loadplan.internalmodel;

/**
 * Internal model representation of a LoadPlanStepType.
 *
 */
public enum LoadPlanStepType {
    SERIAL("Serial"),
    PARALLEL("Parallel"),
    SCENARIO("Scenario"),
    CASE("Case"),
    CASEWHEN("When"),
    CASEELSE("Else"),
    EXCEPTION("exception");
    private final String theValue;

    LoadPlanStepType(final String v) {
        this.theValue = v;
    }

    public String value() {
        return this.theValue;
    }

}
