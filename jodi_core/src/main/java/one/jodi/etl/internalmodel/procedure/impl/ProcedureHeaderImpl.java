package one.jodi.etl.internalmodel.procedure.impl;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.etl.internalmodel.procedure.ProcedureHeader;
import one.jodi.etl.internalmodel.procedure.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;

public class ProcedureHeaderImpl implements ProcedureHeader, Validate {

    private static final Logger logger = LogManager.getLogger(ProcedureHeaderImpl.class);

    private static final String ERROR_MESSAGE_62100 =
            "Procedure must define a non-empty name in its definition file %1$s. " +
                    "This procedure specification will be ignored.";

    private static final String ERROR_MESSAGE_62110 =
            "Procedure '%1$s' must define a non-empty folder name in its definition " +
                    "file %2$s. This procedure specification will be ignored.";

    private static final String ERROR_MESSAGE_62120 =
            "Procedure '%1$s' defines an incorrect folder name in its definition " +
                    "file %2$s. This procedure specification will be ignored.";

    // folder path from root to target folder. Separator is '/'
    private final List<String> folderPath;
    private final String name;
    // supplemental information
    private final String filePath;

    public ProcedureHeaderImpl(final List<String> folderPath, final String name, final String filePath) {
        super();
        this.name = name;
        this.folderPath = folderPath;
        this.filePath = filePath;
    }

    @Override
    public List<String> getFolderNames() {
        return Collections.unmodifiableList(this.folderPath);
    }

    @Override
    public String getFolderPath() {
        return String.join("/", this.folderPath);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public boolean validate(final ErrorWarningMessageJodi errorWarningMessages) {
        boolean isValid = true;
        if (this.name == null || this.name.isEmpty()) {
            String msg = errorWarningMessages.formatMessage(62100, ERROR_MESSAGE_62100, this.getClass(), this.filePath);
            errorWarningMessages.addMessage(msg, ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
            logger.error(msg);
            isValid = false;
        }

        if (this.folderPath == null || this.folderPath.isEmpty()) {
            String msg = errorWarningMessages.formatMessage(62110, ERROR_MESSAGE_62110, this.getClass(), this.name,
                                                            this.filePath);
            errorWarningMessages.addMessage(msg, ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
            logger.error(msg);
            isValid = false;
        } else if (this.folderPath.stream()
                                  .filter(n -> n == null || n.isEmpty())
                                  .findFirst()
                                  .isPresent()) {
            String msg = errorWarningMessages.formatMessage(62120, ERROR_MESSAGE_62120, this.getClass(), this.name,
                                                            this.filePath);
            errorWarningMessages.addMessage(msg, ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
            logger.error(msg);
            isValid = false;
        }

        return isValid;
    }

    @Override
    public String getFilePath() {
        return filePath;
    }

}
