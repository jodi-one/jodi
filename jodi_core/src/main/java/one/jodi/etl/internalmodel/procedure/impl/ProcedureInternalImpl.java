package one.jodi.etl.internalmodel.procedure.impl;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.core.procedure.Option;
import one.jodi.etl.internalmodel.procedure.OptionInternal;
import one.jodi.etl.internalmodel.procedure.ProcedureInternal;
import one.jodi.etl.internalmodel.procedure.TaskInternal;
import one.jodi.etl.internalmodel.procedure.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ProcedureInternalImpl extends ProcedureHeaderImpl implements ProcedureInternal, Validate {

    private static final Logger logger = LogManager.getLogger(ProcedureInternalImpl.class);

    private static final String EOL = System.getProperty("line.separator");

    private static final String ERROR_MESSAGE_62200 = "Procedure must define at least one task in its definition file %1$s. "
            + "This procedure specification will be ignored.";

    private final Optional<String> description;
    private final boolean isMultiConnectionSupported;
    private final boolean isRemoveTemporaryObjectsonError;
    private final boolean isUseUniqueTemporaryObjectNames;
    private final List<OptionInternal> internalOptions = new ArrayList<OptionInternal>();

    private List<TaskInternal> tasks = new ArrayList<>();

    public ProcedureInternalImpl(final List<String> folderNames, final String name, final String description,
                                 final boolean isMultiConnectionSupported, final boolean isRemoveTemporaryObjectsonError,
                                 final boolean isUseUniqueTemporaryObjectNames, final List<Option> options, final String filePath) {
        super(folderNames, name, filePath);
        this.description = Optional.ofNullable(description);
        this.isMultiConnectionSupported = isMultiConnectionSupported;
        this.isRemoveTemporaryObjectsonError = isRemoveTemporaryObjectsonError;
        this.isUseUniqueTemporaryObjectNames = isUseUniqueTemporaryObjectNames;
        List<Option> externalOptions = options == null || options.isEmpty() ? Collections.emptyList() : options;
        transform(externalOptions);
    }

    private void transform(List<Option> externalOptions) {
        externalOptions.forEach(eo -> internalOptions.add(transfrom(eo)));
    }

    private OptionInternal transfrom(Option eo) {
        return new OptionInternalImpl(eo.getName(), eo.getType(), eo.getDefaultValue(), eo.getOptionList(),
                eo.getCondition(), eo.getDescription(), eo.getHelp());
    }

    @Override
    public Optional<String> getDescription() {
        return this.description;
    }

    void addTask(final TaskInternal task) {
        this.tasks.add(task);
    }

    @Override
    public List<TaskInternal> getTasks() {
        return Collections.unmodifiableList(this.tasks);
    }

    @Override
    public boolean isMultiConnectionSupported() {
        return isMultiConnectionSupported;
    }

    @Override
    public boolean isRemoveTemporaryObjectsonError() {
        return isRemoveTemporaryObjectsonError;
    }

    @Override
    public boolean isUseUniqueTemporaryObjectNames() {
        return isUseUniqueTemporaryObjectNames;
    }

    // default values only

    @Override
    public String getSourceTechnology() {
        return "ORACLE";
    }

    @Override
    public String getTargetTechnology() {
        return "ORACLE";
    }

    // validation logic

    @Override
    public boolean validate(ErrorWarningMessageJodi errorWarningMessages) {
        boolean isValid = super.validate(errorWarningMessages);
        if (tasks == null || tasks.isEmpty()) {
            String msg = errorWarningMessages.formatMessage(62200, ERROR_MESSAGE_62200, this.getClass(), getFilePath());
            errorWarningMessages.addMessage(msg, ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
            logger.error(msg);
            isValid = false;
        }
        boolean areTaskValid = this.tasks.stream().map(t -> ((Validate) t).validate(errorWarningMessages))
                // intentional to collect first to capture all errors
                .collect(Collectors.toList()).stream().reduce(true, Boolean::logicalAnd);
        isValid = isValid && areTaskValid;

        return isValid;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("folder: \"").append(getFolderPath()).append("\" name: \"").append(this.getName()).append("\"")
                .append(EOL).append("Description: ")
                .append(getDescription().isPresent() ? getDescription().get() : "<undefined>").append(EOL)
                .append("multi: ").append(this.isMultiConnectionSupported).append(" rmTmp: ")
                .append(this.isRemoveTemporaryObjectsonError).append(" useUnique: ")
                .append(this.isUseUniqueTemporaryObjectNames).append(EOL)
                .append(tasks.stream().map(t -> t.toString()).collect(Collectors.joining(EOL)))
                .append(internalOptions.stream().map(io -> io.toString()).collect(Collectors.joining(EOL)));
        return sb.toString();
    }

    @Override
    public List<OptionInternal> getOptions() {
        return this.internalOptions;
    }

}
