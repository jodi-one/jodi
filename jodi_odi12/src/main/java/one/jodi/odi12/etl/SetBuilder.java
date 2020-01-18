package one.jodi.odi12.etl;

import one.jodi.etl.internalmodel.Transformation;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.mapping.MapRootContainer;
import oracle.odi.domain.mapping.exception.MappingException;

public interface SetBuilder {

    /**
     * Add the empty set component if used
     *
     * @param transformation
     * @param mapping
     * @param useExpressions
     * @param etlOperators   @throws AdapterException @throws
     *                       MappingException
     */
    public void addSetComponent(Transformation transformation,
                                MapRootContainer mapping, boolean useExpressions,
                                EtlOperators etlOperators) throws AdapterException,
            MappingException;

}