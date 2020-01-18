package one.jodi.core.datastore.impl;

import one.jodi.InputModelMockHelper;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import one.jodi.base.model.MockDatastoreHelper;
import one.jodi.base.model.types.DataStore;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.config.PropertyValueHolder;
import one.jodi.core.config.modelproperties.ModelProperties;
import one.jodi.core.config.modelproperties.ModelPropertiesImpl;
import one.jodi.core.datastore.ModelCodeContext;
import one.jodi.core.extensions.contexts.ModelNameExecutionContext;
import one.jodi.core.extensions.strategies.AmbiguousModelException;
import one.jodi.core.extensions.strategies.IncorrectCustomStrategyException;
import one.jodi.core.extensions.strategies.ModelCodeStrategy;
import one.jodi.core.extensions.strategies.NoModelFoundException;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.core.validation.etl.ETLValidator;
import one.jodi.etl.internalmodel.impl.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * <p>
 * The ETL/internal model unit tests have been added, all postfixed by .  Cleanup should remove
 * unit tests without postfix.
 */
@RunWith(JUnit4.class)
public class ModelCodeContextImplTest {

    private static String STAR_PREFIX = "W_";
    private static String OTHER_PREFIX = "O_";
    private static String DATA_STORE_ALIAS = "ALIAS";
    private static String DATA_STORE_NO_POLICY = "SOURCE_TABLE";
    private static String DATA_STORE_NO_POLICY_TMP = DATA_STORE_NO_POLICY + "_S01";
    private static String DATA_STORE_MATCH_POLICY = STAR_PREFIX + DATA_STORE_NO_POLICY;
    private static String DATA_STORE_MATCH_POLICY_TMP = DATA_STORE_MATCH_POLICY + "_S01";
    private static String DATA_STORE_OTHER_POLICY = OTHER_PREFIX + DATA_STORE_NO_POLICY;

    private static String MODEL_EDW_CODE = "STAR_MODEL";
    private static String SOURCE_MODEL = "SOURCE";
    private static String STAR_MODEL_CODE = "model.dm.code";
    private static String STAR_MODEL = "STAR";
    private static String OTHER_MODEL = "EDW";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    // Specification model
    TransformationImpl transformation = new TransformationImpl();
    DatasetImpl dataset = new DatasetImpl();
    SourceImpl source = new SourceImpl();
    LookupImpl lookup = new LookupImpl();
    MappingsImpl mappings = new MappingsImpl();

    @Mock
    ETLValidator validator;
    // Infrastructure
    @Mock
    JodiProperties mockProperties;
    @Mock
    DatabaseMetadataService commonContextBuilder;
    @Mock
    ModelCodeStrategy failingCustomStrategy;
    Map<String, String> mockProps = new HashMap<String, String>();
    List<ModelProperties> mpList = new ArrayList<ModelProperties>();
    ModelCodeContext fixture;
    private ErrorWarningMessageJodi errorWarningMessages =
            ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mockProps.put(STAR_MODEL_CODE, STAR_MODEL);
        mockProps.put("model.dm.prefix", "W_");
        mockProps.put("model.source.code", SOURCE_MODEL);
        mockProps.put(MODEL_EDW_CODE, OTHER_MODEL);

        Map<String, PropertyValueHolder> coreProperties = new HashMap<String, PropertyValueHolder>();
        // define properties file - identical to the mockProp Map
        for (String key : mockProps.keySet()) {
            when(mockProperties.getProperty(key)).thenReturn(mockProps.get(key));
            coreProperties.put(key, InputModelMockHelper.createMockPropertyValueHolder(key, mockProps.get(key)));
        }


        DatasetImpl dataset = new DatasetImpl();
        transformation.addDataset(dataset);
        dataset.setParent(transformation);
        dataset.addSource(source);
        source.setParent(dataset);
        source.setAlias(DATA_STORE_ALIAS);
        source.addLookup(lookup);
        lookup.setParent(source);
        lookup.setAlias(DATA_STORE_ALIAS);

        transformation.setMappings(mappings);
        mappings.setParent(transformation);

        when(commonContextBuilder.getCoreProperties()).thenReturn(coreProperties);

