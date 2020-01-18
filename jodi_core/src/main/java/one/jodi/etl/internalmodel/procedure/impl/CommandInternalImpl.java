package one.jodi.etl.internalmodel.procedure.impl;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.etl.builder.impl.DictionaryModelLogicalSchema;
import one.jodi.etl.internalmodel.procedure.CommandInternal;
import one.jodi.etl.internalmodel.procedure.TaskInternal;
import one.jodi.etl.internalmodel.procedure.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

public class CommandInternalImpl implements CommandInternal, Validate {

    private final static Logger logger =
            LogManager.getLogger(CommandInternalImpl.class);

    ;
    private final static String EOL = System.getProperty("line.separator");
    private final static String ERROR_MESSAGE_62400 =
            "A command in procedure definition file %1$s does not define a logical model. " +
                    "This procedure specification will be ignored.";
    private final static String ERROR_MESSAGE_62410 =
            "A command in procedure definition file %1$s defines an unknown logical " +
                    "schema '%2$s'. This procedure specification will be ignored.";
    private final static String ERROR_MESSAGE_62420 =
            "A command in procedure definition file %1$s does not define the required " +
                    "expression. This procedure specification will be ignored.";
    private final TaskInternal parent;
    private final String technology;
    private final String modelName;
    private final String command;
    private final Set<String> logicalModelNames;
    private final String context;
    private final DictionaryModelLogicalSchema dictionaryModelLogicalSchema;
    private final String logicalSchema;
    public CommandInternalImpl(final TaskInternal parent, final String technology,
                               final String modelName, final String context,
                               final String command,
                               final Location location,
                               // used for validation only
                               final Set<String> logicalModelNames,
                               final DictionaryModelLogicalSchema dictionaryModelLogicalSchema,
                               boolean doNotTranslate) {
        super();
        assert (technology != null && !technology.isEmpty());
        this.parent = parent;
        this.technology = technology;
        this.modelName = modelName;
        this.command = command;
        this.logicalModelNames = logicalModelNames;
        this.context = context == null ? "EXECUTION_CONTEXT" : context;
        this.dictionaryModelLogicalSchema = dictionaryModelLogicalSchema;
        this.logicalSchema = doNotTranslate ? modelName : this.dictionaryModelLogicalSchema.translateToLogicalSchema(this.modelName);
        // adds itself to parent
        ((TaskInternalImpl) this.parent).addCommand(this, location);
    }

    @Override
    public TaskInternal getParent() {
        return this.parent;
    }

    @Override
    public String getTechnology() {
        return technology;
    }

    @Override
    public String getLogicalSchema() {
        return logicalSchema;
    }

    @Override
    public String getCommand() {
        return command;
    }

    @Override
    public boolean isAlwaysExecuteOptions() {
        return true;
    }

    @Override
    public String getTransactionIsolation() {
        return "NONE";
    }

    // default values only

    @Override
    public String getExecutionContext() {
        return this.context;
    }

    @Override
    public String getTransaction() {
        return "Autocommit";
    }

    @Override
    public boolean validate(ErrorWarningMessageJodi errorWarningMessages) {
        boolean isValid = true;
        String aLogicalSchema = this.dictionaryModelLogicalSchema.translateToLogicalSchema(modelName);
        if (modelName == null || modelName.isEmpty()) {
            String msg = errorWarningMessages.formatMessage(62400, ERROR_MESSAGE_62400,
                    this.getClass(),
                    getParent().getParent()
                            .getFilePath());
            errorWarningMessages.addMessage(msg, ErrorWarningMessageJodi.MESSAGE_TYPE
                    .ERRORS);
            logger.error(msg);
            isValid = false;
        } else if (!logicalModelNames.contains(aLogicalSchema)) {
            String msg = errorWarningMessages.formatMessage(62410, ERROR_MESSAGE_62410,
                    this.getClass(),
                    getParent().getParent()
                            .getFilePath(),
                    aLogicalSchema);
            errorWarningMessages.addMessage(msg, ErrorWarningMessageJodi.MESSAGE_TYPE
                    .ERRORS);
            logger.error(msg);
            isValid = false;
        }

        if (command == null || command.isEmpty()) {
            String msg = errorWarningMessages.formatMessage(62420, ERROR_MESSAGE_62420,
                    this.getClass(),
                    getParent().getParent()
                            .getFilePath());
            errorWarningMessages.addMessage(msg, ErrorWarningMessageJodi.MESSAGE_TYPE
                    .ERRORS);
            logger.error(msg);
            isValid = false;
        }
        return isValid;
    }

    // validation logic

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("    COMMAND - technology: \"")
                .append(this.technology)
                .append("\" modelname: \"")
                .append(this.modelName)
                .append("\" context: \"")
                .append(this.context)
                .append("\"")
                .append(EOL)
                .append("    Expression: ")
                .append(this.command)
                .append(EOL);
        return sb.toString();
    }

    public enum Location {SOURCE, TARGET}

}
