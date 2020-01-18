package one.jodi;

import one.jodi.base.model.types.DataStore;
import one.jodi.base.model.types.DataStoreColumn;
import one.jodi.base.model.types.SCDType;
import one.jodi.core.config.PropertyValueHolder;
import one.jodi.core.model.Dataset;
import one.jodi.core.model.JoinTypeEnum;
import one.jodi.core.model.Lookup;
import one.jodi.core.model.LookupTypeEnum;
import one.jodi.core.model.Mappings;
import one.jodi.core.model.Properties;
import one.jodi.core.model.SetOperatorTypeEnum;
import one.jodi.core.model.Source;
import one.jodi.core.model.Targetcolumn;
import one.jodi.core.model.Transformation;
import one.jodi.core.model.*;
import one.jodi.etl.internalmodel.AggregateFunctionEnum;
import one.jodi.etl.internalmodel.ExecutionLocationtypeEnum;
import one.jodi.etl.internalmodel.RoleEnum;
import one.jodi.etl.internalmodel.*;
import oracle.odi.domain.model.OdiColumn;
import oracle.odi.domain.model.OdiDataStore;
import oracle.odi.domain.project.OdiInterface;
import oracle.odi.domain.project.OdiInterface.TargetDataStore;
import oracle.odi.domain.project.interfaces.*;
import oracle.odi.domain.relational.IColumn;
import oracle.odi.domain.topology.OdiDataType;
import oracle.odi.domain.topology.OdiLogicalSchema;
import oracle.odi.domain.topology.OdiTechnology;
import oracle.odi.domain.xrefs.expression.Expression;
import oracle.odi.domain.xrefs.expression.Expression.SqlGroupType;
import oracle.odi.interfaces.interactive.IInterfaceAction;
import oracle.odi.interfaces.interactive.IInterfaceIssue;
import oracle.odi.interfaces.interactive.IIssueFix;

