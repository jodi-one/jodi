package one.jodi.etl.service.loadplan.internalmodel;

/**
 * Internal model representation of a Variable used in a loadplan
 */
public class Variable {
    private final String name;
    private final boolean refresh;
    private final Object value;

    public Variable(final String name, final boolean refresh, final Object value) {
        this.name = name.trim();
        this.refresh = refresh;
        this.value = value;
    }

    public boolean isRefresh() {
        return refresh;
    }

    public Object getValue() {
        if(value instanceof String){
            return ((String) value).trim();
        }
        return value;
    }

    public String getName() {
        return name;
    }

}
