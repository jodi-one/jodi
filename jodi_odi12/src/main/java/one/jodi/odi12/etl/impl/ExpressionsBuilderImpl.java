package one.jodi.odi12.etl.impl;

import com.google.inject.Inject;
import one.jodi.etl.internalmodel.ComponentPrefixType;
import one.jodi.etl.internalmodel.ExecutionLocationtypeEnum;
import one.jodi.etl.internalmodel.Targetcolumn;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.odi.etl.OdiCommon;
import one.jodi.odi12.etl.EtlOperators;
import one.jodi.odi12.etl.ExpressionsBuilder;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.mapping.IMapComponent;
import oracle.odi.domain.mapping.MapAttribute;
import oracle.odi.domain.mapping.MapRootContainer;
import oracle.odi.domain.mapping.component.DatastoreComponent;
import oracle.odi.domain.mapping.component.ExpressionComponent;
import oracle.odi.domain.mapping.exception.MapComponentException;
import oracle.odi.domain.mapping.exception.MappingException;
import oracle.odi.domain.mapping.expression.MapExpression;
import oracle.odi.domain.mapping.expression.MapExpression.ExecuteOnLocation;
import oracle.odi.domain.model.OdiDataStore;
import oracle.odi.domain.model.OdiModel;
import oracle.odi.domain.topology.OdiDataType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;

public class ExpressionsBuilderImpl implements ExpressionsBuilder {
    private final static Logger logger = LogManager.getLogger(ExpressionsBuilderImpl.class);
    private final OdiCommon odiCommon;

    @Inject
    protected ExpressionsBuilderImpl(final OdiCommon odiCommon) {
        this.odiCommon = odiCommon;
    }

    protected ExpressionComponent createExpressionComponent(final MapRootContainer mapping) throws MappingException {
        return new ExpressionComponent(mapping,
                ComponentPrefixType.TARGET_EXPRESSIONS.getAbbreviation());
    }

    /* (non-Javadoc)
     * @see one.jodi.odi12.mappings.ExpressionsBuilder#addTargetExpressions(oracle.odi.domain.mapping.MapRootContainer, boolean, one.jodi.odi12.etl.EtlOperators)
     */
    @Override
    public void addTargetExpressions(final MapRootContainer mapping, final boolean useExpressions,
                                     final EtlOperators etlOperators) throws MapComponentException, MappingException {
        if (useExpressions) {
            logger.debug("Creating target expressions for mapping " + mapping.getName());
            ExpressionComponent targetExpression = createExpressionComponent(mapping);
            etlOperators.addTargetExpressions(targetExpression);
            mapping.addComponent(etlOperators.getTargetExpressions().get(0));
        }
    }