import java.util.*;
import java.util.stream.Collectors;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("deprecation")
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

    public static one.jodi.etl.internalmodel.Mappings createMockETLMappings(final String targetDataStore, String dataType, String model, int numberDatasets) {
        one.jodi.etl.internalmodel.Mappings mockMap = mock(one.jodi.etl.internalmodel.Mappings.class);
        when(mockMap.getTargetDataStore()).thenReturn(targetDataStore);
        when(mockMap.getModel()).thenReturn(model);

        one.jodi.etl.internalmodel.ExecutionLocationtypeEnum el = one.jodi.etl.internalmodel.ExecutionLocationtypeEnum.SOURCE;
        one.jodi.etl.internalmodel.Targetcolumn mockColumn = mock(one.jodi.etl.internalmodel.Targetcolumn.class);
        when(mockColumn.getName()).thenReturn("colname");
        when(mockColumn.isUpdateKey()).thenReturn(true);
        when(mockColumn.getParent()).thenReturn(mockMap);
        when(mockColumn.getExecutionLocations()).thenReturn(Collections.<ExecutionLocationtypeEnum>singletonList(el));

        when(mockColumn.getMappingExpressions()).thenReturn(Arrays.asList(new String[numberDatasets]).stream().map(s -> "alias1.expr").collect(Collectors.toList()));
        //when(mockColumn.getMappingExpressions()).thenReturn(Collections.<String> singletonList("alias1.expr"));
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
        one.jodi.etl.internalmodel.Mappings mockMap = createMockETLMappings(TARGET_STORE, null, null, dsnames.length);

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

        one.jodi.etl.internalmodel.Mappings mockMap = createMockETLMappings(target, null, null, dsnames.length);

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


    public static Collection<IInterfaceIssue> createMockIssues() {
        IInterfaceAction action = mock(IInterfaceAction.class);
        IIssueFix fix = mock(IIssueFix.class);
        when(fix.getActions()).thenReturn(new IInterfaceAction[]{action});
        IInterfaceIssue issue = mock(IInterfaceIssue.class);
        when(issue.getAvailableFixes()).thenReturn(new IIssueFix[]{fix});

        return Collections.<IInterfaceIssue>singletonList(issue);
    }

    public static DataSet createMockDataSet(String dsname, String[] sourcenames, String[] sourcealiases, OdiInterface odiInterface) throws Exception {
        DataSet ds = mock(DataSet.class);
        when(ds.getName()).thenReturn(dsname);


        List<SourceDataStore> sourceDataStores = new ArrayList<SourceDataStore>();
        SourceDataStore sds = null;
        for (int i = 0; i < sourcenames.length; i++) {
            sds = mock(SourceDataStore.class);
            when(sds.getName()).thenReturn(sourcenames[i]);
            when(sds.getAlias()).thenReturn(sourcealiases[i]);
            ISourceColumn col = mock(ISourceColumn.class);
            IColumn<?> icol = mock(IColumn.class);
            when(col.getUnderlyingColumn()).thenReturn(icol);
            when(sds.getColumns()).thenReturn(Collections.<ISourceColumn>singletonList(col));
            when(sds.getColumn(anyString())).thenReturn(col);
            OdiDataType mockDT = mock(OdiDataType.class);
            when(col.getDataType()).thenReturn(mockDT);
            if (odiInterface != null) {
                when(sds.getUnderlyingOdiInterface()).thenReturn(odiInterface);
            }
            sourceDataStores.add(sds);
        }

        Join j = mock(Join.class);
        when(j.isNatural()).thenReturn(true);
        when(j.getAttachedDataStore1()).thenReturn(sds);
        when(j.getAttachedDataStore2()).thenReturn(sds);
        when(j.getSql()).thenReturn("join.sql");

        when(ds.getJoins()).thenReturn(Collections.<Join>singletonList(j));
        when(ds.getSourceDataStores()).thenReturn(sourceDataStores);

        SourceSet ss = mock(SourceSet.class);
        when(ds.getSourceSets()).thenReturn(Collections.<SourceSet>singletonList(ss));

        OdiLogicalSchema logicalSchema = mock(OdiLogicalSchema.class);

        when(ss.getLogicalSchema()).thenReturn(logicalSchema);
        when(logicalSchema.getLogicalSchemaId()).thenReturn(Integer.valueOf(0));
        OdiTechnology technology = mock(OdiTechnology.class);
        when(technology.getName()).thenReturn("");
        when(logicalSchema.getTechnology()).thenReturn(technology);


        if (odiInterface != null) {
            when(ds.getInterface()).thenReturn(odiInterface);
        }
        return ds;
    }

    public static List<TargetColumn> createMockTargetColumn(final String[] names) throws Exception {
        List<TargetColumn> colList = new ArrayList<TargetColumn>();

        for (String name : names) {
            TargetColumn col = mock(TargetColumn.class);
            when(col.getName()).thenReturn(name);
            List<TargetMapping> mappings = createMockTargetMapping(new String[]{"alias.expr"});
            when(col.getTargetMappings()).thenReturn(mappings);
            colList.add(col);
        }


        return colList;
    }

    public static List<TargetMapping> createMockTargetMapping(String[] expressions) throws Exception {
        List<TargetMapping> mapList = new ArrayList<TargetMapping>();

        for (String sqlString : expressions) {
            TargetMapping map = mock(TargetMapping.class);
            Expression expression = new Expression(sqlString, null, SqlGroupType.NONE);
            when(map.getSqlExpression()).thenReturn(expression);
            DataSet parentDataset = createMockDataSet("parent", new String[]{"name"}, new String[]{"alias"}, null);
            when(map.getParentDataSet()).thenReturn(parentDataset);
            mapList.add(map);
        }


        return mapList;
    }

    public static OdiInterface createMockOdiInterface() throws Exception {
        OdiInterface mockInterface = mock(OdiInterface.class);

        TargetDataStore mockDS = mock(TargetDataStore.class);

        List<TargetColumn> mockCol = createMockTargetColumn(new String[]{"col1", "col2"});
        when(mockDS.getColumns()).thenReturn(mockCol);
        when(mockInterface.getTargetDataStore()).thenReturn(mockDS);
        when(mockDS.getOdiInterface()).thenReturn(mockInterface);
        return mockInterface;
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

    public static MockMappingsExtension createMockMappingsExtension() {
        return new MockMappingsExtension("field1value", 100, true);
    }

    public static MockSourceExtension createMockSourceExtension() {
        return new MockSourceExtension("field1value", 100, true);
    }

    public static MockTransformationExtension createMockTransformationExtension() {
        return new MockTransformationExtension("field1value", 100, true);
    }

    public static MockTargetColumnExtension createMockTargetColumnExtension() {
        return new MockTargetColumnExtension("field1value", 100, true);
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

    public static Pivot createMockPivot(String name, String rowLocator, String aggregateFunction, String[] attributeNames, String[] attributeValues, String[] attributeExpressions) {
        assert (attributeNames.length == attributeValues.length && attributeValues.length == attributeExpressions.length);
        Pivot pivot = mock(Pivot.class);
        when(pivot.getName()).thenReturn(name);
        when(pivot.getRowLocator()).thenReturn(rowLocator);
        when(pivot.getAggregateFunction()).thenReturn(AggregateFunctionEnum.fromValue(aggregateFunction));
        ArrayList<OutputAttribute> outputAttributes = new ArrayList<OutputAttribute>();
        when(pivot.getOutputAttributes()).thenReturn(outputAttributes);
        for (int i = 0; i < attributeNames.length; i++) {
            OutputAttribute outputAttribute = mock(OutputAttribute.class);
            when(outputAttribute.getName()).thenReturn(attributeNames[i]);
            Map<String, String> expressions = new HashMap<String, String>();
            expressions.put(attributeValues[i], attributeExpressions[i]);
            when(outputAttribute.getExpressions()).thenReturn(expressions);
            outputAttributes.add(outputAttribute);
        }

        return pivot;
    }

    public static UnPivot createMockUnPivot(String name, String rowLocator, String[] attributeNames, String[] attributeValues, String[] attributeExpressions) {
        assert (attributeNames.length == attributeValues.length && attributeValues.length == attributeExpressions.length);
        UnPivot up = mock(UnPivot.class);
        when(up.getName()).thenReturn(name);
        when(up.getRowLocator()).thenReturn(rowLocator);
        ArrayList<OutputAttribute> outputAttributes = new ArrayList<OutputAttribute>();
        when(up.getOutputAttributes()).thenReturn(outputAttributes);
        for (int i = 0; i < attributeNames.length; i++) {
            OutputAttribute outputAttribute = mock(OutputAttribute.class);
            when(outputAttribute.getName()).thenReturn(attributeNames[i]);
            Map<String, String> expressions = new HashMap<String, String>();
            expressions.put(null, attributeExpressions[i]);
            when(outputAttribute.getExpressions()).thenReturn(expressions);
            outputAttributes.add(outputAttribute);
        }

        return up;
    }

    public static SubQuery createMockSubQuery(String name, String filterSource, GroupComparisonEnum gc, RoleEnum role, String condition, String[] attributeNames, String[] attributeValues, String[] attributeExpressions) {
        assert (attributeNames.length == attributeValues.length && attributeValues.length == attributeExpressions.length);
        SubQuery sq = mock(SubQuery.class);
        when(sq.getName()).thenReturn(name);
        when(sq.getFilterSource()).thenReturn(filterSource);
        when(sq.getGroupComparison()).thenReturn(gc);
        when(sq.getCondition()).thenReturn(condition);
        if (role != null) when(sq.getRole()).thenReturn(role);
        ArrayList<OutputAttribute> outputAttributes = new ArrayList<OutputAttribute>();
        when(sq.getOutputAttributes()).thenReturn(outputAttributes);
        for (int i = 0; i < attributeNames.length; i++) {
            OutputAttribute outputAttribute = mock(OutputAttribute.class);
            when(outputAttribute.getName()).thenReturn(attributeNames[i]);
            Map<String, String> expressions = new HashMap<String, String>();
            expressions.put(attributeValues[i], attributeExpressions[i]);
            when(outputAttribute.getExpressions()).thenReturn(expressions);
            outputAttributes.add(outputAttribute);
        }

        return sq;
    }

}
