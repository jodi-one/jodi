package one.jodi.tools;

import one.jodi.core.model.Common;
import one.jodi.core.model.Targetcolumn;
import one.jodi.core.model.impl.CommonImpl;
import one.jodi.core.model.impl.TargetcolumnImpl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;


public class ModelMethods {

    private static List<String> createNormalizedList(List<String> in) {
        ArrayList<String> out = new ArrayList<String>();
        for (String s : in) {
            out.add(s.toUpperCase().trim());
        }
        return out;
    }

    public static one.jodi.etl.internalmodel.Targetcolumn getTargetcolumn(String name, one.jodi.etl.internalmodel.Transformation transformation) {
        for (one.jodi.etl.internalmodel.Targetcolumn targetcolumn : transformation.getMappings().getTargetColumns()) {
            if (targetcolumn.getName().equalsIgnoreCase(name)) {
                return targetcolumn;
            }
        }

        return null;
    }

    public static TargetcolumnImpl getTargetcolumn(String name, one.jodi.core.model.Transformation transformation) {
        if (transformation != null && transformation.getMappings() != null && transformation.getMappings().getTargetColumn() != null) {
            for (Targetcolumn targetcolumn : transformation.getMappings().getTargetColumn()) {
                if (targetcolumn.getName().equalsIgnoreCase(name)) {
                    return (TargetcolumnImpl) targetcolumn;
                }
            }
        }

        return null;
    }


    public static Map<String, List<String>> getTargetcolumns(one.jodi.etl.internalmodel.Transformation enrichedTransformation) {
        HashMap<String, List<String>> result = new HashMap<String, List<String>>();
        List<one.jodi.etl.internalmodel.Targetcolumn> enrichedTargetColumns = enrichedTransformation.getMappings().getTargetColumns();
        for (one.jodi.etl.internalmodel.Targetcolumn tc : enrichedTargetColumns) {
            result.put(tc.getName(), createNormalizedList(tc.getMappingExpressions()));
        }

        return result;
    }


    public static Map<String, List<String>> getTargetcolumns(one.jodi.core.model.Transformation transformation) {
        HashMap<String, List<String>> result = new HashMap<String, List<String>>();
        List<one.jodi.core.model.Targetcolumn> targetcolumns = transformation.getMappings().getTargetColumn();
        for (one.jodi.core.model.Targetcolumn tc : targetcolumns) {
            if (tc.getMappingExpressions() == null)
                continue;
            result.put(tc.getName(), createNormalizedList(tc.getMappingExpressions().getExpression()));

        }


        return result;
    }

    /**
     * There is no equals method.
     *
     * @param transformation
     * @param name
     */
    public static void removeTargetColumn(one.jodi.core.model.Transformation transformation, String name) {
        for (one.jodi.core.model.Targetcolumn tc : transformation.getMappings().getTargetColumn()) {
            if (name.equals(tc.getName())) {
                transformation.getMappings().getTargetColumn().remove(tc);
                break;
            }
        }
    }

    /**
     * @param expression in the form SRC.C1 + EXP.C2
     * @param columns        of expressions to descope eg EXP.C1 -> SRC.C1, EXP.C2 -> SRC.C2
     * @return expression
     */
    public static String telescopeNamesInExpression(String expression, Map<String, String> columns) {

        return expression;
    }


    /**
     * Remove target columns from internal model that are similarly defined in external model.
     * <p>
     * Similarly means target column name and expressions are identical case insensitive.
     *
     * @param transformation
     * @param enrichedTransformation
     */
    public static void removeSuperfluousTargetColumns(one.jodi.core.model.Transformation transformation, one.jodi.etl.internalmodel.Transformation enrichedTransformation, List<String> skipColumns) {
        Map<String, List<String>> internalTCs = getTargetcolumns(enrichedTransformation);
        Map<String, List<String>> externalTCs = getTargetcolumns(transformation);

        for (String key : externalTCs.keySet()) {
            if (skipColumns.contains(key))
                continue;

            List<String> list = internalTCs.get(key);
            if (list != null && list.size() > 0) {
                list = new ArrayList<String>(list);
                list.removeAll(externalTCs.get(key));
                if (list.size() == 0) {
                    removeTargetColumn(transformation, key);
                }
            }
        }
    }

    /**
     * Traverse an Object looking for children that implement the Common interface, setting the parent
     * so that the call to Common.getParent() returns correct value (not null.)
     *
     * @param transformation
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    public static void setCommonParent(one.jodi.core.model.Transformation transformation) {
        try {
            setCommon(transformation);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Unknown error generated during setting of parent in Transformation object graph.");
        }
    }

    private static void setCommon(Common o) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        Collection<Common> children = getCommonChildren(o);
        for (Common common : children) {
            ((CommonImpl) common).afterUnmarshal(null, o);
            setCommon(common);
        }
    }


    @SuppressWarnings("unchecked")
    private static Collection<Common> getCommonChildren(Object o) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        ArrayList<Common> children = new ArrayList<Common>();

        for (Method method : o.getClass().getMethods()) {
            if (!(method.getParameterTypes().length == 0 && method.getName().startsWith("get") && !method.getName().equals("getParent")))
                continue;

            if (Common.class.isAssignableFrom(method.getReturnType())) {
                Common child = (Common) method.invoke(o);
                if (child != null)
                    children.add(child);
            } else {
                boolean aCollection = false;
                for (Class<?> c : method.getReturnType().getInterfaces()) {
                    if (c.equals(Collection.class)) {
                        aCollection = true;
                    }
                }

                Type returnType = method.getGenericReturnType();
                if (!aCollection)
                    continue;
                if (!(returnType instanceof ParameterizedType))
                    continue;

                for (Type typeArgument : ((ParameterizedType) returnType).getActualTypeArguments()) {
                    if (Common.class.isAssignableFrom((Class<?>) typeArgument)) {
                        children.addAll((Collection<Common>) method.invoke(o));
                    }
                }
            }
        }

        return children;
    }


}
