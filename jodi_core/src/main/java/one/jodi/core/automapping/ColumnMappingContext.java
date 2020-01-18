package one.jodi.core.automapping;

import one.jodi.etl.internalmodel.Transformation;

import java.util.List;
import java.util.Map;


/**
 * Interface of the context object for the strategy to define the target mapping expressions.
 * It is mostly used as an interface to facilitate Inversion of Control. The interface is
 * passed to the appropriate class and the proper implementation is injected using Guice.
 *
 */
public interface ColumnMappingContext {

    /**
     * Determine the mapping expressions given the transformations sources and target.  When
     * explicitly defined using TargetColumn, the column mapping will be omitted.
     *
     * @param transformation
     * @return map of source to target column, e.g. <code>column -> datastore.column</code>
     */
    Map<String, List<String>> getMappings(Transformation transformation);

}
