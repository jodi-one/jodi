package one.jodi.core.extensions.types;

import one.jodi.base.model.types.DataStore;
import one.jodi.model.extensions.SourceExtension;

public interface DataStoreWithAlias {

    /**
     * Extends the {@link DataStore} type by alias name used in the XML
     * specification. This interface should be used when referring to data
     * stores that are specified as sources or lookup data stores.
     *
     * @return alias of the data store defined in the XML transformation
     * specification.
     */
    public String getAlias();

    public DataStore getDataStore();

    /**
     * Fetches the type of DataStore represented.
     *
     * @return type
     */
    public Type getType();

    /**
     * Fetches the SourceExtension.
     *
     * @return Source extension
     */
    public SourceExtension getSourceExtension();

    public enum Type {Source, Lookup, Filter}

}
