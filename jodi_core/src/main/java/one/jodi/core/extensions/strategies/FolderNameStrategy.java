package one.jodi.core.extensions.strategies;

import one.jodi.core.extensions.contexts.FolderNameExecutionContext;

/**
 * This interface defines the plug-in for naming the folder into which a
 * transformation (a.k.a. ODI interface) is inserted. It is used to implement
 * the default naming policy and a custom policy. The default policy plug-in is
 * always executed before the custom plug-in is executed.
 *
 */
public interface FolderNameStrategy {

    /**
     * This method determines the name of folder using the provided execution
     * context for this plug-in feature.
     *
     * @param defaultFolderName is <code>null</code> when passed into the default strategy;
     *                          the value is the result of the default strategy when passed to
     *                          the custom strategy. In this case the value will not be
     *                          <code>null</code>.
     * @param execContext       execution context object that provides contextual information
     *                          related to the folder name decision.
     * @return the name of the folder
     * @throws IncorrectCustomStrategyException if folder name is <code>null</code> or empty String
     */
    // jkm
    String getFolderName(String defaultFolderName, FolderNameExecutionContext execContext, final boolean isJournalizedData);
    // jkm
}
