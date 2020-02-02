package one.jodi.core.datastore;

import one.jodi.etl.internalmodel.Lookup;
import one.jodi.etl.internalmodel.Mappings;
import one.jodi.etl.internalmodel.Source;
import one.jodi.etl.internalmodel.SubQuery;


/**
 * Interface of the context object for the strategy to define the model code of
 * the data store. It is mostly used as an interface to facilitate Inversion of
 * Control. The interface is passed to the appropriate class and the proper
 * implementation is injected using Guice.
 *
 */
public interface ModelCodeContext {

    /**
     * Determines the model code of a source data store.
     *
     * @param source Source object for which a model needs to be determined
     * @return selected model code as referenced in within the ETL tool
     */
    String getModelCode(final Source source);

    /**
     * Determines the model code for a lookup data store.
     *
     * @param lookup Lookup object for which a model needs to be determined
     * @return selected model code as referenced in within the ETL tool
     */
    String getModelCode(final Lookup lookup);

    /**
     * Determines the model code for a target data store.
     *
     * @param target Mapping containing the target store definition for which
     *               a model needs to be determined
     * @return selected model code as referenced in within the ETL tool
     */
    String getModelCode(final Mappings target);

    /**
     * Determines the model for a Begin or EndCommand Location.
     *
     * @param model in odi
     * @return select model code as referenced in within the ETL tool
     */
    String getModelCode(String model);

    /**
     * Determines model code for data source in subQuery.
     *
     * @param subQuery
     * @return modelCode
     */
    String getModelCode(final SubQuery subQuery);

}
