package one.jodi.etl.builder.impl;


import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import one.jodi.core.model.*;
import one.jodi.core.model.impl.*;
import one.jodi.core.model.visitors.DepthFirstTraverserImpl;
import one.jodi.core.model.visitors.TraversingVisitor;
import one.jodi.etl.internalmodel.SetOperatorTypeEnum;
import one.jodi.etl.internalmodel.SubQuery;
import one.jodi.etl.internalmodel.SubQuery.ExpressionSource;
import one.jodi.etl.internalmodel.impl.DatasetImpl;
import one.jodi.etl.internalmodel.impl.KmTypeImpl;
import one.jodi.etl.internalmodel.impl.LookupImpl;
import one.jodi.etl.internalmodel.impl.MappingsImpl;
import one.jodi.etl.internalmodel.impl.SourceImpl;
import one.jodi.etl.internalmodel.impl.TargetcolumnImpl;
import one.jodi.etl.internalmodel.impl.TransformationImpl;
import org.junit.Test;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;


/**
 * Test case for builder and visitor.
 *
 */
public class TransmutingVisitorTest {

    int packageSequence = 123;
    private ErrorWarningMessageJodi errorWarningMessages =
            ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();

    public ExternalTransformation createExternalTransformation() {
        ExternalTransformation transformation = new ExternalTransformation();
        ExternalDatasets datasets = new ExternalDatasets();
        transformation.setDatasets(datasets);
        datasets.afterUnmarshal(null, transformation);
        datasets.setModel("DATASETS MODEL");
        ExternalDataset dataset = new ExternalDataset();
        datasets.addDataset(dataset);
        dataset.afterUnmarshal(null, datasets);
        dataset.setModel("DATASET MODEL");
        ExternalSource source = new ExternalSource();
        dataset.addSource(source);
        source.afterUnmarshal(null, dataset);
        source.setAlias("ALIAS");
        source.setFilter("FILTER");
        source.setFilterExecutionLocation(ExecutionLocationtypeEnum.TARGET);
        source.setJoin("JOIN");
        source.setJoinExecutionLocation(ExecutionLocationtypeEnum.SOURCE);
        source.setJoinType(JoinTypeEnum.INNER);
        source.setModel("SOURCE MODEL");
        source.setSubSelect(true);
        source.setName("SOURCE NAME");

        ExternalLookups lookups = new ExternalLookups();
        lookups.afterUnmarshal(null, source);
        source.setLookups(lookups);
        lookups.setModel("LOOKUPSMODEL");
        ExternalLookup lookup = new ExternalLookup();
        lookup.afterUnmarshal(null, lookups);
        lookups.addLookup(lookup);
        lookup.setModel("LOOKUPMODEL");
        lookup.setAlias("ALIAS");
        lookup.setJoin("JOIN");
        lookup.setJoinExecutionLocation(ExecutionLocationtypeEnum.WORK);
        lookup.setLookupDataStore("DATASTORE");
        lookup.setLookupType(LookupTypeEnum.SCALAR);
        lookup.afterUnmarshal(null, lookups);

        ExternalMappings mappings = new ExternalMappings();
        mappings.afterUnmarshal(null, transformation);
        transformation.setMappings(mappings);
        mappings.setDistinct(true);
        mappings.setModel("MAPPINGS MODEL");
        mappings.setTargetDataStore("MAPPINGS TARGET DATA STORE");
        mappings.setStagingModel("STAGING MODEL");

        ExternalTargetcolumn targetcolumn = new ExternalTargetcolumn();
        mappings.addTargetcolumn(targetcolumn);
        targetcolumn.afterUnmarshal(null, mappings);
        targetcolumn.setKey(true);
        targetcolumn.setMandatory(true);
        targetcolumn.setName("TARGET COLUMN NAME");

        ExternalMappingExpressions expressions = new ExternalMappingExpressions();
        expressions.afterUnmarshal(null, targetcolumn);
        targetcolumn.setMappingExpressions(expressions);
        expressions.addExpression("MAPPING EXPRESSION");

        ExternalProperties properties = new ExternalProperties();
        properties.afterUnmarshal(null, targetcolumn);
        targetcolumn.setProperties(properties);
        properties.setDataType("DATA TYPE");
        properties.setLength(100);
        properties.setScale(100);

        return transformation;
    }

