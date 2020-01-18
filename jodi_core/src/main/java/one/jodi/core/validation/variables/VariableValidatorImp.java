package one.jodi.core.validation.variables;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.etl.internalmodel.Variable;
import one.jodi.etl.internalmodel.Variables;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static one.jodi.core.service.VariableService.DATE_FORMAT;

public class VariableValidatorImp implements VariableValidator {
    public final static String ERROR_MESSAGE_4007 = "The schema  must exist in underlying ETLSystem of the variable %s.";
    public final static String ERROR_MESSAGE_4020 = "The variables are not valid and won't be processed.";
    private final static Logger logger = LogManager.getLogger(VariableValidatorImp.class);
    private final static String ERROR_MESSAGE_4001 = "VariableNames must be unique, " + "%s is not unique.";
    private final static String ERROR_MESSAGE_4002 = "Can't parse default value of date format of "
            + "VariableImpl %s  with format: %s.";
    private final static String ERROR_MESSAGE_4003 = "The name of the variable can't be null.";
    private final static String ERROR_MESSAGE_4004 = "The datatype of the variable %s can't be null.";
    private final static String ERROR_MESSAGE_4005 = "The schema of the variable %s can't be null.";
    private final static String ERROR_MESSAGE_4006 = "The query of the variable %s can't be null.";
    private final ErrorWarningMessageJodi errorWarningMessageJodi;
    private final DatabaseMetadataService databaseMetadataService;

    @Inject
    public VariableValidatorImp(final ErrorWarningMessageJodi errorWarningMessageJodi,
                                final DatabaseMetadataService databaseMetadataService) {
        this.databaseMetadataService = databaseMetadataService;
        this.errorWarningMessageJodi = errorWarningMessageJodi;
    }

    @Override
    public boolean validate(Variables internalVariables) {
        final Set<String> schemaNames = this.databaseMetadataService.getSchemaNames();
        final Set<String> uniqueNames = new HashSet<>();
        boolean hasUniquenames = internalVariables.getVariables().stream()
                .filter(internalVar -> !validateUniqueNames(internalVar, uniqueNames)).count() > 0 ? false : true;
        boolean isValid = internalVariables.getVariables().stream()
                .filter(internalVar -> !validate(internalVar, schemaNames)).count() > 0 ? false : true;
        boolean result = hasUniquenames && isValid;
        if (!result) {
            String message = this.errorWarningMessageJodi.formatMessage(4002, ERROR_MESSAGE_4020,
                    VariableValidatorImp.class);
            logger.error(message);
            this.errorWarningMessageJodi.addMessage(message, ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
        }
        return result;
    }

    private boolean validateUniqueNames(Variable internalVar, Set<String> uniqueNames) {
        if (uniqueNames.contains(internalVar.getName())) {
            String message = this.errorWarningMessageJodi.formatMessage(4001, ERROR_MESSAGE_4001,
                    VariableValidatorImp.class, internalVar.getName());
            logger.error(message);
            this.errorWarningMessageJodi.addMessage(message, ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
            return false;
        }
        uniqueNames.add(internalVar.getName());
        return true;
    }

    private boolean validate(Variable internalVar, Set<String> schemaNames) {

        if (internalVar.getDataType().equals(Variable.Datatype.DATE) && internalVar.getDefaultValue() != null) {
            SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH);
            try {
                formatter.parse(internalVar.getDefaultValue());
            } catch (ParseException e) {
                String message = this.errorWarningMessageJodi.formatMessage(4002, ERROR_MESSAGE_4002,
                        VariableValidatorImp.class, internalVar.getName(), internalVar.getDefaultValue());
                logger.error(message, e);
                this.errorWarningMessageJodi.addMessage(message, ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
                return false;
            }
        }
        if (internalVar.getName() == null) {
            String message = this.errorWarningMessageJodi.formatMessage(4003, ERROR_MESSAGE_4003,
                    VariableValidatorImp.class);
            logger.error(message);
            this.errorWarningMessageJodi.addMessage(message, ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
            return false;
        }
        if (internalVar.getDataType() == null) {
            String message = this.errorWarningMessageJodi.formatMessage(4004, ERROR_MESSAGE_4004,
                    VariableValidatorImp.class, internalVar.getName());
            logger.error(message);
            this.errorWarningMessageJodi.addMessage(message, ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
            return false;
        }
        if (internalVar.getSchema() == null) {
            String message = this.errorWarningMessageJodi.formatMessage(4005, ERROR_MESSAGE_4005,
                    VariableValidatorImp.class, internalVar.getName());
            logger.error(message);
            this.errorWarningMessageJodi.addMessage(message, ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
            return false;
        }
        if (internalVar.getQuery() == null) {
            String message = this.errorWarningMessageJodi.formatMessage(4006, ERROR_MESSAGE_4006,
                    VariableValidatorImp.class, internalVar.getName());
            logger.error(message);
            this.errorWarningMessageJodi.addMessage(message, ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
            return false;
        }
        return true;
    }
}
