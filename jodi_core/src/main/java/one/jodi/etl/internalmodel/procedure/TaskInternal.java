package one.jodi.etl.internalmodel.procedure;

import java.util.Optional;

public interface TaskInternal {

    ProcedureHeader getParent();

    String getName();

    Optional<CommandInternal> getSourceCommand();

    Optional<CommandInternal> getTargetCommand();

    boolean isCleanup();

    boolean isIgnoreErrors();

    //
    // will be enriched with default values
    //

    // default value:  null = 'None'
    String getLogCounter();

    // default value: 5
    int getLogLevel();

    // default value:  false
    boolean isLogFinalCommand();

}
