package one.jodi.odi12.etl;

import one.jodi.etl.internalmodel.Source;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.etl.service.ResourceFoundAmbiguouslyException;
import one.jodi.etl.service.ResourceNotFoundException;
import one.jodi.etl.service.interfaces.TransformationAccessStrategyException;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.mapping.IMapComponent;
import oracle.odi.domain.mapping.MapRootContainer;
import oracle.odi.domain.mapping.exception.MappingException;

import java.util.List;
import java.util.Map;

public interface FlowsBuilder {

    public void addFlow(MapRootContainer mapping, Source source,
                        EtlOperators etlOperators, boolean journalized)
            throws AdapterException, ResourceNotFoundException,
            MappingException, TransformationAccessStrategyException,
            ResourceFoundAmbiguouslyException;

    public void setFlows(Transformation transformation,
                         Map<Source, List<IMapComponent>> flowComponents,
                         boolean useExpressions) throws AdapterException, MappingException;

}