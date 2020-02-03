package one.jodi.odi12.etl;

import one.jodi.etl.internalmodel.Transformation;
import one.jodi.etl.service.interfaces.TransformationException;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.mapping.IMapComponent;
import oracle.odi.domain.mapping.MapRootContainer;
import oracle.odi.domain.mapping.exception.MappingException;
import oracle.odi.mapping.generation.GenerationException;

import java.util.List;

public interface KMBuilder {

    /**
     * @param mapping
     * @param transformation
     * @throws MappingException
     * @throws AdapterException @throws GenerationException @throws
     *                          TransformationException
     */
    public void setLKM(MapRootContainer mapping,
                       Transformation transformation) throws MappingException,
            AdapterException, GenerationException, TransformationException;

    /**
     * @param mapping          the odi mapping
     * @param transformation   the transformation from textual specifications
     * @param targetComponents list of targetcomponents
     * @throws AdapterException        exception from the adapter
     * @throws MappingException        exception from the mapping
     * @throws GenerationException     exception while generating
     * @throws TransformationException exception while reading the transformation from textual specifications
     */
    public void setIKM(MapRootContainer mapping,
                       Transformation transformation, List<IMapComponent> targetComponents)
            throws AdapterException, MappingException, GenerationException,
            TransformationException;

    /**
     * @param mapping        odi mapping
     * @param transformation textual specification
     * @throws AdapterException        exception from adapter
     * @throws MappingException        exception from mapping
     * @throws GenerationException     exception while generating
     * @throws TransformationException exception while creating transformation
     */
    public void setCKM(MapRootContainer mapping,
                       Transformation transformation) throws AdapterException,
            MappingException, GenerationException, TransformationException;

}