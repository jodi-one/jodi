package one.jodi.base.service.bi.ast;

import java.util.stream.Stream;

/**
 * Represents an AST that is created by the BiExpressionParser.
 */
public interface ExprNode {

    NodeType getNodeType();

    void accept(NodeVisitor visitor);

    public Stream<ExprNode> flatten();

    enum NodeType {
        NUMBER, INTEGER, STRING, DATE, TIME, DATETIME, COLUMN, OP,
        VARIABLE, LEVEL, TEMPLATE
    }
}
