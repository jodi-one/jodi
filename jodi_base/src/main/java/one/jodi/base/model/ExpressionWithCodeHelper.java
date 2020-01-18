package one.jodi.base.model;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.model.expression.ColumnExpression;
import one.jodi.base.model.expression.Expression;
import one.jodi.base.model.expression.StringExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reusable logic to process an annotation with the format
 * <schema name>.<table name>.<column name>:<code name>
 *
 */
public abstract class ExpressionWithCodeHelper {

    private final static Logger logger = LogManager.getLogger(ExpressionWithCodeHelper.class);

    private final static String SOURCE_COLUMN_EXPRESSION =
            "(\\w+)\\.(\\w+)\\.(\"([\\w\\., ]+)\"||(\\w+)):(\\w+)";

    private final static Pattern EXPR_PATTERN = Pattern.compile(SOURCE_COLUMN_EXPRESSION);

    private final static String ERROR_MSG_BASE =
            "Column '%1$s' in table '%2$s' of schema '%3$s contains annotation '%4$s' " +
                    "for annotation key '%5$s' in position '%6$d'. ";

    private final static String ERROR_MESSAGE_71000 = ERROR_MSG_BASE +
            "The annotation must be of type String.";

    private final static String ERROR_MESSAGE_71010 = ERROR_MSG_BASE +
            "The annotation value is malformed and will be ignored.";

    private final static String ERROR_MESSAGE_71020 = ERROR_MSG_BASE +
            "A source schema with name '%7$s' does not exist.";

    private final static String ERROR_MESSAGE_71030 = ERROR_MSG_BASE +
            "Schema '%7$s' does not contain source table with name '%8$s'.";

    private final static String ERROR_MESSAGE_71040 = ERROR_MSG_BASE +
            "Table '%8$s' in schema '%7$s' does not contain source column '%9$s'.";

    private final static String ERROR_MESSAGE_71050 = ERROR_MSG_BASE +
            "The value is malformed and will be ignored: %7$s";

    private final static String ERROR_MESSAGE_71060 = ERROR_MSG_BASE +
            "The source column has already been assigned the role '%7$s'. " +
            "This annotation will be ignored.";

    private final ErrorWarningMessageJodi errorWarningMessages;

    public ExpressionWithCodeHelper(final ErrorWarningMessageJodi errorWarningMessages) {
        this.errorWarningMessages = errorWarningMessages;
    }

    protected abstract String validateValue(final String stringValue)
            throws MalformedAnnotationValueException;

    protected Optional<? extends ExpressionWithCode> createResult(
            final TableBase parent,
            final Object leaf,
            final String value) {
        assert (parent != null);
        return Optional.of(new ExpressionWithCode() {
            @Override
            public Expression getExpression() {
                if (leaf instanceof String) {
                    return new StringExpression(parent, (String) leaf);
                } else {
                    return new ColumnExpression((ColumnBase) leaf);
                }
            }

            @Override
            public String getCode() {
                return value;
            }
        });
    }

    private String getTableKey(final String schemaName, final String tableName) {
        return schemaName + "." + tableName;
    }

