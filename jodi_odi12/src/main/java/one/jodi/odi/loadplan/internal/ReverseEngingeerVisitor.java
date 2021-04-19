package one.jodi.odi.loadplan.internal;

import one.jodi.core.config.JodiConstants;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.lpmodel.Loadplan;
import one.jodi.core.lpmodel.Loadplanbranchesteptype;
import one.jodi.core.lpmodel.Loadplancoresteptype;
import one.jodi.core.lpmodel.Loadplanstepcasetype;
import one.jodi.core.lpmodel.Loadplanstepelsetype;
import one.jodi.core.lpmodel.Loadplanstepexceptiontype;
import one.jodi.core.lpmodel.Loadplanstepparalleltype;
import one.jodi.core.lpmodel.Loadplanstepscenariotype;
import one.jodi.core.lpmodel.Loadplanstepserialtype;
import one.jodi.core.lpmodel.Loadplansteptype;
import one.jodi.core.lpmodel.Loadplanstepwhentype;
import one.jodi.core.lpmodel.LogsessionsType;
import one.jodi.core.lpmodel.LogsessionstepType;
import one.jodi.core.lpmodel.Restarttypeparallel;
import one.jodi.core.lpmodel.Restarttypescenario;
import one.jodi.core.lpmodel.Restarttypeserial;
import one.jodi.core.lpmodel.Variables;
import one.jodi.core.lpmodel.ViolatebehaviorType;
import one.jodi.core.lpmodel.impl.ChildrenImpl;
import one.jodi.core.lpmodel.impl.ExceptionsImpl;
import one.jodi.core.lpmodel.impl.LoadplanstepcasetypeImpl;
import one.jodi.core.lpmodel.impl.LoadplanstepelsetypeImpl;
import one.jodi.core.lpmodel.impl.LoadplanstepexceptiontypeImpl;
import one.jodi.core.lpmodel.impl.LoadplanstepparalleltypeImpl;
import one.jodi.core.lpmodel.impl.LoadplanstepscenariotypeImpl;
import one.jodi.core.lpmodel.impl.LoadplanstepserialtypeImpl;
import one.jodi.core.lpmodel.impl.LoadplanstepwhentypeImpl;
import one.jodi.core.lpmodel.impl.ObjectFactory;
import one.jodi.core.lpmodel.impl.VariableImpl;
import one.jodi.core.lpmodel.impl.VariablesImpl;
import one.jodi.etl.service.loadplan.internalmodel.LoadPlanDetails;
import one.jodi.etl.service.loadplan.internalmodel.LoadPlanDetails.LogsessionType;
import one.jodi.etl.service.loadplan.internalmodel.LoadPlanStepType;
import one.jodi.etl.service.loadplan.internalmodel.RestartType;
import one.jodi.etl.service.loadplan.internalmodel.Variable;
import one.jodi.odi.loadplan.OdiLoadPlanTree;
import one.jodi.odi.loadplan.OdiLoadPlanVisitor;
import one.jodi.odi.loadplan.Odiloadplanstep;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * The class traverses the internal model of a loadplantree, and transforms it
 * into an external model of a loadplan, which is represented by the xml file.
 * The visitor pattern is used to traverse the internal model tree.
 */
public class ReverseEngingeerVisitor implements OdiLoadPlanVisitor<Odiloadplanstep> {

    private static final Logger logger = LogManager.getLogger(ReverseEngingeerVisitor.class);
    private final File file;
    private final Loadplan target;
    private final Map<String, Loadplansteptype> node = new HashMap<>();
    private final Map<String, Loadplanbranchesteptype> nodeBranche = new HashMap<>();
    private final JodiProperties jodiProperties;
    private final int indent;

