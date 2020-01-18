package one.jodi.odi12.etl;

import one.jodi.etl.internalmodel.Source;
import one.jodi.etl.service.ResourceFoundAmbiguouslyException;
import one.jodi.etl.service.ResourceNotFoundException;
import one.jodi.etl.service.interfaces.TransformationAccessStrategyException;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.adapter.IModelObject;
import oracle.odi.domain.mapping.IMapComponent;
import oracle.odi.domain.mapping.MapRootContainer;
import oracle.odi.domain.mapping.exception.MapPhysicalException;
import oracle.odi.domain.mapping.exception.MappingException;

public interface DatastoreBuilder {

    public void addDatasource(MapRootContainer mapping, Source source,
                              int packageSequence, boolean journalized, EtlOperators etlOperators)
            throws AdapterException, MapPhysicalException, MappingException,
            TransformationAccessStrategyException, ResourceNotFoundException,
            ResourceFoundAmbiguouslyException;

    /**
     * Create a FILECOMPONENT, DATASTORE_COMPONENT or REUSABLEMAPPING_COMPONENT.
     *
     * @param mapping
     * @param boundObject
     * @param autoJoinEnabled
     * @return @throws AdapterException @throws MappingException
     */
    public IMapComponent createComponent(MapRootContainer mapping,
                                         IModelObject boundObject, boolean autoJoinEnabled)
            throws AdapterException, MappingException;

}