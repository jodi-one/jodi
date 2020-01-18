package one.jodi.core.extensions.strategies;

import one.jodi.base.model.types.DataStore;
import one.jodi.base.model.types.DataStoreColumn;
import one.jodi.base.util.StringUtils;
import one.jodi.core.config.JodiConstants;
import one.jodi.core.extensions.contexts.ColumnMappingExecutionContext;
import one.jodi.core.extensions.contexts.TargetColumnExecutionContext;
import one.jodi.core.extensions.types.DataStoreWithAlias;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Implementation of the strategy object which attempts to form target mapping expressions based on source
 * {@link one.jodi.core.metadata.types.DataStore}s  This can be performed in several different methods,
 * based on the setting of COLUMN_MATCH_TYPE
 *
 * <ul>
 * <li>{@link MatchTypes#EQUALS} target column name is identical to source column name
 * </li>
 * <li>{@link MatchTypes#SRCENDSWITH} source column name ends with name of
 * target column
 * </li>
 * <li>{@link MatchTypes#SRCSTARTSWITH} source column name starts with name of target column
 * </li>
 * <li>{@link MatchTypes#TGTSTARTSWITH} target column name starts with name of source column
 * </li>
 * <li>{@link MatchTypes#MAGIC} source column ends with first four characters of target column
 * and target column name does not contain string "WID" or configurable properties value
 *  {@link one.jodi.base.JodiConstants#W_INSERT_DT} (configure in Jodi properties file)
 * </li>
 * </ul>
 * <p>
 * This strategy attempts to duplicate the Jodi 1.0 MagicAutoMappingComputer class' functionality.
 *
 */
public class GenericColumnMappingStrategy implements ColumnMappingStrategy {

    public static final String COLUMN_MATCH_TYPE = "ohi.column_match_type";
    private final static Logger log = LogManager.getLogger(GenericColumnMappingStrategy.class);

    private String getMatchTypeListString() {
        StringBuffer sb = new StringBuffer();
        String sep = "";
        for (MatchTypes type : MatchTypes.values()) {
            sb.append(sep).append(type.toString());
            sep = ", ";
        }

        return sb.toString();
    }

    @Override
    public String getMappingExpression(final String currentMappingExpression,
                                       final ColumnMappingExecutionContext cmContext,
                                       final TargetColumnExecutionContext targetColumnExecutionContext) {

        String mappingExpression = null;

        MatchTypes matchType = null;
        try {
            matchType = MatchTypes.valueOf(cmContext.getCoreProperties()
                    .get(COLUMN_MATCH_TYPE).getString());
        } catch (Exception e) {
            log.error("Column matching setting " + COLUMN_MATCH_TYPE + "(" +
                    cmContext.getCoreProperties().get(COLUMN_MATCH_TYPE) +
                    ") must be one of " + getMatchTypeListString());
            throw new RuntimeException(e.getMessage());
        }

        String targetColumn = targetColumnExecutionContext.getTargetColumnName();

        for (DataStoreWithAlias sourceDataStoreWithAlias : cmContext.getDataStores()) {
            if (!sourceDataStoreWithAlias.getDataStore().getDataModel().getModelCode()
                    .equals("ORACLE_OHICAH_DMT") &&
                    !sourceDataStoreWithAlias.getDataStore().getDataModel()
                            .getModelCode().equals("ORACLE_OHICAH_STO")) {
                return currentMappingExpression;
            }

            String sourceAlias = sourceDataStoreWithAlias.getAlias();
            DataStore sourceDataStore = sourceDataStoreWithAlias.getDataStore();
            for (String sc : sourceDataStore.getColumns().keySet()) {
                DataStoreColumn sourceDataStoreColumn =
                        sourceDataStore.getColumns().get(sc);
                String sourceColumn = sourceDataStoreColumn.getName();

                if (matchType == MatchTypes.MAGIC && (targetColumn.length() > 4) &&
                        sourceColumn.endsWith(
                                targetColumn.substring(4, targetColumn.length())) &&
                        !StringUtils.containsIgnoreCase(targetColumn,
                                cmContext.getCoreProperties().get(JodiConstants.W_INSERT_DT)
                                        .getString()) &&
                        !StringUtils.containsIgnoreCase(targetColumn, "WID")) {
                    mappingExpression = sourceAlias + "." + sourceColumn;
                    break;
                }
                if (matchType == MatchTypes.SRCENDSWITH &&
                        sourceColumn.endsWith(targetColumn)) {
                    mappingExpression = sourceAlias + "." + sourceColumn;
                    break;
                }
                if (matchType == MatchTypes.SRCSTARTSWITH &&
                        sourceColumn.startsWith(targetColumn)) {
                    mappingExpression = sourceAlias + "." + sourceColumn;
                    break;
                }
                if (matchType == MatchTypes.TGTSTARTSWITH &&
                        targetColumn.startsWith(sourceColumn)) {
                    mappingExpression = sourceAlias + "." + sourceColumn;
                    break;
                }
                if (matchType == MatchTypes.TGTENDSWITH &&
                        targetColumn.endsWith(sourceColumn)) {
                    mappingExpression = sourceAlias + "." + sourceColumn;
                    break;
                }
                if (matchType == MatchTypes.EQUALS &&
                        targetColumn.equals(sourceColumn)) {
                    mappingExpression = sourceAlias + "." + sourceColumn;
                    break;
                }
            }

            if (mappingExpression != null) {
                break;
            }
        }

        return (currentMappingExpression != null) ? currentMappingExpression
                : mappingExpression;
    }


    public enum MatchTypes {
        EQUALS, SRCENDSWITH, TGTENDSWITH, SRCSTARTSWITH, TGTSTARTSWITH, MAGIC
    }

}
