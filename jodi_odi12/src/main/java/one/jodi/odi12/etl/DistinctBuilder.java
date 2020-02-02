package one.jodi.odi12.etl;

import one.jodi.etl.internalmodel.Transformation;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.mapping.MapRootContainer;
import oracle.odi.domain.mapping.component.DistinctComponent;
import oracle.odi.domain.mapping.exception.MappingException;

import java.util.List;

public interface DistinctBuilder {

    /**
     * Add the empty distinct component if used.
     *
     * @param mapping
     * @param transformation
     * @param etlOperators
     * @throws AdapterException
     * @throws MappingException
     */
    public void addDistinct(MapRootContainer mapping,
                            Transformation transformation, EtlOperators etlOperators)
            throws AdapterException, MappingException;

    /**
     * Set the distinct component and populate its attributes.
     *
     * @param transformation
     * @param distinctComponents
     * @param useExpressions
     * @throws AdapterException
     * @throws MappingException
     */
    public void setDistinct(Transformation transformation,
                            List<DistinctComponent> distinctComponents, boolean useExpressions)
            throws AdapterException, MappingException;

}