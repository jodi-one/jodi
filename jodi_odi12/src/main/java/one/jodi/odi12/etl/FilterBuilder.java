package one.jodi.odi12.etl;

import one.jodi.etl.internalmodel.ComponentPrefixType;
import one.jodi.etl.internalmodel.Source;
import one.jodi.etl.service.ResourceNotFoundException;
import one.jodi.etl.service.interfaces.TransformationAccessStrategyException;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.mapping.MapRootContainer;
import oracle.odi.domain.mapping.exception.MappingException;

public interface FilterBuilder {

    public static String getFilterKeyForSource(final Source source, final boolean journalized) {
        String key = source.getComponentName();
        return ComponentPrefixType.FILTER.getAbbreviation() + "_" + key;
    }

    public void addFilter(MapRootContainer mapping, Source source,
                          boolean journalized, EtlOperators etlOperators)
            throws AdapterException, MappingException,
            TransformationAccessStrategyException, ResourceNotFoundException;

}