        // setup Model Properties
        ModelPropertiesImpl mp;
        mp = new ModelPropertiesImpl();
        mp.setModelID("source");
        mp.setCode(SOURCE_MODEL);
        mp.setDefault(true);
        mp.setOrder(10);
        mp.setLayer("source");
        mp.setPrefix(Collections.<String>emptyList());
        mp.setPostfix(Collections.<String>emptyList());
        mpList.add(mp);

        mp = new ModelPropertiesImpl();
        mp.setModelID("edw");
        mp.setCode(OTHER_MODEL);
        mp.setDefault(false);
        mp.setOrder(20);
        mp.setLayer("edw");
        mp.setPrefix(Arrays.asList(new String[]{OTHER_PREFIX}));
        mp.setPostfix(Collections.<String>emptyList());
        mpList.add(mp);

        mp = new ModelPropertiesImpl();
        mp.setModelID("dm");
        mp.setCode(STAR_MODEL);
        mp.setDefault(false);
        mp.setOrder(30);
        mp.setLayer("star");
        mp.setPrefix(Arrays.asList(new String[]{STAR_PREFIX}));
        mp.setPostfix(Collections.<String>emptyList());
        mpList.add(mp);

        when(commonContextBuilder.getConfiguredModels()).thenReturn(mpList);