    @Test
    public void testLookup() {
        TransmutingVisitor fixture = new TransmutingVisitor(errorWarningMessages, "Init ", null);

        ExternalTransformation transformation = createExternalTransformation();

        TraversingVisitor tv = new TraversingVisitor(new DepthFirstTraverserImpl(), fixture);
        tv.setTraverseFirst(true);
        transformation.accept(tv);
        TransformationImpl internalTransformation = fixture.getTransformation(packageSequence);

        assert (internalTransformation != null);
        assertEquals(packageSequence, internalTransformation.getPackageSequence());
        assertEquals(1, internalTransformation.getDatasets().size());
        DatasetImpl internalDataset = (DatasetImpl) internalTransformation.getDatasets().get(0);
        assertEquals(1, internalDataset.getSources().size());
        SourceImpl internalSource = (SourceImpl) internalDataset.getSources().get(0);
        assertEquals(1, internalSource.getLookups().size());
        LookupImpl internalLookup = (LookupImpl) internalSource.getLookups().get(0);
        ExternalLookup externalLookup = (ExternalLookup) transformation.getDatasets().getDataset().get(0).getSource().get(0).getLookups().getLookup().get(0);
        assertEquals(externalLookup.getModel(), internalLookup.getModel());
        assertEquals(externalLookup.getAlias(), internalLookup.getAlias());
        assertEquals(externalLookup.getJoin(), internalLookup.getJoin());
        assertEquals(externalLookup.getLookupDataStore(), internalLookup.getLookupDataStore());
        assertEquals(externalLookup.getJoinExecutionLocation().toString(), internalLookup.getJoinExecutionLocation().toString());
        assertEquals(externalLookup.getLookupType().toString(), internalLookup.getLookupType().toString());
        assertEquals(externalLookup.isSubSelect(), internalLookup.isSubSelect());
    }

    @Test
    public void testLookupWithDefaultRow() {
        TransmutingVisitor fixture = new TransmutingVisitor(errorWarningMessages, "Init ", null);

        ExternalTransformation transformation = createExternalTransformation();

        String str = "C1=expression1,C2=expresion2";
        Map<String, String> map = Arrays.asList(str.split(",")).stream().map(kv -> {
            return kv.split("=");
        }).collect(Collectors.toMap(list -> list[0], list -> list[1]));


        transformation.getDatasets().getDataset().stream()
                .map(d -> d.getSource()).flatMap(ss -> ss.stream())
                .flatMap(l -> l.getLookups().getLookup().stream())
                .map(ExternalLookup.class::cast).forEach(look -> {
            ExternalSyntheticRow row = new ExternalSyntheticRow();
            map.forEach((k, v) -> {
                row.getColumn().add(new ExternalColumn(k, v));
            });
            look.setNoMatchRow(row);
        });


        TraversingVisitor tv = new TraversingVisitor(new DepthFirstTraverserImpl(), fixture);
        tv.setTraverseFirst(true);
        transformation.accept(tv);
        TransformationImpl internalTransformation = fixture.getTransformation(packageSequence);

        assert (internalTransformation != null);
        internalTransformation.getDatasets().stream().flatMap(d -> d.getSources().stream()).flatMap(s -> s.getLookups().stream()).forEach(l -> {
            assert (l.getDefaultRowColumns() != null);
            map.forEach((k, v) -> {
                assert (l.getDefaultRowColumns().get(k) == v);
            });
        });

    }

