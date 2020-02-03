package one.jodi.core.transformation;

import one.jodi.etl.internalmodel.Lookup;
import one.jodi.etl.internalmodel.Source;
import one.jodi.etl.internalmodel.Transformation;


/**
 * Interface of the context object for the strategy to define the name of the
 * transformation. It is mostly used as an interface to facilitate Inversion
 * of Control. The interface is passed to the ODI layer and the proper implementation
 * is injected using Guice. This allows the ODI-layer to call the Jodi core functionality
 * without breaking architecture / dependency rules.
 */
public interface TransformationNameContext {

    /**
     * Determines the name of the interface used within the ETL tool.
     *
     * @param transformation - transformation object that contains the specification
     * @return - transformation name as it will be used within the ETL tool
     */
    String getTransformationName(final Transformation transformation);

    String setSourceName(final Source source);

    String setLookupName(final Lookup lookup);


    String getTemporaryDSLookup(final Lookup lookup);

    String getTemporaryDSSource(final Source source);

    String getTemporaryDSTarget(final Transformation transformation);

}
