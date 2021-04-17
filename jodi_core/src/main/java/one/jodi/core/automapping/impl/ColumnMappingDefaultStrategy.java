package one.jodi.core.automapping.impl;


import com.google.inject.Inject;
import one.jodi.base.config.JodiPropertyNotFoundException;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.model.types.DataStore;
import one.jodi.base.model.types.DataStoreColumn;
import one.jodi.core.config.JodiConstants;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.extensions.contexts.ColumnMappingExecutionContext;
import one.jodi.core.extensions.contexts.TargetColumnExecutionContext;
import one.jodi.core.extensions.strategies.ColumnMappingStrategy;
import one.jodi.core.extensions.types.DataStoreWithAlias;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


/**
 * Implementation of the strategy object which attempts to form target mapping expressions based on source
 * {@link DataStore}
 * <p>
 * The classes utilizes regular expression for all matching.
 * <p>
 * The first regular expression (if specified in Jodi properties) will not check ignore list property,
 * also specified in Jodi Properties.
 * </ul>
 */
public class ColumnMappingDefaultStrategy implements ColumnMappingStrategy {

    private final static Logger LOGGER = LogManager.getLogger(ColumnMappingDefaultStrategy.class);

    private static final Pattern IDENTITY_REGEX = Pattern.compile(".*");
    private final static String ERROR_MESSAGE_01080 = "The Jodi properties file property '" + JodiConstants.COLUMN_MATCH_REGEX + "' contains invalid regex /%s/.";

    private final JodiProperties properties;
    private final ErrorWarningMessageJodi errorWarningMessages;

    @Inject
    public ColumnMappingDefaultStrategy(JodiProperties properties,
                                        final ErrorWarningMessageJodi errorWarningMessages) {
        this.properties = properties;
        this.errorWarningMessages = errorWarningMessages;
    }

    @Override
    public String getMappingExpression(String currentMappingExpression,
                                       ColumnMappingExecutionContext columnMappingContext,
                                       TargetColumnExecutionContext targetColumnContext) {
        ArrayList<Pattern> patterns = new ArrayList<>();
        for (String regex : properties.getPropertyList(JodiConstants.COLUMN_MATCH_REGEX)) {
            try {
                Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                patterns.add(pattern);
            } catch (PatternSyntaxException pse) {
                String msg = errorWarningMessages.formatMessage(1080, ERROR_MESSAGE_01080, this.getClass(), regex);
                LOGGER.error(msg);
                errorWarningMessages.addMessage(columnMappingContext.getDataset().getParent().getPackageSequence(), msg, MESSAGE_TYPE.ERRORS);
                throw new RuntimeException(msg);
            }
        }

        if (patterns.size() == 0) {
            patterns.add(IDENTITY_REGEX);
            LOGGER.debug("Jodi Properties " + JodiConstants.COLUMN_MATCH_REGEX + " not set. Defaulting to identity " +
                    "regex (source and target columns must have identical names).");
        }

        List<String> ignoreList;
        try {
            ignoreList = properties.getPropertyList(JodiConstants.COLUMN_MATCH_SOURCE_IGNORE);
        } catch (JodiPropertyNotFoundException e) {
            ignoreList = Collections.emptyList();
        }

        // Call the registered regexes, making sure first regex does not get ignoreList
        String mappingExpression = match(patterns.get(0), Collections.emptyList(), columnMappingContext, targetColumnContext);

        for (int i = 1; i < patterns.size(); i++) {
            if (mappingExpression != null) {
                break;
            }
            mappingExpression = match(patterns.get(i), ignoreList, columnMappingContext, targetColumnContext);
        }

        return mappingExpression;
    }

    private String match(Pattern pattern, List<String> ignoreList, ColumnMappingExecutionContext cmContext,
                         TargetColumnExecutionContext columnContext) {
        String mappingExpression = null;

        for (DataStoreWithAlias dataStoreWithAlias : cmContext.getDataStores()) {
            for (String sc : dataStoreWithAlias.getDataStore().getColumns().keySet()) {
                if (ignoreList.contains(sc)) {
                    break;
                }

                DataStoreColumn sourceDataStoreColumn = dataStoreWithAlias.getDataStore().getColumns().get(sc);
                String sourceColumn = sourceDataStoreColumn.getName();
                String targetColumn = columnContext.getTargetColumnName();

                Matcher sourceMatcher = pattern.matcher(sourceColumn);
                Matcher targetMatcher = pattern.matcher(targetColumn);

                if (sourceMatcher.find() && targetMatcher.find()) {
                    if (sourceMatcher.group().equalsIgnoreCase(targetMatcher.group())) {
                        mappingExpression = dataStoreWithAlias.getAlias() + "." + sourceColumn;
                        break;
                    }
                }
            }

            if (mappingExpression != null) {
                break;
            }
        }

        return mappingExpression;
    }

}