    // Set model null on Lookup and make sure its populated from Lookups
    @Test
    public void testLookupModel() {
        TransmutingVisitor fixture = new TransmutingVisitor(errorWarningMessages, "Init ", null);

        ExternalTransformation transformation = createExternalTransformation();
        transformation.getDatasets().getDataset().get(0).getSource().get(0).getLookups().getLookup().get(0).setModel(null);
        ExternalLookups externalLookups = (ExternalLookups) transformation.getDatasets().getDataset().get(0).getSource().get(0).getLookups();

        TraversingVisitor tv = new TraversingVisitor(new DepthFirstTraverserImpl(), fixture);
        tv.setTraverseFirst(true);
        transformation.accept(tv);
        TransformationImpl internalTransformation = fixture.getTransformation(packageSequence);

        assertEquals(externalLookups.getModel(), internalTransformation.getDatasets().get(0).getSources().get(0).getLookups().get(0).getModel());
    }

    @Test
    public void testSource() {
        TransmutingVisitor fixture = new TransmutingVisitor(errorWarningMessages, "Init ", null);

        ExternalTransformation transformation = createExternalTransformation();
        transformation.getDatasets().getDataset().get(0).getSource().get(0).getLookups().getLookup().get(0).setJoinExecutionLocation(null);
        ExternalSource externalSource = (ExternalSource) transformation.getDatasets().getDataset().get(0).getSource().get(0);

        TraversingVisitor tv = new TraversingVisitor(new DepthFirstTraverserImpl(), fixture);
        tv.setTraverseFirst(true);
        transformation.accept(tv);
        TransformationImpl internalTransformation = fixture.getTransformation(packageSequence);

        assertEquals(1, internalTransformation.getDatasets().get(0).getSources().size());
        SourceImpl internalSource = (SourceImpl) internalTransformation.getDatasets().get(0).getSources().get(0);

        assertEquals(externalSource.getAlias(), internalSource.getAlias());
        assertEquals(externalSource.getFilter(), internalSource.getFilter());
        assertEquals(externalSource.getFilterExecutionLocation().toString(), internalSource.getFilterExecutionLocation().toString());
        assertEquals(externalSource.getJoin(), internalSource.getJoin());
        assertEquals(externalSource.getJoinExecutionLocation().toString(), internalSource.getJoinExecutionLocation().toString());
        externalSource.getJoinType().toString();
        internalSource.getJoinType().toString();
        assertEquals(externalSource.getJoinType().toString(), internalSource.getJoinType().toString());
        assertEquals(externalSource.getModel(), internalSource.getModel());
        assertEquals(externalSource.getName(), internalSource.getName());
    }

    @Test
    public void testSourceModelFromDataset() {
        TransmutingVisitor fixture = new TransmutingVisitor(errorWarningMessages, "Init ", null);

        ExternalTransformation transformation = createExternalTransformation();
        transformation.getDatasets().getDataset().get(0).getSource().get(0).setModel(null);

        TraversingVisitor tv = new TraversingVisitor(new DepthFirstTraverserImpl(), fixture);
        tv.setTraverseFirst(true);
        transformation.accept(tv);
        TransformationImpl internalTransformation = fixture.getTransformation(packageSequence);

        SourceImpl internalSource = (SourceImpl) internalTransformation.getDatasets().get(0).getSources().get(0);
        assertEquals(transformation.getDatasets().getDataset().get(0).getModel(), internalSource.getModel());
    }

    @Test
    public void testSourceModelFromDatasets() {
        TransmutingVisitor fixture = new TransmutingVisitor(errorWarningMessages, "Init ", null);

        ExternalTransformation transformation = createExternalTransformation();
        transformation.getDatasets().getDataset().get(0).getSource().get(0).setModel(null);
        transformation.getDatasets().getDataset().get(0).setModel(null);

        TraversingVisitor tv = new TraversingVisitor(new DepthFirstTraverserImpl(), fixture);

        tv.setTraverseFirst(true);
        transformation.accept(tv);
        TransformationImpl internalTransformation = fixture.getTransformation(packageSequence);

        SourceImpl internalSource = (SourceImpl) internalTransformation.getDatasets().get(0).getSources().get(0);
        assertEquals(transformation.getDatasets().getModel(), internalSource.getModel());
    }