    public ReverseEngingeerVisitor(final File file, final Loadplan loadPlan, final LoadPlanDetails loadPlanDetails,
                                   int indent, final JodiProperties jodiProperties) {

        this.file = file;
        this.target = loadPlan;
        if (loadPlanDetails.getFolderName() != null) {
            this.target.setFolder(loadPlanDetails.getFolderName());
        }
        if (loadPlanDetails.getLoadPlanName() != null) {
            this.target.setName(loadPlanDetails.getLoadPlanName());
        }
        this.target.setKeeploghistory(BigInteger.valueOf(loadPlanDetails.getKeeplogHistory()));
        this.target.setLogsessions(mapFrom(loadPlanDetails.getLogsessionType()));
        this.target.setLogsessionstep(mapFrom(loadPlanDetails.getLogsessionstepType()));
        this.target.setSessiontaskloglevel(BigInteger.valueOf(loadPlanDetails.getSessiontaskloglevel()));
        this.target.setKeywords(loadPlanDetails.getKeywords());
        this.target.setLimitconcurrentexecutions(loadPlanDetails.isLimitconcurrentexecutions());
        this.target.setNumberOfConcurrentexecutions(
                (loadPlanDetails.isLimitconcurrentexecutions() ? BigInteger.ONE : BigInteger.ZERO));
        this.target.setViolateBehavior(mapFrom(loadPlanDetails.getViolatebehaviourType()));
        this.target.setWaitpollinginterval(loadPlanDetails.getWaitpollinginterval());
        this.target.setDescription(loadPlanDetails.getDescription());
        this.indent = indent;
        this.jodiProperties = jodiProperties;
        // this is equal to setting the root to null,
        // since this is root;
    }

    public Loadplan getTarget() {
        return target;
    }

    private ViolatebehaviorType mapFrom(LoadPlanDetails.ViolatebehaviourType violatebehaviourType) {
        final ViolatebehaviorType aViolatebehaviourType;
        if (violatebehaviourType.equals(LoadPlanDetails.ViolatebehaviourType.WAIT_TO_EXECUTE)) {
            aViolatebehaviourType = ViolatebehaviorType.WAIT_TO_EXECUTE;
        } else if (violatebehaviourType.equals(LoadPlanDetails.ViolatebehaviourType.RAISE_EXECUTION_ERROR)) {
            aViolatebehaviourType = ViolatebehaviorType.RAISE_EXECUTION_ERROR;
        } else {
            throw new UnsupportedOperationException();
        }
        return aViolatebehaviourType;
    }

    private LogsessionstepType mapFrom(LoadPlanDetails.LogsessionstepType logsessionstepType) {
        final LogsessionstepType alogsessionstepType;
        if (logsessionstepType.equals(LoadPlanDetails.LogsessionstepType.BYSCENARIOSETTINGS)) {
            alogsessionstepType = LogsessionstepType.BYSCENARIOSETTINGS;
        } else if (logsessionstepType.equals(LoadPlanDetails.LogsessionstepType.ERRORS)) {
            alogsessionstepType = LogsessionstepType.ERRORS;
        } else if (logsessionstepType.equals(LoadPlanDetails.LogsessionstepType.NEVER)) {
            alogsessionstepType = LogsessionstepType.NEVER;
        } else {
            throw new UnsupportedOperationException();
        }
        return alogsessionstepType;
    }

    private LogsessionsType mapFrom(LogsessionType logsessionType) {
        final LogsessionsType logsessionsType;
        if (logsessionType.equals(LogsessionType.ALWAYS)) {
            logsessionsType = LogsessionsType.ALWAYS;
        } else if (logsessionType.equals(LogsessionType.ERRORS)) {
            logsessionsType = LogsessionsType.ERRORS;
        } else if (logsessionType.equals(LogsessionType.NEVER)) {
            logsessionsType = LogsessionsType.NEVER;
        } else {
            throw new UnsupportedOperationException();
        }
        return logsessionsType;
    }

    @Override
    public OdiLoadPlanVisitor<Odiloadplanstep> visit(OdiLoadPlanTree<Odiloadplanstep> tree) {
        return this;
    }

