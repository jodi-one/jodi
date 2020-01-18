package one.jodi.base.model;

import one.jodi.base.model.expression.Expression;

public interface ExpressionWithCode {
    Expression getExpression();

    String getCode();
}
