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
     * @param mapping
     * @param transformation
     * @param journalized
     * @param filterComponentsss @param etlOperators @param
     *                           useExpressions @throws AdapterException @throws
     *                           MapPhysicalException @throws MappingException @throws
     *                           TransformationAccessStrategyException @throws
     *                           TransformationException
     * @throws ResourceFoundAmbiguouslyException
     * @throws ResourceNotFoundException
     */
    public abstract void addDatasets(MapRootContainer mapping,
                                     Transformation transformation, boolean journalized,
                                     List<FilterComponent> filterComponentsss,
                                     EtlOperators etlOperators, boolean useExpressions)
            throws AdapterException, MapPhysicalException, MappingException,
            TransformationAccessStrategyException, TransformationException,
            ResourceNotFoundException, ResourceFoundAmbiguouslyException;

}