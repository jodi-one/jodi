package one.jodi.base.model.expression;

import one.jodi.base.model.TableBase;

public class StringExpression implements Expression {

    private final TableBase table;
    private final String sValue;

    public StringExpression(final TableBase table, final String sValue) {
        this.table = table;
        this.sValue = sValue;
    }

    @Override
    public EType getType() {
        return EType.STRING;
    }

    @Override
    public TableBase getParent() {
        return this.table;
    }

    @Override
    public String getValue() {
        return this.sValue;
    }
}
