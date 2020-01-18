package one.jodi.etl.internalmodel;


public interface VariableStep extends ETLStep {

    VariableStepType getStepType();

    String getOperator();

    Integer getIncrementBy();

    VariableSetOperatorType getSetOperator();

    String getValue();

    enum VariableStepType {
        REFRESH,
        EVALUATE,
        SET,
        DECLARE
    }

    enum VariableSetOperatorType {
        ASSIGN,
        INCREMENT
    }
}
