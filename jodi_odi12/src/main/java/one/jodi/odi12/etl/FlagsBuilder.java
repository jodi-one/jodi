package one.jodi.odi12.etl;

import one.jodi.etl.internalmodel.Transformation;
import one.jodi.etl.service.interfaces.TransformationException;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.mapping.IMapComponent;
import oracle.odi.domain.mapping.exception.MappingException;
import oracle.odi.domain.mapping.properties.PropertyException;

import java.util.List;

public interface FlagsBuilder {

    /**
     * Set flags for update, insert, key and check not null. Use the info that
     * has been set in flagscontext and is enriched in internal model.
     *
     * @param transformation
     * @param targetComponents
     * @throws AdapterException
     * @throws PropertyException @throws MappingException @throws
     *                           TransformationException
     */
    public abstract void setFlags(Transformation transformation,
                                  List<IMapComponent> targetComponents) throws AdapterException,
            PropertyException, MappingException, TransformationException;

}