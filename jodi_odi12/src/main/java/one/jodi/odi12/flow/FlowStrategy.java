package one.jodi.odi12.flow;

import one.jodi.etl.internalmodel.Lookup;
import one.jodi.etl.internalmodel.Source;
import one.jodi.odi12.etl.EtlOperators;
import oracle.odi.domain.mapping.IMapComponent;
import oracle.odi.domain.mapping.exception.MappingException;

public interface FlowStrategy {

    /**
     * To handle the next component means connecting the sourceComponent to the
     * next component.
     *
     * @param EtlOperator     all operators
     * @param source
     * @param lookup
     * @param sourceComponent the source component can be datastorecomponent,
     *                        filtercomponent, joincomponent or lookupcomponent etc.
     *                        depending on which type it is, it is handled accordingly.
     *                        <p>
     *                        either source or lookup is null; they can't both be null and
     *                        they can't both be not null.
     * @throws MappingException
     */
    void handleNextComponent(final EtlOperators EtlOperator, Source source, Lookup lookup,
                             IMapComponent sourceComponent) throws MappingException;
}
