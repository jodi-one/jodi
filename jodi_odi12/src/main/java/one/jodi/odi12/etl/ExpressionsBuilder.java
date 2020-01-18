package one.jodi.odi12.etl;

import one.jodi.etl.internalmodel.Transformation;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.mapping.IMapComponent;
import oracle.odi.domain.mapping.MapRootContainer;
import oracle.odi.domain.mapping.component.ExpressionComponent;
import oracle.odi.domain.mapping.exception.MapComponentException;
import oracle.odi.domain.mapping.exception.MappingException;
import oracle.odi.domain.model.OdiDataStore;

import java.util.List;

public interface ExpressionsBuilder {

    public void addTargetExpressions(MapRootContainer mapping,
                                     boolean useExpressions, EtlOperators etlOperators)
            throws MapComponentException, MappingException;

    public void createExpressionComponent(MapRootContainer mapping,
                                          Transformation transformation,
                                          List<ExpressionComponent> targetExpressions, boolean useExpressions)
            throws AdapterException, MappingException;

    public void setMappingFields(MapRootContainer mapping,
                                 Transformation transformation,
                                 List<IMapComponent> targetComponents, boolean useExpressions,
                                 OdiDataStore targetDataStore) throws AdapterException,
            MappingException;

}