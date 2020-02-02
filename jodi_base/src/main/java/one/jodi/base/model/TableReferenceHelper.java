package one.jodi.base.model;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reusable logic to process an annotation with the format
 * (schema_name.)?table_name
 *
 */
public class TableReferenceHelper {

    private final static Logger logger = LogManager.getLogger(TableReferenceHelper.class);

    private final static String TABLE_REFERENCE = "((\\w+)\\.)?(\\w+)";

    private final static Pattern EXPR_PATTERN = Pattern.compile(TABLE_REFERENCE);

    private final static String ERROR_MSG_BASE =
            "Table '%1$s' of schema '%2$s' contains annotation key '%3$s' " +
                    "with value '%4$s'. ";

    private final static String ERROR_MESSAGE_72010 = ERROR_MSG_BASE +
            "The annotation value is malformed and will be ignored.";

    private final static String ERROR_MESSAGE_72020 = ERROR_MSG_BASE +
            "A schema with name '%5$s' does not exist.";

    private final static String ERROR_MESSAGE_72030 = ERROR_MSG_BASE +
            "A table with name '%5$s' does not exist in schema '%6$s'.";

    private final ErrorWarningMessageJodi errorWarningMessages;

    @Inject
    TableReferenceHelper(final ErrorWarningMessageJodi errorWarningMessages) {
        this.errorWarningMessages = errorWarningMessages;
    }

    private String getTableKey(final String schemaName, final String tableName) {
        return schemaName + "." + tableName;
    }

    public Optional<? extends TableBase> parseAnnotation(final TableBase table,
                                                         final String annotationValue,
                                                         final String annotationKey) {
        String tableName = table.getName();
        String schemaName = table.getParent().getName();
        ApplicationBase application = table.getParent().getParent();
        // default schema is the one that contains the table
        SchemaBase referencedSchema = table.getParent();

        Matcher m = EXPR_PATTERN.matcher(annotationValue);
        if (!m.matches()) {
            String msg =
                    errorWarningMessages.formatMessage(72010, ERROR_MESSAGE_72010,
                            this.getClass(), tableName, schemaName,
                            annotationKey, annotationValue);
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            logger.error(msg);
            return Optional.empty();
        }

        String refSchemaName = m.group(2);
        // validate if schema is defined
        if (refSchemaName != null && !refSchemaName.isEmpty()) {
            referencedSchema = application.getSchemaMap().get(refSchemaName);
            if (referencedSchema == null) {
                String msg =
                        errorWarningMessages.formatMessage(72020, ERROR_MESSAGE_72020,
                                this.getClass(), tableName,
                                schemaName, annotationKey,
                                annotationValue, refSchemaName);
                errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                        msg, MESSAGE_TYPE.ERRORS);
                logger.error(msg);
                return Optional.empty();
            }
        }

        String refTableName = m.group(3);
        final String tableKey = getTableKey(referencedSchema.getName(), refTableName);
        TableBase refTable = referencedSchema.getTable(tableKey);
        if (refTable == null) {
            String msg =
                    errorWarningMessages.formatMessage(72030, ERROR_MESSAGE_72030,
                            this.getClass(), tableName, schemaName,
                            annotationKey, annotationValue,
                            refTableName,
                            referencedSchema.getName());
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            logger.error(msg);
            return Optional.empty();
        }
        return Optional.of(refTable);
    }

    public Optional<? extends TableBase> parseAnnotation(ColumnBase column, String annotationKey, Optional<String> annotationValue) {
        if (!annotationValue.isPresent()) {
            return Optional.empty();
        }

        String tableName = column.getParent().getName();
        String schemaName = column.getParent().getParent().getName();
        ApplicationBase application = column.getParent().getParent().getParent();
        // default schema is the one that contains the table
        SchemaBase referencedSchema = column.getParent().getParent();

        Matcher m = EXPR_PATTERN.matcher(annotationValue.get());
        if (!m.matches()) {
            String msg =
                    errorWarningMessages.formatMessage(72010, ERROR_MESSAGE_72010,
                            this.getClass(), tableName, schemaName,
                            annotationKey, annotationValue);
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            logger.error(msg);
            return Optional.empty();
        }

        String refSchemaName = m.group(2);
        // validate if schema is defined
        if (refSchemaName != null && !refSchemaName.isEmpty()) {
            referencedSchema = application.getSchemaMap().get(refSchemaName);
            if (referencedSchema == null) {
                String msg =
                        errorWarningMessages.formatMessage(72020, ERROR_MESSAGE_72020,
                                this.getClass(), tableName,
                                schemaName, annotationKey,
                                annotationValue, refSchemaName);
                errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                        msg, MESSAGE_TYPE.ERRORS);
                logger.error(msg);
                return Optional.empty();
            }
        }

        String refTableName = m.group(3);
        final String tableKey = getTableKey(referencedSchema.getName(), refTableName);
        TableBase refTable = referencedSchema.getTable(tableKey);
        if (refTable == null) {
            String msg =
                    errorWarningMessages.formatMessage(72030, ERROR_MESSAGE_72030,
                            this.getClass(), tableName, schemaName,
                            annotationKey, annotationValue,
                            refTableName,
                            referencedSchema.getName());
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            logger.error(msg);
            return Optional.empty();
        }
        return Optional.of(refTable);
    }

}
