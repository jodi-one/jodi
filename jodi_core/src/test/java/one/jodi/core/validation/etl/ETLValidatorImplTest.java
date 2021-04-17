package one.jodi.core.validation.etl;

import one.jodi.InputModelMockHelper;
import one.jodi.base.config.JodiPropertyNotFoundException;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import one.jodi.base.error.ErrorWarningMessageJodiImpl;
import one.jodi.base.model.types.DataModel;
import one.jodi.base.model.types.DataStore;
import one.jodi.base.model.types.DataStoreColumn;
import one.jodi.base.service.schema.DataStoreNotInModelException;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.context.packages.PackageCache;
import one.jodi.core.executionlocation.impl.ExecutionLocationIDStrategy;
import one.jodi.core.extensions.strategies.FolderNameStrategy;
import one.jodi.core.extensions.strategies.JournalizingStrategy;
import one.jodi.core.extensions.strategies.KnowledgeModulePropertiesException;
import one.jodi.core.extensions.strategies.KnowledgeModuleStrategy;
import one.jodi.core.extensions.strategies.NoKnowledgeModuleFoundException;
import one.jodi.core.extensions.strategies.NoModelFoundException;
import one.jodi.core.km.impl.KnowledgeModuleIDStrategy;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.core.metadata.ETLSubsystemService;
import one.jodi.core.metadata.types.KnowledgeModule;
import one.jodi.etl.common.EtlSubSystemVersion;
import one.jodi.etl.internalmodel.Dataset;
import one.jodi.etl.internalmodel.ETLPackage;
import one.jodi.etl.internalmodel.ExecutionLocationtypeEnum;
import one.jodi.etl.internalmodel.Flow;
import one.jodi.etl.internalmodel.GroupComparisonEnum;
import one.jodi.etl.internalmodel.JoinTypeEnum;
import one.jodi.etl.internalmodel.KmType;
import one.jodi.etl.internalmodel.Lookup;
import one.jodi.etl.internalmodel.Mappings;
import one.jodi.etl.internalmodel.Pivot;
import one.jodi.etl.internalmodel.RoleEnum;
import one.jodi.etl.internalmodel.SetOperatorTypeEnum;
import one.jodi.etl.internalmodel.Source;
import one.jodi.etl.internalmodel.SubQuery;
import one.jodi.etl.internalmodel.Targetcolumn;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.etl.service.packages.PackageServiceProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runners.MethodSorters;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Collections.singletonList;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ETLValidatorImplTest {

    @Rule
    public TestName testMethodName = new TestName();
    @Mock
    JodiProperties properties;
    @Mock
    DatabaseMetadataService metadataService;
    @Mock
    ETLSubsystemService etlSubsystemService;
    @Mock
    PackageCache packageCache;
    @Mock
    PackageServiceProvider packageService;
    @Mock
    EtlSubSystemVersion etlSubSystemVersion;
    ErrorWarningMessageJodi errorWarningMessages = ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();
    ETLValidatorImpl fixture;
    int packageSequence = 11;
    Expected expected;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        fixture = new ETLValidatorImpl(metadataService, etlSubsystemService, properties, packageCache,
                                       errorWarningMessages, true, etlSubSystemVersion, null);
    }

    @Before
    public void setContextBeforeTestCase() throws NoSuchMethodException, SecurityException {
        Method method = this.getClass()
                            .getMethod(testMethodName.getMethodName());
        expected = method.getAnnotation(Expected.class);
        if (expected == null) {
            throw new RuntimeException(
                    "All test methods for " + getClass().getName() + " must be annotated with Expected class");
        }
    }


    @After
    public void verifyErrorCode() {
        int ps = expected.packageSequences().length > 0 ? expected.packageSequences()[0] : packageSequence;
        List<Integer> missingErrors = findMissingCodes(fixture.errorWarningMessages.getErrorMessages()
                                                                                   .get(ps), expected.errors());
        for (int i : missingErrors) {
            System.err.println(
                    "Test " + testMethodName.getMethodName() + " expected error " + i + " but was not received.");
        }
        assert (missingErrors.isEmpty());
    }

    @After
    public void print() {
        errorWarningMessages.printMessages();
    }


    @After
    public void verifyWarningCode() {
        int ps = expected.packageSequences().length > 0 ? expected.packageSequences()[0] : packageSequence;

        List<Integer> missingWarnings = findMissingCodes(fixture.errorWarningMessages.getWarningMessages()
                                                                                     .get(ps), expected.warnings());
        for (int i : missingWarnings) {
            System.err.println(
                    "Test " + testMethodName.getMethodName() + " expected warning " + i + " but was not received.");
        }

        assert (missingWarnings.isEmpty());
    }


    // Make sure that expected codes are found in messages
    private List<Integer> findMissingCodes(List<String> messages, int[] expectedCodes) {
        List<Integer> missingCodes = new ArrayList<>();

        if (expectedCodes.length > 0) {
            if (messages == null) {
                throw new RuntimeException("Error and/or warning messages not set");
            }
            for (int expectedCode : expectedCodes) {
                boolean found = false;
                for (String message : messages) {
                    String messageCode = message.substring(1, 6);
                    if (messageCode.equals(expectedCode + "")) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    missingCodes.add(expectedCode);
                }
            }
        }

        return missingCodes;
    }


    @Test
    @Expected(errors = {10000})
    public void testValidate_10000() {

        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence - 1);
        when(transformation.getName()).thenReturn("transformationName");
        Mappings mappings = mock(Mappings.class);
        when(transformation.getMappings()).thenReturn(mappings);
        fixture.validateTransformationName(transformation);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        fixture.validateTransformationName(transformation);
    }


    @Test
    @Expected(errors = {10001})
    public void testValidate_10001() {

        fixture.validatePackageSequence(packageSequence, "file 1");
        fixture.validatePackageSequence(packageSequence, "file 2");
    }


    @Test
    @Expected(errors = {10040})
    public void testValidate_10040() {
        FolderNameStrategy strategy = (defaultFolderName, execContext, isJournalizedData) -> "";

        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);

        fixture.validateFolderName(transformation, strategy);
    }

    @Test
    @Expected(errors = {10050})
    public void testValidate_10050() {
        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        when(transformation.getName()).thenReturn("TransformationName");
        Mappings mappings = mock(Mappings.class);
        when(transformation.getMappings()).thenReturn(mappings);
        final String prefix = "TargetDataStoreExceedsMaxLength";
        String tooLongDS = prefix + IntStream.range(prefix.length(), 129)
                                             .mapToObj(i -> "x")
                                             .collect(Collectors.joining());
        when(mappings.getTargetDataStore()).thenReturn(tooLongDS);
        fixture.validateTransformationName(transformation);
    }

    @Test
    @Expected(errors = {10060})
    public void testValidate_10060() {
        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        when(transformation.getPackageList()).thenReturn(null);
        fixture.validatePackageAssociations(transformation);
    }

    @Test
    @Expected(errors = {10061})
    public void testValidate_10061() {
        String association = "COMMON";
        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        when(transformation.getPackageList()).thenReturn(association + "_1");
        ETLPackage etlPackage = mock(ETLPackage.class);
        when(etlPackage.getTargetPackageList()).thenReturn(singletonList(association));
        Set<String> nonEmptySet = new HashSet<>();
        nonEmptySet.add("PCK");
        when(packageCache.getPackageAssociations()).thenReturn(nonEmptySet);
        //when(packageCache.getPackagesInCreationOrder()).thenReturn(Arrays.asList(etlPackage));
        fixture.validatePackageAssociations(transformation);
    }

    @Test
    @Expected()
    public void testValidate_10061_success() {
        String association = "COMMON";
        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        when(transformation.getPackageList()).thenReturn(association);
        ETLPackage etlPackage = mock(ETLPackage.class);
        when(etlPackage.getTargetPackageList()).thenReturn(singletonList(association));
        //when(packageCache.getPackagesInCreationOrder()).thenReturn(Arrays.asList(etlPackage));
        fixture.validatePackageAssociations(transformation);
    }

    @Test
    @Expected(errors = {10070})
    public void testValidate_10070() {
        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        Dataset dataset = mock(Dataset.class);
        when(dataset.getParent()).thenReturn(transformation);
        when(transformation.getDatasets()).thenReturn(singletonList(dataset));
        Source source = mock(Source.class);
        when(source.isJournalized()).thenReturn(true);
        when(source.getParent()).thenReturn(dataset);
        when(dataset.getSources()).thenReturn(singletonList(source));
        Lookup lookup = mock(Lookup.class);
        when(lookup.getParent()).thenReturn(source);
        when(lookup.isJournalized()).thenReturn(true);
        when(source.getLookups()).thenReturn(singletonList(lookup));

        fixture.validateJournalized(transformation);
    }


    @Test
    @Expected(warnings = {20002})
    public void testValidate_20002() {
        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        Dataset dataset = mock(Dataset.class);
        when(dataset.getSetOperator()).thenReturn(SetOperatorTypeEnum.INTERSECT);
        ArrayList<Dataset> datasets = new ArrayList<>();
        datasets.add(dataset);
        when(dataset.getParent()).thenReturn(transformation);
        when(transformation.getDatasets()).thenReturn(datasets);
        fixture.validateDataset(datasets);
    }

    @Test
    @Expected(warnings = {30000})
    public void testValidate_30000() {
        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        List<Dataset> datasets =
                InputModelMockHelper.createMockETLDatasets(transformation, new String[]{"alias1", "alias2"},
                                                           new String[]{"name1", "name2"},
                                                           new String[]{"model1", "model2"});
        when(transformation.getDatasets()).thenReturn(datasets);
        when(datasets.get(0)
                     .getSources()
                     .get(0)
                     .isSubSelect()).thenReturn(true);
        when(metadataService.isTemporaryTransformation("name1")).thenReturn(true);

        fixture.validateSubselect(transformation.getDatasets()
                                                .get(0)
                                                .getSources()
                                                .get(0));
    }


    @Test
    @Expected(errors = {30010})
    public void testValidate_30010() {
        Transformation transformation =
                InputModelMockHelper.createMockETLTransformation(new String[]{"alias1", "alias2"},
                                                                 new String[]{"name1", "name2"},
                                                                 new String[]{"model1", "model2"});
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        DataStore dataStore = mock(DataStore.class);
        when(this.metadataService.findDataStoreInAllModels("other")).thenReturn(singletonList(dataStore));

        fixture.validateSourceDataStore(transformation.getDatasets()
                                                      .get(0)
                                                      .getSources()
                                                      .get(0));
    }

    @Test
    @Expected(errors = {30012})
    public void testValidate_30012() {
        String modelWithoutSource = "validModel";
        String invalidModel = "invalidModel";
        Transformation transformation =
                InputModelMockHelper.createMockETLTransformation(new String[]{"alias1", "alias2"},
                                                                 new String[]{"name1", "name2"},
                                                                 new String[]{modelWithoutSource, invalidModel});
        when(transformation.getPackageSequence()).thenReturn(packageSequence);

        Source source0 = transformation.getDatasets()
                                       .get(1)
                                       .getSources()
                                       .get(0);
        Source source1 = transformation.getDatasets()
                                       .get(1)
                                       .getSources()
                                       .get(0);

        DataStore dataStore = mock(DataStore.class);
        when(metadataService.getSourceDataStoreInModel(source0.getName(), modelWithoutSource)).thenReturn(dataStore);

        fixture.handleModelCode(new JodiPropertyNotFoundException("property not found", invalidModel), source1);
    }

    @Test
    @Expected(errors = {30013})
    public void testValidate_30013() {
        String modelWithoutSource = "validModel";
        Transformation transformation =
                InputModelMockHelper.createMockETLTransformation(new String[]{"alias1", "alias2"},
                                                                 new String[]{"name1", "name2"},
                                                                 new String[]{modelWithoutSource, modelWithoutSource});
        when(transformation.getPackageSequence()).thenReturn(packageSequence);

        when(properties.getProperty(modelWithoutSource)).thenReturn(modelWithoutSource);
        Source source0 = transformation.getDatasets()
                                       .get(1)
                                       .getSources()
                                       .get(0);
        when(source0.getModel()).thenReturn("MODEL");

        fixture.handleModelCode(new DataStoreNotInModelException(""), source0);
    }


    @Test
    @Expected(errors = {30030})
    public void testValidate_30030() {
        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        Source source1 = InputModelMockHelper.createMockETLSource("alias", "name", "model");
        Source source2 = InputModelMockHelper.createMockETLSource("alias", "name", "model");
        ArrayList<Source> sources = new ArrayList<>();
        sources.add(source1);
        sources.add(source2);
        Dataset dataset = mock(Dataset.class);
        when(dataset.getSources()).thenReturn(sources);
        when(source1.getParent()).thenReturn(dataset);
        when(source2.getParent()).thenReturn(dataset);
        when(dataset.getParent()).thenReturn(transformation);
        List<Dataset> datasets = singletonList(dataset);
        when(transformation.getDatasets()).thenReturn(datasets);
        when(dataset.getParent()).thenReturn(transformation);


        fixture.validateDataset(datasets);
    }

    @Test
    @Expected(errors = {30031})
    public void testValidate_30031() {
        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        Source source1 = InputModelMockHelper.createMockETLSource("alias", "name1", "model");
        Source source2 = InputModelMockHelper.createMockETLSource("alias", "name2", "model");
        ArrayList<Source> sources = new ArrayList<>();
        sources.add(source1);
        sources.add(source2);
        Dataset dataset = mock(Dataset.class);
        when(dataset.getSources()).thenReturn(sources);
        when(source1.getParent()).thenReturn(dataset);
        when(source2.getParent()).thenReturn(dataset);
        when(dataset.getParent()).thenReturn(transformation);
        List<Dataset> datasets = singletonList(dataset);
        when(transformation.getDatasets()).thenReturn(datasets);
        when(dataset.getParent()).thenReturn(transformation);


        fixture.validateDataset(datasets);
    }


    @Test
    @Expected(errors = {30100})
    public void testValidate_30100() {
        testValidateSourceFilter("name1.c1 = alias2.c1", ExecutionLocationtypeEnum.SOURCE);
    }


    @Test
    @Expected(errors = {30103})
    public void testValidate_30103() {
        testValidateSourceFilter("unknown.c1 = unknown.c2 and alias1 = alias2.c1", ExecutionLocationtypeEnum.SOURCE);
    }


    @Test
    @Expected(errors = {30101})
    public void testValidate_30101() {
        testValidateSourceFilter("alias1.c1 = alias1.c1", ExecutionLocationtypeEnum.SOURCE);
    }


    @Test
    @Expected(errors = {30121})
    public void testValidate_30121() {
        //testValidateSourceFilter("alias1.c1 = alias2.c1", ExecutionLocationtypeEnum.TARGET);
        Transformation transformation =
                InputModelMockHelper.createMockETLTransformation(new String[]{"alias1"}, new String[]{"name1"},
                                                                 new String[]{"model"});
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        Source source = transformation.getDatasets()
                                      .get(0)
                                      .getSources()
                                      .get(0);
        when(source.getFilterExecutionLocation()).thenReturn(ExecutionLocationtypeEnum.TARGET);
        fixture.validateFilterExecutionLocation(source);
    }

    @Test
    @Expected(errors = {30130})
    public void testValidate_30130_undefinedDatastore() {

        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        Source source = InputModelMockHelper.createMockETLSource("alias", "source", "model");
        Dataset dataset = mock(Dataset.class);
        when(dataset.getSources()).thenReturn(singletonList(source));
        when(source.getParent()).thenReturn(dataset);
        when(dataset.getParent()).thenReturn(transformation);
        when(transformation.getDatasets()).thenReturn(singletonList(dataset));
        when(source.getFilter()).thenReturn("alias.column = 1");
        when(metadataService.getSourceDataStoreInModel("source", "model")).thenThrow(
                new DataStoreNotInModelException(""));
        fixture.validateFilterEnriched(source);
    }

    @Test
    @Expected(errors = {30131})
    public void testValidate_30131_undefinedDatastoreColumn() {

        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        Source source = InputModelMockHelper.createMockETLSource("alias", "source", "model");
        Dataset dataset = mock(Dataset.class);
        when(dataset.getSources()).thenReturn(singletonList(source));
        when(source.getParent()).thenReturn(dataset);
        when(dataset.getParent()).thenReturn(transformation);
        when(transformation.getDatasets()).thenReturn(singletonList(dataset));
        when(source.getFilter()).thenReturn("alias.column = 1");
        DataStore dataStore = mock(DataStore.class);
        when(dataStore.getDataStoreName()).thenReturn("name");
        when(dataStore.getColumns()).thenReturn(new HashMap<>());
        when(metadataService.getSourceDataStoreInModel("source", "model")).thenReturn(dataStore);
        fixture.validateFilterEnriched(source);
    }


    private void testValidateSourceFilter(String filter, ExecutionLocationtypeEnum el) {
        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        when(transformation.getName()).thenReturn("TRANSFORMATION NAME");
        Source source1 = InputModelMockHelper.createMockETLSource("alias1", "name1", "model");
        Source source2 = InputModelMockHelper.createMockETLSource("alias2", "name2", "model");
        ArrayList<Source> sources = new ArrayList<>();
        sources.add(source1);
        sources.add(source2);
        Dataset dataset = mock(Dataset.class);
        when(dataset.getSources()).thenReturn(sources);
        when(source1.getParent()).thenReturn(dataset);
        when(source2.getParent()).thenReturn(dataset);
        when(source2.getFilterExecutionLocation()).thenReturn(el);
        when(dataset.getParent()).thenReturn(transformation);
        ArrayList<Dataset> datasets = new ArrayList<>();
        when(transformation.getDatasets()).thenReturn(datasets);
        datasets.add(dataset);
        when(dataset.getParent()).thenReturn(transformation);

        when(source2.getFilter()).thenReturn(filter);

        fixture.validateFilter(source2);
    }

    private void testValidateSourceJoin(String join, ExecutionLocationtypeEnum el, JoinTypeEnum joinType) {
        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        when(transformation.getName()).thenReturn("TRANSFORMATION NAME");
        Source source1 = InputModelMockHelper.createMockETLSource("alias1", "name1", "model");
        Source source2 = InputModelMockHelper.createMockETLSource("alias2", "name2", "model");
        when(source2.getJoinType()).thenReturn(joinType);
        ArrayList<Source> sources = new ArrayList<>();
        sources.add(source1);
        sources.add(source2);
        Dataset dataset = mock(Dataset.class);
        when(dataset.getSources()).thenReturn(sources);
        when(source1.getParent()).thenReturn(dataset);
        when(source2.getParent()).thenReturn(dataset);
        when(source2.getJoinExecutionLocation()).thenReturn(el);
        when(dataset.getParent()).thenReturn(transformation);
        ArrayList<Dataset> datasets = new ArrayList<>();
        when(transformation.getDatasets()).thenReturn(datasets);
        datasets.add(dataset);
        when(dataset.getParent()).thenReturn(transformation);

        when(source2.getJoin()).thenReturn(join);

        fixture.validateJoin(source2);
    }


    @Test
    @Expected(errors = {30221})
    public void testValidate_30221() {
        testValidateSourceJoin("alias1.c1 = alias2.c1", ExecutionLocationtypeEnum.TARGET, null);

        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        when(transformation.getName()).thenReturn("TRANSFORMATION NAME");
        Source source2 = InputModelMockHelper.createMockETLSource("alias2", "name2", "model");
        when(source2.getJoinType()).thenReturn(null);
        ArrayList<Source> sources = new ArrayList<>();
        sources.add(source2);
        Dataset dataset = mock(Dataset.class);
        when(dataset.getSources()).thenReturn(sources);
        when(source2.getParent()).thenReturn(dataset);
        when(source2.getJoinExecutionLocation()).thenReturn(ExecutionLocationtypeEnum.TARGET);
        when(dataset.getParent()).thenReturn(transformation);
        ArrayList<Dataset> datasets = new ArrayList<>();
        when(transformation.getDatasets()).thenReturn(datasets);
        datasets.add(dataset);
        when(dataset.getParent()).thenReturn(transformation);

        when(source2.getJoin()).thenReturn("alias1.c1 = alias2.c1");

        fixture.validateJoinExecutionLocation(source2);
    }

    @Test
    @Expected(errors = {30230})
    public void testValidate_30230_undefinedDatastore() {

        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        Source source = InputModelMockHelper.createMockETLSource("alias", "source", "model");
        Dataset dataset = mock(Dataset.class);
        when(dataset.getSources()).thenReturn(singletonList(source));
        when(source.getParent()).thenReturn(dataset);
        when(dataset.getParent()).thenReturn(transformation);
        when(transformation.getDatasets()).thenReturn(singletonList(dataset));
        when(source.getJoin()).thenReturn("alias.column = 1");
        when(metadataService.getSourceDataStoreInModel("source", "model")).thenThrow(
                new DataStoreNotInModelException(""));
        fixture.validateJoinEnriched(source);
    }

    @Test
    @Expected(errors = {30231})
    public void testValidate_30231_undefinedDatastoreColumn() {

        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        Source source = InputModelMockHelper.createMockETLSource("alias", "source", "model");
        Dataset dataset = mock(Dataset.class);
        when(dataset.getSources()).thenReturn(singletonList(source));
        when(source.getParent()).thenReturn(dataset);
        when(dataset.getParent()).thenReturn(transformation);
        when(transformation.getDatasets()).thenReturn(singletonList(dataset));
        when(source.getJoin()).thenReturn("alias.column = 1");
        DataStore dataStore = mock(DataStore.class);
        when(dataStore.getDataStoreName()).thenReturn("name");
        when(dataStore.getColumns()).thenReturn(new HashMap<>());
        when(metadataService.getSourceDataStoreInModel("source", "model")).thenReturn(dataStore);
        fixture.validateJoinEnriched(source);
    }

    @Test
    @Expected(warnings = {30232})
    public void testValidate_30232_typecastWarning() {

        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        Source source = InputModelMockHelper.createMockETLSource("alias", "source", "model");
        Dataset dataset = mock(Dataset.class);
        when(dataset.getSources()).thenReturn(singletonList(source));
        when(source.getParent()).thenReturn(dataset);
        when(dataset.getParent()).thenReturn(transformation);
        when(transformation.getDatasets()).thenReturn(singletonList(dataset));
        DataStore dataStore = mock(DataStore.class);
        when(dataStore.getDataStoreName()).thenReturn("alias");
        DataStoreColumn col1 = mock(DataStoreColumn.class);
        when(col1.getName()).thenReturn("col1");
        when(col1.getColumnDataType()).thenReturn("VARCHAR");
        DataStoreColumn col2 = mock(DataStoreColumn.class);
        when(col2.getName()).thenReturn("col2");
        when(col2.getColumnDataType()).thenReturn("NUMERIC");
        HashMap<String, DataStoreColumn> map = new HashMap<>();
        map.put("col1", col1);
        map.put("col2", col2);
        when(dataStore.getColumns()).thenReturn(map);
        when(metadataService.getSourceDataStoreInModel("source", "model")).thenReturn(dataStore);
        fixture.validateJoinEnriched(source);
    }


    @Test
    @Expected(errors = {30222})
    public void testValidate_30222() {
        testValidateSourceJoin("alias1.c1 = alias2.c1", ExecutionLocationtypeEnum.TARGET, null);

        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        when(transformation.getName()).thenReturn("TRANSFORMATION NAME");
        Source source2 = InputModelMockHelper.createMockETLSource("alias2", "name2", "model");
        when(source2.getJoinType()).thenReturn(null);
        ArrayList<Source> sources = new ArrayList<>();
        sources.add(source2);
        Dataset dataset = mock(Dataset.class);
        when(dataset.getSources()).thenReturn(sources);
        when(source2.getParent()).thenReturn(dataset);
        when(source2.getJoinExecutionLocation()).thenReturn(ExecutionLocationtypeEnum.TARGET);
        when(dataset.getParent()).thenReturn(transformation);
        ArrayList<Dataset> datasets = new ArrayList<>();
        when(transformation.getDatasets()).thenReturn(datasets);
        datasets.add(dataset);
        when(dataset.getParent()).thenReturn(transformation);

        when(source2.getJoin()).thenReturn("alias1.c1 = alias2.c1");

        fixture.validateJoinExecutionLocation(source2, new ExecutionLocationIDStrategy());
    }

    @Test
    @Expected(errors = {30200})
    public void testValidate_30200() {
        testValidateSourceJoin("name1.c1 = name1.c1 and alias1.c1 = alias2.c1", ExecutionLocationtypeEnum.SOURCE, null);
    }

    @Test
    @Expected(errors = {30204})
    public void testValidate_30204() {
        testValidateSourceJoin("other.c1 = other.c1 and alias2.c1 = alias1.c1", ExecutionLocationtypeEnum.SOURCE, null);
    }

    @Test
    @Expected(errors = {30201})
    public void testValidate_30201() {
        testValidateSourceJoin("alias1.c1 = alias1.c1", ExecutionLocationtypeEnum.SOURCE, null);
    }


    @Test
    @Expected(errors = {30213})
    public void testValidate_30213() {
        testValidateSourceJoin("alias1.c1 = alias2.c1", ExecutionLocationtypeEnum.SOURCE, JoinTypeEnum.CROSS);
    }

    @Test
    @Expected(errors = {30214})
    public void testValidate_30214() {
        testValidateSourceJoin("alias1.c1 = alias2.c1", ExecutionLocationtypeEnum.SOURCE, JoinTypeEnum.NATURAL);
    }

    @Test
    @Expected(errors = {30206})
    public void testValidate_30206_inner() {
        testValidateSourceJoin(null, ExecutionLocationtypeEnum.SOURCE, JoinTypeEnum.INNER);
    }

    @Test
    @Expected(errors = {30206})
    public void testValidate_30206_outer() {
        testValidateSourceJoin(null, ExecutionLocationtypeEnum.SOURCE, JoinTypeEnum.LEFT_OUTER);
    }

    @Test
    @Expected(errors = {30300})
    public void testValidate_30300() {
        String kmName = "KM Name";
        when(properties.getProperty(kmName)).thenThrow(new RuntimeException("no property defined"));
        testValidateLKM(kmName, new String[]{}, new String[]{});
    }

    @Test
    @Expected(errors = {30301})
    public void testValidate_30301() {
        String kmName = "KM Name";
        when(properties.getProperty(kmName + ".name")).thenReturn(kmName);
        ArrayList<KnowledgeModule> kms = new ArrayList<>();
        when(etlSubsystemService.getKMs()).thenReturn(kms);
        KnowledgeModule odiKm = mockKnowledgeModule(kmName);
        kms.add(odiKm);

        testValidateLKM("KM Name", new String[]{"OPTION"}, new String[]{"1"});
    }


    @Test
    @Expected(errors = {30302})
    public void testValidate_30302_checkbox() {
        String kmName = "KM Name";
        when(properties.getProperty(kmName + ".name")).thenReturn(kmName);
        ArrayList<KnowledgeModule> kms = new ArrayList<>();
        when(etlSubsystemService.getKMs()).thenReturn(kms);
        KnowledgeModule odiKm = mockKnowledgeModule(kmName);
        kms.add(odiKm);

        testValidateLKM("KM Name", new String[]{"CHECKBOX"}, new String[]{"INVALID"});
    }


    @Test
    @Expected(errors = {30302})
    public void testValidate_30302_shorttext() {
        String kmName = "KM Name";
        when(properties.getProperty(kmName + ".name")).thenReturn(kmName);
        ArrayList<KnowledgeModule> kms = new ArrayList<>();
        when(etlSubsystemService.getKMs()).thenReturn(kms);
        KnowledgeModule odiKm = mockKnowledgeModule(kmName);
        kms.add(odiKm);

        StringBuilder outputBuffer = new StringBuilder(255);
        for (int i = 0; i < 255; i++) {
            outputBuffer.append("X");
        }

        testValidateLKM(kmName, new String[]{"SHORTTEXT"}, new String[]{outputBuffer.toString()});
    }

    @Test
    @Expected(errors = {30305})
    public void testValidate_30305() {
        String kmName = "KM Name";
        when(properties.getProperty(kmName + ".name")).thenReturn(kmName);
        ArrayList<KnowledgeModule> kms = new ArrayList<>();
        when(etlSubsystemService.getKMs()).thenReturn(kms);

        testValidateLKM("KM Name", new String[]{}, new String[]{});
    }

    @Test
    @Expected(errors = {40240})
    public void testValidate_40240() {
        String kmName = "KM Name";
        when(properties.getProperty(kmName)).thenThrow(
                new JodiPropertyNotFoundException("no property defined", kmName));
        KnowledgeModuleStrategy customStrategy = new KnowledgeModuleIDStrategy();

        testValidateKM("ckm", kmName, new String[]{}, new String[]{}, customStrategy);
    }

    @Test
    @Expected(errors = {40241})
    public void testValidate_40241() {
        String kmName = "KM Name";
        when(properties.getProperty(kmName + ".name")).thenReturn(kmName);
        ArrayList<KnowledgeModule> kms = new ArrayList<>();
        when(etlSubsystemService.getKMs()).thenReturn(kms);
        KnowledgeModule odiKm = mockKnowledgeModule(kmName);
        kms.add(odiKm);

        testValidateKM("ckm", kmName, new String[]{"UNKNOWN"}, new String[]{"UNKNOWN"},
                       new KnowledgeModuleIDStrategy());
    }

    @Test
    @Expected(errors = {40242})
    public void testValidate_40242() {
        String kmName = "KM Name";
        when(properties.getProperty(kmName + ".name")).thenReturn(kmName);
        ArrayList<KnowledgeModule> kms = new ArrayList<>();
        when(etlSubsystemService.getKMs()).thenReturn(kms);
        KnowledgeModule odiKm = mockKnowledgeModule(kmName);
        kms.add(odiKm);

        testValidateKM("ckm", kmName, new String[]{"CHECKBOX"}, new String[]{"INVALID"},
                       new KnowledgeModuleIDStrategy());
    }


    @Test
    @Expected(errors = {40140})
    public void testValidate_40140() {
        String kmName = "KM Name";
        when(properties.getProperty(kmName)).thenThrow(
                new JodiPropertyNotFoundException("no property defined", kmName));
        KnowledgeModuleStrategy customStrategy = new KnowledgeModuleIDStrategy();

        testValidateKM("ikm", kmName, new String[]{}, new String[]{}, customStrategy);
    }

    @Test
    @Expected(errors = {40141})
    public void testValidate_40141() {
        String kmName = "KM Name";
        when(properties.getProperty(kmName + ".name")).thenReturn(kmName);
        ArrayList<KnowledgeModule> kms = new ArrayList<>();
        when(etlSubsystemService.getKMs()).thenReturn(kms);
        KnowledgeModule odiKm = mockKnowledgeModule(kmName);
        kms.add(odiKm);

        testValidateKM("ikm", kmName, new String[]{"UNKNOWN"}, new String[]{"UNKNOWN"},
                       new KnowledgeModuleIDStrategy());
    }

    @Test
    @Expected(errors = {40142})
    public void testValidate_40142() {
        String kmName = "KM Name";
        when(properties.getProperty(kmName + ".name")).thenReturn(kmName);
        ArrayList<KnowledgeModule> kms = new ArrayList<>();
        when(etlSubsystemService.getKMs()).thenReturn(kms);
        KnowledgeModule odiKm = mockKnowledgeModule(kmName);
        kms.add(odiKm);

        testValidateKM("ikm", kmName, new String[]{"CHECKBOX"}, new String[]{"INVALID"},
                       new KnowledgeModuleIDStrategy());
    }

    @Test
    @Expected(errors = {40143})
    public void testValidate_41043() {
        String kmName = "IKM";
        when(properties.getProperty(kmName + ".name")).thenReturn(kmName);
        KnowledgeModule odiKm = mock(KnowledgeModule.class);
        when(odiKm.getName()).thenReturn(kmName);
        when(odiKm.isMultiTechnology()).thenReturn(true);
        when(etlSubsystemService.getKMs()).thenReturn(singletonList(odiKm));
        KmType ikm = mock(KmType.class);
        when(ikm.getName()).thenReturn(kmName);
        Mappings mappings = mock(Mappings.class);
        when(mappings.getIkm()).thenReturn(ikm);
        when(mappings.getStagingModel()).thenReturn(null);
        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        when(mappings.getParent()).thenReturn(transformation);

        fixture.validateStagingModel(mappings);
    }

    @Test
    @Expected(errors = {40144})
    public void testValidate_41044() {
        Mappings mappings = mock(Mappings.class);
        when(mappings.getStagingModel()).thenReturn(null);
        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        when(mappings.getParent()).thenReturn(transformation);

        Exception e = new Exception("PlaceHolder");
        fixture.handleStagingModel(e, mappings);
    }


    private void testValidateLKM(String kmName, String[] keys, String[] values) {
        assert (keys.length == values.length);

        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        when(transformation.getName()).thenReturn("TRANSFORMATION NAME");
        Source source1 = InputModelMockHelper.createMockETLSource("alias1", "name1", "model");
        Source source2 = InputModelMockHelper.createMockETLSource("alias2", "name2", "model");
        when(source2.getJoinType()).thenReturn(JoinTypeEnum.INNER);
        ArrayList<Source> sources = new ArrayList<>();
        sources.add(source1);
        sources.add(source2);
        Dataset dataset = mock(Dataset.class);
        when(dataset.getSources()).thenReturn(sources);
        when(source1.getParent()).thenReturn(dataset);
        when(source2.getParent()).thenReturn(dataset);
        when(source2.getJoinExecutionLocation()).thenReturn(ExecutionLocationtypeEnum.SOURCE);
        when(dataset.getParent()).thenReturn(transformation);
        ArrayList<Dataset> datasets = new ArrayList<>();
        when(transformation.getDatasets()).thenReturn(datasets);
        datasets.add(dataset);
        when(dataset.getParent()).thenReturn(transformation);

        when(source2.getJoin()).thenReturn("alias1.c1 = alias2.c1");

        HashMap<String, String> map = new HashMap<>();
        for (int i = 0; i < keys.length; i++) {
            map.put(keys[i], values[i]);
        }

        KmType km = mock(KmType.class);
        when(km.getName()).thenReturn(kmName);
        when(km.getOptions()).thenReturn(map);
        when(source2.getLkm()).thenReturn(km);

        fixture.validateLKM(source2);
    }


    @Test
    @Expected(errors = {31010})
    public void testValidate_31010() {
        testHandleModelCodeException(new NoModelFoundException(""));
    }

    @Test
    @Expected(errors = {31011})
    public void testValidate_31011() {
        Transformation transformation =
                InputModelMockHelper.createMockETLTransformation(new String[]{"alias1"}, new String[]{"name1"},
                                                                 new String[]{"model"});
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        Source source = transformation.getDatasets()
                                      .get(0)
                                      .getSources()
                                      .get(0);
        Lookup lookup = mock(Lookup.class);
        when(lookup.getLookupDataStore()).thenReturn("LookupDataStore");
        when(lookup.getModel()).thenReturn("model"); //explicit
        when(lookup.getAlias()).thenReturn("LookupAlias");
        when(source.getLookups()).thenReturn(singletonList(lookup));
        when(lookup.getParent()).thenReturn(source);

        fixture.handleModelCode(new DataStoreNotInModelException(""), lookup);
    }


    @Test
    @Expected(errors = {31013})
    public void testValidate_31013() {
        testHandleModelCodeException(new DataStoreNotInModelException(""));
    }


    @Test
    @Expected(errors = {31020})
    public void testValidate_31020() {
        testHandleModelCodeException(new RuntimeException());
    }


    private void testHandleModelCodeException(RuntimeException e) {
        Transformation transformation =
                InputModelMockHelper.createMockETLTransformation(new String[]{"alias1"}, new String[]{"name1"},
                                                                 new String[]{"model"});
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        Source source = transformation.getDatasets()
                                      .get(0)
                                      .getSources()
                                      .get(0);
        Lookup lookup = mock(Lookup.class);
        when(lookup.getLookupDataStore()).thenReturn("LookupDataStore");
        when(lookup.getAlias()).thenReturn("LookupAlias");
        when(source.getLookups()).thenReturn(singletonList(lookup));
        when(lookup.getParent()).thenReturn(source);

        fixture.handleModelCode(e, lookup);
    }


    @Test
    @Expected(warnings = {31000})
    public void testValidate_31000() {
        String lookupDataStore = "LOOKUP_DATA_STORE";
        String alias = "ALIAS";
        String modelCode = "MODELCODE";
        when(properties.getProperty(modelCode)).thenReturn(modelCode);

        when(metadataService.getSourceDataStoreInModel(lookupDataStore, modelCode)).thenReturn(null);
        when(metadataService.isTemporaryTransformation(lookupDataStore)).thenReturn(true);

        testValidateLookup(lookupDataStore, alias, modelCode, true, new LinkedHashMap<>());
    }

    @Test
    @Expected(errors = {31304})
    public void testValidate_31304() {
        String lookupDataStore = "LOOKUP_DATA_STORE";
        String alias = "ALIAS";
        String modelCode = "MODELCODE";
        when(properties.getProperty(modelCode)).thenReturn(modelCode);

        DataStore lookupDS = mock(DataStore.class);
        LinkedHashMap<String, DataStoreColumn> columns = new LinkedHashMap<>();
        when(lookupDS.getColumns()).thenReturn(columns);
        for (int i = 0; i < 2; i++) {
            DataStoreColumn column = mock(DataStoreColumn.class);
            columns.put("C" + (i + 1), column);
        }


        when(metadataService.getSourceDataStoreInModel(lookupDataStore, modelCode)).thenReturn(lookupDS);
        when(metadataService.isTemporaryTransformation(lookupDataStore)).thenReturn(true);

        LinkedHashMap<String, String> defaultColumns = new LinkedHashMap<>();
        defaultColumns.put("UKNOWN", "7");
        defaultColumns.put("C2", "8");
        testValidateLookup(lookupDataStore, alias, modelCode, false, defaultColumns);
    }

    @Test
    @Expected(errors = {31305})
    public void testValidate_31305() {
        String lookupDataStore = "LOOKUP_DATA_STORE";
        String alias = "ALIAS";
        String modelCode = "MODELCODE";
        when(properties.getProperty(modelCode)).thenReturn(modelCode);

        DataStore lookupDS = mock(DataStore.class);
        LinkedHashMap<String, DataStoreColumn> columns = new LinkedHashMap<>();
        when(lookupDS.getColumns()).thenReturn(columns);
        for (int i = 0; i < 2; i++) {
            DataStoreColumn column = mock(DataStoreColumn.class);
            columns.put("C" + (i + 1), column);
        }


        when(metadataService.getSourceDataStoreInModel(lookupDataStore, modelCode)).thenReturn(lookupDS);
        when(metadataService.isTemporaryTransformation(lookupDataStore)).thenReturn(true);

        LinkedHashMap<String, String> defaultColumns = new LinkedHashMap<>();
        defaultColumns.put("C2", "8");
        testValidateLookup(lookupDataStore, alias, modelCode, false, defaultColumns);
    }

    @Test
    @Expected(errors = {31300})
    public void testValidate_31300() {
        String lookupDataStore = "LOOKUP_DATA_STORE";
        String alias = "ALIAS";
        String modelCode = "MODELCODE";
        when(properties.getProperty(modelCode)).thenReturn(modelCode);

        DataStore lookupDS = mock(DataStore.class);
        LinkedHashMap<String, DataStoreColumn> columns = new LinkedHashMap<>();
        when(lookupDS.getColumns()).thenReturn(columns);
        for (int i = 0; i < 2; i++) {
            DataStoreColumn column = mock(DataStoreColumn.class);
            columns.put("C" + (i + 1), column);
        }
        when(metadataService.getSourceDataStoreInModel(lookupDataStore, modelCode)).thenReturn(lookupDS);


        DataStore sourceDS = mock(DataStore.class);
        when(sourceDS.getColumns()).thenReturn(columns);
        when(metadataService.getSourceDataStoreInModel("SOURCE", modelCode)).thenReturn(sourceDS);

        when(metadataService.isTemporaryTransformation(lookupDataStore)).thenReturn(false);

        LinkedHashMap<String, String> defaultColumns = new LinkedHashMap<>();
        defaultColumns.put("C2", "SOURCE.C2");
        testValidateLookup(lookupDataStore, alias, modelCode, false, defaultColumns);
    }

    @Test
    @Expected(errors = {31300})
    public void testValidate_31302() {
        String lookupDataStore = "LOOKUP_DATA_STORE";
        String alias = "ALIAS";
        String modelCode = "MODELCODE";
        when(properties.getProperty(modelCode)).thenReturn(modelCode);

        DataStore lookupDS = mock(DataStore.class);
        LinkedHashMap<String, DataStoreColumn> columns = new LinkedHashMap<>();
        when(lookupDS.getColumns()).thenReturn(columns);
        for (int i = 0; i < 2; i++) {
            DataStoreColumn column = mock(DataStoreColumn.class);
            columns.put("C" + (i + 1), column);
        }
        when(metadataService.getSourceDataStoreInModel(lookupDataStore, modelCode)).thenReturn(lookupDS);

        DataStore sourceDS = mock(DataStore.class);
        when(sourceDS.getColumns()).thenReturn(columns);
        when(metadataService.getSourceDataStoreInModel("SOURCE", modelCode)).thenReturn(sourceDS);

        when(metadataService.isTemporaryTransformation(lookupDataStore)).thenReturn(false);

        LinkedHashMap<String, String> defaultColumns = new LinkedHashMap<>();
        defaultColumns.put("C2", "UNKNOWN.C2");
        testValidateLookup(lookupDataStore, alias, modelCode, false, defaultColumns);
    }

    @Test
    @Expected(errors = {31303})
    public void testValidate_31303() {
        String lookupDataStore = "LOOKUP_DATA_STORE";
        String alias = "ALIAS";
        String modelCode = "MODELCODE";
        when(properties.getProperty(modelCode)).thenReturn(modelCode);

        DataStore lookupDS = mock(DataStore.class);
        LinkedHashMap<String, DataStoreColumn> columns = new LinkedHashMap<>();
        when(lookupDS.getColumns()).thenReturn(columns);
        for (int i = 0; i < 2; i++) {
            DataStoreColumn column = mock(DataStoreColumn.class);
            columns.put("C" + (i + 1), column);
        }
        when(metadataService.getSourceDataStoreInModel(lookupDataStore, modelCode)).thenReturn(lookupDS);

        DataStore sourceDS = mock(DataStore.class);
        when(sourceDS.getColumns()).thenReturn(columns);
        when(metadataService.getSourceDataStoreInModel("SOURCE", modelCode)).thenReturn(sourceDS);

        when(metadataService.isTemporaryTransformation(lookupDataStore)).thenReturn(false);

        LinkedHashMap<String, String> defaultColumns = new LinkedHashMap<>();
        defaultColumns.put("C1", "8");
        defaultColumns.put("C2", "SOURCEALIAS.U1 + ALIAS.U2");
        testValidateLookup(lookupDataStore, alias, modelCode, false, defaultColumns);
    }


    private void testValidateLookup(String lookupDataStore, String alias, String modelCode, boolean subselect,
                                    Map<String, String> defaultColumns) {
        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        when(transformation.getName()).thenReturn("TRANSFORMATION NAME");
        Source source = InputModelMockHelper.createMockETLSource("SOURCEALIAS", "SOURCE", "MODELCODE");
        when(source.getJoinType()).thenReturn(JoinTypeEnum.INNER);
        ArrayList<Source> sources = new ArrayList<>();
        sources.add(source);
        Dataset dataset = mock(Dataset.class);
        when(dataset.getSources()).thenReturn(sources);
        when(source.getParent()).thenReturn(dataset);
        when(dataset.getParent()).thenReturn(transformation);
        ArrayList<Dataset> datasets = new ArrayList<>();
        when(transformation.getDatasets()).thenReturn(datasets);
        datasets.add(dataset);
        when(dataset.getParent()).thenReturn(transformation);
        Lookup lookup = mock(Lookup.class);
        when(lookup.getParent()).thenReturn(source);
        ArrayList<Lookup> lookups = new ArrayList<>();
        lookups.add(lookup);
        when(source.getLookups()).thenReturn(lookups);
        when(lookup.getModel()).thenReturn(modelCode);
        when(lookup.getLookupDataStore()).thenReturn(lookupDataStore);
        when(lookup.getAlias()).thenReturn(alias);
        when(lookup.isSubSelect()).thenReturn(subselect);

        fixture.validateLookup(lookup);

        when(lookup.getDefaultRowColumns()).thenReturn(defaultColumns);
        if (defaultColumns.size() > 0) {
            fixture.validateNoMatchRows(lookup);

        }
    }


    @Test
    @Expected(errors = {40010})
    public void testValidate_40010() {
        String targetDataStore = "TARGET_DATA_STORE";

        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        Mappings mappings = mock(Mappings.class);
        when(mappings.getParent()).thenReturn(transformation);
        when(mappings.getTargetDataStore()).thenReturn(targetDataStore);
        when(metadataService.findDataStoreInAllModels(targetDataStore)).thenReturn(new ArrayList<>());

        fixture.handleModelCode(new NoModelFoundException(""), mappings);
    }


    @Test
    @Expected(errors = {40012})
    public void testValidate_40012() {
        String targetDataStore = "TARGET_DATA_STORE";
        String modelCode = "MODEL_CODE";

        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        Mappings mappings = mock(Mappings.class);
        when(mappings.getParent()).thenReturn(transformation);
        when(mappings.getTargetDataStore()).thenReturn(targetDataStore);
        when(mappings.getModel()).thenReturn(modelCode);

        fixture.handleModelCode(new JodiPropertyNotFoundException("", ""), mappings);
    }


    @Test
    @Expected(errors = {40013})
    public void testValidate_40013() {
        String modelCode = "MODEL_CODE";
        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        Mappings mappings = mock(Mappings.class);
        when(mappings.getParent()).thenReturn(transformation);
        when(mappings.getModel()).thenReturn(modelCode);
        when(properties.getProperty(modelCode)).thenReturn(modelCode);
        when(mappings.getModel()).thenReturn(modelCode);
        fixture.handleModelCode(new DataStoreNotInModelException(""), mappings);
    }

    @Test
    @Expected()
    public void testValidate_40011() {
        String modelCode = "MODEL_CODE";
        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        Mappings mappings = mock(Mappings.class);
        when(mappings.getParent()).thenReturn(transformation);
        when(mappings.getModel()).thenReturn(modelCode);
        when(properties.getProperty(modelCode)).thenReturn(modelCode);
        when(mappings.getModel()).thenReturn(null);
        fixture.handleModelCode(new DataStoreNotInModelException(""), mappings);
    }


    @Test
    @Expected(errors = {40100})
    public void testValidate_40100() {
        String kmName = "IKM_OFF";

        when(properties.getProperty(kmName + ".name")).thenReturn(kmName);

        ArrayList<KnowledgeModule> kms = new ArrayList<>();
        when(etlSubsystemService.getKMs()).thenReturn(kms);

        testValidateKM("ikm", kmName, new String[]{}, new String[]{}, null);
    }


    @Test
    @Expected(errors = {40111})
    public void testValidate_40111() {
        String kmName = "IKM";

        when(properties.getProperty(kmName + ".name")).thenReturn(kmName);

        KnowledgeModule km = mockKnowledgeModule(kmName);
        ArrayList<KnowledgeModule> kms = new ArrayList<>();
        kms.add(km);
        when(etlSubsystemService.getKMs()).thenReturn(kms);

        testValidateKM("ikm", kmName, new String[]{"INVALID_OPTION"}, new String[]{"INVALID_OPTION_VALUE"}, null);
    }


    @Test
    @Expected(errors = {40112})
    public void testValidate_40112_shortext() {
        String kmName = "IKM";

        when(properties.getProperty(kmName + ".name")).thenReturn(kmName);

        KnowledgeModule km = mockKnowledgeModule(kmName);
        ArrayList<KnowledgeModule> kms = new ArrayList<>();
        kms.add(km);
        when(etlSubsystemService.getKMs()).thenReturn(kms);

        StringBuilder outputBuffer = new StringBuilder(255);
        for (int i = 0; i < 255; i++) {
            outputBuffer.append("X");
        }

        testValidateKM("ikm", kmName, new String[]{"SHORTTEXT"}, new String[]{outputBuffer.toString()}, null);
    }

    @Test
    @Expected(errors = {40112})
    public void testValidate_40112_checkbox() {
        String kmName = "IKM";

        when(properties.getProperty(kmName + ".name")).thenReturn(kmName);

        KnowledgeModule km = mockKnowledgeModule(kmName);
        ArrayList<KnowledgeModule> kms = new ArrayList<>();
        kms.add(km);
        when(etlSubsystemService.getKMs()).thenReturn(kms);

        testValidateKM("ikm", kmName, new String[]{"CHECKBOX"}, new String[]{"INVALID_OPTION_VALUE"}, null);
    }


    @Test
    @Expected(errors = {40110})
    public void testValidate_40110() {
        String kmName = "IKM";
        when(properties.getProperty(kmName + ".name")).thenThrow(new JodiPropertyNotFoundException("", ""));
        testValidateKM("ikm", kmName, new String[]{}, new String[]{}, null);
    }

    @Test
    @Expected(errors = {40200})
    public void testValidate_40200() {
        String kmName = "IKM_OFF";

        when(properties.getProperty(kmName + ".name")).thenReturn(kmName);

        ArrayList<KnowledgeModule> kms = new ArrayList<>();
        when(etlSubsystemService.getKMs()).thenReturn(kms);

        testValidateKM("ckm", kmName, new String[]{}, new String[]{}, null);
    }


    @Test
    @Expected(errors = {40211})
    public void testValidate_40211() {
        String kmName = "CKM";

        when(properties.getProperty(kmName + ".name")).thenReturn(kmName);

        KnowledgeModule km = mockKnowledgeModule(kmName);
        ArrayList<KnowledgeModule> kms = new ArrayList<>();
        kms.add(km);
        when(etlSubsystemService.getKMs()).thenReturn(kms);

        testValidateKM("ckm", kmName, new String[]{"INVALID_OPTION"}, new String[]{"INVALID_OPTION_VALUE"}, null);
    }


    @Test
    @Expected(errors = {40212})
    public void testValidate_40212_shortext() {
        String kmName = "IKM";

        when(properties.getProperty(kmName + ".name")).thenReturn(kmName);

        KnowledgeModule km = mockKnowledgeModule(kmName);
        ArrayList<KnowledgeModule> kms = new ArrayList<>();
        kms.add(km);
        when(etlSubsystemService.getKMs()).thenReturn(kms);

        StringBuilder outputBuffer = new StringBuilder(255);
        for (int i = 0; i < 255; i++) {
            outputBuffer.append("X");
        }

        testValidateKM("ckm", kmName, new String[]{"SHORTTEXT"}, new String[]{outputBuffer.toString()}, null);
    }

    @Test
    @Expected(errors = {40212})
    public void testValidate_40212_checkbox() {
        String kmName = "CKM";

        when(properties.getProperty(kmName + ".name")).thenReturn(kmName);

        KnowledgeModule km = mockKnowledgeModule(kmName);
        ArrayList<KnowledgeModule> kms = new ArrayList<>();
        kms.add(km);
        when(etlSubsystemService.getKMs()).thenReturn(kms);

        testValidateKM("ckm", kmName, new String[]{"CHECKBOX"}, new String[]{"INVALID_OPTION_VALUE"}, null);
    }

    @Test
    @Expected(errors = {40210})
    public void testValidate_40210() {
        String kmName = "CKM";
        when(properties.getProperty(kmName + ".name")).thenThrow(new JodiPropertyNotFoundException("", ""));
        testValidateKM("ckm", kmName, new String[]{}, new String[]{}, null);
    }


    private void testValidateKM(String type, String kmName, String[] keys, String[] values,
                                KnowledgeModuleStrategy customStrategy) {
        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        Mappings mappings = mock(Mappings.class);
        when(mappings.getParent()).thenReturn(transformation);
        when(transformation.getMappings()).thenReturn(mappings);

        HashMap<String, String> map = new HashMap<>();
        for (int i = 0; i < keys.length; i++) {
            map.put(keys[i], values[i]);
        }

        KmType kmType = mock(KmType.class);
        when(kmType.getName()).thenReturn(kmName);
        when(kmType.getOptions()).thenReturn(map);


        if ("ikm".equals(type)) {
            when(mappings.getIkm()).thenReturn(kmType);
            if (customStrategy != null) {
                fixture.validateIKM(mappings, customStrategy);
            } else {
                fixture.validateIKM(mappings);
            }

        } else {
            when(mappings.getCkm()).thenReturn(kmType);
            if (customStrategy != null) {
                fixture.validateCKM(mappings, customStrategy);
            } else {
                fixture.validateCKM(mappings);
            }

        }
    }


    private KnowledgeModule mockKnowledgeModule(String name) {
        KnowledgeModule km = mock(KnowledgeModule.class);
        when(km.getName()).thenReturn(name);
        HashMap<String, KnowledgeModule.KMOptionType> options = new HashMap<>();
        options.put("CHECKBOX", KnowledgeModule.KMOptionType.CHECKBOX);
        options.put("LONGTEXT", KnowledgeModule.KMOptionType.LONG_TEXT);
        options.put("SHORTTEXT", KnowledgeModule.KMOptionType.SHORT_TEXT);

        when(km.getOptions()).thenReturn(options);
        return km;
    }

    @SuppressWarnings("unused")
    @Test
    @Expected(errors = {41000})
    public void testValidate_41000() {
        Targetcolumn targetColumn = createMockTargetColumn("TARGET", false, "DOESNT_EXIST", null, 0, 0, "ALIAS.COL_2");
        Mappings mappings = targetColumn.getParent();
        Transformation transformation = mappings.getParent();
        List<Dataset> datasets = InputModelMockHelper.createMockETLDatasets(transformation, new String[]{"ALIAS"},
                                                                            new String[]{"SOURCE"},
                                                                            new String[]{"MODEL"});
        when(transformation.getDatasets()).thenReturn(datasets);

        DataStore targetDataStore =
                createMockDataStore(new String[]{"COL_1"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{11});
        when(metadataService.getTargetDataStoreInModel(mappings)).thenReturn(targetDataStore);
        DataStore ds = metadataService.getTargetDataStoreInModel(mappings);
        DataStore sourceDataStore =
                createMockDataStore(new String[]{"COL_2"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{11});
        when(metadataService.getSourceDataStoreInModel("SOURCE", "MODEL")).thenReturn(sourceDataStore);

        fixture.validateTargetColumn(targetColumn);
    }


    @SuppressWarnings("unused")
    @Test
    @Expected(warnings = {41001})
    public void testValidate_41001() {
        Targetcolumn targetColumn = createMockTargetColumn("TARGET", false, "COL_1", null, 0, 0, "ALIAS.COL_2");
        Mappings mappings = targetColumn.getParent();
        Transformation transformation = mappings.getParent();
        List<Dataset> datasets = InputModelMockHelper.createMockETLDatasets(transformation, new String[]{"ALIAS"},
                                                                            new String[]{"SOURCE"},
                                                                            new String[]{"MODEL"});
        when(transformation.getDatasets()).thenReturn(datasets);

        DataStore targetDataStore =
                createMockDataStore(new String[]{"COL_1"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{11});
        when(metadataService.getTargetDataStoreInModel(mappings)).thenReturn(targetDataStore);
        DataStore ds = metadataService.getTargetDataStoreInModel(mappings);
        DataStore sourceDataStore =
                createMockDataStore(new String[]{"COL_2"}, new String[]{"DATE"}, new int[]{11}, new int[]{11});
        when(metadataService.getSourceDataStoreInModel("SOURCE", "MODEL")).thenReturn(sourceDataStore);

        fixture.validateTargetColumn(targetColumn);

    }

    @Test
    @Expected(warnings = {41002})
    public void testValidate_41002_length() {
        Targetcolumn targetColumn = createMockTargetColumn("TARGET", false, "COL_1", null, 0, 0, "ALIAS.COL_2");
        Mappings mappings = targetColumn.getParent();
        Transformation transformation = mappings.getParent();
        List<Dataset> datasets = InputModelMockHelper.createMockETLDatasets(transformation, new String[]{"ALIAS"},
                                                                            new String[]{"SOURCE"},
                                                                            new String[]{"MODEL"});
        when(transformation.getDatasets()).thenReturn(datasets);

        DataStore targetDataStore =
                createMockDataStore(new String[]{"COL_1"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{11});
        when(metadataService.getTargetDataStoreInModel(mappings)).thenReturn(targetDataStore);
        @SuppressWarnings("unused") DataStore ds = metadataService.getTargetDataStoreInModel(mappings);
        DataStore sourceDataStore =
                createMockDataStore(new String[]{"COL_2"}, new String[]{"VARCHAR"}, new int[]{22}, new int[]{11});
        when(metadataService.getSourceDataStoreInModel("SOURCE", "MODEL")).thenReturn(sourceDataStore);

        fixture.validateTargetColumn(targetColumn);
    }

    // non-temporary, source scale greater than target scale
    @Test
    @Expected(warnings = {41002})
    public void testValidate_41002_scale() {
        Targetcolumn targetColumn = createMockTargetColumn("TARGET", false, "COL_1", null, 0, 0, "ALIAS.COL_2");
        Mappings mappings = targetColumn.getParent();
        Transformation transformation = mappings.getParent();
        List<Dataset> datasets = InputModelMockHelper.createMockETLDatasets(transformation, new String[]{"ALIAS"},
                                                                            new String[]{"SOURCE"},
                                                                            new String[]{"MODEL"});
        when(transformation.getDatasets()).thenReturn(datasets);

        DataStore targetDataStore =
                createMockDataStore(new String[]{"COL_1"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{11});
        when(metadataService.getTargetDataStoreInModel(mappings)).thenReturn(targetDataStore);
        @SuppressWarnings("unused") DataStore ds = metadataService.getTargetDataStoreInModel(mappings);
        DataStore sourceDataStore =
                createMockDataStore(new String[]{"COL_2"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{22});
        when(metadataService.getSourceDataStoreInModel("SOURCE", "MODEL")).thenReturn(sourceDataStore);

        fixture.validateTargetColumn(targetColumn);
    }

    @Test
    @Expected(warnings = {41002})
    public void testValidate_41002_scale_temporary() {
        Targetcolumn targetColumn = createMockTargetColumn("TARGET", true, "COL_12", "VARCHAR", 10, 0, "ALIAS.COL_2");
        Mappings mappings = targetColumn.getParent();
        Transformation transformation = mappings.getParent();
        List<Dataset> datasets = InputModelMockHelper.createMockETLDatasets(transformation, new String[]{"ALIAS"},
                                                                            new String[]{"SOURCE"},
                                                                            new String[]{"MODEL"});
        when(transformation.getDatasets()).thenReturn(datasets);

        @SuppressWarnings("unused") DataStore ds = metadataService.getTargetDataStoreInModel(mappings);
        DataStore sourceDataStore =
                createMockDataStore(new String[]{"COL_2"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{22});
        when(metadataService.getSourceDataStoreInModel("SOURCE", "MODEL")).thenReturn(sourceDataStore);

        fixture.validateTargetColumn(targetColumn);
    }

    @Test
    @Expected(warnings = {41002})
    public void testValidate_41002_length_temporary() {
        Targetcolumn targetColumn = createMockTargetColumn("TARGET", true, "COL_12", "VARCHAR", 0, 11, "ALIAS.COL_2");
        Mappings mappings = targetColumn.getParent();
        Transformation transformation = mappings.getParent();
        List<Dataset> datasets = InputModelMockHelper.createMockETLDatasets(transformation, new String[]{"ALIAS"},
                                                                            new String[]{"SOURCE"},
                                                                            new String[]{"MODEL"});
        when(transformation.getDatasets()).thenReturn(datasets);

        @SuppressWarnings("unused") DataStore ds = metadataService.getTargetDataStoreInModel(mappings);
        DataStore sourceDataStore =
                createMockDataStore(new String[]{"COL_2"}, new String[]{"VARCHAR"}, new int[]{22}, new int[]{22});
        when(metadataService.getSourceDataStoreInModel("SOURCE", "MODEL")).thenReturn(sourceDataStore);

        fixture.validateTargetColumn(targetColumn);
    }

    // Two datasets with only a single expression
    @Test
    @Expected(errors = {41004})
    public void testValidate_41004() {
        Targetcolumn targetColumn = createMockTargetColumn("TARGET", false, "COL_1", null, 0, 0, "");
        Mappings mappings = targetColumn.getParent();
        Transformation transformation = mappings.getParent();
        List<Dataset> datasets =
                InputModelMockHelper.createMockETLDatasets(transformation, new String[]{"ALIAS1", "ALIAS2"},
                                                           new String[]{"SOURCE1", "SOURCE2"},
                                                           new String[]{"MODEL", "MODEL"});
        when(transformation.getDatasets()).thenReturn(datasets);

        DataStore targetDataStore =
                createMockDataStore(new String[]{"COL_1"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{11});
        when(metadataService.getTargetDataStoreInModel(mappings)).thenReturn(targetDataStore);
        @SuppressWarnings("unused") DataStore ds = metadataService.getTargetDataStoreInModel(mappings);
        DataStore sourceDataStore =
                createMockDataStore(new String[]{"COL_2"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{11});
        when(metadataService.getSourceDataStoreInModel("SOURCE", "MODEL")).thenReturn(sourceDataStore);

        fixture.validateTargetColumn(targetColumn);
    }

    @Test
    @Expected(warnings = {41003})
    public void testValidate_41003() {
        // no expressions passed to createMockTargetColumn
        Targetcolumn targetColumn = createMockTargetColumn("TARGET", false, "COL_1", null, 0, 0);
        Mappings mappings = targetColumn.getParent();
        Transformation transformation = mappings.getParent();
        List<Dataset> datasets =
                InputModelMockHelper.createMockETLDatasets(transformation, new String[]{"ALIAS1", "ALIAS2"},
                                                           new String[]{"SOURCE1", "SOURCE2"},
                                                           new String[]{"MODEL", "MODEL"});
        when(transformation.getDatasets()).thenReturn(datasets);

        DataStore targetDataStore =
                createMockDataStore(new String[]{"COL_1"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{11});
        when(metadataService.getTargetDataStoreInModel(mappings)).thenReturn(targetDataStore);
        @SuppressWarnings("unused") DataStore ds = metadataService.getTargetDataStoreInModel(mappings);
        DataStore sourceDataStore =
                createMockDataStore(new String[]{"COL_2"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{11});
        when(metadataService.getSourceDataStoreInModel("SOURCE", "MODEL")).thenReturn(sourceDataStore);

        fixture.validateTargetColumn(targetColumn);
    }


    @Test
    @Expected(errors = {41010})
    public void testValidate_41010() {
        Targetcolumn targetColumn = createMockTargetColumn("TARGET", false, "COL_1", null, 0, 0, "SOURCE.COL_2");
        Mappings mappings = targetColumn.getParent();
        Transformation transformation = mappings.getParent();
        List<Dataset> datasets = InputModelMockHelper.createMockETLDatasets(transformation, new String[]{"ALIAS"},
                                                                            new String[]{"SOURCE"},
                                                                            new String[]{"MODEL"});
        when(transformation.getDatasets()).thenReturn(datasets);

        DataStore targetDataStore =
                createMockDataStore(new String[]{"COL_1"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{11});
        when(metadataService.getTargetDataStoreInModel(mappings)).thenReturn(targetDataStore);
        @SuppressWarnings("unused") DataStore ds = metadataService.getTargetDataStoreInModel(mappings);
        DataStore sourceDataStore =
                createMockDataStore(new String[]{"COL_2"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{11});
        when(metadataService.getSourceDataStoreInModel("SOURCE", "MODEL")).thenReturn(sourceDataStore);

        fixture.validateTargetColumn(targetColumn);
    }

    @Test
    @Expected(errors = {41011})
    public void testValidate_41011() {
        Targetcolumn targetColumn =
                createMockTargetColumn("TARGET", false, "COL_1", null, 0, 0, "ALIAS2.COL_2", "NULL");
        Mappings mappings = targetColumn.getParent();
        Transformation transformation = mappings.getParent();
        List<Dataset> datasets =
                InputModelMockHelper.createMockETLDatasets(transformation, new String[]{"ALIAS1", "ALIAS2"},
                                                           new String[]{"SOURCE1", "SOURCE2"},
                                                           new String[]{"MODEL", "MODEL"});
        when(transformation.getDatasets()).thenReturn(datasets);

        DataStore targetDataStore =
                createMockDataStore(new String[]{"COL_1"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{11});
        when(metadataService.getTargetDataStoreInModel(mappings)).thenReturn(targetDataStore);
        @SuppressWarnings("unused") DataStore ds = metadataService.getTargetDataStoreInModel(mappings);
        DataStore sourceDataStore =
                createMockDataStore(new String[]{"COL_2"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{11});
        when(metadataService.getSourceDataStoreInModel("SOURCE", "MODEL")).thenReturn(sourceDataStore);

        fixture.validateTargetColumn(targetColumn);
    }

    @Test
    @Expected(errors = {41016})
    @Ignore
    public void testValidate_41016() {
        Targetcolumn targetColumn = createMockTargetColumn("TARGET", false, "COL_1", null, 0, 0, "UNKNOWN_ALIAS.C1");
        Mappings mappings = targetColumn.getParent();
        Transformation transformation = mappings.getParent();
        List<Dataset> datasets = InputModelMockHelper.createMockETLDatasets(transformation, new String[]{"ALIAS"},
                                                                            new String[]{"SOURCE"},
                                                                            new String[]{"MODEL"});
        when(transformation.getDatasets()).thenReturn(datasets);

        DataStore targetDataStore =
                createMockDataStore(new String[]{"COL_1"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{11});
        when(metadataService.getTargetDataStoreInModel(mappings)).thenReturn(targetDataStore);
        @SuppressWarnings("unused") DataStore ds = metadataService.getTargetDataStoreInModel(mappings);
        DataStore sourceDataStore =
                createMockDataStore(new String[]{"COL_2"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{11});
        when(metadataService.getSourceDataStoreInModel("SOURCE", "MODEL")).thenReturn(sourceDataStore);

        fixture.validateTargetColumn(targetColumn);
    }

    @Test
    @Expected()
    public void testValidate_targetColumnNEXTVAL() {
        Targetcolumn targetColumn =
                createMockTargetColumn("TARGET", false, "COL_1", null, 0, 0, "UNKNOWN_ALIAS.C1.NEXTVAL");
        Mappings mappings = targetColumn.getParent();
        Transformation transformation = mappings.getParent();
        List<Dataset> datasets = InputModelMockHelper.createMockETLDatasets(transformation, new String[]{"ALIAS"},
                                                                            new String[]{"SOURCE"},
                                                                            new String[]{"MODEL"});
        when(transformation.getDatasets()).thenReturn(datasets);

        DataStore targetDataStore =
                createMockDataStore(new String[]{"COL_1"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{11});
        when(metadataService.getTargetDataStoreInModel(mappings)).thenReturn(targetDataStore);
        @SuppressWarnings("unused") DataStore ds = metadataService.getTargetDataStoreInModel(mappings);
        DataStore sourceDataStore =
                createMockDataStore(new String[]{"COL_2"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{11});
        when(metadataService.getSourceDataStoreInModel("SOURCE", "MODEL")).thenReturn(sourceDataStore);

        fixture.validateTargetColumn(targetColumn);
    }

    @Test
    @Expected(errors = {41012})
    public void testValidate_41012() {
        Targetcolumn targetColumn =
                createMockTargetColumn("TARGET", false, "COL_1", null, 0, 0, "ALIAS.UNDEFINED_COLUMN");
        Mappings mappings = targetColumn.getParent();
        Transformation transformation = mappings.getParent();
        List<Dataset> datasets = InputModelMockHelper.createMockETLDatasets(transformation, new String[]{"ALIAS"},
                                                                            new String[]{"SOURCE"},
                                                                            new String[]{"MODEL"});
        when(transformation.getDatasets()).thenReturn(datasets);

        DataStore targetDataStore =
                createMockDataStore(new String[]{"COL_1"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{11});
        when(metadataService.getTargetDataStoreInModel(mappings)).thenReturn(targetDataStore);
        @SuppressWarnings("unused") DataStore ds = metadataService.getTargetDataStoreInModel(mappings);
        DataStore sourceDataStore =
                createMockDataStore(new String[]{"COL_2"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{11});
        when(metadataService.getSourceDataStoreInModel("SOURCE", "MODEL")).thenReturn(sourceDataStore);

        fixture.validateTargetColumn(targetColumn);
    }

    @Test
    @Expected(errors = {41013})
    public void testValidate_41013() {
        // target column is created with empty expression (from external model)
        Targetcolumn targetColumn = createMockTargetColumn("TARGET", false, "COL_1", null, 0, 0, "");
        Mappings mappings = targetColumn.getParent();
        Transformation transformation = mappings.getParent();
        List<Dataset> datasets = InputModelMockHelper.createMockETLDatasets(transformation, new String[]{"ALIAS"},
                                                                            new String[]{"SOURCE"},
                                                                            new String[]{"MODEL"});
        when(transformation.getDatasets()).thenReturn(datasets);

        DataStore targetDataStore =
                createMockDataStore(new String[]{"COL_1"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{11});
        when(metadataService.getTargetDataStoreInModel(mappings)).thenReturn(targetDataStore);
        @SuppressWarnings("unused") DataStore ds = metadataService.getTargetDataStoreInModel(mappings);
        DataStore sourceDataStore =
                createMockDataStore(new String[]{"COL_2"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{11});
        when(metadataService.getSourceDataStoreInModel("SOURCE", "MODEL")).thenReturn(sourceDataStore);

        fixture.validateTargetColumn(targetColumn);
    }

    @Test
    @Expected(warnings = {41030})
    public void testValidate_41030_datatype() {
        Targetcolumn targetColumn = createMockTargetColumn("TARGET", false, "COL_1", "VARCHAR", 0, 0, "NULL");
        Mappings mappings = targetColumn.getParent();
        Transformation transformation = mappings.getParent();
        List<Dataset> datasets = InputModelMockHelper.createMockETLDatasets(transformation, new String[]{"ALIAS"},
                                                                            new String[]{"SOURCE"},
                                                                            new String[]{"MODEL"});
        when(transformation.getDatasets()).thenReturn(datasets);

        DataStore targetDataStore =
                createMockDataStore(new String[]{"COL_1"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{11});
        when(metadataService.getTargetDataStoreInModel(mappings)).thenReturn(targetDataStore);
        @SuppressWarnings("unused") DataStore ds = metadataService.getTargetDataStoreInModel(mappings);
        DataStore sourceDataStore =
                createMockDataStore(new String[]{"COL_2"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{11});
        when(metadataService.getSourceDataStoreInModel("SOURCE", "MODEL")).thenReturn(sourceDataStore);

        fixture.validateTargetColumn(targetColumn);
    }

    @Test
    @Expected(warnings = {41030})
    public void testValidate_41030_length() {
        Targetcolumn targetColumn = createMockTargetColumn("TARGET", false, "COL_1", null, 1, 0, "NULL");
        Mappings mappings = targetColumn.getParent();
        Transformation transformation = mappings.getParent();
        List<Dataset> datasets = InputModelMockHelper.createMockETLDatasets(transformation, new String[]{"ALIAS"},
                                                                            new String[]{"SOURCE"},
                                                                            new String[]{"MODEL"});
        when(transformation.getDatasets()).thenReturn(datasets);

        DataStore targetDataStore =
                createMockDataStore(new String[]{"COL_1"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{11});
        when(metadataService.getTargetDataStoreInModel(mappings)).thenReturn(targetDataStore);
        @SuppressWarnings("unused") DataStore ds = metadataService.getTargetDataStoreInModel(mappings);
        DataStore sourceDataStore =
                createMockDataStore(new String[]{"COL_2"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{11});
        when(metadataService.getSourceDataStoreInModel("SOURCE", "MODEL")).thenReturn(sourceDataStore);

        fixture.validateTargetColumn(targetColumn);
    }

    @Test
    @Expected(warnings = {41030})
    public void testValidate_41030_scale() {
        Targetcolumn targetColumn = createMockTargetColumn("TARGET", false, "COL_1", null, 0, 1, "NULL");
        Mappings mappings = targetColumn.getParent();
        Transformation transformation = mappings.getParent();
        List<Dataset> datasets = InputModelMockHelper.createMockETLDatasets(transformation, new String[]{"ALIAS"},
                                                                            new String[]{"SOURCE"},
                                                                            new String[]{"MODEL"});
        when(transformation.getDatasets()).thenReturn(datasets);

        DataStore targetDataStore =
                createMockDataStore(new String[]{"COL_1"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{11});
        when(metadataService.getTargetDataStoreInModel(mappings)).thenReturn(targetDataStore);
        @SuppressWarnings("unused") DataStore ds = metadataService.getTargetDataStoreInModel(mappings);
        DataStore sourceDataStore =
                createMockDataStore(new String[]{"COL_2"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{11});
        when(metadataService.getSourceDataStoreInModel("SOURCE", "MODEL")).thenReturn(sourceDataStore);

        fixture.validateTargetColumn(targetColumn);
    }

    @Test
    @Expected(errors = {41031})
    public void testValidate_41031() {
        Targetcolumn targetColumn = createMockTargetColumn("TARGET", true, "COL_1", null, 0, 0, "NULL");
        Mappings mappings = targetColumn.getParent();
        Transformation transformation = mappings.getParent();
        List<Dataset> datasets = InputModelMockHelper.createMockETLDatasets(transformation, new String[]{"ALIAS"},
                                                                            new String[]{"SOURCE"},
                                                                            new String[]{"MODEL"});
        when(transformation.getDatasets()).thenReturn(datasets);

        DataStore targetDataStore =
                createMockDataStore(new String[]{"COL_1"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{11});
        when(metadataService.getTargetDataStoreInModel(mappings)).thenReturn(targetDataStore);
        @SuppressWarnings("unused") DataStore ds = metadataService.getTargetDataStoreInModel(mappings);
        DataStore sourceDataStore =
                createMockDataStore(new String[]{"COL_2"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{11});
        when(metadataService.getSourceDataStoreInModel("SOURCE", "MODEL")).thenReturn(sourceDataStore);

        fixture.validateTargetColumn(targetColumn);
    }

    @Test
    @Expected(errors = {41032})
    public void testValidate_41032() {
        Targetcolumn targetColumn = createMockTargetColumn("TARGET", true, "COL_1", null, 22, 22, "NULL");
        Mappings mappings = targetColumn.getParent();
        Transformation transformation = mappings.getParent();
        List<Dataset> datasets = InputModelMockHelper.createMockETLDatasets(transformation, new String[]{"ALIAS"},
                                                                            new String[]{"SOURCE"},
                                                                            new String[]{"MODEL"});
        when(transformation.getDatasets()).thenReturn(datasets);

        DataStore targetDataStore =
                createMockDataStore(new String[]{"COL_1"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{11});
        when(metadataService.getTargetDataStoreInModel(mappings)).thenReturn(targetDataStore);
        @SuppressWarnings("unused") DataStore ds = metadataService.getTargetDataStoreInModel(mappings);
        DataStore sourceDataStore =
                createMockDataStore(new String[]{"COL_2"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{11});
        when(metadataService.getSourceDataStoreInModel("SOURCE", "MODEL")).thenReturn(sourceDataStore);

        fixture.validateTargetColumn(targetColumn);
    }


    private DataStore createMockDataStore(String[] columns, String[] dataTypes, int[] scales, int[] lengths) {
        assert (columns.length == dataTypes.length && scales.length == lengths.length &&
                dataTypes.length == scales.length);

        DataStore dataStore = mock(DataStore.class);
        HashMap<String, DataStoreColumn> dataStoreColumns = new HashMap<>();
        when(dataStore.getColumns()).thenReturn(dataStoreColumns);
        for (int i = 0; i < columns.length; i++) {
            DataStoreColumn dataStoreColumn = mock(DataStoreColumn.class);
            when(dataStoreColumn.getColumnDataType()).thenReturn(dataTypes[i]);
            when(dataStoreColumn.getLength()).thenReturn(lengths[i]);
            when(dataStoreColumn.getScale()).thenReturn(scales[i]);
            when(dataStoreColumn.getName()).thenReturn(columns[i]);
            dataStoreColumns.put(columns[i], dataStoreColumn);
        }

        return dataStore;
    }

    private Targetcolumn createMockTargetColumn(String targetDataStore, boolean temporary, String name, String dataType,
                                                int length, int scale, String... expressions) {
        Targetcolumn targetColumn = mock(Targetcolumn.class);
        when(targetColumn.getName()).thenReturn(name);
        when(targetColumn.getLength()).thenReturn(length);
        when(targetColumn.getDataType()).thenReturn(dataType);
        when(targetColumn.getScale()).thenReturn(scale);
        ArrayList<String> expressionList = new ArrayList<>();
        Collections.addAll(expressionList, expressions);
        when(targetColumn.getMappingExpressions()).thenReturn(expressionList);

        Mappings mappings = mock(Mappings.class);
        when(mappings.getTargetDataStore()).thenReturn(targetDataStore);
        when(targetColumn.getParent()).thenReturn(mappings);
        when(mappings.getTargetColumns()).thenReturn(singletonList(targetColumn));

        Transformation transformation = mock(Transformation.class);
        when(mappings.getParent()).thenReturn(transformation);
        when(transformation.isTemporary()).thenReturn(temporary);
        when(transformation.getMappings()).thenReturn(mappings);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);

        return targetColumn;
    }

    @Test
    @Expected(errors = {10020})
    public void testHandleTransformationName() {
        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);

        fixture.handleTransformationName(new Exception(""), transformation);
    }

    @Test
    @Expected(errors = {31221})
    public void testValidate_31221_LookupExecutionLocation() {
        String lookupDataStore = "LN";
        String alias = "LA";
        String modelCode = "MODELCODE";
        when(properties.getProperty(modelCode)).thenReturn(modelCode);

        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        when(transformation.getName()).thenReturn("TRANSFORMATION NAME");
        Source source1 = InputModelMockHelper.createMockETLSource("S1A", "S1N", "model");
        when(source1.getJoinType()).thenReturn(JoinTypeEnum.INNER);
        ArrayList<Source> sources = new ArrayList<>();
        sources.add(source1);
        Source source2 = InputModelMockHelper.createMockETLSource("S2A", "S2N", "model");
        sources.add(source2);

        Dataset dataset = mock(Dataset.class);
        when(dataset.getSources()).thenReturn(sources);
        when(source1.getParent()).thenReturn(dataset);
        when(dataset.getParent()).thenReturn(transformation);
        ArrayList<Dataset> datasets = new ArrayList<>();
        when(transformation.getDatasets()).thenReturn(datasets);
        datasets.add(dataset);
        when(dataset.getParent()).thenReturn(transformation);
        Lookup lookup = mock(Lookup.class);
        when(lookup.getParent()).thenReturn(source1);
        when(lookup.getJoin()).thenReturn("");
        ArrayList<Lookup> lookups = new ArrayList<>();
        lookups.add(lookup);
        when(source1.getLookups()).thenReturn(lookups);
        when(lookup.getModel()).thenReturn(modelCode);
        when(lookup.getLookupDataStore()).thenReturn(lookupDataStore);
        when(lookup.getAlias()).thenReturn(alias);
        when(lookup.isSubSelect()).thenReturn(false);
        when(lookup.getJoinExecutionLocation()).thenReturn(ExecutionLocationtypeEnum.TARGET);

        fixture.validateExecutionLocation(lookup);
    }

    @Test
    @Expected(errors = {31222})
    public void testValidate_31222_LookupExecutionLocationFromStrategy() {
        String lookupDataStore = "LN";
        String alias = "LA";
        String modelCode = "MODELCODE";
        when(properties.getProperty(modelCode)).thenReturn(modelCode);

        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        when(transformation.getName()).thenReturn("TRANSFORMATION NAME");
        Source source1 = InputModelMockHelper.createMockETLSource("S1A", "S1N", "model");
        when(source1.getJoinType()).thenReturn(JoinTypeEnum.INNER);
        ArrayList<Source> sources = new ArrayList<>();
        sources.add(source1);
        Source source2 = InputModelMockHelper.createMockETLSource("S2A", "S2N", "model");
        sources.add(source2);

        Dataset dataset = mock(Dataset.class);
        when(dataset.getSources()).thenReturn(sources);
        when(source1.getParent()).thenReturn(dataset);
        when(dataset.getParent()).thenReturn(transformation);
        ArrayList<Dataset> datasets = new ArrayList<>();
        when(transformation.getDatasets()).thenReturn(datasets);
        datasets.add(dataset);
        when(dataset.getParent()).thenReturn(transformation);
        Lookup lookup = mock(Lookup.class);
        when(lookup.getParent()).thenReturn(source1);
        when(lookup.getJoin()).thenReturn("");
        ArrayList<Lookup> lookups = new ArrayList<>();
        lookups.add(lookup);
        when(source1.getLookups()).thenReturn(lookups);
        when(lookup.getModel()).thenReturn(modelCode);
        when(lookup.getLookupDataStore()).thenReturn(lookupDataStore);
        when(lookup.getAlias()).thenReturn(alias);
        when(lookup.isSubSelect()).thenReturn(false);
        when(lookup.getJoinExecutionLocation()).thenReturn(ExecutionLocationtypeEnum.TARGET);

        fixture.validateExecutionLocation(lookup, new ExecutionLocationIDStrategy() {
        });
    }

    private void testLookupJoin(String join) {
        String lookupDataStore = "LN";
        String alias = "LA";
        String modelCode = "MODELCODE";
        when(properties.getProperty(modelCode)).thenReturn(modelCode);

        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        when(transformation.getName()).thenReturn("TRANSFORMATION NAME");
        Source source1 = InputModelMockHelper.createMockETLSource("S1A", "S1N", "model");
        when(source1.getJoinType()).thenReturn(JoinTypeEnum.INNER);
        ArrayList<Source> sources = new ArrayList<>();
        sources.add(source1);
        Source source2 = InputModelMockHelper.createMockETLSource("S2A", "S2N", "model");
        sources.add(source2);

        Dataset dataset = mock(Dataset.class);
        when(dataset.getSources()).thenReturn(sources);
        when(source1.getParent()).thenReturn(dataset);
        when(dataset.getParent()).thenReturn(transformation);
        ArrayList<Dataset> datasets = new ArrayList<>();
        when(transformation.getDatasets()).thenReturn(datasets);
        datasets.add(dataset);
        when(dataset.getParent()).thenReturn(transformation);
        Lookup lookup = mock(Lookup.class);
        when(lookup.getParent()).thenReturn(source1);
        when(lookup.getJoin()).thenReturn(join);
        ArrayList<Lookup> lookups = new ArrayList<>();
        lookups.add(lookup);
        when(source1.getLookups()).thenReturn(lookups);
        when(lookup.getModel()).thenReturn(modelCode);
        when(lookup.getLookupDataStore()).thenReturn(lookupDataStore);
        when(lookup.getAlias()).thenReturn(alias);
        when(lookup.isSubSelect()).thenReturn(false);

        fixture.validateLookupJoin(lookup);
    }

    @Test
    @Expected(errors = {31200})
    public void testValidate_31200() {
        testLookupJoin("LA.C1 = S1N.C1");
    }

    @Test
    @Expected(errors = {31201})
    public void testValidate_31201() {
        testLookupJoin("LA.C1 = 0");
    }

    @Test
    @Expected(errors = {31202})
    public void testValidate_31202() {
        testLookupJoin("LA.C1 = UNKNOWN.C1");
    }

    @Test
    @Expected(errors = {31203})
    public void testValidate_31203() {
        testLookupJoin("LA.C1 = S2N.C1");
    }

    @Test
    @Expected(errors = {31230})
    public void testValidate_31230_undefinedDatastore() {

        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        Source source = InputModelMockHelper.createMockETLSource("alias1", "source1", "model");
        Dataset dataset = mock(Dataset.class);
        when(dataset.getSources()).thenReturn(singletonList(source));
        when(source.getParent()).thenReturn(dataset);
        when(dataset.getParent()).thenReturn(transformation);
        when(transformation.getDatasets()).thenReturn(singletonList(dataset));

        Lookup lookup = mock(Lookup.class);
        when(lookup.getLookupDataStore()).thenReturn("lookup");
        when(lookup.getAlias()).thenReturn("alias");
        when(lookup.getModel()).thenReturn("model");
        when(lookup.getParent()).thenReturn(source);
        when(lookup.getJoin()).thenReturn("alias.col1 = alias.col2");
        when(source.getLookups()).thenReturn(singletonList(lookup));
        when(metadataService.getSourceDataStoreInModel("lookup", "model")).thenThrow(
                new DataStoreNotInModelException(""));
        fixture.validateJoinEnriched(lookup);
    }

    @Test
    @Expected(errors = {31231})
    public void testValidate_31231_undefinedDatastoreColumn() {

        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        Source source = InputModelMockHelper.createMockETLSource("alias1", "source1", "model");
        Dataset dataset = mock(Dataset.class);
        when(dataset.getSources()).thenReturn(singletonList(source));
        when(source.getParent()).thenReturn(dataset);
        when(dataset.getParent()).thenReturn(transformation);
        when(transformation.getDatasets()).thenReturn(singletonList(dataset));

        Lookup lookup = mock(Lookup.class);
        when(lookup.getLookupDataStore()).thenReturn("lookup");
        when(lookup.getAlias()).thenReturn("alias");
        when(lookup.getModel()).thenReturn("model");
        when(lookup.getParent()).thenReturn(source);
        when(lookup.getJoin()).thenReturn("alias.col1 = alias.col2");
        when(source.getLookups()).thenReturn(singletonList(lookup));
        DataStore dataStore = mock(DataStore.class);
        when(dataStore.getDataStoreName()).thenReturn("lookup");
        when(dataStore.getColumns()).thenReturn(new HashMap<>());
        when(metadataService.getSourceDataStoreInModel("lookup", "model")).thenReturn(dataStore);
        fixture.validateJoinEnriched(lookup);
    }

    @Test
    @Expected()
    public void testValidate_31231_temporary() {

        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        Source source = InputModelMockHelper.createMockETLSource("alias1", "source1", "model");
        Dataset dataset = mock(Dataset.class);
        when(dataset.getSources()).thenReturn(singletonList(source));
        when(source.getParent()).thenReturn(dataset);
        when(dataset.getParent()).thenReturn(transformation);
        when(transformation.getDatasets()).thenReturn(singletonList(dataset));

        Lookup lookup = mock(Lookup.class);
        when(lookup.getLookupDataStore()).thenReturn("lookup");
        when(lookup.getAlias()).thenReturn("alias");
        when(lookup.getModel()).thenReturn("model");
        when(lookup.getParent()).thenReturn(source);
        when(lookup.getJoin()).thenReturn("alias.col1 = alias.col2");
        when(source.getLookups()).thenReturn(singletonList(lookup));
        DataStore dataStore = mock(DataStore.class);
        when(dataStore.getDataStoreName()).thenReturn("lookup");
        when(dataStore.getColumns()).thenReturn(new HashMap<>());
        when(dataStore.isTemporary()).thenReturn(true);
        when(metadataService.getSourceDataStoreInModel("lookup", "model")).thenReturn(dataStore);
        fixture.validateJoinEnriched(lookup);
    }

    @Test
    @Expected(warnings = {31232})
    public void testValidate_31232_joinTypeMismatch() {

        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        Source source = InputModelMockHelper.createMockETLSource("alias1", "source1", "model");
        Dataset dataset = mock(Dataset.class);
        when(dataset.getSources()).thenReturn(singletonList(source));
        when(source.getParent()).thenReturn(dataset);
        when(dataset.getParent()).thenReturn(transformation);
        when(transformation.getDatasets()).thenReturn(singletonList(dataset));

        Lookup lookup = mock(Lookup.class);
        when(lookup.getLookupDataStore()).thenReturn("lookup");
        when(lookup.getAlias()).thenReturn("alias");
        when(lookup.getModel()).thenReturn("model");
        when(lookup.getParent()).thenReturn(source);
        when(lookup.getJoin()).thenReturn("alias.col1 = alias.col2");
        when(source.getLookups()).thenReturn(singletonList(lookup));
        DataStore dataStore = mock(DataStore.class);
        when(dataStore.getDataStoreName()).thenReturn("lookup");
        DataStoreColumn col1 = mock(DataStoreColumn.class);
        when(col1.getName()).thenReturn("col1");
        when(col1.getColumnDataType()).thenReturn("VARCHAR");
        DataStoreColumn col2 = mock(DataStoreColumn.class);
        when(col2.getName()).thenReturn("col2");
        when(col2.getColumnDataType()).thenReturn("NUMERIC");
        HashMap<String, DataStoreColumn> map = new HashMap<>();
        map.put("col1", col1);
        map.put("col2", col2);
        when(dataStore.getColumns()).thenReturn(map);
        when(metadataService.getSourceDataStoreInModel("lookup", "model")).thenReturn(dataStore);
        fixture.validateJoinEnriched(lookup);
    }


    @Test
    @Expected(errors = {40121})
    public void testValidate_40121() {
        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        Mappings mappings = mock(Mappings.class);
        when(mappings.getParent()).thenReturn(transformation);
        when(mappings.getTargetDataStore()).thenReturn("TargetDataStore");
        DataStore ds = mock(DataStore.class);
        DataModel dm = mock(DataModel.class);
        when(ds.getDataModel()).thenReturn(dm);
        when(dm.getDataServerTechnology()).thenReturn("TargetTechnology");
        when(metadataService.getTargetDataStoreInModel(mappings)).thenReturn(ds);

        try {
            fixture.handleIKM(new NoKnowledgeModuleFoundException(""), mappings);
            // if here it failed
            fail();
        } catch (Exception e) {
            // no-op
        }
    }

    @Test
    @Expected(errors = {50000})
    public void testValidate_5000_IKM() {
        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        Mappings mappings = mock(Mappings.class);
        when(mappings.getParent()).thenReturn(transformation);
        when(mappings.getTargetDataStore()).thenReturn("TargetDataStore");
        try {
            fixture.handleIKM(new KnowledgeModulePropertiesException(singletonList("bad rule")), mappings);
            // if here it failed
            fail();
        } catch (Exception e) {
            // no-op
        }
    }

    @Test
    @Expected(errors = {40120})
    public void testValidate_40120() {
        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        Mappings mappings = mock(Mappings.class);
        when(mappings.getParent()).thenReturn(transformation);
        when(mappings.getTargetDataStore()).thenReturn("TargetDataStore");
        try {
            fixture.handleIKM(new RuntimeException(""), mappings);
            // if here it failed
            fail();
        } catch (Exception e) {
            // no-op
        }
    }

    @Test
    @Expected(errors = {40221})
    public void testValidate_40221() {
        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        Mappings mappings = mock(Mappings.class);
        when(mappings.getParent()).thenReturn(transformation);
        when(mappings.getTargetDataStore()).thenReturn("TargetDataStore");
        DataStore ds = mock(DataStore.class);
        DataModel dm = mock(DataModel.class);
        when(ds.getDataModel()).thenReturn(dm);
        when(dm.getDataServerTechnology()).thenReturn("TargetTechnology");
        when(metadataService.getTargetDataStoreInModel(mappings)).thenReturn(ds);

        try {
            fixture.handleCKM(new NoKnowledgeModuleFoundException(""), mappings);
            // if here it failed
            fail();
        } catch (Exception e) {
            // no-op
        }
    }

    @Test
    @Expected(errors = {50000})
    public void testValidate_50000_CKM() {
        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        Mappings mappings = mock(Mappings.class);
        when(mappings.getParent()).thenReturn(transformation);
        when(mappings.getTargetDataStore()).thenReturn("TargetDataStore");
        try {
            fixture.handleIKM(new KnowledgeModulePropertiesException(singletonList("bad rule")), mappings);
            // if here it failed
            fail();
        } catch (Exception e) {
            // no-op
        }
    }

    @Test
    @Expected(errors = {40220})
    public void testValidate_40220() {
        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        Mappings mappings = mock(Mappings.class);
        when(mappings.getParent()).thenReturn(transformation);
        when(mappings.getTargetDataStore()).thenReturn("TargetDataStore");
        try {
            fixture.handleCKM(new RuntimeException(""), mappings);
            // if here it failed
            fail();
        } catch (Exception e) {
            // no-op
        }
    }


    @Test
    @Expected(errors = {70010}, packageSequences = {ErrorWarningMessageJodiImpl.PackageSequenceGlobal})
    public void testValidate_70010() {
        KnowledgeModule km = mockKnowledgeModule("KM");
        when(etlSubsystemService.getKMs()).thenReturn(singletonList(km));
        HashMap<String, String> options = new HashMap<>();
        String modelCode = "MODELCODE";
        String jkm = "JKM";
        fixture.validateJournalizingOptions(modelCode, jkm, options);
    }

    @Test
    @Expected(errors = {70011}, packageSequences = {ErrorWarningMessageJodiImpl.PackageSequenceGlobal})
    public void testValidate_70011() {
        KnowledgeModule km = mockKnowledgeModule("JKM");
        when(etlSubsystemService.getKMs()).thenReturn(singletonList(km));
        HashMap<String, String> options = new HashMap<>();
        options.put("OPTION_1", "false");
        String modelCode = "MODELCODE";
        String jkm = "JKM";
        fixture.validateJournalizingOptions(modelCode, jkm, options);
    }

    @Test
    @Expected(errors = {70012}, packageSequences = {ErrorWarningMessageJodiImpl.PackageSequenceGlobal})
    public void testValidate_70012() {
        KnowledgeModule km = mockKnowledgeModule("JKM");
        when(etlSubsystemService.getKMs()).thenReturn(singletonList(km));
        HashMap<String, String> options = new HashMap<>();
        options.put("CHECKBOX", "2");
        String modelCode = "MODELCODE";
        String jkm = "JKM";
        fixture.validateJournalizingOptions(modelCode, jkm, options);
    }

    @Test
    @Expected(packageSequences = {ErrorWarningMessageJodiImpl.PackageSequenceGlobal})
    public void testValidate_70013() {
        KnowledgeModule km = mockKnowledgeModule("KM");
        when(etlSubsystemService.getKMs()).thenReturn(singletonList(km));
        HashMap<String, String> options = new HashMap<>();
        String modelCode = "MODELCODE";
        String jkm = "JKM";
        JournalizingStrategy strategy = mock(JournalizingStrategy.class);
        fixture.validateJournalizingOptions(modelCode, jkm, options, strategy.getClass()
                                                                             .getCanonicalName());
    }

    @Test
    @Expected(errors = {70014}, packageSequences = {ErrorWarningMessageJodiImpl.PackageSequenceGlobal})
    public void testValidate_70014() {
        KnowledgeModule km = mockKnowledgeModule("JKM");
        when(etlSubsystemService.getKMs()).thenReturn(singletonList(km));
        HashMap<String, String> options = new HashMap<>();
        options.put("OPTION_1", "false");
        String modelCode = "MODELCODE";
        String jkm = "JKM";
        JournalizingStrategy strategy = mock(JournalizingStrategy.class);
        fixture.validateJournalizingOptions(modelCode, jkm, options, strategy.getClass()
                                                                             .getCanonicalName());
    }

    @Test
    @Expected(errors = {70015}, packageSequences = {ErrorWarningMessageJodiImpl.PackageSequenceGlobal})
    public void testValidate_70015() {
        KnowledgeModule km = mockKnowledgeModule("JKM");
        when(etlSubsystemService.getKMs()).thenReturn(singletonList(km));
        HashMap<String, String> options = new HashMap<>();
        options.put("CHECKBOX", "2");
        String modelCode = "MODELCODE";
        String jkm = "JKM";
        JournalizingStrategy strategy = mock(JournalizingStrategy.class);
        fixture.validateJournalizingOptions(modelCode, jkm, options, strategy.getClass()
                                                                             .getCanonicalName());
    }

    @Test
    @Expected(errors = {70010, 70020}, packageSequences = {ErrorWarningMessageJodiImpl.PackageSequenceGlobal})
    public void testValidate_70020() {
        when(properties.hasDeprecateCDCProperty()).thenReturn(true);
        HashMap<String, String> options = new HashMap<>();
        fixture.validateJournalizingOptions("MODELCODE", "JKM", options);
    }


/*
	30140 = "Name for Pivot %s in Source %s in Dataset[%s] uses name used by either source or another Flow item.";
	30141 = "Row locator for Pivot %s in Source %s in Dataset[%s] refers to unknown alias %s.";
	30142 = "Row locator for Pivot %s in Source %s in Dataset[%s] refers to unknown column %s.";
	30143 = "Row locator for Pivot %s in Source %s in Dataset[%s] refers to source/lookup by name %s; please use alias instead.";
	30144 = "Output Attribute expression for Pivot %s in Source %s in Dataset[%s] refers to unknown component %s.";
	30145 = "Output attribute expression for Pivot %s in Source %s in Dataset[%s] refers to unknown column %s. ";
	30146 = "Output attribute expression for Pivot %s in Source %s in Dataset[%s] refers to unknown project variable %s.";
*/


    @Test
    @Expected(errors = {30140})
    public void testValidate_30140() {
        //buildPivot(String name, String rowLocator, String[] attributeNames, String[] attributeValues, String[] attributeExpressions)
        Pivot pivot =
                buildPivotFollowingSource("alias1", "source1.c1", new String[]{"PivotCol"}, new String[]{"PivotValue"},
                                          new String[]{"source1.unknown"});
        fixture.validateFlow(pivot);
    }

    @Test
    @Expected(errors = {30140})
    public void testValidate_30140_sibling() {
        Pivot pivot1 = buildPivotFollowingSource("OurPivot", "source1.c1", new String[]{"PivotCol"},
                                                 new String[]{"PivotValue"}, new String[]{"source1.unknown"});
        Pivot pivot2 = InputModelMockHelper.createMockPivot("OurPivot", "source.c1", "SUM", new String[]{"PivotCol"},
                                                            new String[]{"PivotValue"},
                                                            new String[]{"source1.unknown"});
        Source source1 = pivot1.getParent();
        when(pivot2.getParent()).thenReturn(source1);
        pivot1.getParent()
              .getFlows()
              .add(pivot2);
        fixture.validateFlow(pivot1);
    }


    @Test
    @Expected(errors = {30141})
    public void testValidate_30141() {
        Pivot pivot = buildPivotFollowingSource("MyPivot", "'unknown.unknown'", new String[]{"PivotCol"},
                                                new String[]{"PivotValue"}, new String[]{"source1.unknown"});
        fixture.validateFlow(pivot);
    }

    @Test
    @Expected(errors = {30141})
    public void testValidate_30141_flows() {
        Pivot pivot =
                buildPivotFollowingFlow("Pivot2", "'UNKNOWN.C1'", new String[]{"PivotCol"}, new String[]{"PivotValue"},
                                        new String[]{"source1.unknown"});
        fixture.validateFlow(pivot);
    }


    @Test
    @Expected(errors = {30142})
    public void testValidate_30142() {
        //            buildPivot(String name, String rowLocator, String[] attributeNames, String[] attributeValues, String[] attributeExpressions)
        Pivot pivot = buildPivotFollowingSource("MyPivot", "alias1.UNKNOWN", new String[]{"PivotCol"},
                                                new String[]{"PivotValue"}, new String[]{"source1.unknown"});
        DataStore targetDataStore =
                createMockDataStore(new String[]{"COL_1"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{11});
        when(metadataService.getSourceDataStoreInModel(pivot.getParent()
                                                            .getName(), pivot.getParent()
                                                                             .getModel())).thenReturn(targetDataStore);
        fixture.validateFlow(pivot);
    }

    @Test
    @Expected(errors = {30142})
    public void testValidate_30142_flows() {
        //            buildPivot(String name, String rowLocator, String[] attributeNames, String[] attributeValues, String[] attributeExpressions)
        Pivot pivot = buildPivotFollowingFlow("Pivot2", "Pivot1.UNKNOWN", new String[]{"PivotCol"},
                                              new String[]{"PivotValue"}, new String[]{"source1.COL_1"});
        DataStore targetDataStore =
                createMockDataStore(new String[]{"COL_1"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{11});
        when(metadataService.getSourceDataStoreInModel(pivot.getParent()
                                                            .getName(), pivot.getParent()
                                                                             .getModel())).thenReturn(targetDataStore);
        fixture.validateFlow(pivot);
    }


    @Test
    @Expected(errors = {30143})
    public void testValidate_30143() {
        Pivot pivot =
                buildPivotFollowingSource("MyPivot", "'name1.c1'", new String[]{"PivotCol"}, new String[]{"PivotValue"},
                                          new String[]{"source1.unknown"});
        fixture.validateFlow(pivot);
    }

    @Test
    @Expected(errors = {30144})
    public void testValidate_30144() {
        Pivot pivot = buildPivotFollowingSource("MyPivot", "a.c1", new String[]{"PivotCol"}, new String[]{"PivotValue"},
                                                new String[]{"UnknownSource.col"});
        fixture.validateFlow(pivot);
    }

    @Test //Unknown Column
    @Expected(errors = {30142})
    public void testValidate_30145() {
        Pivot pivot =
                buildPivotFollowingSource("MyPivot", "alias1.c1", new String[]{"PivotCol"}, new String[]{"PivotValue"},
                                          new String[]{"alias1.UnknownColumn"});
        DataStore targetDataStore =
                createMockDataStore(new String[]{"COL_1"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{11});
        when(metadataService.getSourceDataStoreInModel(pivot.getParent()
                                                            .getName(), pivot.getParent()
                                                                             .getModel())).thenReturn(targetDataStore);

        fixture.validateFlow(pivot);
    }

    @Test
    @Expected(errors = {30146})
    public void testValidate_30146() {
        when(packageService.projectVariableExists("project", "variable")).thenReturn(false);
        Pivot pivot =
                buildPivotFollowingSource("MyPivot", "source.c1", new String[]{"PivotCol"}, new String[]{"PivotValue"},
                                          new String[]{"#project.variable"});
        fixture.validateFlow(pivot);
    }

    @Test //Unknown Column
    @Expected(errors = {30147})
    public void testValidate_30147() {
        Pivot pivot =
                buildPivotFollowingSource("MyPivot", "a1.c1", new String[]{"PivotCol"}, new String[]{"PivotValue"},
                                          new String[]{"name1.col1"});
        DataStore targetDataStore =
                createMockDataStore(new String[]{"COL_1"}, new String[]{"VARCHAR"}, new int[]{11}, new int[]{11});
        when(metadataService.getSourceDataStoreInModel(pivot.getParent()
                                                            .getName(), pivot.getParent()
                                                                             .getModel())).thenReturn(targetDataStore);

        fixture.validateFlow(pivot);
    }


    @Test
    @Expected(errors = {30148})
    public void testValidate_30148_duplicated_outputattributes() {
        //buildPivot(String name, String rowLocator, String[] attributeNames, String[] attributeValues, String[] attributeExpressions)
        Pivot pivot = buildPivotFollowingSource("alias1", "source1.c1", new String[]{"PivotCol", "PivotCol"},
                                                new String[]{"PivotValue", "PivotValue"},
                                                new String[]{"source1.unknown", "source.unknown"});
        fixture.validateFlow(pivot);
    }


    @Test
    @Expected()
    public void testValidate_30142_IgnoredOutDotPrint() {
        when(metadataService.projectVariableExists("project", "variable")).thenReturn(false);
        buildPivotFollowingSource("MyPivot", "source.c1", new String[]{"PivotCol"}, new String[]{"PivotValue"},
                                  new String[]{"out.print"});
    }


    @Test
    @Expected()
    public void testValidate_30142_IgnoredOdiRef() {
        buildPivotFollowingSource("MyPivot", "source.c1", new String[]{"PivotCol"}, new String[]{"PivotValue"},
                                  new String[]{"odiref.unknown_but_ignore"});

    }

    @Test
    @Expected()
    public void testValidate_subquery_perfect() {
        SubQuery subquery =
                buildSubQueryFollowingSource("SQRY", "F", GroupComparisonEnum.NONE, RoleEnum.GREATER, "alias1.c1=F.c01",
                                             new String[]{"c01", "c02"}, new String[]{"c01", "c02"},
                                             new String[]{"alias1.c1", "alias1.c2"});
        DataStore sourceDS =
                this.createMockDataStore(new String[]{"c1", "c2"}, new String[]{"VARCHAR", "VARCHAR"}, new int[]{0, 0},
                                         new int[]{0, 0});

        Source source = subquery.getParent();
        when(metadataService.getSourceDataStoreInModel(source.getName(), source.getModel())).thenReturn(sourceDS);
        fixture.validateFlow(subquery);

    }

    @Test
    @Expected(errors = {30151})
    public void testValidate_30151() {
        SubQuery subquery = buildSubQueryFollowingSource("SQRY", "F", GroupComparisonEnum.NONE, RoleEnum.GREATER,
                                                         "UNKNOWN.c1=F.c01", new String[]{"c01", "c02"},
                                                         new String[]{"c01", "c02"},
                                                         new String[]{"alias1.c1", "alias1.c2"});
        DataStore sourceDS =
                this.createMockDataStore(new String[]{"c1", "c2"}, new String[]{"VARCHAR", "VARCHAR"}, new int[]{0, 0},
                                         new int[]{0, 0});

        Source source = subquery.getParent();
        when(metadataService.getSourceDataStoreInModel(source.getName(), source.getModel())).thenReturn(sourceDS);
        fixture.validateFlow(subquery);
    }

    @Test
    @Expected(errors = {30152})
    public void testValidate_30152_source() {
        SubQuery subquery = buildSubQueryFollowingSource("SQRY", "F", GroupComparisonEnum.NONE, RoleEnum.GREATER,
                                                         "alias1.UNKNOWN=F.UNK", new String[]{"c01", "c02"},
                                                         new String[]{"c01", "c02"},
                                                         new String[]{"alias1.c1", "alias1.c2"});
        DataStore sourceDS =
                createMockDataStore(new String[]{"c1", "c2"}, new String[]{"VARCHAR", "VARCHAR"}, new int[]{0, 0},
                                    new int[]{0, 0});

        Source source = subquery.getParent();
        when(metadataService.getSourceDataStoreInModel(source.getName(), source.getModel())).thenReturn(sourceDS);
        DataStore filterDS =
                createMockDataStore(new String[]{"c3", "c4"}, new String[]{"VARCHAR", "VARCHAR"}, new int[]{0, 0},
                                    new int[]{0, 0});
        when(metadataService.getSourceDataStoreInModel(subquery.getFilterSource(),
                                                       subquery.getFilterSourceModel())).thenReturn(filterDS);
        fixture.validateFlow(subquery);
    }

    @Test
    @Expected(errors = {30152})
    public void testValidate_30152_filter() {
        SubQuery subquery =
                buildSubQueryFollowingSource("SQRY", "F", GroupComparisonEnum.NONE, RoleEnum.GREATER, "alias1.c1=F.UNK",
                                             new String[]{"c01", "c02"}, new String[]{"c01", "c02"},
                                             new String[]{"alias1.c1", "alias1.c2"});
        DataStore sourceDS =
                createMockDataStore(new String[]{"c1", "c2"}, new String[]{"VARCHAR", "VARCHAR"}, new int[]{0, 0},
                                    new int[]{0, 0});

        Source source = subquery.getParent();
        when(metadataService.getSourceDataStoreInModel(source.getName(), source.getModel())).thenReturn(sourceDS);
        DataStore filterDS =
                createMockDataStore(new String[]{"c3", "c4"}, new String[]{"VARCHAR", "VARCHAR"}, new int[]{0, 0},
                                    new int[]{0, 0});
        when(metadataService.getSourceDataStoreInModel(subquery.getFilterSource(),
                                                       subquery.getFilterSourceModel())).thenReturn(filterDS);
        fixture.validateFlow(subquery);
    }

    @Test
    @Expected(errors = {30152})
    public void testValidate_30152_filter_afterpivot() {
        SubQuery subquery =
                buildSubQueryFollowingPivot("SQRY", "F", GroupComparisonEnum.NONE, RoleEnum.GREATER, "PVT.UNKOWN=F.c3",
                                            new String[]{"c01", "c02"}, new String[]{"c01", "c02"},
                                            new String[]{"SQRY.c1", "SQRY.c2"});
        DataStore sourceDS =
                createMockDataStore(new String[]{"c1", "c2"}, new String[]{"VARCHAR", "VARCHAR"}, new int[]{0, 0},
                                    new int[]{0, 0});

        Source source = subquery.getParent();
        when(metadataService.getSourceDataStoreInModel(source.getName(), source.getModel())).thenReturn(sourceDS);
        DataStore filterDS =
                createMockDataStore(new String[]{"c3", "c4"}, new String[]{"VARCHAR", "VARCHAR"}, new int[]{0, 0},
                                    new int[]{0, 0});
        when(metadataService.getSourceDataStoreInModel(subquery.getFilterSource(),
                                                       subquery.getFilterSourceModel())).thenReturn(filterDS);
        fixture.validateFlow(subquery);
    }


    @Test
    @Expected(errors = {30153})
    public void testValidate_30153() {
        SubQuery subquery =
                buildSubQueryFollowingSource("SQRY", "F", GroupComparisonEnum.NONE, RoleEnum.GREATER, "name1.c1=F.c01",
                                             new String[]{"c01", "c02"}, new String[]{"c01", "c02"},
                                             new String[]{"alias1.c1", "alias1.c2"});
        DataStore sourceDS =
                createMockDataStore(new String[]{"c1", "c2"}, new String[]{"VARCHAR", "VARCHAR"}, new int[]{0, 0},
                                    new int[]{0, 0});

        Source source = subquery.getParent();
        when(metadataService.getSourceDataStoreInModel(source.getName(), source.getModel())).thenReturn(sourceDS);
        fixture.validateFlow(subquery);

    }


    @Test
    @Expected(errors = {30154})
    public void testValidate_30154() {
        SubQuery subquery = buildSubQueryFollowingSource("SQRY", "F", GroupComparisonEnum.NONE, RoleEnum.GREATER,
                                                         "alias1.c1=alias1.c1", new String[]{"c01", "c02"},
                                                         new String[]{"c01", "c02"},
                                                         new String[]{"alias1.c1", "alias1.c2"});
        DataStore sourceDS =
                this.createMockDataStore(new String[]{"c1", "c2"}, new String[]{"VARCHAR", "VARCHAR"}, new int[]{0, 0},
                                         new int[]{0, 0});

        Source source = subquery.getParent();
        when(metadataService.getSourceDataStoreInModel(source.getName(), source.getModel())).thenReturn(sourceDS);
        fixture.validateFlow(subquery);
    }


    private Pivot buildPivotFollowingSource(String name, String rowLocator, String[] attributeNames,
                                            String[] attributeValues, String[] attributeExpressions) {
        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        when(transformation.getName()).thenReturn("TRANSFORMATION NAME");
        Source source1 = InputModelMockHelper.createMockETLSource("alias1", "name1", "model");
        Source source2 = InputModelMockHelper.createMockETLSource("alias2", "name2", "model");
        ArrayList<Source> sources = new ArrayList<>();
        sources.add(source1);
        sources.add(source2);
        Dataset dataset = mock(Dataset.class);
        when(dataset.getSources()).thenReturn(sources);
        when(source1.getParent()).thenReturn(dataset);
        when(source2.getParent()).thenReturn(dataset);
        when(dataset.getParent()).thenReturn(transformation);
        ArrayList<Dataset> datasets = new ArrayList<>();
        when(transformation.getDatasets()).thenReturn(datasets);
        datasets.add(dataset);
        when(dataset.getParent()).thenReturn(transformation);

        Pivot pivot = InputModelMockHelper.createMockPivot(name, rowLocator, "SUM", attributeNames, attributeValues,
                                                           attributeExpressions);
        when(pivot.getParent()).thenReturn(source1);
        ArrayList<Flow> flows = new ArrayList<>();
        flows.add(pivot);
        when(source1.getFlows()).thenReturn(flows);

        return pivot;
    }

    private Pivot buildPivotFollowingFlow(String name, String rowLocator, String[] attributeNames,
                                          String[] attributeValues, String[] attributeExpressions) {
        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        when(transformation.getName()).thenReturn("TRANSFORMATION NAME");
        Source source1 = InputModelMockHelper.createMockETLSource("alias1", "name1", "model");
        ArrayList<Source> sources = new ArrayList<>();
        sources.add(source1);
        Dataset dataset = mock(Dataset.class);
        when(dataset.getSources()).thenReturn(sources);
        when(source1.getParent()).thenReturn(dataset);
        when(dataset.getParent()).thenReturn(transformation);
        ArrayList<Dataset> datasets = new ArrayList<>();
        when(transformation.getDatasets()).thenReturn(datasets);
        datasets.add(dataset);
        when(dataset.getParent()).thenReturn(transformation);

        Pivot previousPivot =
                InputModelMockHelper.createMockPivot("Pivot1", "alias1.COL_1", "SUM", new String[]{"C1", "C2"},
                                                     new String[]{"", ""}, new String[]{"", ""});
        Pivot pivot = InputModelMockHelper.createMockPivot(name, rowLocator, "SUM", attributeNames, attributeValues,
                                                           attributeExpressions);
        when(pivot.getParent()).thenReturn(source1);
        when(previousPivot.getParent()).thenReturn(source1);
        ArrayList<Flow> flows = new ArrayList<>();
        flows.add(previousPivot);
        flows.add(pivot);
        when(source1.getFlows()).thenReturn(flows);

        return pivot;
    }

    private SubQuery buildSubQueryFollowingSource(String name, String filterSource, GroupComparisonEnum gc,
                                                  RoleEnum role, String condition, String[] attributeNames,
                                                  String[] attributeValues, String[] attributeExpressions) {
        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        when(transformation.getName()).thenReturn("TRANSFORMATION NAME");
        Source source1 = InputModelMockHelper.createMockETLSource("alias1", "name1", "model");
        Source source2 = InputModelMockHelper.createMockETLSource("alias2", "name2", "model");
        ArrayList<Source> sources = new ArrayList<>();
        sources.add(source1);
        sources.add(source2);
        Dataset dataset = mock(Dataset.class);
        when(dataset.getSources()).thenReturn(sources);
        when(source1.getParent()).thenReturn(dataset);
        when(source2.getParent()).thenReturn(dataset);
        when(dataset.getParent()).thenReturn(transformation);
        ArrayList<Dataset> datasets = new ArrayList<>();
        when(transformation.getDatasets()).thenReturn(datasets);
        datasets.add(dataset);
        when(dataset.getParent()).thenReturn(transformation);

        SubQuery sq = InputModelMockHelper.createMockSubQuery(name, filterSource, gc, role, condition, attributeNames,
                                                              attributeValues, attributeExpressions);
        when(sq.getParent()).thenReturn(source1);
        ArrayList<Flow> flows = new ArrayList<>();
        flows.add(sq);
        when(source1.getFlows()).thenReturn(flows);

        return sq;
    }

    private SubQuery buildSubQueryFollowingPivot(String name, String filterSource, GroupComparisonEnum gc,
                                                 RoleEnum role, String condition, String[] attributeNames,
                                                 String[] attributeValues, String[] attributeExpressions) {
        Transformation transformation = mock(Transformation.class);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        when(transformation.getName()).thenReturn("TRANSFORMATION NAME");
        Source source1 = InputModelMockHelper.createMockETLSource("alias1", "name1", "model");
        Source source2 = InputModelMockHelper.createMockETLSource("alias2", "name2", "model");
        ArrayList<Source> sources = new ArrayList<>();
        sources.add(source1);
        sources.add(source2);
        Dataset dataset = mock(Dataset.class);
        when(dataset.getSources()).thenReturn(sources);
        when(source1.getParent()).thenReturn(dataset);
        when(source2.getParent()).thenReturn(dataset);
        when(dataset.getParent()).thenReturn(transformation);
        ArrayList<Dataset> datasets = new ArrayList<>();
        when(transformation.getDatasets()).thenReturn(datasets);
        datasets.add(dataset);
        when(dataset.getParent()).thenReturn(transformation);

        Pivot pivot = InputModelMockHelper.createMockPivot("PVT", "alias1.COL_1", "SUM", new String[]{"c5", "c6"},
                                                           new String[]{"", ""}, new String[]{"", ""});
        when(pivot.getParent()).thenReturn(source1);
        SubQuery sq = InputModelMockHelper.createMockSubQuery(name, filterSource, gc, role, condition, attributeNames,
                                                              attributeValues, attributeExpressions);
        when(sq.getParent()).thenReturn(source1);
        ArrayList<Flow> flows = new ArrayList<>();
        flows.add(pivot);
        flows.add(sq);

        when(source1.getFlows()).thenReturn(flows);

        return sq;
    }

}


