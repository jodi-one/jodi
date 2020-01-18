package one.jodi.odi.loadplan.service;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.etl.service.loadplan.internalmodel.LoadPlanDetails;
import one.jodi.odi.loadplan.*;
import oracle.odi.domain.IOdiEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OdiLoadPlanValidationServiceImpl<B extends IOdiEntity> implements OdiLoadPlanValidationService {

    private final Logger logger = LogManager.getLogger(OdiLoadPlanValidationServiceImpl.class);
    private final ErrorWarningMessageJodi errorWarningMessage;
    private final OdiLoadPlanValidator loadPlanValidator;

    @Inject
    public OdiLoadPlanValidationServiceImpl(final OdiLoadPlanValidator loadPlanValidator, final ErrorWarningMessageJodi errorWarningMessage) {
        this.loadPlanValidator = loadPlanValidator;
        this.errorWarningMessage = errorWarningMessage;
    }

    /**
     * Validate the internal model of a loadplan, by the visitor pattern, that visits ever node.
     * The validator is reset on each loadplan.
     */
    @Override
    public boolean validate(final OdiLoadPlanTree<Odiloadplanstep> pLoadPlanTree) {
        // logger.info("Validating loadplan: "+ pLoadPlanTree.getRoot().getName());
        //loadPlanValidator.reset(loadPlanName);
        pLoadPlanTree.accept(loadPlanValidator);
        OdiLoadPlanVisitor vistor = loadPlanValidator.visit(pLoadPlanTree);
        // for findbugs;
        assert (vistor != null);
        return loadPlanValidator.isValid();
    }

    /**
     * Validate the internal model of a loadplan; the details like name and folder.
     */
    @Override
    public boolean validate(LoadPlanDetails pLoadPlanDetails) {
        assert (pLoadPlanDetails != null);
        logger.info("Validating loadplan details: " + pLoadPlanDetails.getLoadPlanName());
        boolean result = true;
        if (pLoadPlanDetails.getLoadPlanName() == null) {
            String message = "Loadplanname can't be null.";
            logger.error(message);
            result = false;
            errorWarningMessage.addMessage(message, MESSAGE_TYPE.ERRORS);
        }
        if (pLoadPlanDetails.getFolderName() == null) {
            String message = "Folder can't be null for loadplan: " + pLoadPlanDetails.getLoadPlanName();
            logger.error(message);
            result = false;
            errorWarningMessage.addMessage(message, MESSAGE_TYPE.ERRORS);
        }
        return result;
    }

    @Override
    public void reset(String loadPlanName) {
        this.loadPlanValidator.reset(loadPlanName);
    }

}
