package one.jodi.etl.internalmodel;

import java.util.List;


/**
 * The Dataset class defines its Source data stores and a set operation with the Dataset previous defined in
 * the enclosing Datasets element. It has optional elements to overwrite Source models and ODI Load Knowledge
 * Modules (LKMs). One important consideration is that set operations are not applied to the source datasets
 * but the target datasets as defined by the MappingsExpressions in the Mappings element. This implies the set
 * operations are applied to the data after its transformation from the source datasets to the target data store.
 *
 */
public interface Dataset {
    /**
     * Convenience method which provides the parent object of this Dataset.
     *
     * @return transformation
     */
    Transformation getParent();

    /**
     * Fetches the name of of dataset, the source data store.  This may be explicitly defined in the input models <code>Transformation/Datasets/Dataset/Name</code>
     * node, or, when not defined, computed by Jodi using the name of the first Source from {@link #getSources()}.
     *
     * @return data source name
     */
    String getName();

    /**
     * Fetches the list of {@link Source}s used in the definition of the dataset describing source data stores.
     * <p>
     * This call will always return a non-null list, however only a properly constructed Dataset will have one or
     * more Sources.
     *
     * @return list of sources
     */
    List<Source> getSources();

    /**
     * Gets the set operator for combining datasets.
     *
     * @return set operator
     */
    SetOperatorTypeEnum getSetOperator();

    int getDataSetNumber();

    Source getDriverSourceInDataset();

    List<? extends Source> findJoinedSourcesInDataset();

    String translateExpression(String exprText);
}