    @Override
    public void visit(Odiloadplanstep loadPlanStep) {
        if (loadPlanStep != null) {
            if (loadPlanStep.getParent() != null && loadPlanStep.getParent()
                                                                .getRoot()
                                                                .getName() != null) {
                Loadplansteptype parent = getParentByName(loadPlanStep.getParent()
                                                                      .getRoot()
                                                                      .getName());
                if (loadPlanStep.getType()
                                .equals(LoadPlanStepType.CASEELSE) || loadPlanStep.getType()
                                                                                  .equals(LoadPlanStepType.CASEWHEN)) {
                    Loadplanbranchesteptype child = populateBrancheNode(loadPlanStep);
                    nodeBranche.put(child.getName(), child);
                    attachNode(child, parent);
                } else if (parent != null) {
                    Loadplansteptype child = populateNode(loadPlanStep);
                    node.put(child.getName(), child);
                    attachNode(child, parent);
                } else {
                    Loadplanbranchesteptype parentBranche = getBrancheParentByName(loadPlanStep.getParent()
                                                                                               .getRoot()
                                                                                               .getName());
                    Loadplansteptype child = populateNode(loadPlanStep);
                    node.put(child.getName(), child);
                    attachNode(child, parentBranche);
                }
            } else if (loadPlanStep.getType()
                                   .equals(LoadPlanStepType.EXCEPTION)) {
                // is parent (exception node)
                Loadplansteptype rootNode = populateExceptionNode(loadPlanStep);
                node.put(rootNode.getName(), rootNode);
                this.target.getExceptions()
                           .getException()
                           .add(getXmlInstanceOfLoadplanstepException(rootNode).getValue());
            } else {
                // is parent (root node)
                Loadplansteptype rootNode = populateRootNode(loadPlanStep);
                node.put(rootNode.getName(), rootNode);
            }
        }
    }

    private void attachNode(Loadplansteptype child, Loadplanbranchesteptype parent) {
        assert (parent != null);
        assert (parent.getName() != null);
        assert (child.getName() != null);
        if (parent instanceof Loadplanstepwhentype) {
            ((Loadplanstepwhentype) parent).getChildren()
                                           .getLoadplancorestep()
                                           .add(getXmlInstanceOfLoadplanstep(child));
        } else if (parent instanceof Loadplanstepelsetype) {
            ((Loadplanstepelsetype) parent).getChildren()
                                           .getLoadplancorestep()
                                           .add(getXmlInstanceOfLoadplanstep(child));
        } else {
            logger.warn("Not attaching child " + child.getName() + " to parent: " + parent.getName());
        }
    }

    private void attachNode(Loadplanbranchesteptype child, Loadplansteptype parent) {
        assert (parent != null);
        assert (parent.getName() != null);
        assert (child.getName() != null);
        if (parent instanceof Loadplanstepcasetype) {
            if (child instanceof Loadplanstepwhentype) {
                ((Loadplanstepcasetype) parent).getWhen()
                                               .add(getXmlInstanceOfLoadplanWhenstep(child).getValue());
            }
            if (child instanceof Loadplanstepelsetype) {
                ((Loadplanstepcasetype) parent).setElse(getXmlInstanceOfLoadplanElsestep(child).getValue());
            }
        } else {
            logger.warn("Not attaching child " + child.getName() + " to parent: " + parent.getName());
        }

    }

    private void attachNode(Loadplansteptype child, Loadplansteptype parent) {
        assert (parent != null);
        assert (parent.getName() != null);
        assert (child.getName() != null);
        if (parent instanceof Loadplanstepserialtype) {
            ((Loadplanstepserialtype) parent).getChildren()
                                             .getLoadplancorestep()
                                             .add(getXmlInstanceOfLoadplanstep(child));
        } else if (parent instanceof Loadplanstepparalleltype) {
            ((Loadplanstepparalleltype) parent).getChildren()
                                               .getLoadplancorestep()
                                               .add(getXmlInstanceOfLoadplanstep(child));
//		} else if (parent instanceof Loadplanstepcasetype) {
//		((Loadplanstepcasetype) parent).getCaseChildren().getLoadplanbranchestep()
//					.add(getXmlInstanceOfLoadplanbranchestep((Loadplanbranchesteptype) child));
        } else if (parent instanceof Loadplanstepwhentype) {
            ((Loadplanstepwhentype) parent).getChildren()
                                           .getLoadplancorestep()
                                           .add(getXmlInstanceOfLoadplanstep(child));
        } else if (parent instanceof Loadplanstepelsetype) {
            ((Loadplanstepelsetype) parent).getChildren()
                                           .getLoadplancorestep()
                                           .add(getXmlInstanceOfLoadplanstep(child));
        } else if (parent instanceof Loadplanstepexceptiontype) {
            ((Loadplanstepexceptiontype) parent).getChildren()
                                                .getLoadplancorestep()
                                                .add(getXmlInstanceOfLoadplanstep(child));
        } else {
            logger.warn("Not attaching child " + child.getName() + " to parent: " + parent.getName());
        }
    }