    /* (non-Javadoc)
     * @see one.jodi.odi12.mappings.ExpressionsBuilder#createExpressionComponent(oracle.odi.domain.mapping.MapRootContainer, one.jodi.etl.internalmodel.Transformation, java.util.List, boolean)
     */
    @Override
    public void createExpressionComponent(final MapRootContainer mapping, final Transformation transformation,
                                          final List<ExpressionComponent> targetExpressions, final boolean useExpressions)
            throws AdapterException, MappingException {
        if (transformation.getMappings() == null || !useExpressions) {
            return;
        }
        for (Targetcolumn targetColumn : transformation.getMappings().getTargetColumns()) {
            for (int dataSetIndex = 0; dataSetIndex < targetColumn.getMappingExpressions().size(); dataSetIndex++) {
                String mappingExpression = targetColumn.getMappingExpressions().get(dataSetIndex);
                OdiModel model = odiCommon.getOdiModel(transformation.getMappings().getModel());
                OdiDataType odidatatype = model.getLogicalSchema().getTechnology()
                        .getDataType(targetColumn.getDataType());
                String name = targetColumn.getName();
                Integer size = targetColumn.getLength();
                Integer scale = targetColumn.getScale();
                String expression = "";
                if (targetColumn.getMappingExpressions().size() > 1) {
                    expression = ComponentPrefixType.SETCOMPONENT.getAbbreviation() + "." + targetColumn.getName();
                } else if (transformation.getMappings().isAggregateTransformation((dataSetIndex + 1))) {
                    expression = "D" + (dataSetIndex + 1) + "_" + ComponentPrefixType.AGGREGATE.getAbbreviation() + "." + targetColumn.getName();
                } else {
                    expression = transformation.getDatasets().get(dataSetIndex).translateExpression(mappingExpression);
                }
                if (transformation.getDatasets().size() > 1) {
                    if (dataSetIndex == 1) {
                        MapAttribute mapAttribute = targetExpressions.get(0).addExpression(name, expression, odidatatype, size,
                                scale);
                        // set the execution location of the expression to the
                        // same as the targetcol.
                        MapExpression me = mapAttribute.getExpression();
                        if (targetColumn.getExecutionLocations() != null && targetColumn.getExecutionLocations().size() > 0) {
                            ExecuteOnLocation execLoc = mapFromExecutionLocationType(
                                    targetColumn.getExecutionLocations().get(0));
                            me.setExecuteOnHint(execLoc);
                        }
                    }
                } else {
                    MapAttribute mapAttribute = targetExpressions.get(0).addExpression(name, expression, odidatatype, size,
                            scale);
                    // set the execution location of the expression to the
                    // same as the targetcol.
                    MapExpression me = mapAttribute.getExpression();
                    if (targetColumn.getExecutionLocations() != null && targetColumn.getExecutionLocations().size() > 0) {
                        ExecuteOnLocation execLoc = mapFromExecutionLocationType(
                                targetColumn.getExecutionLocations().get(0));
                        me.setExecuteOnHint(execLoc);
                    }
                }

            }
        }

    }


    /* (non-Javadoc)
     * @see one.jodi.odi12.mappings.ExpressionsBuilder#setMappingFields(oracle.odi.domain.mapping.MapRootContainer, one.jodi.etl.internalmodel.Transformation, java.util.List, boolean, oracle.odi.domain.model.OdiDataStore)
     */
    @Override
    public void setMappingFields(final MapRootContainer mapping, final Transformation transformation,
                                 final List<IMapComponent> targetComponents, final boolean useExpressions,
                                 final OdiDataStore targetDataStore) throws AdapterException, MappingException {
        if (!transformation.isTemporary()) {
            assert (targetComponents.get(0) != null);
            assert (targetComponents.get(0).getPersistentComponent() != null);
        }
        if (transformation.getMappings() != null) {
            for (Targetcolumn targetColumn : transformation.getMappings().getTargetColumns()) {
                for (MapAttribute a : targetComponents.get(0).getAttributes()) {
                    for (int dataSetIndex = 0; dataSetIndex < targetColumn.getMappingExpressions()
                            .size(); dataSetIndex++) {
                        String mappingExpression = targetColumn.getMappingExpressions().get(dataSetIndex);
                        if (a.getName().equals(targetColumn.getName()) && dataSetIndex == 0) {
                            final String expressionText;
                            if (transformation.getMappings().isDistinct() && !mappingExpression.toLowerCase().contains("nextval")) {
                                expressionText = ComponentPrefixType.DISTINCT.getAbbreviation() + "."
                                        + targetColumn.getName();
                            } else if (transformation.useExpressions()) {
                                expressionText = ComponentPrefixType.TARGET_EXPRESSIONS.getAbbreviation() + "."
                                        + targetColumn.getName();
                            } else if (targetColumn.getParent().getParent().getMaxDatasetNumber() > 1) {
                                expressionText = ComponentPrefixType.SETCOMPONENT.getAbbreviation() + "."
                                        + targetColumn.getName();
                            } else if (targetColumn.getParent().isAggregateTransformation(1)) {
                                expressionText = "D" + (dataSetIndex + 1) + "_" + ComponentPrefixType.AGGREGATE.getAbbreviation() + "."
                                        + targetColumn.getName();
                            } else if (mappingExpression.toLowerCase().contains("nextval")) {
                                expressionText = null;
                            } else {
                                expressionText = transformation.getDatasets().get(dataSetIndex)
                                        .translateExpression(mappingExpression);
                            }
                            a.setExpressionText(targetComponents.get(0).getConnectorPoints().get(dataSetIndex),
                                    expressionText);
                        }
                    }
                }
                String[] expressionsArray = getTranslation(targetColumn, transformation);
                String expression;
                if (transformation.getMappings().isDistinct()) {
                    expression = ComponentPrefixType.DISTINCT.getAbbreviation() + "." + targetColumn.getName();
                } else if (transformation.useExpressions()) {
                    expression = ComponentPrefixType.TARGET_EXPRESSIONS.getAbbreviation() + "."
                            + targetColumn.getName();
                } else if (transformation.getMaxDatasetNumber() > 1) {
                    expression = ComponentPrefixType.SETCOMPONENT.getAbbreviation() + "." + targetColumn.getName();
                } else if (transformation.getMappings().isAggregateTransformation(1)) {
                    expression = "D1_" + ComponentPrefixType.AGGREGATE.getAbbreviation() + "." + targetColumn.getName();
                } else {
                    if (expressionsArray.length > 0) {
                        expression = expressionsArray[0];
                    } else {
                        expression = " null ";
                    }
                }
                Optional<String> sequenceMappingExpression = targetColumn.getMappingExpressions().stream().filter(c -> c.toLowerCase().contains("nextval")).findFirst();
                if (sequenceMappingExpression.isPresent()) {
                    expression = sequenceMappingExpression.get();
                }
                logger.debug("Set TargetExpression;" + expression);
                if (targetComponents.get(0) instanceof DatastoreComponent && !expression.trim().equals("null")) {
                    createTgtExpression(targetComponents.get(0), targetDataStore, targetColumn, transformation,
                            expression);
                }
            }
        }

    }

