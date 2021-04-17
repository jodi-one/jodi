package one.jodi.base.model.expression;

import one.jodi.base.model.TableBase;

public interface Expression {
    EType getType();

    TableBase getParent();

    Object getValue();

    enum EType {COLUMN, STRING}

}
