package one.jodi.odi12.service;

import com.google.inject.Inject;
import one.jodi.base.annotations.Nullable;
import one.jodi.base.annotations.XmlFolderName;
import one.jodi.core.config.JodiProperties;
import one.jodi.etl.service.loadplan.internalmodel.LoadPlanDetails.ViolatebehaviourType;
import one.jodi.odi.loadplan.OdiLoadPlanAccessStrategy;
import one.jodi.odi.loadplan.OdiLoadPlanValidationService;
import one.jodi.odi.loadplan.service.OdiLoadPlanExportServiceImpl;
import oracle.odi.domain.mapping.Mapping;
import oracle.odi.domain.runtime.common.CecBehavior;
import oracle.odi.domain.runtime.loadplan.OdiLoadPlan;

import java.math.BigInteger;

@SuppressWarnings("rawtypes")
public class Odi12LoadPlanExportService extends OdiLoadPlanExportServiceImpl {


    @SuppressWarnings("unchecked")
    @Inject
    public Odi12LoadPlanExportService(OdiLoadPlanAccessStrategy<OdiLoadPlan, Mapping> odiAccessStrategy,
                                      @Nullable @XmlFolderName String metadataFolder, OdiLoadPlanValidationService loadPlanValidationService,
                                      JodiProperties jodiProperties) {
        super(odiAccessStrategy, metadataFolder, loadPlanValidationService, jodiProperties);
    }

    @Override
    public BigInteger getWaitPollingInterval(OdiLoadPlan odiLoadPlan) {
        return odiLoadPlan.getCecLprPollIntv() == null ? BigInteger.ZERO : BigInteger.valueOf(odiLoadPlan.getCecLprPollIntv().longValue());
    }

    @Override
    public ViolatebehaviourType getViolateBehaviorType(OdiLoadPlan odiLoadPlan) {
        ViolatebehaviourType violatebehaviourType;
        if (odiLoadPlan.getCecLprBehavior().equals(CecBehavior.WAIT)) {
            violatebehaviourType = ViolatebehaviourType.WAIT_TO_EXECUTE;
        } else if (odiLoadPlan.getCecLprBehavior().equals(CecBehavior.ERROR)) {
            violatebehaviourType = ViolatebehaviourType.RAISE_EXECUTION_ERROR;
        } else {
            violatebehaviourType = ViolatebehaviourType.WAIT_TO_EXECUTE;
        }
        return violatebehaviourType;
    }

    @Override
    public boolean getLimitconcurrentExecutions(OdiLoadPlan odiLoadPlan) {
        boolean limitconccurentexecutions;
        if (odiLoadPlan.getMaxCecLpr() == null || odiLoadPlan.getMaxCecLpr().intValue() == 0) {
            limitconccurentexecutions = false;
        } else {
            limitconccurentexecutions = true;
        }
        return limitconccurentexecutions;
    }

    @Override
    public Number getNumberOfConcurrentexecution(OdiLoadPlan odiLoadPlan) {
        return odiLoadPlan.getMaxCecLpr();
    }

}
