package one.jodi.base.service.bi.ast;

public interface NodeVisitor {

    void visit(IntegerNode visited);

    void visit(NumberNode visited);

    void visit(StringNode visited);

    void visit(ColumnNode visited);

    void visit(LevelNode visited);

    void visit(VariableNode visited);

    void visit(OperationNode visited);

    void visit(DateNode visited);

    // legacy only
    void visit(TemplateNode visited);
}
