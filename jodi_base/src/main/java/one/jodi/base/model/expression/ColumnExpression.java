package one.jodi.base.model.expression;

import one.jodi.base.model.ColumnBase;
import one.jodi.base.model.TableBase;

public class ColumnExpression implements Expression {

    private final ColumnBase column;

    public ColumnExpression(final ColumnBase column) {
        this.column = column;
    }

    @Override
    public EType getType() {
        return EType.COLUMN;
    }

    @Override
    public TableBase getParent() {
        return this.column.getParent();
    }

    @Override
    public ColumnBase getValue() {
        return this.column;
    }
}
