package one.jodi.core.folder.impl;

import one.jodi.core.config.JodiConstants;
import one.jodi.core.config.PropertyValueHolder;
import one.jodi.core.extensions.contexts.FolderNameExecutionContext;
import one.jodi.core.extensions.strategies.FolderNameStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * This class implements the default logic to determine the folder name in which
 * the transformation is inserted within the ETL tool and implements the
 * interface {@link FolderNameStrategy}. This logic is always executed before
 * the custom plug-in is executed.
 * <p>
 * The class is a concrete strategy participating in the Strategy Pattern.
 */
public class FolderNameDefaultStrategy implements FolderNameStrategy {

    private final static Logger logger =
            LogManager.getLogger(FolderNameDefaultStrategy.class);

    /**
     * This method determines the name of the folder to insert the
     * transformation using the execution context that is created for this
     * plug-in feature.
     * <p>
     * The default implementation combines a prefix defined in properties files
     * with the name of the model in which the target data store is located
     *
     * @param defaultFolderName will be ignored in the default strategy
     * @param execContext       offers a set of Jodi and encapsulated ODI information to
     *                          support the decision
     */
    @Override
    public String getFolderName(final String defaultFolderName,
                                final FolderNameExecutionContext execContext,
                                final boolean isJournalizedData) {
        Map<String, PropertyValueHolder> props = execContext.getProperties();

        String prefix;
        if (!isJournalizedData) {
            PropertyValueHolder valueHolder = props.get(JodiConstants.INITIAL_LOAD_FOLDER);
            prefix = (valueHolder != null) ? valueHolder.getString() : "";
        } else {
            PropertyValueHolder valueHolder =
                    props.get(JodiConstants.INCREMENTALL_LOAD_FOLDER);
            if (valueHolder != null) { // allow empty value
                prefix = valueHolder.getString().trim();
            } else {
                prefix = JodiConstants.INCREMENTALL_LOAD_FOLDER_DEFAULT;
                logger.debug("Default value " +
                        JodiConstants.INCREMENTALL_LOAD_FOLDER_DEFAULT +
                        " is selected as the folder prefix.");
            }
        }

        String folderName;
        if (defaultFolderName != null && !defaultFolderName.trim().isEmpty()) {
            folderName = prefix + defaultFolderName;
        } else {
            folderName = prefix + execContext.getTargetDataStore()
                    .getDataModel()
                    .getModelCode();
        }
        return folderName;
    }

}
