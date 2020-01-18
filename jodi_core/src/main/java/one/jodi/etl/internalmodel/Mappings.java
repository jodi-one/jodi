package one.jodi.etl.internalmodel;

import one.jodi.core.extensions.strategies.KnowledgeModuleStrategy;
import one.jodi.core.extensions.strategies.ModelCodeStrategy;
import one.jodi.core.extensions.strategies.TransformationNameStrategy;
import one.jodi.model.extensions.MappingsExtension;

import java.util.List;

/**
 * A Source describes a data store or a Temporary Interface as part of a {@link Dataset} definition. Sources are combined
 * with filter, join and lookup operations.
 *
 */
public interface Mappings {
    /**
     * Convenience method used to fetch parent object.
     *
     * @return parent
     */
    Transformation getParent();

    /**
     * Returns value for distinct optionally specified in <code>Transformation/Mappings/Distinct</code> node.
     * When set to true this enforces distinct option for rows inserted into target data store.
     *
     * @return is distinct
     */
    boolean isDistinct();

    /**
     * Describes the model used for lookup's data store.  This may be specified in <code>Transformation/Datasets/Dataset/Source/Lookup/Model</code>
     * or, when not explicitly specified, derived by Jodi using the {@link ModelCodeStrategy}
     *
     * @return model name
     */
    String getModel();

    /**
     * Describes the staging model used only for multi-technology IKMs.  This is required if all sources are on external systems.
     *
     * @return staging model
     */
    String getStagingModel();

    /**
     * Name of the target data store, which must be explicitly defined in the <code>Transformation/Mappings/TargetDataStore<code>
     *
     * @return target data store name
     */
    String getTargetDataStore();

    /**
     * Fetch the list of target column mappings in target data store.  This may be explicitly defined
     * using the <code>Transformation/Mappings/TargetColumn</code> nodes or may be deduced by Jodi using
     * auto-mapping, when source and target data stores have identical column names.
     *
     * @return list of target columns
     */
    List<Targetcolumn> getTargetColumns();


    /**
     * Fetches the IKM used to write data to target.  The may be explicitly defined using <code>Transformation/Datasets/Dataset/Source/Lookup/LKM</code>
     * or, when not explicitly defined, be derived using rules based approach
     * defined the Jodi properties file.  Extensions using the plug in {@link KnowledgeModuleStrategy}
     * may change that behavior.
     *
     * @return IKM
     */
    KmType getIkm();

    /**
     * Fetches the CKM used to check data for target mapping.  The may be explicitly defined using <code>Transformation/Datasets/Dataset/Source/Lookup/LKM</code>
     * or, when not explicitly defined, be derived using rules based approach
     * defined the Jodi properties file.  Extensions using the plug in {@link KnowledgeModuleStrategy}
     * may change that behavior.
     *
     * @return CKM
     */
    KmType getCkm();

    /**
     * Fetch the mappings extensions optionally provided by Jodi customization
     *
     * @return extension
     */
    MappingsExtension getExtension();

    /**
     * Fetches the name of the target's temporary data store when applicable.  This value is set by Jodi and may be customized by creating
     * and wiring in {@link TransformationNameStrategy}
     *
     * @return temporary data store name
     */
    //String getTemporaryDataStore();

    /**
     * Indicating whether this is an aggregate transformation as per SQL documentation.
     * {@link "http://docs.oracle.com/database/121/SQLRF/functions003.htm#SQLRF20035"}
     *
     * @param dataSetNumber the number of the dataset for set operations, starts with 1.
     * @return presence of aggregate expression in one of the targetcolumns
     */
    boolean isAggregateTransformation(int dataSetNumber);

    /**
     * There is at least one update key in one or more of the target columns.
     *
     * @return indicating valid or not
     */
    boolean hasUpdateKeys();
}

