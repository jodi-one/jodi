package one.jodi.odi12.loadplan;

import com.google.inject.Inject;
import one.jodi.odi.loadplan.OdiLoadPlanAccessStrategy;
import one.jodi.odi.variables.OdiVariableAccessStrategy;
import oracle.odi.core.OdiInstance;
import oracle.odi.domain.mapping.Mapping;
import oracle.odi.domain.mapping.finder.IMappingFinder;
import oracle.odi.domain.runtime.loadplan.OdiLoadPlan;

import java.io.Serializable;

public class Odi12LoadPlanAccessStrategy
        extends OdiLoadPlanAccessStrategy<OdiLoadPlan, Mapping> {

    private final OdiInstance odiInstance;

    @Inject
    public Odi12LoadPlanAccessStrategy(final OdiInstance odiInstance,
                                       final OdiVariableAccessStrategy odiVariableService) {
        super(odiInstance, odiVariableService);
        this.odiInstance = odiInstance;
    }

    @Override
    public Mapping findMapping(Serializable sourceComponentId) {
        IMappingFinder finder =
                (IMappingFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(Mapping.class);
        return (Mapping) finder.findById(sourceComponentId);

    }

}
