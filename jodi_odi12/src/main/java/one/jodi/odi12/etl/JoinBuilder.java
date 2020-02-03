package one.jodi.odi12.etl;

import one.jodi.etl.internalmodel.Transformation;
import one.jodi.etl.service.ResourceFoundAmbiguouslyException;
import one.jodi.etl.service.ResourceNotFoundException;
import one.jodi.etl.service.interfaces.TransformationAccessStrategyException;
import one.jodi.etl.service.interfaces.TransformationException;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.mapping.MapRootContainer;
import oracle.odi.domain.mapping.component.FilterComponent;
import oracle.odi.domain.mapping.exception.MapPhysicalException;
import oracle.odi.domain.mapping.exception.MappingException;

import java.util.List;

public interface JoinBuilder {

    /**
     * Add sources filter, joins and lookups; in that specific order; first
     * source than filter then more source then joins and then lookups. process
     * datasets at a time.
     *
     * @param mapping          odiMapping
     * @param transformation   transformation from textual specifications
     * @param journalized      indicating journalization
     * @param filterComponents filter components in odi
     * @param etlOperators     etlOperators
     * @param useExpressions   indicating the use of expressions in odi
     * @throws AdapterException                      exception from the adapter
     * @throws MapPhysicalException                  exception from odi
     * @throws MappingException                      exception from odi
     * @throws TransformationAccessStrategyException exception while accessing transformations
     * @throws TransformationException               exception while generating mappings
     * @throws ResourceFoundAmbiguouslyException     exception finding a resource
     * @throws ResourceNotFoundException             exception the resource is not found
     */
    public abstract void addDatasets(MapRootContainer mapping,
                                     Transformation transformation, boolean journalized,
                                     List<FilterComponent> filterComponents,
                                     EtlOperators etlOperators, boolean useExpressions)
            throws AdapterException, MapPhysicalException, MappingException,
            TransformationAccessStrategyException, TransformationException,
            ResourceNotFoundException, ResourceFoundAmbiguouslyException;

}