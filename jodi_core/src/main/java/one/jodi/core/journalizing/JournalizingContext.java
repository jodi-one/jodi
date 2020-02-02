package one.jodi.core.journalizing;

import one.jodi.etl.internalmodel.Lookup;
import one.jodi.etl.internalmodel.Source;
import one.jodi.etl.journalizng.JournalizingConfiguration;

import java.util.List;

/**
 * Interface of the context object for the strategy to define the Journalizing
 * Context. It is mostly used as an interface to facilitate Inversion of
 * Control. The interface is passed to the appropriate class and the proper
 * implementation is injected using Guice.
 *
 */
public interface JournalizingContext {
    /**
     * @param source source within transformation
     * @return indication of journalized source
     */
    boolean isJournalizedSource(Source source);

    /**
     * @param lookup the lookup
     * @return indication of journalized {@link Lookup}
     */
    boolean isJournalizedLookup(Lookup lookup);

    /**
     * Retrieves the JournalizingConfiguration for a model
     *
     * @return List<{ @ link JournalizingConfiguration }>
     */
    List<JournalizingConfiguration> getJournalizingConfiguration();

}
