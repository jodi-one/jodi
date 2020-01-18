package one.jodi.etl.internalmodel;

public interface SubQuery extends Flow {

    public String getFilterSource();

    /**
     * Filter Source is temporary
     *
     * @return
     */
    boolean isTemporary();

    public ExecutionLocationtypeEnum getExecutionLocation();

    public String getFilterSourceModel();

    public RoleEnum getRole();

    public String getCondition();

    public GroupComparisonEnum getGroupComparison();

    public enum ExpressionSource {
        DRIVER,
        FILTER;
    }

}
