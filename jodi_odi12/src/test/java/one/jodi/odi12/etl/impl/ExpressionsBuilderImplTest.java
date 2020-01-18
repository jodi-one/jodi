package one.jodi.odi12.etl.impl;

import one.jodi.InputModelMockHelper;
import one.jodi.etl.internalmodel.ComponentPrefixType;
import one.jodi.etl.internalmodel.Dataset;
import one.jodi.etl.internalmodel.Targetcolumn;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.odi.etl.OdiCommon;
import one.jodi.odi12.etl.EtlOperators;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.mapping.MapAttribute;
import oracle.odi.domain.mapping.MapRootContainer;
import oracle.odi.domain.mapping.component.ExpressionComponent;
import oracle.odi.domain.mapping.exception.MapComponentException;
import oracle.odi.domain.mapping.exception.MappingException;
import oracle.odi.domain.mapping.expression.MapExpression;
import oracle.odi.domain.model.OdiModel;
import oracle.odi.domain.topology.OdiDataType;
import oracle.odi.domain.topology.OdiLogicalSchema;
import oracle.odi.domain.topology.OdiTechnology;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;


public class ExpressionsBuilderImplTest {

    @Mock
    OdiCommon odiCommon;
    @Mock
    MapRootContainer mapping;
    @Mock
    EtlOperators etlOperators;
    @Mock
    ExpressionComponent expressionComponent;
    ExpressionsBuilderImpl fixture;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        fixture = new ExpressionsBuilderImpl(odiCommon) {
            @Override
            protected ExpressionComponent createExpressionComponent(MapRootContainer mapping) throws MappingException {
                return expressionComponent;
            }
        };
    }

    @Test
    public void testAdd() throws MapComponentException, MappingException {
        EtlOperators etlOperators = mock(EtlOperators.class);
        when(etlOperators.getTargetExpressions()).thenReturn(Arrays.asList(expressionComponent));

        fixture.addTargetExpressions(mapping, true, etlOperators);

        verify(etlOperators).addTargetExpressions(expressionComponent);
        verify(mapping).addComponent(expressionComponent);
    }

    @Test
    public void testAddNoExpressions() throws MapComponentException, MappingException {
        EtlOperators etlOperators = mock(EtlOperators.class);

        fixture.addTargetExpressions(mapping, false, etlOperators);

        verify(etlOperators, times(0)).addTargetExpressions(expressionComponent);
    }


    @Test
    public void testCreateNoExpression() throws AdapterException, MappingException {
        Transformation transformation = InputModelMockHelper.createMockETLTransformation();
        fixture.createExpressionComponent(mapping, transformation, Arrays.asList(expressionComponent), false);

        verify(expressionComponent, times(0)).addExpression(any(String.class), any(String.class), any(OdiDataType.class), any(Integer.class), any(Integer.class));
    }

    @Test
    public void testCreateExpressionA() throws MappingException {
        testCreateExpressionComponent(1, false, false);
    }

    @Test
    public void testCreateExpressionB() throws MappingException {
        testCreateExpressionComponent(1, false, true);
    }

    @Test
    public void testCreateExpressionC() throws MappingException {
        testCreateExpressionComponent(1, true, true);
    }

    private void testCreateExpressionComponent(int numberDatasets, boolean useExpressions, boolean isAggregate) throws AdapterException, MappingException {
        int n = numberDatasets + 1;
        List<String> aliases = IntStream.range(1, n).mapToObj(i -> "alias" + i).collect(Collectors.toList());
        List<String> names = IntStream.range(1, n).mapToObj(i -> "source" + i).collect(Collectors.toList());
        List<String> models = IntStream.range(1, n).mapToObj(i -> "model").collect(Collectors.toList());
        Transformation transformation = InputModelMockHelper
                .createMockETLTransformation(aliases.toArray(new String[]{}),
                        names.toArray(new String[]{}),
                        models.toArray(new String[]{}));
        when(transformation.getMaxDatasetNumber()).thenReturn(numberDatasets);
        when(transformation.useExpressions()).thenReturn(useExpressions);
        when(transformation.getMappings().isAggregateTransformation(any(Integer.class))).thenReturn(isAggregate);

        OdiModel odiModel = mock(OdiModel.class);
        when(odiCommon.getOdiModel(transformation.getMappings().getModel())).thenReturn(odiModel);
        OdiLogicalSchema odiLogicalSchema = mock(OdiLogicalSchema.class);
        when(odiModel.getLogicalSchema()).thenReturn(odiLogicalSchema);
        OdiTechnology odiTechnology = mock(OdiTechnology.class);
        when(odiLogicalSchema.getTechnology()).thenReturn(odiTechnology);
        OdiDataType odiDataType = mock(OdiDataType.class);
        when(odiTechnology.getDataType(any(String.class))).thenReturn(odiDataType);
        MapAttribute mapAttribute = mock(MapAttribute.class);
        MapExpression mapExpression = mock(MapExpression.class);
        when(mapAttribute.getExpression()).thenReturn(mapExpression);
        for (Dataset ds : transformation.getDatasets()) {
            when(ds.translateExpression(any(String.class))).thenAnswer(
                    (Answer<String>) invocation -> invocation.getArgumentAt(0, String.class));
            for (Targetcolumn tc : transformation.getMappings().getTargetColumns()) {
                when(expressionComponent.addExpression(any(String.class), any(String.class), any(OdiDataType.class), any(Integer.class), any(Integer.class))).thenReturn(mapAttribute);

            }
        }


        fixture.createExpressionComponent(mapping, transformation, Arrays.asList(expressionComponent), useExpressions);


        // verify each expression is added to aggregate.
        int i = 0;

        if (useExpressions) {
            for (Dataset ds : transformation.getDatasets()) {
                for (Targetcolumn tc : transformation.getMappings().getTargetColumns()) {
                    String expression = "";
                    if (numberDatasets > 1)
                        expression = ComponentPrefixType.SETCOMPONENT.getAbbreviation() + "." + tc.getName();

                    else if (transformation.getMappings().isAggregateTransformation((i + 1)))
                        expression = "D" + (i + 1) + "_" + ComponentPrefixType.AGGREGATE.getAbbreviation() + "." + tc.getName();
                    else
                        expression = tc.getMappingExpressions().get(i);
                    verify(expressionComponent).addExpression(tc.getName(), expression, odiDataType, 0, 0);
                }
                i++;
            }
        } else {
            verify(expressionComponent, times(0)).addExpression(any(String.class), any(String.class), any(OdiDataType.class), any(Integer.class), any(Integer.class));
        }

    }
	
	
	/*
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
				} else if (transformation.getMappings().isAggregateTransformation((dataSetIndex+1))) {
					expression = "D"+ (dataSetIndex+1)+"_"+ComponentPrefixType.AGGREGATE.getAbbreviation() + "." + targetColumn.getName();
				} else {
					expression = transformation.getDatasets().get(dataSetIndex).translateExpression(mappingExpression);
				}
				if(transformation.getDatasets().size() > 1){
					if(dataSetIndex == 1){
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
				}else{
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
	 */

}
	