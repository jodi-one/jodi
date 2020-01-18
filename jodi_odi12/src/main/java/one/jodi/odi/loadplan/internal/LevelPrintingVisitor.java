package one.jodi.odi.loadplan.internal;

import one.jodi.etl.service.loadplan.internalmodel.Variable;
import one.jodi.odi.loadplan.OdiLoadPlanTree;
import one.jodi.odi.loadplan.OdiLoadPlanVisitor;
import one.jodi.odi.loadplan.Odiloadplanstep;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * This class prints a tree of LoadPlanTree with subtypes LoadPlanStep.
 * The visitor pattern is used to traverse the tree.
 */
public class LevelPrintingVisitor implements OdiLoadPlanVisitor<Odiloadplanstep> {

    private final Logger logger = LogManager.getLogger(LevelPrintingVisitor.class);

    private final int indent;

    LevelPrintingVisitor(int indent) {

        this.indent = indent;

    }

    public LevelPrintingVisitor() {

        this.indent = 0;

    }

    public OdiLoadPlanVisitor<Odiloadplanstep> visit(OdiLoadPlanTree<Odiloadplanstep> tree) {

        return new LevelPrintingVisitor(indent + 3);

    }

    public void visit(Odiloadplanstep loadPlanStep) {
        if (loadPlanStep == null) {
            return;
        }
        StringBuilder indentString = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            indentString.append(" ");
        }
        if (loadPlanStep != null) {
            println(indentString.toString() + "--start--" + loadPlanStep.getName());
            String message = indentString.toString() + " Name: " + loadPlanStep.getName();
            println(message);
            message = indentString.toString() + " Enabled: " + loadPlanStep.isEnabled();
            println(message);
            message = indentString.toString() + " Type: " + loadPlanStep.getType();
            println(message);
            message = indentString.toString() + " Priority: " + loadPlanStep.getPriority();
            println(message);
            message = indentString.toString() + " Keywords: " + loadPlanStep.getKeywords();
            println(message);
            message = indentString.toString() + " BehaviorOnException: " + loadPlanStep.getExceptionBehavior();
            println(message);
            message = indentString.toString() + " ExceptionName: " + loadPlanStep.getExceptionStep();
            println(message);
            message = indentString.toString() + " Restarttype: " + loadPlanStep.getRestartType();
            println(message);
            message = indentString.toString() + " Keywords: " + loadPlanStep.getKeywords();
            println(message);
            message = indentString.toString() + " MaxErrorChildCount: " + loadPlanStep.getMaxErrorChildCount();
            println(message);
            message = indentString.toString() + " Operator: " + loadPlanStep.getOperator();
            println(message);
            message = indentString.toString() + " Scenario: " + loadPlanStep.getScenario();
            println(message);
            message = indentString.toString() + " Version: " + loadPlanStep.getScenarioVersion();
            println(message);
            message = indentString.toString() + " TestVariable: " + loadPlanStep.getTestVariable();
            println(message);
            message = indentString.toString() + " Value: " + loadPlanStep.getValue();
            println(message);
            if (loadPlanStep.getVariables() != null && loadPlanStep.getVariables().size() > 0) {
                List<Variable> variables = loadPlanStep.getVariables();
                Collections.sort(variables,
                        (o1, o2) -> o1.getName().compareTo(o2.getName()));
                for (Variable variable : variables) {
                    message = indentString.toString() + " variable name: " + variable.getName();
                    print(message);
                    message = indentString.toString() + " variable value: " + variable.getValue();
                    print(message);
                    message = indentString.toString() + " variable refresh: " + variable.isRefresh();
                    print(message);
                }
                println("");
            }
            println(indentString.toString() + "--end--" + loadPlanStep.getName());
        }
    }

    @Override
    public Collection<Odiloadplanstep> getChildren() {
        // TODO Auto-generated method stub
        return null;
    }

    private void print(String message) {
        logger.info(message);
    }

    private void println(String message) {
        logger.info(message);
    }

}