    /**
     * Get the internal representation of the expression of the column, for
     * instance if the alias of the source was SRC and the expression was
     * (SRC.KEY + 1) and there was only 1 Dataset this translates to (D1SRC.KEY
     * + 1)
     *
     * @param targetColumn
     * @param transformation
     * @return
     */
    private String[] getTranslation(final Targetcolumn targetColumn, final Transformation transformation) {
        String[] array = new String[targetColumn.getMappingExpressions().size()];
        targetColumn.getMappingExpressions().toArray(array);
        String[] translated = new String[targetColumn.getMappingExpressions().size()];
        for (int counter = 0; counter < array.length; counter++) {
            String expression = array[counter];
            translated[counter] = transformation.getDatasets().get(counter).translateExpression(expression);
        }
        return translated;
    }

    /**
     * This method is used for mapping expressions to targetdatastores
     *
     * @param targetDatastoreComponent2
     * @param tgtTable
     * @param propertyName
     * @param prefix                    @param targetcolumn @param useExpressions @param
     *                                  transformation @throws MappingException @throws
     *                                  AdapterException @throws Exception
     */
    private void createTgtExpression(final IMapComponent targetDatastoreComponent2, final OdiDataStore tgtTable,
                                     final Targetcolumn targetcolumn, final Transformation transformation, final String expression)
            throws AdapterException, MappingException {
        DatastoreComponent.findAttributeForColumn(targetDatastoreComponent2, tgtTable.getColumn(targetcolumn.getName()))
                .setExpressionText(expression);
    }


    private MapExpression.ExecuteOnLocation mapFromExecutionLocationType(
            final ExecutionLocationtypeEnum executionLocation) {
        if (executionLocation == null) {
            return null;
        }
        switch (executionLocation) {
            case SOURCE:
                return MapExpression.ExecuteOnLocation.SOURCE;
            case TARGET:
                return MapExpression.ExecuteOnLocation.TARGET;
            default:
                return MapExpression.ExecuteOnLocation.STAGING;
        }
    }


}
