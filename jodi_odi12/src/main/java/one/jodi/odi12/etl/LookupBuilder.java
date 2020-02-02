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
     * @param mapping mapping
     * @param sources sources
     * @param journalized flag indicating journalization
     * @param joiners list of join components
     * @param etlOperators etl operator collections
     * @throws AdapterException exception from the adapter
     * @throws MappingException exception from the mapping
     * @throws TransformationAccessStrategyException excepction while accessing the transformation
     * @throws AdapterException exception from the adapter
     * @throws MappingException exception from the mapping
     */
    public abstract void addLookups(MapRootContainer mapping,
                                    List<Source> sources, boolean journalized,
                                    List<JoinComponent> joiners, EtlOperators etlOperators)
            throws AdapterException, MappingException,
            TransformationAccessStrategyException, ResourceNotFoundException,
            ResourceFoundAmbiguouslyException;

}