package one.jodi.tools.dependency;


/**
 * First attempt at describing pattern of
 */
public enum MappingType {
    SourceToTarget("DATASTORE,DATASTORE".split(",")),
    SourceToExpressionToTarget("DATASTORE,EXPRESSION,DATASTORE".split(",")),
    SourceToSetToExpressionToTarget("DATASTORE,EXPRESSION,SET,DATASTORE".split(",")),
    SourceToFilterToExpressionToTarget("DATASTORE,FILTER,EXPRESSION,DATASTORE".split(",")),
    Indeterminate("DATASTORE".split(","));

    private String[] type;

    MappingType(String[] type) {
        this.type = type;
    }

    public String[] getValue() {
        return type;
    }
}
