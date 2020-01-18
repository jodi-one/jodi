package one.jodi.etl.service;

import one.jodi.core.model.Mappings;
import one.jodi.core.model.Properties;
import one.jodi.core.model.Targetcolumn;
import one.jodi.core.model.Transformation;

public interface EtlDataStoreBuildService {

    /**
     * Create an ODI Datastore from a Transformation, the specifications of the
     * target {@link Mappings#getTargetDataStore()} are
     * taken into account.
     * <p>
     * Especially the {@link Targetcolumn} name and
     * {@link Properties} length, scale and datatype.
     *
     * @param Transformation
     */
    void build(Transformation t);

}
