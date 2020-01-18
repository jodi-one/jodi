package one.jodi.core.validation.sequences;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.core.validation.variables.VariableValidatorImp;
import one.jodi.etl.internalmodel.NativeSequence;
import one.jodi.etl.internalmodel.Sequence;
import one.jodi.etl.internalmodel.Sequences;
import one.jodi.etl.internalmodel.SpecificSequence;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

public class SequenceValidatorImpl implements SequenceValidator {

    private final static String ERROR_MESSAGE_4001 = "Sequence name must be unique, %s is not unique.";
    private final static String ERROR_MESSAGE_4002 = "Sequence name must be specified.";
    private final static String ERROR_MESSAGE_4003 = "Sequence schema must be specified for sequence %s.";
    private final static String ERROR_MESSAGE_4004 = "Sequence table must be specified for sequence %s.";
    private final static String ERROR_MESSAGE_4005 = "Sequence column must be specified for sequence %s.";
    private final static String ERROR_MESSAGE_4006 = "NativeSequenceName must be specified for sequence %s.";
    private final static String ERROR_MESSAGE_4008 = "Sequence table must exist in underlying ETLSystem for sequence %s for model %s and table %s.";
    private final static String ERROR_MESSAGE_4009 = "Sequence column must exist in underlying ETLSystem for sequence %s.";
    private final static String ERROR_MESSAGE_4020 = "Sequences are not valid and won't be processed.";
    private final static Logger logger = LogManager.getLogger(SequenceValidatorImpl.class);
    private final ErrorWarningMessageJodi errorWarningMessageJodi;
    private final DatabaseMetadataService databaseMetadataService;

    @Inject
    public SequenceValidatorImpl(final ErrorWarningMessageJodi errorWarningMessageJodi,
                                 final DatabaseMetadataService databaseMetadataService) {
        this.errorWarningMessageJodi = errorWarningMessageJodi;
        this.databaseMetadataService = databaseMetadataService;
    }

    @Override
    public boolean validate(final Sequences internalSequences) {
        final Set<String> schemaNames = this.databaseMetadataService.getSchemaNames();
        final Set<String> tableNames = this.databaseMetadataService.getTableNames();
        final Set<String> columnNames = this.databaseMetadataService.getColumnNames();

        Set<String> uniqueNames = new HashSet<>();
        boolean isUniqueName = internalSequences.getSequences().stream()
                .filter(s -> !validateUniqueNames(s, uniqueNames)).count() > 0 ? false : true;
        boolean isValid = internalSequences.getSequences().stream()
                .filter(s -> !validate(s, schemaNames, tableNames, columnNames)).count() > 0 ? false : true;
        boolean result = isUniqueName && isValid;
        if (!result) {
            String message = this.errorWarningMessageJodi.formatMessage(4002, ERROR_MESSAGE_4020,
                    VariableValidatorImp.class);
            logger.error(message);
            this.errorWarningMessageJodi.addMessage(message, ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
        }
        return result;
    }

    private boolean validateUniqueNames(Sequence s, Set<String> uniqueNames) {
        if (uniqueNames.contains(s.getName())) {
            String message = this.errorWarningMessageJodi.formatMessage(4001, ERROR_MESSAGE_4001,
                    SequenceValidatorImpl.class, s.getName());
            logger.error(message);
            this.errorWarningMessageJodi.addMessage(message, ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
            return false;
        }
        uniqueNames.add(s.getName());
        return true;
    }

    private boolean validate(final Sequence s, final Set<String> schemaNames, final Set<String> tableNames,
                             final Set<String> columnNames) {
        if (s.getName() == null) {
            String message = this.errorWarningMessageJodi.formatMessage(4002, ERROR_MESSAGE_4002,
                    SequenceValidatorImpl.class);
            logger.error(message);
            this.errorWarningMessageJodi.addMessage(message, ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
            return false;
        }
        if (s instanceof SpecificSequence) {
            if (((SpecificSequence) s).getSchema() == null) {
                String message = this.errorWarningMessageJodi.formatMessage(4003, ERROR_MESSAGE_4003,
                        SequenceValidatorImpl.class, s.getName());
                logger.error(message);
                this.errorWarningMessageJodi.addMessage(message, ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
                return false;
            }
            if (((SpecificSequence) s).getTable() == null) {
                String message = this.errorWarningMessageJodi.formatMessage(4004, ERROR_MESSAGE_4004,
                        SequenceValidatorImpl.class, s.getName());
                logger.error(message);
                this.errorWarningMessageJodi.addMessage(message, ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
                return false;
            }
            if (((SpecificSequence) s).getColumn() == null) {
                String message = this.errorWarningMessageJodi.formatMessage(4005, ERROR_MESSAGE_4005,
                        SequenceValidatorImpl.class, s.getName());
                logger.error(message);
                this.errorWarningMessageJodi.addMessage(message, ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
                return false;
            }
            if (!tableNames.contains(((SpecificSequence) s).getSchema() + "." + ((SpecificSequence) s).getTable())) {
                String message = this.errorWarningMessageJodi.formatMessage(4008, ERROR_MESSAGE_4008,
                        SequenceValidatorImpl.class, s.getName(), ((SpecificSequence) s).getSchema(), ((SpecificSequence) s).getTable());
                logger.error(message);
                this.errorWarningMessageJodi.addMessage(message, ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
                return false;
            }
            if (!columnNames.contains((((SpecificSequence) s).getSchema() + "." + ((SpecificSequence) s).getTable()
                    + "." + ((SpecificSequence) s).getColumn()))) {
                String message = this.errorWarningMessageJodi.formatMessage(4009, ERROR_MESSAGE_4009,
                        SequenceValidatorImpl.class, s.getName());
                logger.error(message);
                this.errorWarningMessageJodi.addMessage(message, ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
                return false;
            }
        }
        if (s instanceof NativeSequence) {
            if (((NativeSequence) s).getSchema() == null) {
                String message = this.errorWarningMessageJodi.formatMessage(4003, ERROR_MESSAGE_4003,
                        SequenceValidatorImpl.class, s.getName());
                logger.error(message);
                this.errorWarningMessageJodi.addMessage(message, ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
                return false;
            }
            if (((NativeSequence) s).getNativeName() == null) {
                String message = this.errorWarningMessageJodi.formatMessage(4006, ERROR_MESSAGE_4006,
                        SequenceValidatorImpl.class, s.getName());
                logger.error(message);
                this.errorWarningMessageJodi.addMessage(message, ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
                return false;
            }
//			if (!schemaNames.contains(((NativeSequence) s).getSchema())) {
//				String message = this.errorWarningMessageJodi.formatMessage(4007, ERROR_MESSAGE_4007,
//						SequenceValidatorImpl.class, s.getName());
//				logger.error(message);
//				this.errorWarningMessageJodi.addMessage(message, ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
//				return false;
//			}
        }
        return true;
    }

}
