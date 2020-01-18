package one.jodi.odi12.loadplan;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.odi.loadplan.OdiLoadPlanAccessStrategy;
import one.jodi.odi.loadplan.OdiLoadPlanValidator;
import oracle.odi.domain.mapping.Mapping;
import oracle.odi.domain.runtime.loadplan.OdiLoadPlan;

public class Odi12LoadPlanValidator extends OdiLoadPlanValidator {

    @Inject
    public Odi12LoadPlanValidator(OdiLoadPlanAccessStrategy<OdiLoadPlan, Mapping> odiLoadPlanAccessStrategy,
                                  ErrorWarningMessageJodi errorWarningMessages) {
        super(odiLoadPlanAccessStrategy, errorWarningMessages);
    }

}