    @Test
    public void testSetOperatorDefaults() {
        TransmutingVisitor fixture = new TransmutingVisitor(errorWarningMessages, "Init ", null);

        ExternalTransformation transformation = createExternalTransformation();
        transformation.getDatasets().getDataset().get(0).setSetOperator(null);

        ExternalDatasets datasets = (ExternalDatasets) transformation.getDatasets();
        ExternalDataset dataset = new ExternalDataset();
        dataset.afterUnmarshal(null, datasets);
        datasets.setModel("DATASETS MODEL");
        datasets.addDataset(dataset);
        dataset.setSetOperator(null);

        TraversingVisitor tv = new TraversingVisitor(new DepthFirstTraverserImpl(), fixture);

        tv.setTraverseFirst(true);
        transformation.accept(tv);
        TransformationImpl internalTransformation = fixture.getTransformation(packageSequence);
        assertEquals(SetOperatorTypeEnum.NOT_DEFINED, internalTransformation.getDatasets().get(0).getSetOperator());
        assertEquals(SetOperatorTypeEnum.UNION_ALL, internalTransformation.getDatasets().get(1).getSetOperator());

    }


    // The Source should pick up the KM defined at the external Datasets.
    @Test
    public void testSourceKMFromDatasets() {
        TransmutingVisitor fixture = new TransmutingVisitor(errorWarningMessages, "Init ", null);

        ExternalTransformation transformation = createExternalTransformation();
        KmType externalKM = buildExternalKM(transformation.getDatasets(), "CODE", "OPTION", "VALUE");
        transformation.getDatasets().setLkm(externalKM);

        TraversingVisitor tv = new TraversingVisitor(new DepthFirstTraverserImpl(), fixture);

        tv.setTraverseFirst(true);
        transformation.accept(tv);
        TransformationImpl internalTransformation = fixture.getTransformation(packageSequence);
        KmTypeImpl internalKM = (KmTypeImpl) internalTransformation.getDatasets().get(0).getSources().get(0).getLkm();
        assertEquals(externalKM.getCode(), internalKM.getName());
        assertEquals(externalKM.getKmOptions().getKmOption().size(), internalKM.getOptions().size());
        assertEquals(externalKM.getKmOptions().getKmOption().get(0).getValue(), internalKM.getOptions().get(externalKM.getKmOptions().getKmOption().get(0).getName()));
    }


    // The Source should pick up the KM defined at the external Dataset over the external Datasets.
    @Test
    public void testSourceKMFromDataset() {
        TransmutingVisitor fixture = new TransmutingVisitor(errorWarningMessages, "Init ", null);

        ExternalTransformation transformation = createExternalTransformation();
        KmType externalDatasetsKM = buildExternalKM(transformation.getDatasets(), "CODE1", "OPTION1", "VALUE1");
        transformation.getDatasets().setLkm(externalDatasetsKM);
        KmType externalDatasetKM = buildExternalKM(transformation.getDatasets().getDataset().get(0), "CODE", "OPTION", "VALUE");
        transformation.getDatasets().getDataset().get(0).setLkm(externalDatasetKM);

        TraversingVisitor tv = new TraversingVisitor(new DepthFirstTraverserImpl(), fixture);

        tv.setTraverseFirst(true);
        transformation.accept(tv);
        TransformationImpl internalTransformation = fixture.getTransformation(packageSequence);
        KmTypeImpl internalKM = (KmTypeImpl) internalTransformation.getDatasets().get(0).getSources().get(0).getLkm();
        assertEquals(externalDatasetKM.getCode(), internalKM.getName());
        assertEquals(externalDatasetKM.getKmOptions().getKmOption().size(), internalKM.getOptions().size());
        assertEquals(externalDatasetKM.getKmOptions().getKmOption().get(0).getValue(), internalKM.getOptions().get(externalDatasetKM.getKmOptions().getKmOption().get(0).getName()));
    }

    @Test
    public void testNoMappings() {
        TransmutingVisitor fixture = new TransmutingVisitor(errorWarningMessages, "Init ", null);

        ExternalTransformation transformation = createExternalTransformation();
        transformation.setMappings(null);
        TraversingVisitor tv = new TraversingVisitor(new DepthFirstTraverserImpl(), fixture);

        tv.setTraverseFirst(true);
        transformation.accept(tv);
        TransformationImpl internalTransformation = fixture.getTransformation(packageSequence);
        assert (internalTransformation.getMappings() == null);
    }


