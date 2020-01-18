package one.jodi.core.extensions.strategies;

import one.jodi.core.config.PropertyValueHolder;
import one.jodi.core.extensions.contexts.FolderNameExecutionContext;
import one.jodi.model.extensions.TransformationExtension;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.validation.constraints.Null;

@Deprecated
public class GenericFolderNameStrategy implements FolderNameStrategy {

    private final static Logger logger =
            LogManager.getLogger(GenericFolderNameStrategy.class);
    private final static String GENERIC_DEFAULT_FOLDER_KEY =
            "generic" + ".defaultfolder";
    private final static String GENERIC_DEFAULT_FOLDER_NAME_NOT_FOUND =
            "Generic ODI default folder is not properly configured. Please add " +
                    " property '%1$s' into your property file and assign a default folder name.";
    private static int logCount = 0;

    @Override
    public String getFolderName(@Null final String defaultFolderName,
                                @Null final
                                FolderNameExecutionContext execContext,
                                @Null final boolean
                                        isJournalizedData) {
        if (execContext == null) {
            return defaultFolderName;
        }

        PropertyValueHolder valueHolder =
                execContext.getProperties().get(GENERIC_DEFAULT_FOLDER_KEY);
        // Determine if default folder property is not available and log error.
        // If not corrected this will lead to a failure in the ODI interface creation
        // process if an transformation specification does not define a folder explicitly.
        if ((logCount < 1) && valueHolder == null) {
            logger.info(String.format(GENERIC_DEFAULT_FOLDER_NAME_NOT_FOUND,
                    GENERIC_DEFAULT_FOLDER_KEY));
            logCount++;
        }

        String derivedName =
                valueHolder != null ? valueHolder.getString() : defaultFolderName;

        TransformationExtension extension = execContext.getTransformationExtension();
        if (((extension != null) && (!extension.getFolder().equals("")))) {
            derivedName = extension.getFolder();
        }

        return derivedName;
    }
}
