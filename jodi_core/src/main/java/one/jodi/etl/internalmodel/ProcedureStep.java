package one.jodi.etl.internalmodel;

import java.util.Collection;

public interface ProcedureStep extends ETLStep {
    Collection<StepParameter> getParameters();
}
