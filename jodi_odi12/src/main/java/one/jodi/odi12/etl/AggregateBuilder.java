package one.jodi.odi12.etl;

import one.jodi.etl.internalmodel.Transformation;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.mapping.MapRootContainer;
import oracle.odi.domain.mapping.component.AggregateComponent;
import oracle.odi.domain.mapping.exception.MappingException;

import java.util.List;

public interface AggregateBuilder {

    /**
     * set aggregation columns and populate its attributes.
     *
     * @param transformation
     * @param aggregateComponents
     * @throws AdapterException
     * @throws MappingException
     */
    public void setAggregate(Transformation transformation,
                             List<AggregateComponent> aggregateComponents, int dataSetNumber)
            throws AdapterException, MappingException;

    /**
     * Add the empty aggregate component if used.  This must be configured using {@link #setAggregate(Transformation, List, int)}
     *
     * @param mapping
     * @param transformation
     * @param etlOperators
     * @throws AdapterException @throws MappingException
     */
    public void addAggregate(MapRootContainer mapping,
                             Transformation transformation, EtlOperators etlOperators,
                             int dataSetNumber) throws AdapterException, MappingException;

}