package one.jodi.tools.impl;


import com.google.inject.Inject;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.model.Properties;
import one.jodi.core.model.Targetcolumn;
import one.jodi.core.model.impl.MappingExpressionsImpl;
import one.jodi.core.model.impl.MappingsImpl;
import one.jodi.core.model.impl.PropertiesImpl;
import one.jodi.core.model.impl.TargetcolumnImpl;
import one.jodi.tools.ModelBuildingStep;
import one.jodi.tools.ModelMethods;
import one.jodi.tools.dependency.MappingHolder;
import one.jodi.tools.dependency.MappingType;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.adapter.relational.IDataStore;
import oracle.odi.domain.mapping.IMapComponent;
import oracle.odi.domain.mapping.MapAttribute;
import oracle.odi.domain.mapping.MapComponent;
import oracle.odi.domain.mapping.Mapping;
import oracle.odi.domain.mapping.component.DatastoreComponent;
import oracle.odi.domain.mapping.exception.MapConnectionException;
import oracle.odi.domain.mapping.exception.MappingException;
import oracle.odi.domain.mapping.expression.MapExpression;
import oracle.odi.domain.mapping.properties.PropertyException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TargetColumnBuildingStep implements ModelBuildingStep {

    @SuppressWarnings("unused")
    private final JodiProperties properties;

    private final Logger logger = LogManager.getLogger(TargetColumnBuildingStep.class);

    @Inject
    public TargetColumnBuildingStep(JodiProperties properties) {
        this.properties = properties;
    }


    @Override
    public void processPreEnrichment(
            one.jodi.core.model.Transformation transformation,
            Mapping mapping,
            MappingHolder mappingHolder) {

        try {
            pre(transformation, mapping, mappingHolder);

        } catch (AdapterException ae) {
            handleError(ae, mapping);
        } catch (PropertyException pe) {
            handleError(pe, mapping);
        } catch (MappingException me) {
            handleError(me, mapping);

        }
    }

    private void handleError(Exception e, Mapping mapping) {
        String error = "Cannot set target columns for mapping '" + mapping.getName() + "' due to ODI exception";
        logger.error(error);
        e.printStackTrace();
        throw new RuntimeException(error);
    }

    private void pre(one.jodi.core.model.Transformation transformation, Mapping mapping, MappingHolder mappingHolder)
            throws AdapterException, PropertyException, MappingException {
        MappingsImpl mappings = new MappingsImpl();
        transformation.setMappings(mappings);
        for (IMapComponent mo : mapping.getTargets()) {
            DatastoreComponent targetDatastore = (DatastoreComponent) mo;
            IDataStore targetDS = targetDatastore.getBoundDataStore();
            mappings.setTargetDataStore(targetDS.getName());
            for (MapAttribute ma : mo.getAttributes()) {
                String name = ma.getName();

                TargetcolumnImpl tc = new TargetcolumnImpl();
                tc.setName(name);
                MappingExpressionsImpl mei = new MappingExpressionsImpl();
                List<String> expressions = getExpression(ma, targetDatastore, mappingHolder);
                mei.getExpression().addAll(expressions);
                tc.setMappingExpressions(mei);
                mappings.getTargetColumn().add(tc);


            }
        }
    }


    // Retrieve expression based on pattern.
    private List<String> getExpression(MapAttribute mapAttribute, DatastoreComponent targetDatastore, MappingHolder mappingHolder) throws MapConnectionException, MappingException {
        MapComponent expressionComponent = null;
        MapComponent setComponent = null;
        for (IMapComponent imc : targetDatastore.getUpstreamConnectedLeafComponents()) {
            if ("EXPRESSION".equalsIgnoreCase(imc.getComponentTypeName())) {
                expressionComponent = (MapComponent) imc;
                break;
            }
        }


        if (mappingHolder.getType() == MappingType.SourceToSetToExpressionToTarget) {
            for (IMapComponent imc : expressionComponent.getUpstreamConnectedLeafComponents()) {
                if ("SET".equalsIgnoreCase(imc.getComponentTypeName())) {
                    setComponent = (MapComponent) imc;
                    break;
                }
            }
        }

        ArrayList<String> expressions = new ArrayList<String>();
        for (MapExpression mapExpression : mapAttribute.getExpressions()) {
            if (expressionComponent != null && setComponent != null) {

                expressions.addAll(telescopeExpression(Arrays.asList(mapExpression.getText()), expressionComponent, setComponent));
            } else if (expressionComponent != null) {
                expressions.addAll(telescopeExpression(Arrays.asList(mapExpression.getText()), expressionComponent));
            } else {
                expressions.add(mapExpression.getText());
            }
        }

        return expressions;
    }

    /**
     * Telescope expression to replace current value with mapComponent's value
     *
     * @param expression    expression that needs to be telescoped
     * @param mapComponents component that (possibly) contains resolvable references.  Recursive eats away at front of aray.
     * @return
     * @throws AdapterException
     * @throws MappingException
     */
    private List<String> telescopeExpression(List<String> expressions, MapComponent... mapComponents) throws AdapterException, MappingException {
        if (mapComponents.length == 0) {
            //logger.warn("returning expressions " + expressions);
            return expressions;
        }
        MapComponent mapComponent = mapComponents[0];
        mapComponents = Arrays.copyOfRange(mapComponents, 1, mapComponents.length);
        String type = mapComponent.getComponentTypeName();

        ArrayList<String> telescopedExpressions = new ArrayList<String>();

        int numberExpressionsToReturn = mapComponent.getAttributes().get(0).getExpressions().size();

        for (String expression : expressions) {
            for (int i = 0; i < numberExpressionsToReturn; i++) {
                String newExpression = expression;

                for (MapAttribute mapAttribute : mapComponent.getAttributes()) {
                    MapExpression mapExpression = mapAttribute.getExpression(i);
                    //logger.warn("EXPRESSION " + i + " " + mapExpression.getText());
                    newExpression = newExpression.replace(mapComponent.getName() + "." + mapAttribute.getName(), mapExpression.getText());
                    //logger.warn(mapComponent.getName() + "." + mapAttribute.getName() + " -> " + mapExpression.getText());
                }
                //logger.warn(type + " '" + expression + "' -> '" + newExpression + "'");
                telescopedExpressions.add(newExpression);
                //logger.warn(type + " adding " + newExpression);
            }
        }


        return telescopeExpression(telescopedExpressions, mapComponents);
    }


    /**
     * Telescope expression to replace current value with mapComponent's value
     *
     * @param expression    expression that needs to be telescoped
     * @param mapComponents component that (possibly) contains resolvable references.  Recursive eats away at front of aray.
     * @return
     * @throws AdapterException
     * @throws MappingException
     */
//	private List<String> telescopeExpression(List<String> expressions, MapComponent...mapComponents) throws AdapterException, MappingException {
//		if(mapComponents.length == 0) {
//			return expressions;
//		}
//		MapComponent mapComponent = mapComponents[0];
//		mapComponents = Arrays.copyOfRange(mapComponents, 1, mapComponents.length);
//		String type = mapComponent.getComponentTypeName();
//		
//		HashMap<String, String> replaceMap = new HashMap<String, String>();
//		ArrayList<String> telescopedExpressions = new ArrayList<String>();
//		
//		
//		for(String expression : expressions) {
//			String regex = "([a-zA-Z_$#0-9\"]+)([.]{1,1})([a-zA-Z_$#0-9\"]+)";
//			Pattern pattern = Pattern.compile(regex);
//			Matcher matcher = pattern.matcher(expression);
//			while (matcher.find()) {
//				String whole = matcher.group();
//				String qualifier = matcher.group(1);
//				String column = matcher.group(3);
//
//				if(qualifier.equalsIgnoreCase(mapComponent.getName())) {
//					// TARGET_EXPRESSIONS == expressionCOmponetBoundName
//					// find attribute/column in expression component
//					
//					for(MapExpression mapExpression : mapComponent.getAttributeExpressions()) {
//						String mapExpressionName = mapExpression.getOwningAttribute().getName();
//						if(mapExpressionName.equalsIgnoreCase(column) ) {
//							replaceMap.put(whole, mapExpression.getText());
//							logger.warn(type + " " + mapExpression.getName() + " " + whole + " -> " + mapExpression.getText());
//						}
//					}
//				}
//			}
//			
//			for(String key : replaceMap.keySet()) {
//				String replace = replaceMap.get(key);
//				expression = expression.replace(key, replaceMap.get(key));
//			}
//			logger.warn(mapComponent.getTypeName() + "  ADDING " + expression);
//			telescopedExpressions.add(expression);
//		}
//		
//		return telescopeExpression(telescopedExpressions, mapComponents);
//	}
//	
    private List<String> telescopeExpressionOLD(List<String> expressions, MapComponent... mapComponents) throws AdapterException, MappingException {
        if (mapComponents.length == 0) {
            return expressions;
        }
        MapComponent mapComponent = mapComponents[0];
        mapComponents = Arrays.copyOfRange(mapComponents, 1, mapComponents.length);
        String type = mapComponent.getComponentTypeName();

        //logger.warn(mapComponent.getTypeName() + "  expressions = " + expressions);

        ArrayList<String> telescopedExpressions = new ArrayList<String>();

        for (MapAttribute mapAttribute : mapComponent.getAttributes()) {
            for (MapExpression mapExpression : mapAttribute.getExpressions()) {
                HashMap<String, String> replaceMap = new HashMap<String, String>();
                for (String expression : expressions) {
                    String regex = "([a-zA-Z_$#0-9\"]+)([.]{1,1})([a-zA-Z_$#0-9\"]+)";
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(expression);
                    while (matcher.find()) {
                        String whole = matcher.group();
                        String qualifier = matcher.group(1);
                        String column = matcher.group(3);

                        if (qualifier.equalsIgnoreCase(mapComponent.getName())) {
                            // TARGET_EXPRESSIONS == expressionCOmponetBoundName
                            // find attribute/column in expression component

                            String mapExpressionName = mapExpression.getOwningAttribute().getName();
                            if (mapExpressionName.equalsIgnoreCase(column)) {
                                replaceMap.put(whole, mapExpression.getText());
                                //logger.warn(type + " " + mapExpression.getName() + " " + whole + " -> " + mapExpression.getText());
                            }
                        }
                    }

                    for (String key : replaceMap.keySet()) {
                        expression = expression.replace(key, replaceMap.get(key));
                    }

                    //logger.warn(mapComponent.getTypeName() + "  ADDING " + expression);
                    telescopedExpressions.add(expression);
                    //break;
                }

            }

        }
		/*
		
		for(String expression : expressions) {
			String regex = "([a-zA-Z_$#0-9\"]+)([.]{1,1})([a-zA-Z_$#0-9\"]+)";
			Pattern pattern = Pattern.compile(regex);
			Matcher matcher = pattern.matcher(expression);
			while (matcher.find()) {
				String whole = matcher.group();
				String qualifier = matcher.group(1);
				String column = matcher.group(3);

				if(qualifier.equalsIgnoreCase(mapComponent.getName())) {
					// TARGET_EXPRESSIONS == expressionCOmponetBoundName
					// find attribute/column in expression component
					
					for(MapExpression mapExpression : mapComponent.getAttributeExpressions()) {
						String mapExpressionName = mapExpression.getOwningAttribute().getName();
						if(mapExpressionName.equalsIgnoreCase(column) ) {
							replaceMap.put(whole, mapExpression.getText());
							logger.warn(type + " " + mapExpression.getName() + " " + whole + " -> " + mapExpression.getText());
						}
					}
				}
			}
			
			for(String key : replaceMap.keySet()) {
				String replace = replaceMap.get(key);
				expression = expression.replace(key, replaceMap.get(key));
			}
			logger.warn(mapComponent.getTypeName() + "  ADDING " + expression);
			telescopedExpressions.add(expression);
		}
		*/
        return telescopeExpression(telescopedExpressions, mapComponents);
    }


    @Override
    public void processPostEnrichment(
            one.jodi.core.model.Transformation transformation,
            one.jodi.etl.internalmodel.Transformation enrichedTransformation, Mapping mapping,
            MappingHolder mappingHolder) {

        try {
            List<String> skipColumns = preservedColumns(mapping, transformation, enrichedTransformation);

            for (IMapComponent mo : mapping.getTargets()) {
                for (MapAttribute mapAttribute : mo.getAttributes()) {
                    if (ModelMethods.getTargetcolumn(mapAttribute.getName(), enrichedTransformation) == null) {
                        Targetcolumn tc = generateUnmappedTargetcolumn(enrichedTransformation, mappingHolder, mapping, mapAttribute);
                        //transformation.getMappings().getTargetColumn().add(tc);
                    }
                }
            }

            ModelMethods.removeSuperfluousTargetColumns(transformation, enrichedTransformation, skipColumns);


        } catch (AdapterException ae) {
            handleError(ae, mapping);
        } catch (PropertyException pe) {
            handleError(pe, mapping);
        } catch (MappingException me) {
            handleError(me, mapping);

        }
    }

    protected TargetcolumnImpl generateUnmappedTargetcolumn(one.jodi.etl.internalmodel.Transformation enrichedTransformation, MappingHolder mappingHolder, Mapping mapping, MapAttribute mapAttribute) throws MappingException {
        TargetcolumnImpl column = new TargetcolumnImpl();
        column.setName(mapAttribute.getName());
        MappingExpressionsImpl mei = new MappingExpressionsImpl();

        List<String> expressions = getExpression(mapAttribute, (DatastoreComponent) mapAttribute.getOwningComponent(), mappingHolder);
        mei.getExpression().addAll(expressions);
        column.setMappingExpressions(mei);
        column.setKey(mapAttribute.isKeyIndicator());
        column.setMandatory(mapAttribute.isCheckNotNullIndicator());

        return column;
    }

    protected List<String> preservedColumns(Mapping mapping, one.jodi.core.model.Transformation transformation, one.jodi.etl.internalmodel.Transformation enrichedTransformation) throws AdapterException, MappingException {
        ArrayList<String> columnsWithOptions = new ArrayList<String>();

        for (IMapComponent mo : mapping.getTargets()) {
            for (MapAttribute ma : mo.getAttributes()) {
                for (one.jodi.etl.internalmodel.Targetcolumn targetcolumn : enrichedTransformation.getMappings().getTargetColumns()) {
                    if (!targetcolumn.getName().equalsIgnoreCase(ma.getName()))
                        continue;
                    String ignore = null;
                    if (ma.isCheckNotNullIndicator() != targetcolumn.isMandatory()) {
                        if (ModelMethods.getTargetcolumn(targetcolumn.getName(), transformation) != null) {
                            ModelMethods.getTargetcolumn(targetcolumn.getName(), transformation).setMandatory(ma.isCheckNotNullIndicator());
                        }
                        //getTargetcolumn(targetcolumn.getName(), transformation).setMandatory(ma.isCheckNotNullIndicator());
                        ignore = targetcolumn.getName();
                        logger.warn("Flags issue. Mapping '" + mapping.getName() + "' and target column '" + ma.getName() + "' specifies mandatory/checkNotNull '" + ma.isCheckNotNull() + "' but strategy set '" + targetcolumn.isMandatory() + "'.");
                    }

                    if (ma.isKeyIndicator() != targetcolumn.isUpdateKey()) {
                        if (ModelMethods.getTargetcolumn(targetcolumn.getName(), transformation) != null) {
                            ModelMethods.getTargetcolumn(targetcolumn.getName(), transformation).setKey(ma.isKeyIndicator());
                        }
                        //getTargetcolumn(targetcolumn.getName(), transformation).setKey(ma.isKeyIndicator());
                        ignore = targetcolumn.getName();
                        logger.warn("Flags issue. Mapping '" + mapping.getName() + "' and target column '" + ma.getName() + "' specifies update key '" + ma.isKeyIndicator() + "' but strategy set '" + targetcolumn.isUpdateKey() + "'.");
                    }

                    if (ignore != null) columnsWithOptions.add(targetcolumn.getName());

                }
            }
        }

        return columnsWithOptions;
    }

    private void traverseFlowForward(IMapComponent root) {
        try {
            for (IMapComponent imc : root.getDownstreamConnectedLeafComponents()) {
                logger.info(" -> " + imc.getName() + "(" + imc.getTypeName() + ") ");
                traverseFlowForward(imc);
            }
        } catch (MapConnectionException e) {
            e.printStackTrace();
        }
    }

    private void traverseFlowReverse(IMapComponent root) {
        try {
            System.err.print(root.getName() + "(" + root.getTypeName() + ")  <- ");

            for (IMapComponent imc : root.getUpstreamConnectedLeafComponents()) {
                traverseFlowReverse(imc);
            }
        } catch (MapConnectionException e) {
            e.printStackTrace();
        }
    }

    // An expression can be derived from both Expression and Datasource components
    // eg
	

	/*
	private String getExpression(MapAttribute mapAttribute) {
		
		mapAttribute.getOwningComponent();
		
		for(MapExpression mapExpression : mapAttribute.getExpressions()) {
			mapExpression.getText();
			
			String regex = "([a-zA-Z_$#0-9\"]+)([.]{1,1})([a-zA-Z_$#0-9\"]+)";
			
			Pattern pattern = Pattern.compile(regex);
			Matcher matcher = pattern.matcher(mapExpression.getText());
			while (matcher.find()) {
				
			}
		}
		
	}
	*/


    protected TargetcolumnImpl getTargetcolumn(String name, one.jodi.core.model.Transformation transformation) {
        for (Targetcolumn targetcolumn : transformation.getMappings().getTargetColumn()) {
            if (targetcolumn.getName().equalsIgnoreCase(name)) {
                return (TargetcolumnImpl) targetcolumn;
            }
        }

        throw new RuntimeException("Cannot find target column with name '" + name + "' on Transformation with name '" + transformation.getName());
    }

    @SuppressWarnings("unused")
    private Properties getTargetcolumnProperties(String name, one.jodi.core.model.Transformation transformation) {
        TargetcolumnImpl targetcolumn = getTargetcolumn(name, transformation);
        Properties props = targetcolumn.getProperties();

        if (props == null) {
            props = new PropertiesImpl();
            targetcolumn.setProperties(props);
        }

        return props;
    }

}
