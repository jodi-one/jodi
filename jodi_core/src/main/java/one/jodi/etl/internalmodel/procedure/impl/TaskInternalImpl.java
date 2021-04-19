package one.jodi.etl.internalmodel.procedure.impl;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.etl.internalmodel.procedure.CommandInternal;
import one.jodi.etl.internalmodel.procedure.ProcedureHeader;
import one.jodi.etl.internalmodel.procedure.TaskInternal;
import one.jodi.etl.internalmodel.procedure.Validate;
import one.jodi.etl.internalmodel.procedure.impl.CommandInternalImpl.Location;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

public class TaskInternalImpl implements TaskInternal, Validate {
    private static final Logger logger = LogManager.getLogger(TaskInternalImpl.class);

    private static final String EOL = System.getProperty("line.separator");

    private static final String ERROR_MESSAGE_62300 =
            "Procedure must define a non-empty task name in its definition file %1$s. " +
                    "This procedure specification will be ignored.";

    private static final String ERROR_MESSAGE_62310 =
            "Procedure must define either one source command or target command in its " +
                    "definition file %1$s. This procedure specification will be ignored.";

    private final ProcedureHeader parent;
    private final String name;
    private final boolean cleanup;
    private final boolean ignoreErrors;

    private Optional<CommandInternal> sourceCommand = Optional.empty();
    private Optional<CommandInternal> targetCommand = Optional.empty();

    public TaskInternalImpl(final ProcedureHeader parent, final String name, final boolean cleanup,
                            final boolean ignoreErrors) {
        super();
        this.parent = parent;
        this.name = name;
        this.cleanup = cleanup;
        this.ignoreErrors = ignoreErrors;

        // register with parent
        ((ProcedureInternalImpl) this.parent).addTask(this);
    }

    @Override
    public ProcedureHeader getParent() {
        return this.parent;
    }

    @Override
    public String getName() {
        return name;
    }

    void addCommand(final CommandInternal command, final Location location) {
        assert (command != null);
        if (location == Location.TARGET) {
            this.targetCommand = Optional.of(command);
        } else {
            this.sourceCommand = Optional.of(command);
        }
    }

    @Override
    public Optional<CommandInternal> getSourceCommand() {
        return sourceCommand;
    }

    @Override
    public Optional<CommandInternal> getTargetCommand() {
        return targetCommand;
    }

    @Override
    public boolean isCleanup() {
        return cleanup;
    }

    @Override
    public boolean isIgnoreErrors() {
        return ignoreErrors;
    }

    // default values only

    @Override
    public String getLogCounter() {
        return "NONE";
    }

    @Override
    public int getLogLevel() {
        return 5;
    }

    @Override
    public boolean isLogFinalCommand() {
        return false;
    }


    // validation logic

    @Override
    public boolean validate(final ErrorWarningMessageJodi errorWarningMessages) {
        boolean isValid = true;
        if (name == null || name.isEmpty()) {
            String msg = errorWarningMessages.formatMessage(62300, ERROR_MESSAGE_62300, this.getClass(),
                                                            getParent().getFilePath());
            errorWarningMessages.addMessage(msg, ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
            logger.error(msg);
            isValid = false;
        }
        if (!sourceCommand.isPresent() && !targetCommand.isPresent()) {
            String msg = errorWarningMessages.formatMessage(62310, ERROR_MESSAGE_62310, this.getClass(),
                                                            getParent().getFilePath());
            errorWarningMessages.addMessage(msg, ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
            logger.error(msg);
            isValid = false;
        }
        if (sourceCommand.isPresent()) {
            // always execute validation to capture all errors
            boolean srcValid = ((Validate) sourceCommand.get()).validate(errorWarningMessages);
            isValid = isValid && srcValid;
        }
        if (targetCommand.isPresent()) {
            // always execute validation to capture all errors
            boolean trgValid = ((Validate) targetCommand.get()).validate(errorWarningMessages);
            isValid = isValid && trgValid;
        }

        return isValid;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("  LINE - name: \"")
          .append(this.getName())
          .append("\" cleanup: ")
          .append(this.isCleanup())
          .append(" ignoreErrors: ")
          .append(this.isIgnoreErrors())
          .append(EOL)
          .append("  log level: ")
          .append(this.getLogLevel())
          .append(" log counter: ")
          .append(this.getLogCounter())
          .append(" log final: ")
          .append(this.isLogFinalCommand())
          .append(EOL);
        if (this.sourceCommand.isPresent()) {
            sb.append(this.sourceCommand.get()
                                        .toString());
        }
        if (this.targetCommand.isPresent()) {
            sb.append(this.targetCommand.get()
                                        .toString());
        }
        return sb.toString();
    }


}
