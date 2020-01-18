package one.jodi.etl.internalmodel;

import java.util.List;

public interface CommandStep extends ETLStep {
    List<StepParameter> getParameters();
}
