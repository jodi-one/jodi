package one.jodi.odi12.etl;

import one.jodi.etl.internalmodel.Transformation;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.mapping.MapRootContainer;
import oracle.odi.domain.mapping.exception.MappingException;

public interface SetBuilder {

    /**
     * Add the empty set component if used
     *
     * @param transformation transformation
     * @param mapping        mapping
     * @param useExpressions flag indicating use of expressions
     * @param etlOperators   etlOperators
     * @throws AdapterException the exception of the adapter
     * @throws MappingException the exception of the mapping
     */
    public void addSetComponent(Transformation transformation,
                                MapRootContainer mapping, boolean useExpressions,
                                EtlOperators etlOperators) throws AdapterException,
            MappingException;

}