    @Test
    public void testMappings() {
        TransmutingVisitor fixture = new TransmutingVisitor(errorWarningMessages, "Init ", null);

        ExternalTransformation transformation = createExternalTransformation();
        TraversingVisitor tv = new TraversingVisitor(new DepthFirstTraverserImpl(), fixture);

        tv.setTraverseFirst(true);
        transformation.accept(tv);
        TransformationImpl internalTransformation = fixture.getTransformation(packageSequence);
        MappingsImpl internalMappings = (MappingsImpl) internalTransformation.getMappings();
        assert (internalMappings != null);
        assertEquals(transformation.getMappings().getModel(), internalMappings.getModel());
        assertEquals(transformation.getMappings().getTargetDataStore(), internalMappings.getTargetDataStore());
        assertEquals(transformation.getMappings().getTargetColumn().size(), internalMappings.getTargetColumns().size());
        assertEquals(transformation.getMappings().getStagingModel(), internalMappings.getStagingModel());
        assertEquals(internalTransformation, internalTransformation.getMappings().getParent());

    }

    @Test
    public void testNoTargetcolumn() {
        TransmutingVisitor fixture = new TransmutingVisitor(errorWarningMessages, "Init ", null);

        ExternalTransformation transformation = createExternalTransformation();
        transformation.getMappings().getTargetColumn().clear();
        TraversingVisitor tv = new TraversingVisitor(new DepthFirstTraverserImpl(), fixture);

        tv.setTraverseFirst(true);
        transformation.accept(tv);
        TransformationImpl internalTransformation = fixture.getTransformation(packageSequence);
        assert (internalTransformation.getMappings().getTargetColumns().isEmpty());

    }

    // Make sure that when properties are absent, they are set accordingly.
    @Test
    public void testNoProperties() {
        TransmutingVisitor fixture = new TransmutingVisitor(errorWarningMessages, "Init ", null);

        ExternalTransformation transformation = createExternalTransformation();
        transformation.getMappings().getTargetColumn().get(0).setProperties(null);
        TraversingVisitor tv = new TraversingVisitor(new DepthFirstTraverserImpl(), fixture);

        tv.setTraverseFirst(true);
        transformation.accept(tv);
        TargetcolumnImpl internalTargetcolumn = (TargetcolumnImpl) fixture.getTransformation(packageSequence).getMappings().getTargetColumns().get(0);
        assert (internalTargetcolumn.getDataType() == null);
        assert (internalTargetcolumn.getScale() == 0);
        assert (internalTargetcolumn.getLength() == 0);

    }


    @Test
    public void testTargetcolumn() {
        TransmutingVisitor fixture = new TransmutingVisitor(errorWarningMessages, "Init ", null);

        ExternalTransformation transformation = createExternalTransformation();
        TraversingVisitor tv = new TraversingVisitor(new DepthFirstTraverserImpl(), fixture);

        tv.setTraverseFirst(true);
        transformation.accept(tv);
        TransformationImpl internalTransformation = fixture.getTransformation(packageSequence);

        ExternalTargetcolumn externalTargetcolumn = (ExternalTargetcolumn) transformation.getMappings().getTargetColumn().get(0);
        TargetcolumnImpl internalTargetcolumn = (TargetcolumnImpl) internalTransformation.getMappings().getTargetColumns().get(0);
        assertEquals(externalTargetcolumn.getName(), internalTargetcolumn.getName());
        assertEquals(externalTargetcolumn.isKey(), internalTargetcolumn.isUpdateKey());
        assertEquals(externalTargetcolumn.isMandatory(), internalTargetcolumn.isMandatory());
        assertEquals(externalTargetcolumn.isKey(), internalTargetcolumn.isExplicitlyUpdateKey());
        assertEquals(externalTargetcolumn.isMandatory(), internalTargetcolumn.isExplicitlyMandatory());

        ExternalMappingExpressions externalExpressions = (ExternalMappingExpressions) externalTargetcolumn.getMappingExpressions();
        assertEquals(externalExpressions.getExpression().size(), internalTargetcolumn.getMappingExpressions().size());

        ExternalProperties properties = (ExternalProperties) externalTargetcolumn.getProperties();
        assertEquals(properties.getDataType(), internalTargetcolumn.getDataType());
        assertEquals(properties.getLength(), Integer.valueOf(internalTargetcolumn.getLength()));

    }

