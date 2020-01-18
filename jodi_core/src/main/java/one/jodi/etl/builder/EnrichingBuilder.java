package one.jodi.etl.builder;

import one.jodi.base.context.Context;
import one.jodi.base.model.types.DataStore;
import one.jodi.core.automapping.ColumnMappingContext;
import one.jodi.core.datastore.ModelCodeContext;
import one.jodi.core.executionlocation.ExecutionLocationContext;
import one.jodi.core.folder.FolderNameContext;
import one.jodi.core.journalizing.JournalizingContext;
import one.jodi.core.km.KnowledgeModuleContext;
import one.jodi.core.targetcolumn.FlagsContext;
import one.jodi.core.transformation.TransformationNameContext;
import one.jodi.etl.internalmodel.Transformation;

/**
 * At the highest level this interface serves as the single point by which the internal model is enriched with information not
 * defined from the original external input model (XML).  After the sucessful call to {@link #enrich(Transformation, boolean)} the model
 * is considered fully populated and ready to be consumed by the Jodi ODI layer to create an ODI interface.
 * <p>
 * The implementor will delegate the work of deriving information to
 * the various Jodi plugins/contexts
 * <ul>
 * <li>{@link ColumnMappingContext}</li>
 * <li>{@link ExecutionLocationContext}</li>
 * <li>{@link FlagsContext}</li>
 * <li>{@link FolderNameContext}</li>
 * <li>{@link JournalizingContext}</li>
 * <li>{@link KnowledgeModuleContext}</li>
 * <li>{@link ModelCodeContext}</li>
 * <li>{@link TransformationNameContext}</li>
 * </ul>
 * <p>
 * The implementor is responsible for orchestrating the order of calls to plugins so that necessary dependent information required for
 * making the derivation is obtained.  Orchestration also must account for the order that Jodi {@link DataStore}s
 * are created from the input model so that both contexts and strategies can acquire from {@link Context}.
 * <p>
 * <p>
 * The interface also defines a separate method to create information required to delete a transformation, the {@link #createDeleteContext(Transformation, boolean)}
 *
 */
public interface EnrichingBuilder {

    /**
     * Traverses Transformation tree and populates values not specified in input model.
     * This uses the full set of Jodi plugins to compute derived information
     *
     * @param transformation    root of object tree
     * @param isJournalizedData specifies if the ETL build process should be journalized
     * @return the fully populated transformation.
     */
    Transformation enrich(Transformation transformation, boolean isJournalizedData);

    /**
     * Create a context used for deleting the consumed Transformation.  The consumed Transformation
     * should be un-enriched.  All values supplied in {@link DeleteTransformationContext} will be derived
     * by Jodi unless supplied in incoming {@link Transformation}
     *
     * @param transformation
     * @return context
     */
    DeleteTransformationContext createDeleteContext(Transformation transformation, boolean isJournalizedData);


    boolean isTemporaryTransformation(String tableName);

}
