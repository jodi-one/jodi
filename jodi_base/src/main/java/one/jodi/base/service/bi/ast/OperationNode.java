package one.jodi.base.service.bi.ast;

import java.util.List;

public interface OperationNode extends ExprNode {

    public int countParameters();

    public String getOperation();

    public ExprNode getParameter(int index);

    public List<ExprNode> getParameters();

    public boolean isFunction();

}