        fixture = new ModelCodeContextImpl(mockProperties, commonContextBuilder,
                new ModelCodeDefaultStrategy(errorWarningMessages),
                new ModelCodeIDStrategy(errorWarningMessages),
                validator, errorWarningMessages);
    }

    //
    // SHARED Parameterized Setup Logic
    //

    private void setupBaseConfig(final String expectedDataStore,
                                 final String[] providedModels, boolean[] isIgnoredInHeuristics,
                                 boolean isTempTable) {
        List<DataStore> descriptors = getCandidateDataStoreDescriptors(
                expectedDataStore, providedModels, isIgnoredInHeuristics);

        when(commonContextBuilder.isTemporaryTransformation(eq(expectedDataStore)))
                .thenReturn(isTempTable);
        when(commonContextBuilder.findDataStoreInAllModels(eq(expectedDataStore)))
                .thenReturn(descriptors);

        source.setName(expectedDataStore);
        lookup.setLookupDatastore(expectedDataStore);
        mappings.setTargetDataStore(expectedDataStore);
    }

    private void setupBaseConfig(final String expectedDataStore,
                                 final String[] providedModels, boolean isTempTable) {

        boolean[] isIgnoredInHeuristics = new boolean[providedModels.length];
        setupBaseConfig(expectedDataStore, providedModels, isIgnoredInHeuristics, isTempTable);

    }


    @Test
    public void getModelName_OneDataStoreSource_MatchingPolicy() {

        setupBaseConfig(DATA_STORE_MATCH_POLICY, new String[]{STAR_MODEL}, false);
        String selectedModelName = fixture.getModelCode(source);

        verify(commonContextBuilder).findDataStoreInAllModels(eq(DATA_STORE_MATCH_POLICY));
        verify(commonContextBuilder, never()).isTemporaryTransformation(DATA_STORE_MATCH_POLICY);
        verify(commonContextBuilder, never()).getConfiguredModels();
        assertEquals(STAR_MODEL, selectedModelName);
    }

    /*
     * None of the hard-coded policies based on naming conventions apply.
     * In this case the data store must be determine if the data source name
     * exists in one of the models.
     */
    @Test
    public void getModelName_OneDataStoreSource_NoMatchingPolicyInModel() {

        setupBaseConfig(DATA_STORE_NO_POLICY, new String[]{OTHER_MODEL}, false);
        String selectedModelName = fixture.getModelCode(source);

        assertEquals(OTHER_MODEL, selectedModelName);
    }

    /*
     * Policies determine a model for a data store. However, the data store does
     * not exist in the initially determined model but another one.
     */
    @Test
    public void getModelName_OneDataStoreSource_MatchingPolicyNotInModel() {

        setupBaseConfig(DATA_STORE_MATCH_POLICY, new String[]{OTHER_MODEL}, false);
        String selectedModelName = fixture.getModelCode(source);

        assertEquals(OTHER_MODEL, selectedModelName);
    }

    //
    // Failure scenarios with no models found
    //

    /*
     * The hard-coded policies determine a model for a data store. The data
     * store does not exist in any model. Will throw an exception.
     */
    @Test
    public void getModelName_NoDataStoreSource_NotFound() {

        setupBaseConfig(DATA_STORE_OTHER_POLICY, new String[]{}, false);

        thrown.expect(NoModelFoundException.class);
        thrown.expectMessage("Unable to determine a model for data store '" + DATA_STORE_OTHER_POLICY);

        fixture.getModelCode(source);
    }


    //
    // Temporary table test cases
    //

    /*
     * Apply policy directly to temporary table without checking their existence
     * in the model. In this test case a model is identified based on the defined
     * conventions.
     */
    @Test
    public void getModelName_TempTable_MatchingPolicy() {

        setupBaseConfig(DATA_STORE_MATCH_POLICY_TMP, new String[]{}, true);

        String selectedModelName = fixture.getModelCode(source);

        verify(commonContextBuilder).findDataStoreInAllModels(eq(DATA_STORE_MATCH_POLICY_TMP));
        verify(commonContextBuilder).isTemporaryTransformation(DATA_STORE_MATCH_POLICY_TMP);
        verify(commonContextBuilder).getConfiguredModels();

        assertEquals(STAR_MODEL, selectedModelName);

    }

    /*
     * None of the defined policies applies to the temporary table name. As a
     * consequence the configured default model is returned.
     */
    @Test
    public void getModelName_TempTable_FallbackPolicy() {

        setupBaseConfig(DATA_STORE_NO_POLICY_TMP, new String[]{}, true);

        String selectedModelName = fixture.getModelCode(source);

        verify(commonContextBuilder).findDataStoreInAllModels(eq(DATA_STORE_NO_POLICY_TMP));
        verify(commonContextBuilder).isTemporaryTransformation(DATA_STORE_NO_POLICY_TMP);
        verify(commonContextBuilder, times(2)).getConfiguredModels();

        assertEquals(SOURCE_MODEL, selectedModelName);
    }


    /*
     * None of the defined policies applies to the temporary table name. No default
     * model is defined and therefore an ambiguous model exception is thrown.
     */
    @Test
    public void getModelName_TempTable_FallbackUndefined() {

        setupBaseConfig(DATA_STORE_NO_POLICY_TMP, new String[]{}, true);
        mpList.remove(0); // remove data model with default setting

        thrown.expect(AmbiguousModelException.class);
        thrown.expectMessage("Unable to determine a model for temporary table");

        fixture.getModelCode(source);
    }
    //
    // Scenarios with two or more matching models
    //

    /*
     * None of the default policies apply to the data store and the same data
     * store name exists in two data models.
     * Model is requested for target data store. In this case the model with the lower
     * order number is used.
     */
    @Test
    public void getModelName_TwoDataStoreSource_MatchHeuristicallyModelOrderAndDataStoreRoleTarget() {
        boolean[] isIgnoredInHeuristics = new boolean[2];
        isIgnoredInHeuristics[0] = true;
        setupBaseConfig(DATA_STORE_NO_POLICY, new String[]{SOURCE_MODEL, OTHER_MODEL},
                isIgnoredInHeuristics, false);

        String selectedModelName = fixture.getModelCode(mappings);

        verify(commonContextBuilder).findDataStoreInAllModels(eq(DATA_STORE_NO_POLICY));
        verify(commonContextBuilder, never()).isTemporaryTransformation(DATA_STORE_NO_POLICY);
        assertEquals(OTHER_MODEL, selectedModelName);
    }

    /*
     * None of the default policies apply to the data store and the same data
     * store name exists in three data models.
     * Exception is thrown.
     */
    @Test
    public void getModelName_ThreeDataStoreSource_Ambigous() {

        setupBaseConfig(DATA_STORE_NO_POLICY, new String[]{OTHER_MODEL, OTHER_MODEL, OTHER_MODEL}, false);

        thrown.expect(AmbiguousModelException.class);
        thrown.expectMessage("More than two potential models exist");

        fixture.getModelCode(source);
    }

    //
    // Manual Override Scenarios
    //


    @Test
    public void getModelName_Source_override() {

        setupBaseConfig(DATA_STORE_MATCH_POLICY, new String[]{STAR_MODEL, OTHER_MODEL}, false);
        source.setModel(MODEL_EDW_CODE);
        String selectedModelName = fixture.getModelCode(source);

        verify(commonContextBuilder).findDataStoreInAllModels(eq(DATA_STORE_MATCH_POLICY));
        verify(commonContextBuilder).isTemporaryTransformation(DATA_STORE_MATCH_POLICY);
        assertEquals(OTHER_MODEL, selectedModelName);
    }


    //
    // Custom Plugin Failure Scenarios
    //

    /*
     * Custom extension has a runtime exception that must be captured and
     * reported
     */
    @Test(expected = RuntimeException.class)
    public void getModelName_OneDataStoreSource_CustomExtensionFails() {

        setupBaseConfig(DATA_STORE_MATCH_POLICY, new String[]{STAR_MODEL}, false);

        // use custom extension that throws RuntimeException
        doThrow(new RuntimeException("Simulated Exception"))
                .when(failingCustomStrategy)
                .getModelCode(anyString(), any(ModelNameExecutionContext.class));
        ModelCodeContext fixture = new ModelCodeContextImpl(mockProperties,
                commonContextBuilder,
                new ModelCodeDefaultStrategy(errorWarningMessages),
                failingCustomStrategy,
                validator, errorWarningMessages);
        fixture.getModelCode(source);

    }


    /*
     * Custom extension returns empty string and triggers an exception
     */
    @Test
    public void getModelName_OneDataStoreSource_CustomExtensionReturnsNull() {

        setupBaseConfig(DATA_STORE_MATCH_POLICY, new String[]{STAR_MODEL}, false);

        // use custom extension that returns an empty String as a proposed model
        when(failingCustomStrategy.getModelCode(anyString(),
                any(ModelNameExecutionContext.class)))
                .thenReturn("");

        ModelCodeContext fixture = new ModelCodeContextImpl(mockProperties,
                commonContextBuilder,
                new ModelCodeDefaultStrategy(errorWarningMessages),
                failingCustomStrategy,
                validator, errorWarningMessages);

        thrown.expect(IncorrectCustomStrategyException.class);
        thrown.expectMessage("must return non-empty string");

        fixture.getModelCode(source);
    }


    //
    // Check that strategy for Lookup elements is handled as well
    //

    @Test
    public void getModelName_Lookup() {

        setupBaseConfig(DATA_STORE_MATCH_POLICY, new String[]{STAR_MODEL}, false);
        String selectedModelName = fixture.getModelCode(lookup);

        verify(commonContextBuilder).findDataStoreInAllModels(eq(DATA_STORE_MATCH_POLICY));
        verify(commonContextBuilder, never()).isTemporaryTransformation(DATA_STORE_MATCH_POLICY);
        assertEquals(STAR_MODEL, selectedModelName);
    }


    //
    // Check that strategy for Target elements is handled as well
    //


    /*
     * Execution Context contains one lookup data store that matches the
     * expectation of the policy-driven strategy. However, the model definition
     * explicitly overrides the strategy-derived model at the lookup.
     * Nevertheless, the data source is not available in the explicit model.
     */
    @Test
    public void getModelName_Mappings() {

        setupBaseConfig(DATA_STORE_MATCH_POLICY, new String[]{STAR_MODEL, OTHER_MODEL}, false);
        mappings.setModel(MODEL_EDW_CODE);

        String selectedModelName = fixture.getModelCode(mappings);

        verify(commonContextBuilder).findDataStoreInAllModels(eq(DATA_STORE_MATCH_POLICY));
        verify(commonContextBuilder).isTemporaryTransformation(DATA_STORE_MATCH_POLICY);
        assertEquals(OTHER_MODEL, selectedModelName);
    }

    //
    // Helper code to create mock objects
    //

    List<DataStore> getCandidateDataStoreDescriptors(String dataStoreName, String[] models, boolean[] isIgnoreInHeusristics) {

        List<DataStore> dsd = new ArrayList<DataStore>();
        for (int i = 0; i < models.length; i++) {
            dsd.add(MockDatastoreHelper.createMockDataStore(dataStoreName, models[i], isIgnoreInHeusristics[i]));
        }
        return dsd;
    }

}