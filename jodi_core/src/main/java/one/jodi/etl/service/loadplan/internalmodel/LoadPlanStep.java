package one.jodi.etl.service.loadplan.internalmodel;

import one.jodi.core.lpmodel.Exceptionbehavior;

import java.util.List;

/**
 * Internal model representation of a LoadPlanStep.
 * A loadplanstep is a leaf of a LoadPlanTree which represents the tree of a loadplan,
 * and their indivudual loadplansteps.
 */
public class LoadPlanStep {

    private String name;
    private LoadPlanStepType type;
    private Boolean enabled;
    private String keywords;
    // exception handling
    private RestartType restartType;
    private Integer timeout;
    private String exceptionStep;
    private Exceptionbehavior exceptionBehavior;
    private LoadPlanTree<LoadPlanStep> parent;
    private List<Variable> variables;
    private Integer priority;
    private Integer maxErrorChildCount;
    private String scenario;
    private Integer scenarioVersion;
    private String testVariable;
    private String operator;
    private Object value;

    public LoadPlanStep(final LoadPlanTree<LoadPlanStep> root, final String name, final LoadPlanStepType type,
                        final Boolean enabled, final String keywords,
                        // exception handling
                        final RestartType restartType, final Integer timeout, final String exceptionStep,
                        final Exceptionbehavior exceptionBehavior, final Integer priority, final List<Variable> variables,
                        final Integer maxErrorChildCount, final String scenario, final Integer scenarioVersion, final String testVariable,
                        final String operator, final Object value) {
        this.parent = root;
        this.name = name;
        this.type = type;
        this.enabled = enabled;
        this.keywords = keywords;
        // exception handling
        this.restartType = restartType;
        this.timeout = timeout;
        this.exceptionStep = exceptionStep;
        this.exceptionBehavior = exceptionBehavior;
        this.variables = variables;
        this.priority = priority;
        this.maxErrorChildCount = maxErrorChildCount;
        this.scenario = scenario;
        this.scenarioVersion = scenarioVersion;
        this.testVariable = testVariable;
        this.operator = operator;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LoadPlanStepType getType() {
        return type;
    }

    public void setType(LoadPlanStepType type) {
        this.type = type;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public RestartType getRestartType() {
        return restartType;
    }

    public void setRestartType(RestartType restartType) {
        this.restartType = restartType;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public String getExceptionStep() {
        return exceptionStep;
    }

    public void setExceptionStep(String exceptionStep) {
        this.exceptionStep = exceptionStep;
    }

    public Exceptionbehavior getExceptionBehavior() {
        return exceptionBehavior;
    }

    public void setExceptionBehavior(Exceptionbehavior exceptionBehavior) {
        this.exceptionBehavior = exceptionBehavior;
    }

    public LoadPlanTree<LoadPlanStep> getParent() {
        return parent;
    }

    public void setParent(LoadPlanTree<LoadPlanStep> parent) {
        this.parent = parent;
    }

    public List<Variable> getVariables() {
        return variables;
    }

    public void setVariables(List<Variable> variables) {
        this.variables = variables;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Integer getMaxErrorChildCount() {
        return maxErrorChildCount;
    }

    public void setMaxErrorChildCount(Integer maxErrorChildCount) {
        this.maxErrorChildCount = maxErrorChildCount;
    }

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public Integer getScenarioVersion() {
        return scenarioVersion;
    }

    public void setScenarioVersion(Integer scenarioVersion) {
        this.scenarioVersion = scenarioVersion;
    }

    public String getTestVariable() {
        return testVariable;
    }

    public void setTestVariable(String testVariable) {
        this.testVariable = testVariable;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

}