    private JAXBElement<? extends LoadplanstepexceptiontypeImpl> getXmlInstanceOfLoadplanstepException(
            Loadplansteptype child) {
        ObjectFactory factory = new ObjectFactory();
        if (child instanceof LoadplanstepexceptiontypeImpl) {
            JAXBElement<? extends LoadplanstepexceptiontypeImpl> childXml =
                    factory.createException((LoadplanstepexceptiontypeImpl) child);
            return childXml;
        }
        throw new UnsupportedOperationException();
    }

    private JAXBElement<? extends Loadplancoresteptype> getXmlInstanceOfLoadplanstep(Loadplansteptype child) {
        ObjectFactory factory = new ObjectFactory();
        if (child instanceof Loadplanstepparalleltype) {
            JAXBElement<? extends Loadplancoresteptype> childXml =
                    factory.createParallel((LoadplanstepparalleltypeImpl) child);
            return childXml;
        } else if (child instanceof Loadplanstepserialtype) {
            JAXBElement<? extends Loadplancoresteptype> childXml =
                    factory.createSerial((LoadplanstepserialtypeImpl) child);
            return childXml;
        } else if (child instanceof Loadplanstepcasetype) {
            JAXBElement<? extends Loadplancoresteptype> childXml = factory.createCase((LoadplanstepcasetypeImpl) child);
            return childXml;
        } else if (child instanceof Loadplanstepscenariotype) {
            JAXBElement<? extends Loadplancoresteptype> childXml =
                    factory.createRunScenario((LoadplanstepscenariotypeImpl) child);
            return childXml;
        } else if (child instanceof Loadplanstepexceptiontype) {
            JAXBElement<? extends Loadplancoresteptype> childXml =
                    factory.createException((LoadplanstepexceptiontypeImpl) child);
            return childXml;
        } else {
            logger.info(child.getClass()
                             .toString());
            throw new UnsupportedOperationException();
        }
    }

    private JAXBElement<? extends LoadplanstepwhentypeImpl> getXmlInstanceOfLoadplanWhenstep(
            Loadplanbranchesteptype child) {
        ObjectFactory factory = new ObjectFactory();
        if (child instanceof Loadplanstepwhentype) {
            JAXBElement<? extends LoadplanstepwhentypeImpl> childXml =
                    factory.createWhen((LoadplanstepwhentypeImpl) child);
            return childXml;
        } else {
            logger.info(child.getClass()
                             .toString());
            throw new UnsupportedOperationException();
        }
    }

    private JAXBElement<? extends Loadplanstepelsetype> getXmlInstanceOfLoadplanElsestep(
            Loadplanbranchesteptype child) {
        ObjectFactory factory = new ObjectFactory();
        if (child instanceof Loadplanstepelsetype) {
            JAXBElement<? extends LoadplanstepelsetypeImpl> childXml =
                    factory.createElse((LoadplanstepelsetypeImpl) child);
            return childXml;
        } else {
            logger.info(child.getClass()
                             .toString());
            throw new UnsupportedOperationException();
        }
    }