    @Test
    public void testIKMAndCKM() {
        TransmutingVisitor fixture = new TransmutingVisitor(null, "Init ", null);

        ExternalTransformation transformation = createExternalTransformation();
        transformation.getMappings().setCkm(buildExternalKM(transformation.getDatasets(), "CKM CODE", "CKM OPTION", "CKM VALUE"));
        transformation.getMappings().setIkm(buildExternalKM(transformation.getDatasets(), "IKM CODE", "IKM OPTION", "IKM VALUE"));
        TraversingVisitor tv = new TraversingVisitor(new DepthFirstTraverserImpl(), fixture);

        tv.setTraverseFirst(true);
        transformation.accept(tv);
        MappingsImpl internalMappings = (MappingsImpl) fixture.getTransformation(packageSequence).getMappings();
        KmTypeImpl ckm = (KmTypeImpl) internalMappings.getCkm();
        assertEquals(ckm.getName(), transformation.getMappings().getCkm().getCode());
        assertEquals(ckm.getOptions().size(), transformation.getMappings().getCkm().getKmOptions().getKmOption().size());
        assertEquals(ckm.getOptions().get(transformation.getMappings().getCkm().getKmOptions().getKmOption().get(0).getName()),
                transformation.getMappings().getCkm().getKmOptions().getKmOption().get(0).getValue());

        KmTypeImpl ikm = (KmTypeImpl) internalMappings.getIkm();
        assertEquals(ikm.getName(), transformation.getMappings().getIkm().getCode());
        assertEquals(ikm.getOptions().size(), transformation.getMappings().getIkm().getKmOptions().getKmOption().size());
        assertEquals(ikm.getOptions().get(transformation.getMappings().getIkm().getKmOptions().getKmOption().get(0).getName()),
                transformation.getMappings().getIkm().getKmOptions().getKmOption().get(0).getValue());

    }


    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testSubQuery() {
        String name = "SUBQUERY";
        String condition = "CONDITION";
        String filterSource = "FILTERSOURCE";
        String role = "NOT IN";
        TransmutingVisitor fixture = new TransmutingVisitor(errorWarningMessages, "Init ", null);

        ExternalTransformation transformation = createExternalTransformation();
        ExternalSubQuery subQuery = new ExternalSubQuery();
        subQuery.setName(name);
        subQuery.setGroupComparison(GroupComparisonConditionEnum.ANY);
        subQuery.setFilterSource(filterSource);
        subQuery.setRole(RoleEnum.fromValue(role));
        subQuery.setCondition(condition);
        SubQueryType.Column oa = new ExternalSubQueryOutputAttribute();
        oa.setName("C1");
        ExternalSubQueryExpression expression = new ExternalSubQueryExpression();
        expression.setValue("VALUE");
        expression.setIsFilter(true);
        oa.getExpression().add(expression);
        subQuery.getColumn().add(oa);

        Source source = transformation.getDatasets().getDataset().get(0).getSource().get(0);
        source.getFlows().getFlow().add(new JAXBElement(new QName(""), SubQueryType.class, subQuery));

        TraversingVisitor tv = new TraversingVisitor(new DepthFirstTraverserImpl(), fixture);
        tv.setTraverseFirst(true);
        transformation.accept(tv);
        TransformationImpl internalTransformation = fixture.getTransformation(packageSequence);

        SourceImpl internalSource = (SourceImpl) internalTransformation.getDatasets().get(0).getSources().get(0);

        assert (internalSource.getFlows().size() == 1);
        SubQuery internalSubQuery = (SubQuery) internalSource.getFlows().get(0);
        assertEquals(name, internalSubQuery.getName());
        assertEquals(oa.getName(), internalSubQuery.getOutputAttributes().get(0).getName());
        internalSubQuery.getOutputAttributes().stream().forEach(exp -> exp.getExpressions().forEach((k, v) -> System.out.println(k + " -> " + v)));

        assert (internalSubQuery.getOutputAttributes().get(0).getExpressions().containsKey(ExpressionSource.FILTER.name()));
        assert (internalSubQuery.getOutputAttributes().get(0).getExpressions().containsValue(expression.getValue()));
        assertEquals(condition, internalSubQuery.getCondition());

    }


