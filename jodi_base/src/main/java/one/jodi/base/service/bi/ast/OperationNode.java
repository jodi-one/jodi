package one.jodi.base.service.bi.ast;

import java.util.List;

public interface OperationNode extends ExprNode {

    int countParameters();

    String getOperation();

    ExprNode getParameter(int index);

    List<ExprNode> getParameters();

    boolean isFunction();

}
