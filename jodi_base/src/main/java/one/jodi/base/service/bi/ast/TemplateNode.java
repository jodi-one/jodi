package one.jodi.base.service.bi.ast;

import java.util.List;

/**
 * FOR BACKWARDS COMPATIBILITY
 */
public interface TemplateNode extends ExprNode {

    String getTemplate();

    List<ExprNode> getExpressions();
}
