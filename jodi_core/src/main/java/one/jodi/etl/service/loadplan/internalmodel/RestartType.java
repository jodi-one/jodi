package one.jodi.etl.service.loadplan.internalmodel;

/**
 * Restarts type of the generic loadplanstep,
 * it contains restart types of the loadplanstepserial type,
 * loadplanstepparalleltype and loadplanstepscenario type,
 * and the null value.
 */
public enum RestartType {

    SERIAL_STEP_ALL_CHILDREN("SERIAL_STEP_ALL_CHILDREN"),
    SERIAL_STEP_FROM_FAILURE("SERIAL_STEP_FROM_FAILURE"),
    PARALLEL_STEP_ALL_CHILDREN("PARALLEL_STEP_ALL_CHILDREN"),
    PARALLEL_STEP_FAILED_CHILDREN("PARALLEL_STEP_FAILED_CHILDREN"),
    RUN_SCENARIO_NEW_SESSION("RUN_SCENARIO_NEW_SESSION"),
    RUN_SCENARIO_FROM_STEP("RUN_SCENARIO_FROM_STEP"),
    RUN_SCENARIO_FROM_TASK("RUN_SCENARIO_FROM_TASK"),
    NONE("NONE");

    private String theValue;

    RestartType(String value) {
        this.theValue = value;
    }

    public String value() {
        return this.theValue;
    }
}
