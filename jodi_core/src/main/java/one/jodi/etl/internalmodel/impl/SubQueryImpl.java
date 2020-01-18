package one.jodi.etl.internalmodel.impl;

import one.jodi.etl.internalmodel.*;

import java.util.ArrayList;
import java.util.List;

public class SubQueryImpl extends FlowImpl implements SubQuery {

    private String filterSource;
    private String filterSourceModel;
    private RoleEnum role;
    private String condition;
    private GroupComparisonEnum groupComparison;
    private ArrayList<OutputAttribute> outputAttributes = new ArrayList<OutputAttribute>();
    private ExecutionLocationtypeEnum executionLocation;
    private boolean isTemporary = false;

    public SubQueryImpl(int order, String name, String filterSource, String filterSourceModel, ExecutionLocationtypeEnum executionLocation, RoleEnum role, String condition, GroupComparisonEnum groupComparison) {
        this.order = order;
        this.name = name;
        this.filterSource = filterSource;
        this.role = role;
        this.groupComparison = groupComparison;
        this.condition = condition;
        this.filterSourceModel = filterSourceModel;
        this.executionLocation = executionLocation;

    }

    @Override
    public List<OutputAttribute> getOutputAttributes() {
        return outputAttributes;
    }

    public void setOutputAttributes(ArrayList<OutputAttribute> outputAttributes) {
        this.outputAttributes = outputAttributes;
    }

    @Override
    public String getFilterSource() {
        return filterSource;
    }

    public void setFilterSource(String filterSource) {
        this.filterSource = filterSource;
    }

    @Override
    public RoleEnum getRole() {
        return role;
    }

    public void setRole(RoleEnum role) {
        this.role = role;
    }

    @Override
    public String getCondition() {
        return condition != null ? condition : "";
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    @Override
    public GroupComparisonEnum getGroupComparison() {
        return groupComparison;
    }

    public void setGroupComparison(GroupComparisonEnum groupComparison) {
        this.groupComparison = groupComparison;
    }

    @Override
    public String getFilterSourceModel() {
        return filterSourceModel;
    }

    public void setFilterSourceModel(String filterSourceModel) {
        this.filterSourceModel = filterSourceModel;
    }

    public boolean add(OutputAttribute oa) {
        return outputAttributes.add(oa);
    }


    public String getComponentName() {
        int dataSetNumber = this.getParent().getParent().getDataSetNumber();
        String alias = this.getName();
        return ComponentPrefixType.DATASET.getAbbreviation() + dataSetNumber
                + alias;
    }

    @Override
    public ExecutionLocationtypeEnum getExecutionLocation() {
        return executionLocation;
    }

    public void setExecutionLocation(ExecutionLocationtypeEnum executionLocation) {
        this.executionLocation = executionLocation;
    }

    @Override
    public boolean isTemporary() {
        return isTemporary;
    }

    public void setTemporary(boolean temporary) {
        this.isTemporary = temporary;
    }

}