    private Optional<? extends ExpressionWithCode> parseAnnotation(
            final String annotationKey,
            final String annotationValue,
            final ColumnBase targetColumn,
            final int position,
            final Pattern p) {
        ApplicationBase application = targetColumn.getParent()
                .getParent()
                .getParent();
        String targetColumnName = targetColumn.getName();
        String targetTableName = targetColumn.getParent().getName();
        String targetSchemaName = targetColumn.getParent().getParent().getName();

        Matcher m = p.matcher(annotationValue);
        if (!m.matches()) {
            String msg = errorWarningMessages
                    .formatMessage(71010, ERROR_MESSAGE_71010, this.getClass(),
                            targetColumnName, targetTableName,
                            targetSchemaName, annotationValue,
                            annotationKey, position + 1);
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            logger.error(msg);
            return Optional.empty();
        }

        String sourceSchemaName = m.group(1);
        SchemaBase sourceSchema = application.getSchemaMap().get(sourceSchemaName);
        if (sourceSchema == null) {
            String msg = errorWarningMessages
                    .formatMessage(71020, ERROR_MESSAGE_71020, this.getClass(),
                            targetColumnName, targetTableName,
                            targetSchemaName, annotationValue,
                            annotationKey, position + 1,
                            sourceSchemaName);
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            logger.error(msg);
            return Optional.empty();
        }

        String sourceTableName = m.group(2);
        final String tableKey = getTableKey(sourceSchemaName, sourceTableName);
        TableBase sourceTable = sourceSchema.getTable(tableKey);
        if (sourceTable == null) {
            String msg = errorWarningMessages
                    .formatMessage(71030, ERROR_MESSAGE_71030, this.getClass(),
                            targetColumnName, targetTableName,
                            targetSchemaName, annotationValue,
                            annotationKey, position + 1,
                            sourceSchemaName, sourceTableName);
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            logger.error(msg);
            return Optional.empty();
        }

        Object leaf;
        if (m.group(4) == null) {
            // TODO generalize to handle String values as well
            String sourceColumnName = m.group(3);
            leaf = sourceTable.getColumns().get(sourceColumnName);
            if (leaf == null) {
                String msg = errorWarningMessages
                        .formatMessage(71040, ERROR_MESSAGE_71040,
                                this.getClass(),
                                targetColumnName, targetTableName,
                                targetSchemaName, annotationValue,
                                annotationKey, position + 1,
                                sourceSchemaName, sourceTableName,
                                sourceColumnName);
                errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                        msg, MESSAGE_TYPE.ERRORS);
                logger.error(msg);
                return Optional.empty();
            }
        } else {
            leaf = m.group(4);
        }

        String value;
        try {
            value = validateValue(m.group(6).trim());
        } catch (MalformedAnnotationValueException e) {
            String msg = errorWarningMessages
                    .formatMessage(71050, ERROR_MESSAGE_71050, this.getClass(),
                            targetColumnName, targetTableName,
                            targetSchemaName, annotationValue,
                            annotationKey, position + 1, e.getMessage());
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            logger.error(msg, e);
            return Optional.empty();
        }

        return createResult(sourceTable, leaf, value);
    }

    public List<ExpressionWithCode> determineColumnAnnotations(
            final ColumnBase targetColumn,
            final String annotationKey,
            final Optional<List<? extends Object>> codeAnnotations) {
        if (!codeAnnotations.isPresent()) {
            return Collections.emptyList();
        }

        Map<Expression, ExpressionWithCode> previouslyFound = new HashMap<>();
        final List<ExpressionWithCode> annotations = new ArrayList<>();
        for (int i = 0; i < codeAnnotations.get().size(); i++) {
            Object expression = codeAnnotations.get().get(i);
            if (!(expression instanceof String)) {
                String targetSchemaName = targetColumn.getName();
                String targetTableName = targetColumn.getParent().getName();
                String targetColumnName = targetColumn.getParent().getParent().getName();
                String msg = errorWarningMessages
                        .formatMessage(71000, ERROR_MESSAGE_71000, this.getClass(),
                                targetColumnName, targetTableName,
                                targetSchemaName, expression.toString(),
                                annotationKey, i);
                errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                        msg, MESSAGE_TYPE.ERRORS);
                logger.error(msg);
                continue;
            }

            Optional<? extends ExpressionWithCode> cwc =
                    parseAnnotation(annotationKey, (String) codeAnnotations.get().get(i),
                            targetColumn, i, EXPR_PATTERN);
            if (!cwc.isPresent()) {
                continue;
            } else if (previouslyFound.get(cwc.get().getExpression()) != null) {
                String targetSchemaName = targetColumn.getName();
                String targetTableName = targetColumn.getParent().getName();
                String targetColumnName = targetColumn.getParent().getParent().getName();
                String msg = errorWarningMessages
                        .formatMessage(71060, ERROR_MESSAGE_71060, this.getClass(),
                                targetColumnName, targetTableName,
                                targetSchemaName, expression.toString(),
                                annotationKey, i, cwc.get().getCode());
                errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                        msg, MESSAGE_TYPE.ERRORS);
                logger.error(msg);
                continue;
            }

            annotations.add(cwc.get());
            previouslyFound.put(cwc.get().getExpression(), cwc.get());
        }
        return annotations;
    }

}