    private Loadplansteptype populateNode(Odiloadplanstep pLoadPlanStep) {
        Loadplansteptype externalLPS = getInstanceofLoadPlanStep(pLoadPlanStep);
        assert (externalLPS != null);
        if (pLoadPlanStep.getType()
                         .equals(LoadPlanStepType.SERIAL)) {
            ((Loadplanstepserialtype) externalLPS).setName(pLoadPlanStep.getName());
            ((Loadplanstepserialtype) externalLPS).setEnabled(pLoadPlanStep.isEnabled());
            ((Loadplanstepserialtype) externalLPS).setKeywords(pLoadPlanStep.getKeywords());
            ((Loadplanstepserialtype) externalLPS).setRestartType(mapFromSerial(pLoadPlanStep.getRestartType()));
            ((Loadplanstepserialtype) externalLPS).setTimeout(pLoadPlanStep.getTimeout());
            ((Loadplanstepserialtype) externalLPS).setExceptionbehavior(pLoadPlanStep.getExceptionBehavior());
            ((Loadplanstepserialtype) externalLPS).setExceptionName(pLoadPlanStep.getExceptionStep());
            if (pLoadPlanStep.getVariables()
                             .size() > 0) {
                Variables externalVS = new VariablesImpl();
                for (Variable v : pLoadPlanStep.getVariables()) {
                    one.jodi.core.lpmodel.Variable externalV = new VariableImpl();
                    externalV.setName(v.getName());
                    externalV.setRefresh(v.isRefresh());
                    externalV.setValue(v.getValue());
                    externalVS.getVariable()
                              .add(externalV);
                }
                ((Loadplanstepserialtype) externalLPS).setVariables(externalVS);
            }
            if (((Loadplanstepserialtype) externalLPS).getChildren() == null) {
                ((Loadplanstepserialtype) externalLPS).setChildren(new ChildrenImpl());
            }
            return externalLPS;
        } else if (pLoadPlanStep.getType()
                                .equals(LoadPlanStepType.PARALLEL)) {
            ((Loadplanstepparalleltype) externalLPS).setName(pLoadPlanStep.getName());
            ((Loadplanstepparalleltype) externalLPS).setEnabled(pLoadPlanStep.isEnabled());
            ((Loadplanstepparalleltype) externalLPS).setKeywords(pLoadPlanStep.getKeywords());
            ((Loadplanstepparalleltype) externalLPS).setRestartType(mapFromParallel(pLoadPlanStep.getRestartType()));
            ((Loadplanstepparalleltype) externalLPS).setTimeout(pLoadPlanStep.getTimeout());
            ((Loadplanstepparalleltype) externalLPS).setExceptionbehavior(pLoadPlanStep.getExceptionBehavior());
            ((Loadplanstepparalleltype) externalLPS).setExceptionName(pLoadPlanStep.getExceptionStep());
            if (pLoadPlanStep.getVariables()
                             .size() > 0) {
                Variables externalVS = new VariablesImpl();
                for (Variable v : pLoadPlanStep.getVariables()) {
                    one.jodi.core.lpmodel.Variable externalV = new VariableImpl();
                    externalV.setName(v.getName());
                    externalV.setRefresh(v.isRefresh());
                    externalV.setValue(v.getValue());
                    externalVS.getVariable()
                              .add(externalV);
                }
                ((Loadplanstepparalleltype) externalLPS).setVariables(externalVS);
            }
            if (((Loadplanstepparalleltype) externalLPS).getChildren() == null) {
                ((Loadplanstepparalleltype) externalLPS).setChildren(new ChildrenImpl());
            }
            return externalLPS;
        } else if (pLoadPlanStep.getType()
                                .equals(LoadPlanStepType.CASEWHEN)) {
            assert (pLoadPlanStep.getName() != null) : "Loadplanstep name can't be null.";
            ((Loadplanstepwhentype) externalLPS).setName(pLoadPlanStep.getName());
            ((Loadplanstepwhentype) externalLPS).setOperator(pLoadPlanStep.getOperator());
            ((Loadplanstepwhentype) externalLPS).setValue(pLoadPlanStep.getValue());
            if (((Loadplanstepwhentype) externalLPS).getChildren() == null) {
                ((Loadplanstepwhentype) externalLPS).setChildren(new ChildrenImpl());
            }
            return externalLPS;
        } else if (pLoadPlanStep.getType()
                                .equals(LoadPlanStepType.CASEELSE)) {
            ((Loadplanstepelsetype) externalLPS).setName(pLoadPlanStep.getName());
            if (((Loadplanstepelsetype) externalLPS).getChildren() == null) {
                ((Loadplanstepelsetype) externalLPS).setChildren(new ChildrenImpl());
            }
            return externalLPS;
        } else if (pLoadPlanStep.getType()
                                .equals(LoadPlanStepType.CASE)) {
            ((Loadplanstepcasetype) externalLPS).setName(pLoadPlanStep.getName());
            ((Loadplanstepcasetype) externalLPS).setTestVariable(pLoadPlanStep.getTestVariable());
            ((Loadplanstepcasetype) externalLPS).setEnabled(pLoadPlanStep.isEnabled());
            ((Loadplanstepcasetype) externalLPS).setKeywords(pLoadPlanStep.getKeywords());
            ((Loadplanstepcasetype) externalLPS).setTimeout(pLoadPlanStep.getTimeout());
            ((Loadplanstepcasetype) externalLPS).setExceptionName(pLoadPlanStep.getExceptionStep());
            ((Loadplanstepcasetype) externalLPS).setExceptionbehavior(pLoadPlanStep.getExceptionBehavior());
            if (pLoadPlanStep.getVariables()
                             .size() > 0) {
                Variables externalVS = new VariablesImpl();
                for (Variable v : pLoadPlanStep.getVariables()) {
                    one.jodi.core.lpmodel.Variable externalV = new VariableImpl();
                    externalV.setName(v.getName());
                    externalV.setRefresh(v.isRefresh());
                    externalV.setValue(v.getValue());
                    externalVS.getVariable()
                              .add(externalV);
                }
                ((Loadplanstepcasetype) externalLPS).setVariables(externalVS);
            }
            return externalLPS;
        } else if (pLoadPlanStep.getType()
                                .equals(LoadPlanStepType.SCENARIO)) {
            ((Loadplanstepscenariotype) externalLPS).setName(pLoadPlanStep.getName());
            ((Loadplanstepscenariotype) externalLPS).setScenario(pLoadPlanStep.getScenario());
            ((Loadplanstepscenariotype) externalLPS).setVersion(
                    new BigInteger(pLoadPlanStep.getScenarioVersion() + ""));
            ((Loadplanstepscenariotype) externalLPS).setName(pLoadPlanStep.getName());
            ((Loadplanstepscenariotype) externalLPS).setEnabled(pLoadPlanStep.isEnabled());
            ((Loadplanstepscenariotype) externalLPS).setKeywords(pLoadPlanStep.getKeywords());
            ((Loadplanstepscenariotype) externalLPS).setRestartType(mapFromScneario(pLoadPlanStep.getRestartType()));
            ((Loadplanstepscenariotype) externalLPS).setTimeout(pLoadPlanStep.getTimeout());
            ((Loadplanstepscenariotype) externalLPS).setExceptionbehavior(pLoadPlanStep.getExceptionBehavior());
            ((Loadplanstepscenariotype) externalLPS).setExceptionName(pLoadPlanStep.getExceptionStep());
            ((Loadplanstepscenariotype) externalLPS).setPriority(pLoadPlanStep.getPriority());
            if (pLoadPlanStep.getVariables()
                             .size() > 0) {
                Variables externalVS = new VariablesImpl();
                for (Variable v : pLoadPlanStep.getVariables()) {
                    one.jodi.core.lpmodel.Variable externalV = new VariableImpl();
                    externalV.setName(v.getName());
                    externalV.setRefresh(v.isRefresh());
                    externalV.setValue(v.getValue());
                    externalVS.getVariable()
                              .add(externalV);
                }
                ((Loadplanstepscenariotype) externalLPS).setVariables(externalVS);
            }
            return externalLPS;
        } else if (pLoadPlanStep.getType()
                                .equals(LoadPlanStepType.EXCEPTION)) {
            ((Loadplanstepexceptiontype) externalLPS).setName(pLoadPlanStep.getName());
            ((Loadplanstepexceptiontype) externalLPS).setEnabled(pLoadPlanStep.isEnabled());
            ((Loadplanstepexceptiontype) externalLPS).setKeywords(pLoadPlanStep.getKeywords());
            if (((Loadplanstepexceptiontype) externalLPS).getChildren() == null) {
                ((Loadplanstepexceptiontype) externalLPS).setChildren(new ChildrenImpl());
            }
            return externalLPS;
        } else {
            throw new UnsupportedOperationException();
        }
    }


