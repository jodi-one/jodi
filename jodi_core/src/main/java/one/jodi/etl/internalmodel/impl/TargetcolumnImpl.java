package one.jodi.etl.internalmodel.impl;

import one.jodi.core.executionlocation.ExecutionLocationType;
import one.jodi.etl.internalmodel.ExecutionLocationtypeEnum;
import one.jodi.etl.internalmodel.Mappings;
import one.jodi.etl.internalmodel.Targetcolumn;
import one.jodi.etl.internalmodel.UserDefinedFlag;
import one.jodi.model.extensions.TargetColumnExtension;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Implementation of interface.
 */
public class TargetcolumnImpl implements Targetcolumn {

    private static final Logger LOGGER = LogManager.getLogger(TargetcolumnImpl.class);

    // Pattern compilation is slow, do it only once per regex expression
    private static final Pattern PATTERN_AGGREGATE_EXPRESSION =
            Pattern.compile("(?i)(min|max|sum|avg|count|variance|stdev|stats_mode)(\\s)*(\\()");

    Mappings parent;
    String name;
    List<String> mappingExpressions;
    Boolean mandatory;
    Boolean explicitMandatory;
    Boolean updateKey;
    Boolean explicitUpdateKey;
    Boolean insert;
    Boolean update;
    String dataType;
    int scale;
    int length;
    TargetColumnExtension extension;
    Set<UserDefinedFlag> explicitUserDefinedFlags;
    Set<UserDefinedFlag> userDefinedFlags;
    List<ExecutionLocationtypeEnum> executionLocations;
    int position;
    ExecutionLocationType targetcolumnExplicitExecutionLocation;

    public TargetcolumnImpl(Mappings parent,
                            String name,
                            Boolean mandatory,
                            Boolean explicitMandatory,
                            Boolean updateKey,
                            Boolean explicitUpdateKey,
                            Boolean insert,
                            Boolean update,
                            String dataType,
                            int scale,
                            int length,
                            int position,
                            ExecutionLocationType targetcolumnExplicitExecutionLocationType) {
        super();
        assert (parent != null && name != null);

        this.parent = parent;
        this.name = name;
        this.mandatory = mandatory;
        this.explicitMandatory = explicitMandatory;
        this.updateKey = updateKey;
        this.explicitUpdateKey = explicitUpdateKey;
        this.insert = insert;
        this.update = update;
        this.dataType = dataType;
        this.scale = scale;
        this.length = length;
        this.position = position;
        this.targetcolumnExplicitExecutionLocation = targetcolumnExplicitExecutionLocationType;
        mappingExpressions = new ArrayList<>();
        executionLocations = new ArrayList<>();
        userDefinedFlags = new HashSet<>();
    }

    public TargetcolumnImpl() {
        mappingExpressions = new ArrayList<>();
        executionLocations = new ArrayList<>();
        explicitUserDefinedFlags = new HashSet<>();
        userDefinedFlags = new HashSet<>();
    }

    @Override
    public Mappings getParent() {
        return parent;
    }

    public void setParent(Mappings parent) {
        this.parent = parent;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public List<String> getMappingExpressions() {
        return mappingExpressions;
    }


    public void addMappingExpressions(List<String> mappingsExpressions) {
        this.mappingExpressions.addAll(mappingsExpressions);
    }


    public void addMappingExpression(String mappingExpression) {
        this.mappingExpressions.add(mappingExpression);
    }


    @Override
    public Boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(Boolean mandatory) {
        this.mandatory = mandatory;
    }

    @Override
    public Boolean isUpdateKey() {
        return updateKey;
    }

    public void setUpdateKey(Boolean updateKey) {
        this.updateKey = updateKey;
    }

    @Override
    public Boolean isInsert() {
        return insert;
    }

    public void setInsert(boolean insert) {
        this.insert = insert;
    }

    @Override
    public Boolean isUpdate() {
        return update;
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }


    @Override
    public List<ExecutionLocationtypeEnum> getExecutionLocations() {
        return executionLocations;
    }

    public void setExecutionLocations(List<ExecutionLocationtypeEnum> executionLocations) {
        this.executionLocations = executionLocations;
    }

    @Override
    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    @Override
    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    @Override
    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    @Override
    public TargetColumnExtension getExtension() {
        return this.extension;
    }

    public void setExtension(TargetColumnExtension extension) {
        this.extension = extension;
    }

    @Override
    public Set<UserDefinedFlag> getUserDefinedFlags() {
        return userDefinedFlags;
    }

    public void setUserDefinedFlags(Set<UserDefinedFlag> userDefinedFlags) {
        this.userDefinedFlags = userDefinedFlags;
    }

    @Override
    public boolean isAggregateColumn(int dataSetNumber) {
        assert (dataSetNumber > 0) : "Dataset number starts with 1.";
        boolean found = false;
        for (int datasetIndex = 0; datasetIndex < this.getMappingExpressions().size(); datasetIndex++) {
            if ((datasetIndex + 1) != dataSetNumber) {
                continue;
            }
            String me = this.getMappingExpressions().get(datasetIndex);
            if (!isAnalyticalFunction((datasetIndex + 1)) && isAggregateExpression(me)) {
                found = true;
            }
        }
        return found;
    }

    private boolean isAggregateExpression(String me) {
        return PATTERN_AGGREGATE_EXPRESSION.matcher(me).find();
    }

    @Override
    public boolean isAnalyticalFunction(int dataSetNumber) {
        assert (dataSetNumber > 0) : "DataSetNubmer starts with 1";
        // this determines whether it is analytical function or not
        // the presence of "OVER(" or "OVER ("
        //
        for (int dataSetIndex = 0; dataSetIndex < this.mappingExpressions.size(); dataSetIndex++) {
            if ((dataSetIndex + 1) != dataSetNumber) {
                continue;
            }
            String expression = this.mappingExpressions.get(dataSetIndex);
            if (containsOver(expression)) {
                LOGGER.debug(String.format("The targetcolumn %s is an analytical function for expression %s.", this.name, expression));
                return true;
            }
        }
        return false;
    }

    private boolean containsOver(String expression) {
        expression = expression.toLowerCase().trim();
        if (expression.contains(" over")) {
            return expression.replace(" ", "").contains("over(");
        }
        return false;
    }

    @Override
    public Boolean isExplicitlyMandatory() {
        return explicitMandatory;
    }

    @Override
    public Boolean isExplicitlyUpdateKey() {
        return explicitUpdateKey;
    }

    public void setExplicitMandatory(Boolean explicitMandatory) {
        this.explicitMandatory = explicitMandatory;
    }

    public void setExplicitUpdateKey(Boolean explicitUpdateKey) {
        this.explicitUpdateKey = explicitUpdateKey;
    }

    @Override
    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    @Override
    public Set<UserDefinedFlag> getExplicitUserDefinedFlags() {
        return this.explicitUserDefinedFlags;
    }

    public void setExplicitUserDefinedFlags(Set<UserDefinedFlag> explicitUserDefinedFlags) {
        this.explicitUserDefinedFlags = explicitUserDefinedFlags;
    }

    @Override
    public ExecutionLocationType getTargetcolumnExplicitExecutionLocation() {
        return targetcolumnExplicitExecutionLocation;
    }

    public void setTargetcolumnExplicitExecutionLocation(ExecutionLocationType targetcolumnExplicitExecutionLocation) {
        this.targetcolumnExplicitExecutionLocation = targetcolumnExplicitExecutionLocation;
    }

}
