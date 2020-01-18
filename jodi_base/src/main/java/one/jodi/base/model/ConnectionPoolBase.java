package one.jodi.base.model;

public class ConnectionPoolBase {

    private final String name;
    private final DatabaseBase parent;
    private final String description;
    private final String initExpression;

    public ConnectionPoolBase(final String name, final DatabaseBase parent,
                              final String description, final String initExpression) {
        this.name = name;
        this.parent = parent;
        this.description = description;
        this.initExpression = initExpression == null ? "" : initExpression;
    }

    public String getName() {
        return this.name;
    }

    public DatabaseBase getParent() {
        return this.parent;
    }

    public String getDescription() {
        return this.description;
    }

    public String getInitExpression() {
        return this.initExpression;
    }

}
