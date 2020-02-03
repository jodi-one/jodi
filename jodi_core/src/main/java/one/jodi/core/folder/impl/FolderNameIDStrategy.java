package one.jodi.core.folder.impl;

import one.jodi.core.extensions.contexts.FolderNameExecutionContext;
import one.jodi.core.extensions.strategies.FolderNameStrategy;

/**
 * Identity strategy that is used as a placeholder for a custom strategy.
 */
public class FolderNameIDStrategy implements FolderNameStrategy {

    /**
     * Implements the identity (ID) strategy that returns the default value
     * passed into this strategy method
     */
    @Override
    // jkm
    public String getFolderName(final String defaultFolderName,
                                final FolderNameExecutionContext execContext, final boolean isJournalizedData) {
        return defaultFolderName;
    }
    // end jkm
}