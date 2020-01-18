package one.jodi.odi12.etl;

import one.jodi.etl.internalmodel.Source;
import one.jodi.etl.service.ResourceFoundAmbiguouslyException;
import one.jodi.etl.service.ResourceNotFoundException;
import one.jodi.etl.service.interfaces.TransformationAccessStrategyException;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.mapping.MapRootContainer;
import oracle.odi.domain.mapping.component.JoinComponent;
import oracle.odi.domain.mapping.exception.MappingException;

import java.util.List;

public interface LookupBuilder {

    /**
     * Daisy chain the lookups, the first lookup is joined to source (1 source),
     * or joined to filter (1 source), or joined to the inner joined source (2
     * or more sources), the second and all lookups after the first are joined
     * to the previous lookup.
     *
     * @param mapping
     * @param sources
     * @param journalized
     * @param joiners     @param etlOperators @throws
     *                    AdapterException @throws MappingException @throws
     *                    TransformationAccessStrategyException
     * @throws ResourceNotFoundException
     * @throws ResourceFoundAmbiguouslyException
     */
    public abstract void addLookups(MapRootContainer mapping,
                                    List<Source> sources, boolean journalized,
                                    List<JoinComponent> joiners, EtlOperators etlOperators)
            throws AdapterException, MappingException,
            TransformationAccessStrategyException, ResourceNotFoundException,
            ResourceFoundAmbiguouslyException;

}