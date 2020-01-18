package one.jodi.odi12.service;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.core.config.JodiProperties;
import one.jodi.odi.loadplan.OdiLoadPlanAccessStrategy;
import one.jodi.odi.loadplan.OdiLoadPlanTransformationService;
import one.jodi.odi.loadplan.OdiLoadPlanValidationService;
import one.jodi.odi.loadplan.service.OdiLoadPlanService;
import oracle.odi.domain.mapping.Mapping;
import oracle.odi.domain.runtime.loadplan.OdiLoadPlan;

public class Odi12LoadPlanService extends OdiLoadPlanService {

    @Inject
    public Odi12LoadPlanService(final OdiLoadPlanAccessStrategy<OdiLoadPlan, Mapping> pOdiLoadPlanAccessStrategy,
                                final JodiProperties properties, final OdiLoadPlanValidationService loadPlanValidationService,
                                final OdiLoadPlanTransformationService odiLoadPlanTransformationService,
                                final ErrorWarningMessageJodi errorWarningMessageJodi) {
        super(pOdiLoadPlanAccessStrategy, properties, loadPlanValidationService, odiLoadPlanTransformationService,
                errorWarningMessageJodi);
    }

}
