package one.jodi.base.model;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ReferToHelper<T> {

    private final static Logger logger = LogManager.getLogger(ReferToHelper.class);
    private final static String ALL_OTHER_DIMENSIONS = "_All Other Dimensions_";
    private final static String ERROR_MESSAGE_92240 =
            "Column '%1$s' in table '%2$s' contains the annotation '%3$s' " +
                    "with annotation value '%4$s' in position '%5$d'.%6$s " +
                    "This value is malformed and will be ignored.";
    private final static String ERROR_MESSAGE_92250 =
            "Column '%1$s' in table '%2$s' contains the annotation '%3$s' " +
                    "in position '%4$d'. Neither a target column or associated bridge table " +
                    "with name '%5$s' exists. This annotation will be ignored.";
    private final static String ERROR_MESSAGE_92255 =
            "Column '%1$s' in table '%2$s' contains the annotation '%3$s' " +
                    "in position '%4$d'. An associated bridge table with name '%5$s' exists. " +
                    "However, the bridge pattern is malformed. This annotation will be ignored.";
    private final static String ERROR_MESSAGE_92260 =
            "Column '%1$s' in table '%2$s' contains the annotation '%3$s' " +
                    "in position '%4$d'. The target column '%5$s' is not an FK column. " +
                    "This annotation will be ignored.";
    private final static String ERROR_MESSAGE_92270 =
            "Column '%1$s' in table '%2$s' contains the annotation '%3$s' " +
                    "in position '%4$d'. The target column '%5$s' is the first FK column of " +
                    "multiple FK constraints. Only '%6$s' is selected.";
    private final static String ERROR_MESSAGE_92280 =
            "Column '%1$s' in table '%2$s' contains the annotation '%3$s' " +
                    "in position '%4$d'. %5$s" + " This annotation will be ignored.";
    private final static String ERROR_MESSAGE_92290 =
            "Column '%1$s' in table '%2$s' contains the annotation '%3$s' " +
                    "in position '%4$d'. The outrigger table '%5$s' does not exist or is not " +
                    "connected to dimension table '%6$s'. This annotation will be ignored.";
    private final String key;
    private final String regExPattern; // assumes three parts surrounded by '(...)'
    private final ErrorWarningMessageJodi errorWarningMessages;

    protected ReferToHelper(final String key, final String regExPattern,
                            final ErrorWarningMessageJodi errorWarningMessages) {
        this.key = key;
        this.regExPattern = regExPattern;
        this.errorWarningMessages = errorWarningMessages;
    }

    abstract protected T validateAndConvertValue(final String stringValue)
            throws MalformedAnnotationValueException;

    private Optional<RefKey<T>> getRefKey(final String columnName,
                                          final TableBase primaryTable,
                                          final String refKey, final int position,
                                          final Pattern p) {
        Matcher m = p.matcher(refKey);
        if (!m.matches()) {
            String msg = errorWarningMessages.formatMessage(92240, ERROR_MESSAGE_92240,
                    this.getClass(),
                    columnName, primaryTable.getName(),
                    this.key, refKey, position + 1, "");
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            logger.error(msg);
            return Optional.empty();
        }

        String name = m.group(1);
        T value;
        try {
            value = validateAndConvertValue(m.group(4));
        } catch (MalformedAnnotationValueException e) {
            String msg = errorWarningMessages.formatMessage(92240, ERROR_MESSAGE_92240,
                    this.getClass(), columnName,
                    primaryTable.getName(),
                    this.key, refKey, position + 1,
                    e.getMessage());
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            logger.error(msg, e);
            return Optional.empty();
        }

        Optional<RefKey<T>> result;
        if (m.group(3) != null) {
            result = Optional.of(new RefKey<>(name, m.group(3), value));
        } else {
            result = Optional.of(new RefKey<>(name, value));
        }
        return result;
    }

    private Optional<TableBase> findIncoming(final TableBase target, final String name) {
        return target.getIncomingFks()
                .stream()
                .map(FkRelationshipBase::getParent)
                .filter(parent -> parent.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    private Optional<FkRelationshipBase> findMultiValuedDimension(final TableBase bridge,
                                                                  final TableBase fact) {
        if (bridge.getFks().size() != 2) {
            // not the right pattern; either missing dimension or too many dimensions
            return Optional.empty();
        }

        Optional<FkRelationshipBase> fkToDimension;
        if (bridge.getFks().get(0).getReferencedPrimaryKey().getParent() != fact) {
            fkToDimension = Optional.of(bridge.getFks().get(0));
        } else {
            fkToDimension = Optional.of(bridge.getFks().get(1));
        }
        return fkToDimension;
    }

    abstract protected Optional<String> isValidDimensionReference(
            final FkRelationshipBase fk);

    protected Optional<FkRelationshipBase> findDimension(final String columnName,
                                                         final TableBase primaryTable,
                                                         final String targetColumnName,
                                                         final int position) {
        Optional<FkRelationshipBase> targetFk;
        ColumnBase targetColumn = primaryTable.getColumns().get(targetColumnName);
        Optional<TableBase> targetBridge = findIncoming(primaryTable, targetColumnName);
        if (targetColumn == null && !targetBridge.isPresent()) {
            String msg = errorWarningMessages.formatMessage(92250, ERROR_MESSAGE_92250,
                    this.getClass(),
                    columnName,
                    primaryTable.getName(),
                    this.key, position + 1,
                    targetColumnName);
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            logger.error(msg);
            return Optional.empty();
        } else if (targetColumn == null) {
            targetFk = findMultiValuedDimension(targetBridge.get(), primaryTable);
            if (!targetFk.isPresent()) {
                String msg = errorWarningMessages.formatMessage(92255, ERROR_MESSAGE_92255,
                        this.getClass(),
                        columnName,
                        primaryTable.getName(),
                        this.key, position + 1,
                        targetColumnName);
                errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                        msg, MESSAGE_TYPE.ERRORS);
                logger.error(msg);
                return Optional.empty();
            }
        } else if (!targetColumn.isFkColumn() || primaryTable.getFks(targetColumn).isEmpty()) {
            String msg = errorWarningMessages.formatMessage(92260, ERROR_MESSAGE_92260,
                    this.getClass(), columnName,
                    primaryTable.getName(),
                    this.key, position + 1, targetColumnName);
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            logger.error(msg);
            return Optional.empty();
        } else {
            targetFk = Optional.of(primaryTable.getFks(targetColumn).get(0));
            if (primaryTable.getFks(targetColumn).size() > 1) {
                String msg = errorWarningMessages.formatMessage(92270, ERROR_MESSAGE_92270,
                        this.getClass(),
                        columnName, primaryTable.getName(),
                        this.key, position + 1,
                        targetColumnName,
                        targetFk.get().getName());
                errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                        msg, MESSAGE_TYPE.WARNINGS);
                logger.warn(msg);
            }
        }

        Optional<String> errorMessage = isValidDimensionReference(targetFk.get());
        if (errorMessage.isPresent()) {
            String msg = errorWarningMessages.formatMessage(92280, ERROR_MESSAGE_92280,
                    this.getClass(),
                    columnName,
                    primaryTable.getName(),
                    this.key, position + 1,
                    errorMessage.get());
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            logger.error(msg);
            return Optional.empty();
        }

        return targetFk;
    }

    private Optional<? extends FkRelationshipBase> findIncomingFk(final TableBase target,
                                                                  final String name) {
        return target.getIncomingFks()
                .stream()
                .filter(fk -> fk.getParent().getName().equalsIgnoreCase(name))
                .findFirst();
    }

    private Optional<? extends FkRelationshipBase> findOutgoingFk(final TableBase source,
                                                                  final String name) {
        TableBase target = source.getParent()
                .getTable(source.getParent().getName() + "." + name);
        if (target == null) {
            return Optional.empty();
        }

        List<? extends FkRelationshipBase> targetFks = source.getFks(target);
        Optional<? extends FkRelationshipBase> result = Optional.empty();
        if (!targetFks.isEmpty()) {
            result = Optional.of(targetFks.get(0));
        }
        return result;
    }

    private Optional<? extends FkRelationshipBase> determineOutrigger(
            final String columnName, final TableBase primaryTable,
            final String outriggerName,
            final FkRelationshipBase fkToDimension,
            final int position) {
        Optional<? extends FkRelationshipBase> outriggerFk = Optional.empty();
        Optional<? extends FkRelationshipBase> inFk =
                findIncomingFk(fkToDimension.getReferencedPrimaryKey().getParent(),
                        outriggerName);
        Optional<? extends FkRelationshipBase> outFk =
                findOutgoingFk(fkToDimension.getReferencedPrimaryKey().getParent(),
                        outriggerName);
        if (inFk.isPresent()) {
            outriggerFk = inFk;
        } else if (outFk.isPresent()) {
            outriggerFk = outFk;
        } else {
            // the defined table does not exist or is not connected to the dimension
            String msg = errorWarningMessages.formatMessage(92290, ERROR_MESSAGE_92290,
                    this.getClass(), columnName,
                    primaryTable.getName(), this.key,
                    position + 1, outriggerName,
                    fkToDimension.getReferencedPrimaryKey()
                            .getParent().getName());
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            logger.error(msg);
        }
        return outriggerFk;
    }

    private String getKey(final FkRelationshipBase fkToDimension,
                          final Optional<? extends FkRelationshipBase> fkToOutrigger) {
        String key = fkToDimension.getName();
        if (fkToOutrigger.isPresent() &&
                fkToOutrigger.get().getParent() != fkToDimension.getReferencedPrimaryKey()
                        .getParent()) {
            // Different handling of hierarchy
            key += ".Hier";
        }
        return key;
    }

    public List<TargetDefinition<T>> determineTargetAnnotations(
            final String columnName, final TableBase primaryTable,
            final Optional<List<? extends Object>> targetAnnotations) {
        if (!targetAnnotations.isPresent()) {
            return Collections.emptyList();
        }

        Map<String, RefKey<T>> previouslyFound = new HashMap<>();

        Pattern p = Pattern.compile(this.regExPattern);
        final List<TargetDefinition<T>> levels = new ArrayList<>();
        for (int i = 0; i < targetAnnotations.get().size(); i++) {
            Object expression = targetAnnotations.get().get(i);
            if (!(expression instanceof String)) {
                String msg = errorWarningMessages.formatMessage(92240, ERROR_MESSAGE_92240,
                        this.getClass(), columnName,
                        primaryTable.getName(), this.key,
                        expression, i + 1, " A string was expected.");
                errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                        msg, MESSAGE_TYPE.ERRORS);
                logger.error(msg);
                continue;
            }

            Optional<RefKey<T>> oRefKey = getRefKey(columnName, primaryTable,
                    (String) expression, i, p);
            if (!oRefKey.isPresent()) {
                // expression contains errors
                continue;
            }
            RefKey<T> refKey = oRefKey.get();
            // special handling of "_All Other Dimensions_" case
            if (ALL_OTHER_DIMENSIONS.equalsIgnoreCase(refKey.getColumnName())) {
                @SuppressWarnings({"rawtypes", "unchecked"})
                TargetDefinition<T> td =
                        new TargetAllOtherDimensions(refKey.getValue());
                levels.add(td);
                continue;
            }

            Optional<FkRelationshipBase> fk =
                    findDimension(columnName, primaryTable, refKey.getColumnName(), i);
            if (!fk.isPresent()) {
                // dimension cannot be found
                continue;
            }

            Optional<? extends FkRelationshipBase> outriggerFk = Optional.empty();
            Optional<String> outriggerName = Optional.ofNullable(refKey.getTableName());
            if (outriggerName.filter(name -> !"".equals(name)).isPresent()) {
                outriggerFk = determineOutrigger(columnName, primaryTable, outriggerName.get(), fk.get(), i);
                if (!outriggerFk.isPresent()) {
                    continue; // outrigger table not found
                }
            }

            String key = getKey(fk.get(), outriggerFk);
            if (previouslyFound.get(key) != null) {
                String msg = errorWarningMessages.formatMessage(92280, ERROR_MESSAGE_92280,
                        this.getClass(), columnName,
                        primaryTable.getName(), expression, i + 1,
                        " Duplicate target dimension.");
                errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                        msg, MESSAGE_TYPE.ERRORS);
                logger.error(msg);
                continue;
            }
            previouslyFound.put(key, refKey);

            @SuppressWarnings({"rawtypes", "unchecked"})
            TargetDefinition<T> td = new TargetDefinition(fk.get(), outriggerFk,
                    refKey.getValue());
            levels.add(td);
        }
        return levels;
    }

    private static class RefKey<T> {
        private final String columnName;
        private final String tableName;
        private final T value;

        private RefKey(final String columnName, final String tableName, final T value) {
            super();
            this.columnName = columnName;
            this.tableName = tableName;
            this.value = value;
        }

        private RefKey(final String columnName, final T value) {
            this(columnName, null, value);
        }

        private String getColumnName() {
            return this.columnName;
        }

        private String getTableName() {
            return this.tableName;
        }

        private T getValue() {
            return this.value;
        }
    }

}
