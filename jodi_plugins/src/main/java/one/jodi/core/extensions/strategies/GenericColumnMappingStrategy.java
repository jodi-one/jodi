package one.jodi.core.extensions.strategies;

import one.jodi.base.model.types.DataStore;
import one.jodi.base.model.types.DataStoreColumn;
import one.jodi.base.util.StringUtils;
import one.jodi.core.config.JodiConstants;
import one.jodi.core.config.PropertyValueHolder;
import one.jodi.core.extensions.contexts.ColumnMappingExecutionContext;
import one.jodi.core.extensions.contexts.TargetColumnExecutionContext;
import one.jodi.core.extensions.types.DataStoreWithAlias;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Implementation of the strategy object which attempts to form target mapping expressions based on source
 * This can be performed in several different methods, based on the setting of COLUMN_MATCH_TYPE
 *
 * <ul>
 * <li>{@link MatchTypes#EQUALS} target column name is identical to source column name
 * </li>
 * <li>{@link MatchTypes#SRCENDSWITH} source column name ends with name of target column
 * </li>
 * <li>{@link MatchTypes#SRCSTARTSWITH} source column name starts with name of target column
 * </li>
 * <li>{@link MatchTypes#TGTSTARTSWITH} target column name starts with name of source column
 * </li>
 * <li>{@link MatchTypes#MAGIC} source column ends with first four characters of target column and target column name
 * does not contain string "WID" or configurable properties value jodiconstants (configure in Jodi properties file)
 * </li>
 * </ul>
 * <p>
 * This strategy attempts to duplicate the Jodi 1.0 MagicAutoMappingComputer class' functionality.
 */
public class GenericColumnMappingStrategy implements ColumnMappingStrategy {

    private final static Logger LOGGER = LogManager.getLogger(GenericColumnMappingStrategy.class);

    public static final String PROPERTY_COLUMN_MATCH_TYPE = "ohi.column_match_type";
    public static final String PROPERTY_COLUMN_AUTOMAP_FMT = "ext.automapping.%s";

    private String getMatchTypeListString() {
        return Arrays.stream(MatchTypes.values()).map(Enum::toString).collect(Collectors.joining(", "));
    }

    @Override
    public String getMappingExpression(final String currentMappingExpression,
                                       final ColumnMappingExecutionContext cmContext,
                                       final TargetColumnExecutionContext targetColumnExecutionContext) {
        // currentMappingExpression may be null; it assumes that the target exrpression is the same as
        // the source e.g. target.column_x expression is source.column_x
        if ( cmContext == null || targetColumnExecutionContext == null) {
            throw new IllegalArgumentException("Input is mandatory, no null values allowed: currentMappingExpression="
                    + currentMappingExpression + "; cmContext=" + cmContext + "; targetColumnExecutionContext="
                    + targetColumnExecutionContext);
        }

        // there could be an overriding automapping set for this column, check property ext.automapping.<target_col>
        String targetColumn = targetColumnExecutionContext.getTargetColumnName();
        PropertyValueHolder propAutomap = cmContext.getCoreProperties().get(String.format(PROPERTY_COLUMN_AUTOMAP_FMT, targetColumn));
        if (propAutomap != null) {
            try {
                return propAutomap.getString();
            }catch(ClassCastException cce){
                // for expressions with a comma like to_date('99991231','yyyymmdd')
                // it is then casted as list hence need to retrieve as string
                return propAutomap.getListAsString();
            }
        }

        // when property ohi.column_match_type is set, we assume Oracle Health Insurance handling usage
        PropertyValueHolder propColumnMatchType = cmContext.getCoreProperties().get(PROPERTY_COLUMN_MATCH_TYPE);
        if (propColumnMatchType != null) {
            return getMappingOhi(MatchTypes.findMatchType(propColumnMatchType.getString()),
                    currentMappingExpression, cmContext, targetColumnExecutionContext);
        }

        // no special handling
        return currentMappingExpression;
    }

    private String getMappingOhi(MatchTypes matchType, String currentMappingExpression,
                                 ColumnMappingExecutionContext cmContext,
                                 TargetColumnExecutionContext targetColumnExecutionContext) {
        if (matchType == MatchTypes.UNKNOWN) {
            RuntimeException e = new IllegalArgumentException("Column matching setting " + PROPERTY_COLUMN_MATCH_TYPE + "(" +
                    cmContext.getCoreProperties().get(PROPERTY_COLUMN_MATCH_TYPE) +
                    ") must be one of " + getMatchTypeListString());
            LOGGER.error(e.getMessage());
            throw e;
        }
        String targetColumn = targetColumnExecutionContext.getTargetColumnName();

        String mappingExpression = null;
        for (DataStoreWithAlias sourceDataStoreWithAlias : cmContext.getDataStores()) {
            if (!sourceDataStoreWithAlias.getDataStore().getDataModel().getModelCode().equals("ORACLE_OHICAH_DMT")
                    && !sourceDataStoreWithAlias.getDataStore().getDataModel().getModelCode().equals("ORACLE_OHICAH_STO")) {
                return currentMappingExpression;
            }

            String sourceAlias = sourceDataStoreWithAlias.getAlias();
            DataStore sourceDataStore = sourceDataStoreWithAlias.getDataStore();
            for (String sc : sourceDataStore.getColumns().keySet()) {
                DataStoreColumn sourceDataStoreColumn = sourceDataStore.getColumns().get(sc);
                String sourceColumn = sourceDataStoreColumn.getName();

                if (matchType == MatchTypes.MAGIC && (targetColumn.length() > 4) &&
                        sourceColumn.endsWith(targetColumn.substring(4)) &&
                        !StringUtils.containsIgnoreCase(targetColumn,
                                cmContext.getCoreProperties().get(JodiConstants.W_INSERT_DT).getString()) &&
                        !StringUtils.containsIgnoreCase(targetColumn, "WID")) {
                    mappingExpression = sourceAlias + "." + sourceColumn;
                    break;
                }
                if (matchType == MatchTypes.SRCENDSWITH && sourceColumn.endsWith(targetColumn)) {
                    mappingExpression = sourceAlias + "." + sourceColumn;
                    break;
                }
                if (matchType == MatchTypes.SRCSTARTSWITH && sourceColumn.startsWith(targetColumn)) {
                    mappingExpression = sourceAlias + "." + sourceColumn;
                    break;
                }
                if (matchType == MatchTypes.TGTSTARTSWITH && targetColumn.startsWith(sourceColumn)) {
                    mappingExpression = sourceAlias + "." + sourceColumn;
                    break;
                }
                if (matchType == MatchTypes.TGTENDSWITH && targetColumn.endsWith(sourceColumn)) {
                    mappingExpression = sourceAlias + "." + sourceColumn;
                    break;
                }
                if (matchType == MatchTypes.EQUALS && targetColumn.equals(sourceColumn)) {
                    mappingExpression = sourceAlias + "." + sourceColumn;
                    break;
                }
            }

            if (mappingExpression != null) {
                break;
            }
        }
        // TODO should this not be the other way around? Why else did we do the trouble of finding mappingExpression?
        return (currentMappingExpression != null) ? currentMappingExpression : mappingExpression;
    }

    public enum MatchTypes {
        EQUALS, SRCENDSWITH, TGTENDSWITH, SRCSTARTSWITH, TGTSTARTSWITH, MAGIC,
        /**
         * Meaning actually that no value could be determined. The process will fail if this value is found.
         */
        UNKNOWN;

        public static MatchTypes findMatchType(String name) {
            if (name == null || name.trim().isEmpty()) {
                return UNKNOWN;
            }
            try {
                return MatchTypes.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException e) {
                return UNKNOWN;
            }
        }
    }
}
