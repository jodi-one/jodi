package one.jodi.etl.internalmodel.procedure;

import java.util.List;
import java.util.Optional;

public interface ProcedureInternal extends ProcedureHeader {

    Optional<String> getDescription();

    List<TaskInternal> getTasks();

    boolean isMultiConnectionSupported();

    boolean isRemoveTemporaryObjectsonError();

    boolean isUseUniqueTemporaryObjectNames();

    //
    // will be enriched with default values
    //

    // default value: "Oracle"
    String getSourceTechnology();

    // default value: "Oracle"
    String getTargetTechnology();

    List<OptionInternal> getOptions();
}
