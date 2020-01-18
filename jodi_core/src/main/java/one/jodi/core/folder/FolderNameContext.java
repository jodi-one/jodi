package one.jodi.core.folder;

import one.jodi.etl.internalmodel.Transformation;


/**
 * Interface of the context object for the strategy to define the folder name of
 * the transformation. It is mostly used as an interface to facilitate Inversion
 * of Control. The interface is passed to the appropriate class and the proper
 * implementation is injected using Guice.
 *
 */
public interface FolderNameContext {

    /**
     * Determines the folder name into which the transformation is inserted
     * within the ETL tool.
     *
     * @param transformation transformation object that contains specification
     * @return folder name as it will be used within the ETL tool
     */
    String getFolderName(Transformation transformation, final boolean isJournalizedData);
}