    private Loadplanbranchesteptype populateBrancheNode(Odiloadplanstep pLoadPlanStep) {
        Loadplanbranchesteptype externalLPS = getInstanceofLoadPlanStepCase(pLoadPlanStep);
        assert (externalLPS != null);
        if (pLoadPlanStep.getType()
                         .equals(LoadPlanStepType.CASEWHEN)) {
            assert (pLoadPlanStep.getName() != null) : "Loadplanstep name can't be null.";
            ((Loadplanstepwhentype) externalLPS).setName(pLoadPlanStep.getName());
            ((Loadplanstepwhentype) externalLPS).setOperator(pLoadPlanStep.getOperator());
            ((Loadplanstepwhentype) externalLPS).setValue(pLoadPlanStep.getValue());
            if (((Loadplanstepwhentype) externalLPS).getChildren() == null) {
                ((Loadplanstepwhentype) externalLPS).setChildren(new ChildrenImpl());
            }
            return externalLPS;
        } else if (pLoadPlanStep.getType()
                                .equals(LoadPlanStepType.CASEELSE)) {
            ((Loadplanstepelsetype) externalLPS).setName(pLoadPlanStep.getName());
            if (((Loadplanstepelsetype) externalLPS).getChildren() == null) {
                ((Loadplanstepelsetype) externalLPS).setChildren(new ChildrenImpl());
            }
            return externalLPS;
        } else if (pLoadPlanStep.getType()
                                .equals(LoadPlanStepType.CASE)) {
            ((Loadplanstepcasetype) externalLPS).setName(pLoadPlanStep.getName());
            ((Loadplanstepcasetype) externalLPS).setTestVariable(pLoadPlanStep.getTestVariable());
            ((Loadplanstepcasetype) externalLPS).setEnabled(pLoadPlanStep.isEnabled());
            ((Loadplanstepcasetype) externalLPS).setKeywords(pLoadPlanStep.getKeywords());
            ((Loadplanstepcasetype) externalLPS).setTimeout(pLoadPlanStep.getTimeout());
            ((Loadplanstepcasetype) externalLPS).setExceptionName(pLoadPlanStep.getExceptionStep());
            ((Loadplanstepcasetype) externalLPS).setExceptionbehavior(pLoadPlanStep.getExceptionBehavior());
            if (pLoadPlanStep.getVariables()
                             .size() > 0) {
                Variables externalVS = new VariablesImpl();
                for (Variable v : pLoadPlanStep.getVariables()) {
                    one.jodi.core.lpmodel.Variable externalV = new VariableImpl();
                    externalV.setName(v.getName());
                    externalV.setRefresh(v.isRefresh());
                    externalV.setValue(v.getValue());
                    externalVS.getVariable()
                              .add(externalV);
                }
                ((Loadplanstepcasetype) externalLPS).setVariables(externalVS);
            }
            return externalLPS;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private Loadplanbranchesteptype getInstanceofLoadPlanStepCase(Odiloadplanstep pLoadPlanStep) {
        if (pLoadPlanStep.getType()
                         .equals(LoadPlanStepType.CASEWHEN)) {
            return new LoadplanstepwhentypeImpl();
        } else if (pLoadPlanStep.getType()
                                .equals(LoadPlanStepType.CASEELSE)) {
            return new LoadplanstepelsetypeImpl();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private Loadplancoresteptype getInstanceofLoadPlanStep(Odiloadplanstep pLoadPlanStep) {
        if (pLoadPlanStep.getType()
                         .equals(LoadPlanStepType.PARALLEL)) {
            return new LoadplanstepparalleltypeImpl();
        } else if (pLoadPlanStep.getType()
                                .equals(LoadPlanStepType.SERIAL)) {
            return new LoadplanstepserialtypeImpl();
        } else if (pLoadPlanStep.getType()
                                .equals(LoadPlanStepType.SCENARIO)) {
            return new LoadplanstepscenariotypeImpl();
        } else if (pLoadPlanStep.getType()
                                .equals(LoadPlanStepType.CASE)) {
            return new LoadplanstepcasetypeImpl();
        } else if (pLoadPlanStep.getType()
                                .equals(LoadPlanStepType.EXCEPTION)) {
            return new LoadplanstepexceptiontypeImpl();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private Restarttypeparallel mapFromParallel(RestartType restartType) {
        if (restartType.equals(RestartType.PARALLEL_STEP_ALL_CHILDREN)) {
            return Restarttypeparallel.ALL_CHILDREN;
        } else if (restartType.equals(RestartType.PARALLEL_STEP_FAILED_CHILDREN)) {
            return Restarttypeparallel.FAILED_CHILDREN;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private Restarttypeserial mapFromSerial(RestartType restartType) {
        if (restartType.equals(RestartType.SERIAL_STEP_ALL_CHILDREN)) {
            return Restarttypeserial.ALL_CHILDREN;
        } else if (restartType.equals(RestartType.SERIAL_STEP_FROM_FAILURE)) {
            return Restarttypeserial.FROM_FAILURE;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private Restarttypescenario mapFromScneario(RestartType restartType) {
        if (restartType.equals(RestartType.RUN_SCENARIO_FROM_STEP)) {
            return Restarttypescenario.FROM_STEP;
        } else if (restartType.equals(RestartType.RUN_SCENARIO_FROM_TASK)) {
            return Restarttypescenario.FROM_TASK;
        } else if (restartType.equals(RestartType.RUN_SCENARIO_NEW_SESSION)) {
            return Restarttypescenario.NEW_SESSION;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private Loadplanstepserialtype populateRootNode(Odiloadplanstep loadPlanStep) {
        Loadplanstepserialtype rootStep = (Loadplanstepserialtype) populateNode(loadPlanStep);
        this.target.setSerial(rootStep);
        if (this.target.getSerial()
                       .getChildren() == null) {
            this.target.getSerial()
                       .setChildren(new ChildrenImpl());
        }
        return rootStep;
    }

    private Loadplanstepexceptiontype populateExceptionNode(Odiloadplanstep loadPlanStep) {
        Loadplanstepexceptiontype rootStep = (Loadplanstepexceptiontype) populateNode(loadPlanStep);
        if (this.target.getExceptions() == null) {
            this.target.setExceptions(new ExceptionsImpl());
        }
        return rootStep;
    }

    @Override
    public Collection<Odiloadplanstep> getChildren() {
        // TODO Auto-generated method stub
        return null;
    }

    public Loadplansteptype getParentByName(String name) {
        return node.get(name);
    }

    public Loadplanbranchesteptype getBrancheParentByName(String name) {
        return nodeBranche.get(name);
    }

    public void writeLine(String message) {
        logger.info(message);
    }


    public void write() {
        logger.debug(file.getAbsolutePath());
        JAXBContext jaxbCtx;
        Writer writer = null;
        FileOutputStream fos = null;
        BufferedWriter fout = null;
        final String xsdLoc;
        if (jodiProperties.getPropertyKeys()
                          .contains(JodiConstants.XSDLOCPROPERTY)) {
            xsdLoc = jodiProperties.getProperty(JodiConstants.XSDLOCPROPERTY);
        } else {
            xsdLoc = JodiConstants.XSD_FILE_LOADPLAN;
        }
        try {
            jaxbCtx = JAXBContext.newInstance(Loadplan.class.getPackage()
                                                            .getName());
            Marshaller marshaller = jaxbCtx.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty("jaxb.noNamespaceSchemaLocation", xsdLoc);
            marshaller.marshal(target, file);
            String content = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())), "UTF-8");
            content = content.replace("<ns2:", "<")
                             .replace("</ns2:", "</")
                             .replace("xmlns:ns2=\"http://one.jodi/jodi/model/loadplan\"", "");
            fos = new FileOutputStream(file.getAbsolutePath());
            writer = new OutputStreamWriter(fos, "UTF-8");
            fout = new BufferedWriter(writer);
            fout.write(content);
            fout.close();
            logger.debug(content);
        } catch (JAXBException e) {
            logger.error("JAXBException generated during marshalling", e);
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException generated during marshalling", e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            logger.error("IOException generated during marshalling", e);
            throw new RuntimeException(e);
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                logger.debug("IOException generated during marshalling", e);
            }
            try {
                if (fout != null) {
                    fout.close();
                }
            } catch (IOException e) {
                logger.debug("IOException generated during marshalling", e);
            }
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                logger.debug("IOException generated during marshalling", e);
            }
        }
    }
}