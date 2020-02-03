package one.jodi.tools;


import one.jodi.base.model.types.DataStore;
import one.jodi.base.model.types.DataStoreColumn;
import one.jodi.base.model.types.SCDType;
import one.jodi.core.config.PropertyValueHolder;
import one.jodi.core.model.Properties;
import one.jodi.core.model.*;
import oracle.odi.domain.model.OdiColumn;
import oracle.odi.domain.model.OdiDataStore;

import java.util.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InputModelMockHelper {

    public final static String TARGET_STORE = "targetdatastore";

    public static Dataset createMockDataset(final String alias, final String name, final String model) {
        Dataset mockDS = mock(Dataset.class);
        Source mockSource = createMockSource(alias, name, model);

        when(mockDS.getSource()).thenReturn(Collections.singletonList(
                mockSource));
        when(mockDS.getSetOperator()).thenReturn(SetOperatorTypeEnum.UNION_ALL);
        when(mockDS.getName()).thenReturn("datasetname");
        return mockDS;
    }

    public static one.jodi.etl.internalmodel.Dataset createMockETLDataset(final String alias, final String name, final String model) {
        one.jodi.etl.internalmodel.Dataset mockDS = mock(one.jodi.etl.internalmodel.Dataset.class);
        when(mockDS.getName()).thenReturn("");
        one.jodi.etl.internalmodel.Source mockSource = createMockETLSource(alias, name, model);

        when(mockDS.getSources()).thenReturn(Collections.singletonList(mockSource));
        when(mockDS.getSetOperator()).thenReturn(one.jodi.etl.internalmodel.SetOperatorTypeEnum.UNION_ALL);
        when(mockDS.getName()).thenReturn("datasetname");
        when(mockSource.getParent()).thenReturn(mockDS);
        return mockDS;
    }

    public static Dataset createMockDataset(final Datasets parent, final String[] aliases, final String[] names, final String[] models, final String[] filters, JoinTypeEnum joinType) {
        assert (aliases != null);
        assert (names != null);
        assert (models != null);
        assert (filters != null);
        assert (aliases.length == names.length);
        assert (aliases.length == models.length);
        assert (aliases.length == filters.length);
        Dataset mockDS = mock(Dataset.class);

        List<Source> sourceList = new ArrayList<Source>();

        for (int i = 0; i < aliases.length; i++) {
            Source mockSource = createMockSource(aliases[i], names[i], models[i]);

            when(mockSource.getJoin()).thenReturn(aliases[i] + ".join");
            when(mockSource.getFilter()).thenReturn(filters[i]);
            when(mockSource.getJoinType()).thenReturn(joinType);
            Lookups l = createMockLookups(aliases[i] + ".join");
            when(mockSource.getLookups()).thenReturn(l);
            sourceList.add(mockSource);
        }

        when(mockDS.getSource()).thenReturn(sourceList);
        when(mockDS.getSetOperator()).thenReturn(SetOperatorTypeEnum.UNION_ALL);
        when(mockDS.getName()).thenReturn("datasetname");
        when(mockDS.getParent()).thenReturn(parent);

        return mockDS;
    }

    public static one.jodi.etl.internalmodel.Dataset createMockETLDataset(final one.jodi.etl.internalmodel.Transformation parent, final String[] aliases, final String[] names, final String[] models, final String[] filters, one.jodi.etl.internalmodel.JoinTypeEnum joinType) {
        assert (aliases != null);
        assert (names != null);
        assert (models != null);
        assert (filters != null);
        assert (aliases.length == names.length);
        assert (aliases.length == models.length);
        assert (aliases.length == filters.length);
        one.jodi.etl.internalmodel.Dataset mockDS = mock(one.jodi.etl.internalmodel.Dataset.class);

        List<one.jodi.etl.internalmodel.Source> sourceList = new ArrayList<one.jodi.etl.internalmodel.Source>();

        for (int i = 0; i < aliases.length; i++) {
            one.jodi.etl.internalmodel.Source mockSource = createMockETLSource(aliases[i], names[i], models[i]);

            when(mockSource.getJoin()).thenReturn(aliases[i] + ".join");
            when(mockSource.getFilter()).thenReturn(filters[i]);
            when(mockSource.getJoinType()).thenReturn(joinType);
            List<one.jodi.etl.internalmodel.Lookup> lookups = createMockETLLookups(aliases[i] + ".join");
            when(mockSource.getLookups()).thenReturn(lookups);
            sourceList.add(mockSource);
        }

        when(mockDS.getSources()).thenReturn(sourceList);
        when(mockDS.getSetOperator()).thenReturn(one.jodi.etl.internalmodel.SetOperatorTypeEnum.UNION_ALL);
        when(mockDS.getName()).thenReturn("datasetname");
        when(mockDS.getParent()).thenReturn(parent);

        return mockDS;
    }

    public static Lookups createMockLookups(String join) {
        Lookups l = mock(Lookups.class);
        List<Lookup> ll = new ArrayList<Lookup>();

        Lookup lu = mock(Lookup.class);
        when(lu.getJoin()).thenReturn(join);
        when(lu.getLookupType()).thenReturn(LookupTypeEnum.SCALAR);
        when(lu.getAlias()).thenReturn("alias");
        ll.add(lu);
        when(l.getLookup()).thenReturn(ll);
        return l;
    }

    public static List<one.jodi.etl.internalmodel.Lookup> createMockETLLookups(String join) {
        one.jodi.etl.internalmodel.Lookup l = mock(one.jodi.etl.internalmodel.Lookup.class);
        List<one.jodi.etl.internalmodel.Lookup> list = new ArrayList<one.jodi.etl.internalmodel.Lookup>();

        when(l.getJoin()).thenReturn(join);
        when(l.getLookupType()).thenReturn(one.jodi.etl.internalmodel.LookupTypeEnum.SCALAR);
        when(l.getLookupDataStore()).thenReturn("lookup");
        when(l.getAlias()).thenReturn("alias");
        when(l.getModel()).thenReturn("model");
        list.add(l);
        return list;
    }

    public static Datasets createMockDatasets(final String alias, final String name, final String model) {
        Datasets mockDS = mock(Datasets.class);

        Dataset mock1 = createMockDataset(alias, name, model);

        when(mockDS.getDataset()).thenReturn(Collections.singletonList(mock1));

        return mockDS;
    }

    public static Datasets createMockDatasets(final String[] aliases,
                                              final String[] names, final String[] models) {
        assert (aliases != null);
        assert (names != null);
        assert (models != null);
        assert (aliases.length == names.length && aliases.length == models.length);

        List<Dataset> dsList = new ArrayList<Dataset>();

        Datasets mockDS = mock(Datasets.class);
        for (int i = 0; i < names.length; i++) {
            Dataset mock = createMockDataset(aliases[i], names[i], models[i]);
            when(mock.getParent()).thenReturn(mockDS);
            dsList.add(mock);
        }

        when(mockDS.getDataset()).thenReturn(dsList);

        return mockDS;
    }


    public static List<one.jodi.etl.internalmodel.Dataset> createMockETLDatasets(one.jodi.etl.internalmodel.Transformation transformation,
                                                                                 final String[] aliases,
                                                                                 final String[] names,
                                                                                 final String[] models) {
        assert (aliases != null);
        assert (names != null);
        assert (models != null);
        assert (aliases.length == names.length && aliases.length == models.length);

        List<one.jodi.etl.internalmodel.Dataset> dsList = new ArrayList<one.jodi.etl.internalmodel.Dataset>();

        for (int i = 0; i < names.length; i++) {
            one.jodi.etl.internalmodel.Dataset ds = createMockETLDataset(aliases[i], names[i], models[i]);
            when(ds.getParent()).thenReturn(transformation);
            dsList.add(ds);
        }


        return dsList;
    }


    public static Mappings createMockMappings(final String targetDataStore, String dataType) {
        Mappings mockMap = mock(Mappings.class);
        when(mockMap.getTargetDataStore()).thenReturn(targetDataStore);

        Targetcolumn mockColumn = mock(Targetcolumn.class);
        when(mockColumn.getName()).thenReturn("colname");

        when(mockColumn.isKey()).thenReturn(true);
        MappingExpressions mockExps = mock(MappingExpressions.class);
        when(mockExps.getExpression()).thenReturn(Collections.<String>singletonList("alias1.expr"));
        when(mockColumn.getMappingExpressions()).thenReturn(mockExps);
        if (dataType != null) {
            Properties properties = mock(Properties.class);
            when(properties.getDataType()).thenReturn(dataType);
            when(mockColumn.getProperties()).thenReturn(properties);
        }
        when(mockMap.getTargetColumn()).thenReturn(Collections.<Targetcolumn>singletonList(mockColumn));
        return mockMap;
    }

    public static one.jodi.etl.internalmodel.Mappings createMockETLMappings(final String targetDataStore, String dataType, String model) {
        one.jodi.etl.internalmodel.Mappings mockMap = mock(one.jodi.etl.internalmodel.Mappings.class);
        when(mockMap.getTargetDataStore()).thenReturn(targetDataStore);
        when(mockMap.getModel()).thenReturn(model);

        one.jodi.etl.internalmodel.ExecutionLocationtypeEnum el = one.jodi.etl.internalmodel.ExecutionLocationtypeEnum.SOURCE;
        one.jodi.etl.internalmodel.Targetcolumn mockColumn = mock(one.jodi.etl.internalmodel.Targetcolumn.class);
        when(mockColumn.getName()).thenReturn("colname");
        when(mockColumn.isUpdateKey()).thenReturn(true);
        when(mockColumn.getExecutionLocations()).thenReturn(Collections.<one.jodi.etl.internalmodel.ExecutionLocationtypeEnum>singletonList(el));

        when(mockColumn.getMappingExpressions()).thenReturn(Collections.<String>singletonList("alias1.expr"));
        if (dataType != null) {
            when(mockColumn.getDataType()).thenReturn(dataType);
        }
        when(mockMap.getTargetColumns()).thenReturn(Collections.<one.jodi.etl.internalmodel.Targetcolumn>singletonList(mockColumn));
        return mockMap;
    }

    public static Mappings createMockMappings(final String targetDataStore, String[] columnList, String dataType) {

        Mappings mockMap = mock(Mappings.class);
        when(mockMap.getTargetDataStore()).thenReturn(targetDataStore);
        List<Targetcolumn> mockTargetColumns = new ArrayList<Targetcolumn>();

        for (String columnName : columnList) {
            int count;
            try {
                count = Integer.parseInt(columnName.substring(columnName.length() - 1));
            } catch (RuntimeException e) {
                //last character is not a number - use default value
                count = 1;
            }
            Boolean b;
            switch (count % 3) {
                case 1:
                    b = false;
                    break;
                case 2:
                    b = true;
                    break;
                default:
                    b = null;
                    break;
            }

            Targetcolumn mockColumn = mock(Targetcolumn.class);
            when(mockColumn.getName()).thenReturn(columnName);

            when(mockColumn.isKey()).thenReturn(true);
            when(mockColumn.isMandatory()).thenReturn(b);
            MappingExpressions mockExps = mock(MappingExpressions.class);
            when(mockExps.getExpression()).thenReturn(Collections.<String>singletonList("alias1.expr"));
            when(mockColumn.getMappingExpressions()).thenReturn(mockExps);

            if (dataType != null) {
                Properties properties = mock(Properties.class);
                when(properties.getDataType()).thenReturn(dataType);
                when(properties.getLength()).thenReturn(10);
                when(properties.getScale()).thenReturn(1);
                when(mockColumn.getProperties()).thenReturn(properties);
            }
            mockTargetColumns.add(mockColumn);
        }
        when(mockMap.getTargetColumn()).thenReturn(mockTargetColumns);
        return mockMap;
    }


    public static one.jodi.etl.internalmodel.Mappings createMockETLMappings(final String targetDataStore, String[] columnList, String dataType, String model) {

        one.jodi.etl.internalmodel.Mappings mockMap = mock(one.jodi.etl.internalmodel.Mappings.class);
        when(mockMap.getTargetDataStore()).thenReturn(targetDataStore);
        when(mockMap.getModel()).thenReturn(model);
        List<one.jodi.etl.internalmodel.Targetcolumn> mockTargetColumns = new ArrayList<one.jodi.etl.internalmodel.Targetcolumn>();

        for (String columnName : columnList) {
            int count;
            try {
                count = Integer.parseInt(columnName.substring(columnName.length() - 1));
            } catch (RuntimeException e) {
                //last character is not a number - use default value
                count = 1;
            }

            one.jodi.etl.internalmodel.Targetcolumn mockColumn = mock(one.jodi.etl.internalmodel.Targetcolumn.class);
            when(mockColumn.getName()).thenReturn(columnName);

            when(mockColumn.isUpdateKey()).thenReturn(true);
            when(mockColumn.isMandatory()).thenReturn(count % 2 == 0 ? true : false);
            when(mockColumn.getMappingExpressions()).thenReturn(Collections.<String>singletonList("alias1.expr"));

            if (dataType != null) {
                when(mockColumn.getDataType()).thenReturn(dataType);
            }
            mockTargetColumns.add(mockColumn);
        }
        when(mockMap.getTargetColumns()).thenReturn(mockTargetColumns);
        return mockMap;
    }


    public static Dataset createMockMultiSourceDataset(final String[] aliases,
                                                       final String[] names) {
        assert (aliases != null);
        assert (names != null);
        assert (aliases.length == names.length);

        Dataset mockDS = mock(Dataset.class);
        List<Source> sources = new ArrayList<Source>();
        when(mockDS.getSetOperator()).thenReturn(SetOperatorTypeEnum.UNION_ALL);

        for (int i = 0; i < names.length; i++) {
            Source mockSource = createMockSource(aliases[i], names[i], null);
            sources.add(mockSource);
        }

        when(mockDS.getSource()).thenReturn(sources);
        when(mockDS.getName()).thenReturn("datasetname");

        return mockDS;
    }

    public static one.jodi.etl.internalmodel.Dataset createMockMultiSourceETLDataset(final String[] aliases,
                                                                                     final String[] names) {
        assert (aliases != null);
        assert (names != null);
        assert (aliases.length == names.length);

        one.jodi.etl.internalmodel.Dataset mockDS = mock(one.jodi.etl.internalmodel.Dataset.class);
        List<one.jodi.etl.internalmodel.Source> sources = new ArrayList<one.jodi.etl.internalmodel.Source>();
        when(mockDS.getSetOperator()).thenReturn(one.jodi.etl.internalmodel.SetOperatorTypeEnum.UNION_ALL);

        for (int i = 0; i < names.length; i++) {
            one.jodi.etl.internalmodel.Source mockSource = createMockETLSource(aliases[i], names[i], null);
            sources.add(mockSource);
        }

        when(mockDS.getSources()).thenReturn(sources);
        when(mockDS.getName()).thenReturn("datasetname");

        return mockDS;
    }

    public static Source createMockSource(final String alias, final String name, final String model) {
        Source mockSource = mock(Source.class);

        when(mockSource.getAlias()).thenReturn(alias);
        when(mockSource.getName()).thenReturn(name);
        when(mockSource.getModel()).thenReturn(model);
        when(mockSource.getFilter()).thenReturn(alias + ".col1 = " + alias + ".col2");
        when(mockSource.getJoin()).thenReturn(alias + ".col1 = " + alias + ".col2");

        return mockSource;
    }

    public static one.jodi.etl.internalmodel.Source createMockETLSource(String alias, final String name, final String model) {
        one.jodi.etl.internalmodel.Source mockSource = mock(one.jodi.etl.internalmodel.Source.class);

        when(mockSource.getAlias()).thenReturn(alias);
        when(mockSource.getName()).thenReturn(name);
        when(mockSource.getModel()).thenReturn(model);
        when(mockSource.getFilter()).thenReturn(alias + ".col1 = " + alias + ".col2");
        when(mockSource.getJoin()).thenReturn(alias + ".col1 = " + alias + ".col2");

        return mockSource;
    }

    public static Transformation createMockTransformation(String type) {

        return createMockTransformation(TARGET_STORE, type);
    }

    public static one.jodi.etl.internalmodel.Transformation createMockETLTransformation() {
        return createMockETLTransformation(TARGET_STORE);
    }

    public static Transformation createMockTransformation(String transformationName, String type) {
        return createMockTransformation("packagelist",
                new String[]{
                        "alias", "alias"
                }, new String[]{
                        "name", "name"
                }, new String[]{"model", "model"}, transformationName, type);
    }

    public static one.jodi.etl.internalmodel.Transformation createMockETLTransformation(String transformationName) {
        return createMockETLTransformation("packagelist",
                new String[]{
                        "alias", "alias"
                }, new String[]{
                        "name", "name"
                }, new String[]{"model", "model"}, transformationName);
    }


    public static Transformation createMockTransformation(final String[] dsaliases,
                                                          final String[] dsnames, final String[] models) {
        Mappings mockMap = createMockMappings(TARGET_STORE, null);

        return createMockTransformation("packagelist", dsaliases, dsnames, models,
                mockMap);
    }

    public static one.jodi.etl.internalmodel.Transformation createMockETLTransformation(final String[] dsaliases,
                                                                                        final String[] dsnames, final String[] models) {
        one.jodi.etl.internalmodel.Mappings mockMap = createMockETLMappings(TARGET_STORE, null, null);

        return createMockETLTransformation("packagelist", dsaliases, dsnames, models,
                mockMap);
    }


    public static Transformation createMockTransformation(final String packageList,
                                                          final String[] dsaliases, final String[] dsnames, final String[] models, final String target) {

        Mappings mockMap = createMockMappings(target, null);

        return createMockTransformation(packageList, dsaliases, dsnames, models,
                mockMap);
    }

    public static one.jodi.etl.internalmodel.Transformation createMockETLTransformation(final String packageList,
                                                                                        final String[] dsaliases, final String[] dsnames, final String[] models, final String target) {

        one.jodi.etl.internalmodel.Mappings mockMap = createMockETLMappings(target, null, null);

        return createMockETLTransformation(packageList, dsaliases, dsnames, models,
                mockMap);
    }

    public static Transformation createMockTransformation(final String packageList,
                                                          final String[] dsaliases, final String[] dsnames, final String[] models,
                                                          final String target, final String type) {

        Mappings mockMap = createMockMappings(target, type);

        return createMockTransformation(packageList, dsaliases, dsnames, models,
                mockMap);
    }

    public static Transformation createMockTransformation(final String packageList,
                                                          final String[] dsaliases, final String[] dsnames, final String[] models) {
        return createMockTransformation(packageList, dsaliases, dsnames, models, TARGET_STORE);
    }

    public static one.jodi.etl.internalmodel.Transformation createMockETLTransformation(final String packageList,
                                                                                        final String[] dsaliases, final String[] dsnames, final String[] models) {
        return createMockETLTransformation(packageList, dsaliases, dsnames, models, TARGET_STORE);
    }

    public static Transformation createMockTransformation(final String packageList,
                                                          final String[] dsaliases, final String[] dsnames, String[] models,
                                                          final Mappings mockMap) {
        Transformation mockT = mock(Transformation.class);
        Datasets mockDS = createMockDatasets(dsaliases, dsnames, models);
        when(mockT.getDatasets()).thenReturn(mockDS);

        when(mockMap.getParent()).thenReturn(mockT);
        when(mockT.getMappings()).thenReturn(mockMap);
        when(mockT.getPackageList()).thenReturn(packageList);

        return mockT;
    }

    public static one.jodi.etl.internalmodel.Transformation createMockETLTransformation(
            final String packageList, final String[] dsaliases,
            final String[] dsnames, String[] models,
            final one.jodi.etl.internalmodel.Mappings mockMap) {
        one.jodi.etl.internalmodel.Transformation mockT = mock(one.jodi.etl.internalmodel.Transformation.class);
        List<one.jodi.etl.internalmodel.Dataset> datasets = createMockETLDatasets(mockT, dsaliases, dsnames, models);
        when(mockT.getDatasets()).thenReturn(datasets);

        when(mockMap.getParent()).thenReturn(mockT);
        when(mockT.getMappings()).thenReturn(mockMap);
        when(mockT.getPackageList()).thenReturn(packageList);

        return mockT;
    }


    public static OdiDataStore createMockOdiDataStore() {
        OdiDataStore dsmock = mock(OdiDataStore.class);
        OdiColumn colmock = mock(OdiColumn.class);

        when(colmock.getName()).thenReturn("COL_NAME");
        when(dsmock.getColumns()).thenReturn(Collections.<OdiColumn>singletonList(colmock));
        return dsmock;
    }

    public static Targetcolumn createMockTargetcolumn(String name, String... expressions) {
        Targetcolumn col = mock(Targetcolumn.class);
        when(col.getName()).thenReturn(name);

        MappingExpressions expr = mock(MappingExpressions.class);
        List<String> exprList = new ArrayList<String>();
        for (String e : expressions) {
            exprList.add(e);
        }
//		exprList.addAll(Arrays.asList(expressions));
        when(expr.getExpression()).thenReturn(exprList);
        when(col.getMappingExpressions()).thenReturn(expr);

        return col;
    }

    public static one.jodi.etl.internalmodel.Targetcolumn createMockETLTargetcolumn(String name, String... expressions) {
        one.jodi.etl.internalmodel.Targetcolumn col = mock(one.jodi.etl.internalmodel.Targetcolumn.class);
        when(col.getName()).thenReturn(name);

        List<String> exprList = new ArrayList<String>();
        for (String e : expressions) {
            exprList.add(e);
        }
//		exprList.addAll(Arrays.asList(expressions));
        when(col.getMappingExpressions()).thenReturn(exprList);

        return col;
    }

    public static Map<String, DataStoreColumn> createDataStoreColumns(
            final DataStore parent, final String[] names) {
        Map<String, DataStoreColumn> result = new HashMap<String, DataStoreColumn>();

        int position = 1;
        for (final String name : names) {
            final int pos = position++;
            result.put(name, new DataStoreColumn() {
                @Override
                public DataStore getParent() {
                    return parent;
                }

                @Override
                public String getColumnDataType() {
                    return name + "_DataType";
                }

                @Override
                public int getLength() {
                    return 10;
                }

                @Override
                public int getScale() {
                    return 1;
                }

                @Override
                public SCDType getColumnSCDType() {
                    return SCDType.ADD_ROW_ON_CHANGE;
                }

                @Override
                public boolean hasNotNullConstraint() {
                    return false;
                }

                @Override
                public String getName() {
                    return name;
                }

                @Override
                public String getDescription() {
                    return "Column Description";
                }

                @Override
                public int getPosition() {
                    return pos;
                }
            });
        }

        return result;
    }


    public static PropertyValueHolder createMockPropertyValueHolder(String key, String value) {
        PropertyValueHolder mock = mock(PropertyValueHolder.class);

        when(mock.getKey()).thenReturn(key);
        when(mock.getString()).thenReturn(value);
        return mock;
    }

    public static PropertyValueHolder createMockPropertyValueHolder(String key, List<String> value) {
        PropertyValueHolder mock = mock(PropertyValueHolder.class);

        when(mock.getKey()).thenReturn(key);
        when(mock.getList()).thenReturn(value);
        return mock;
    }
}