    private KmType buildExternalKM(Common parent, String code, String key, String value) {
        one.jodi.core.model.impl.KmTypeImpl kmType = new one.jodi.core.model.impl.KmTypeImpl();
        kmType.setCode(code);
        ExternalKmOptions kmOptions = new ExternalKmOptions();
        kmType.setKmOptions(kmOptions);
        kmType.afterUnmarshal(null, parent);
        KmOption option = new KmOptionImpl();
        option.setName(key);
        option.setValue(value);

        kmOptions.addOption(option);

        return kmType;
    }


    class ExternalTransformation extends one.jodi.core.model.impl.TransformationImpl {

    }

    class ExternalKmOptions extends KmOptionsImpl {
        public void addOption(KmOption kmo) {
            if (this.kmOption == null)
                kmOption = new ArrayList<KmOption>();
            kmOption.add(kmo);
        }
    }

    class ExternalDatasets extends DatasetsImpl {
        public void addDataset(Dataset ds) {
            if (dataset == null)
                dataset = new ArrayList<Dataset>();
            dataset.add(ds);
        }
    }

    class ExternalDataset extends one.jodi.core.model.impl.DatasetImpl {
        public void addSource(Source s) {
            if (source == null)
                source = new ArrayList<Source>();
            source.add(s);
        }
    }

    class ExternalSource extends one.jodi.core.model.impl.SourceImpl {
        {
            flows = new FlowsTypeImpl();
        }

    }

    class ExternalLookups extends LookupsImpl {
        public void addLookup(Lookup l) {
            if (lookup == null)
                lookup = new ArrayList<Lookup>();
            lookup.add(l);
        }
    }

    class ExternalLookup extends one.jodi.core.model.impl.LookupImpl {

    }

    class ExternalMappings extends one.jodi.core.model.impl.MappingsImpl {
        public void addTargetcolumn(Targetcolumn targetcolumn) {
            if (targetColumn == null)
                targetColumn = new ArrayList<Targetcolumn>();
            targetColumn.add(targetcolumn);
        }
    }

    class ExternalTargetcolumn extends one.jodi.core.model.impl.TargetcolumnImpl {

    }

    class ExternalProperties extends PropertiesImpl {

    }

    class ExternalMappingExpressions extends MappingExpressionsImpl {
        public void addExpression(String e) {
            if (this.expression == null)
                expression = new ArrayList<String>();
            expression.add(e);
        }
    }

    //JAXBElement< one.jodi.core.model.impl.SubQueryTypeImpl>
    class ExternalSubQuery extends SubQueryTypeImpl {

        {
            column = new ArrayList<SubQueryType.Column>();
        }

    }

    class ExternalOutputAttribute extends ColumnTypeImpl {
        {

            expression = new ArrayList<ColumnType.Expression>();
        }
    }

    class ExternalExpression extends ColumnTypeImpl.ExpressionImpl {
    }

    class ExternalSubQueryOutputAttribute extends SubQueryTypeImpl.ColumnImpl {
        {
            expression = new ArrayList<SubQueryType.Column.Expression>();
        }
    }

    class ExternalSubQueryExpression extends SubQueryTypeImpl.ColumnImpl.ExpressionImpl {
    }

    class ExternalSyntheticRow extends SyntheticRowTypeImpl {

    }

    class ExternalColumn extends SyntheticRowTypeImpl.ColumnImpl {
        ExternalColumn(String columnName, String expression) {
            name = columnName;
            value = expression;
        }

    }


}

