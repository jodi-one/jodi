package one.jodi.core.executionlocation.impl;

import one.jodi.InputModelMockHelper;
import one.jodi.base.model.types.DataStore;
import one.jodi.core.config.JodiConstants;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.config.PropertyValueHolder;
import one.jodi.core.extensions.contexts.ExecutionLocationFilterExecutionContext;
import one.jodi.core.extensions.contexts.ExecutionLocationJoinExecutionContext;
import one.jodi.core.extensions.contexts.ExecutionLocationLookupExecutionContext;
import one.jodi.core.extensions.types.DataStoreWithAlias;
import one.jodi.core.extensions.types.ExecutionLocationType;
import one.jodi.etl.common.EtlSubSystemVersion;
import one.jodi.etl.internalmodel.JoinTypeEnum;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The class <code>ExecutionLocationDefaultStrategyTest</code> contains tests
 * for the class <code>{@link ExecutionLocationDefaultStrategy}</code>.
 *
 */
public class ExecutionLocationDefaultStrategyTest {
    @Mock
    JodiProperties properties;
    @Mock
    EtlSubSystemVersion etlSubSystemVersion;

    ExecutionLocationDefaultStrategy fixture;

    public static void main(String[] args) {
        new org.junit.runner.JUnitCore().run(ExecutionLocationDefaultStrategyTest.class);
    }

    /**
     * Perform pre-test initialization.
     *
     * @throws Exception if the initialization fails for some reason
     */
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        fixture = new ExecutionLocationDefaultStrategy(etlSubSystemVersion);
    }

    //Test TARGET COLUMN execution location

    /**
     * Perform post-test clean-up.
     *
     * @throws Exception if the clean-up fails for some reason
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetTargetColumnExecutionLocation_secondDot() throws Exception {
        testTargetColumnExecutionLocation(null, ExecutionLocationType.WORK, true, "col1", "ds1", "col1 SEQ", "table.col1", "schema.table.col1");
    }

    @Test
    public void testGetTargetColumnExecutionLocation_() throws Exception {
        testTargetColumnExecutionLocation(null, ExecutionLocationType.SOURCE, true, "col1", "ds1", "col1", "table.col1", "a.b");
    }

    @Test
    public void testGetTargetColumnExecutionLocation_alias() throws Exception {
        testTargetColumnExecutionLocation(null, ExecutionLocationType.WORK, true, "col1", "ds1", "col1", "col1.bar", "a.col1 || b.col1");
    }

    @Test
    public void testGetTargetColumnExecutionLocation_rowIdWithSEQ() throws Exception {
        testTargetColumnExecutionLocation(null, ExecutionLocationType.TARGET, true, "col1", "ds1", "col1", "col1 SEQ", "a.col1 || b.col1");
    }

    @Test
    public void testGetTargetColumnExecutionLocation_rowIdWithNEXTVAL() throws Exception {
        testTargetColumnExecutionLocation(null, ExecutionLocationType.TARGET, true, "col1", "ds1", "col1", "col1 NEXTVAL", "a.col1 || b.col1");
    }

    @Test
    public void testGetTargetColumnExecutionLocation_rowId() throws Exception {
        testTargetColumnExecutionLocation(null, ExecutionLocationType.WORK, true, "col1", "ds1", "col1", "null", "null");
    }

    @Test
    public void testGetTargetColumnExecutionLocation_sqlNoSpaces() throws Exception {
        testTargetColumnExecutionLocation(null, ExecutionLocationType.SOURCE, true, "col1", "ds1", "col1", "col1", "a.col2|b");
    }

    @Test
    public void testGetTargetColumnExecutionLocation_overrideWhenNoExpressions() throws Exception {
        testTargetColumnExecutionLocation(ExecutionLocationType.TARGET, ExecutionLocationType.WORK, false, "col1", "ds1", "col1");
    }

    @Test
    public void testGetTargetColumnExecutionLocation_explicitlyDefinedWORK() throws Exception {
        testTargetColumnExecutionLocation(ExecutionLocationType.WORK, ExecutionLocationType.WORK, false, "targetColumn", "dataset", "id", "sql1");
    }

    @Test
    public void testGetTargetColumnExecutionLocation_explicitlyDefinedTARGET() throws Exception {
        testTargetColumnExecutionLocation(ExecutionLocationType.TARGET, ExecutionLocationType.TARGET, false, "targetColumn", "dataset", "id", "sql1");
    }

    @Test
    public void testGetTargetColumnExecutionLocation_expliciltyDefinedSOURCE() throws Exception {
        testTargetColumnExecutionLocation(ExecutionLocationType.SOURCE, ExecutionLocationType.SOURCE, false, "targetColumn", "dataset", "id", "sql1");
    }

    private void testTargetColumnExecutionLocation(
            ExecutionLocationType explicit,
            ExecutionLocationType expected,
            boolean explicitlyMapped,
            String targetColumn,
            String dataset,
            String idColumn,
            String... sqlExpressions) throws Exception {
        ExecutionLocationTargetColumnExecutionContextImpl tcContext = mock(ExecutionLocationTargetColumnExecutionContextImpl.class);
        ExecutionLocationDataStoreExecutionContextImpl dsContext = mock(ExecutionLocationDataStoreExecutionContextImpl.class);

        List<String> list = new ArrayList<String>();
        for (String sqlExpression : sqlExpressions) {
            list.add(sqlExpression);
        }
        System.out.println(list.size());
        when(tcContext.getSqlExpressions()).thenReturn(list);
        when(dsContext.getDataSetIndex()).thenReturn(1);

        when(tcContext.isExplicitlyMapped()).thenReturn(explicitlyMapped);
        when(tcContext.getTargetColumnName()).thenReturn(targetColumn);
        when(dsContext.getDataSetName()).thenReturn(dataset);
        Map<String, PropertyValueHolder> props = new HashMap<String, PropertyValueHolder>();
        props.put(JodiConstants.ROW_WID, InputModelMockHelper.createMockPropertyValueHolder(JodiConstants.ROW_WID, "col1"));
        when(dsContext.getProperties()).thenReturn(props);


        ExecutionLocationType result = fixture.getTargetColumnExecutionLocation(explicit, dsContext, tcContext);

        assertNotNull(result);
        assertEquals(expected, result);
    }

    // Test FILTER execution location
    @Test
    public void testGetFilterExecutionLocation_explicitlyDefinedWORK() {
        testFilterExecutionLocation(ExecutionLocationType.WORK, ExecutionLocationType.WORK, true);
    }

    @Test
    public void testGetFilterExecutionLocation_explicitlyDefinedSOURCE() {
        testFilterExecutionLocation(ExecutionLocationType.SOURCE, ExecutionLocationType.SOURCE, true);
    }

    @Test
    public void testGetFilterExecutionLocation_explicitlyDefinedTARGET() {
        testFilterExecutionLocation(ExecutionLocationType.TARGET, ExecutionLocationType.TARGET, true);
    }

    @Test
    public void testGetFilterExecutionLocation_sameModelAsTransformation() {
        testFilterExecutionLocation(null, ExecutionLocationType.SOURCE, true);
    }

    @Test
    public void testGetFilterExecutionLocation_notSameModelAsTransformation() {
        testFilterExecutionLocation(null, ExecutionLocationType.WORK, false);
    }

    private void testFilterExecutionLocation(ExecutionLocationType explicit, ExecutionLocationType expected, boolean isSameModelAsTransformation) {
        ExecutionLocationFilterExecutionContext context = mock(ExecutionLocationFilterExecutionContext.class);

        when(context.getFilterCondition()).thenReturn("t1.col1=value and t2.col1=t3.col3");
        when(context.isSameModelInTransformation()).thenReturn(isSameModelAsTransformation);

        ExecutionLocationType result = fixture.getFilterExecutionLocation(explicit, context);

        assertNotNull(result);
        assertEquals(expected, result);
    }

    // Test JOIN execution location
    @Test
    public void testGetJoinExecutionLocation_ExplicitlyDefinedSOURCE() {
        testJoinExectionLocation(ExecutionLocationType.SOURCE, ExecutionLocationType.SOURCE, null, false);
    }

    @Test
    public void testGetJoinExecutionLocation_ExplicitlyDefinedWORK() {
        testJoinExectionLocation(ExecutionLocationType.WORK, ExecutionLocationType.WORK, null, false);
    }

    @Test
    public void testGetJoinExecutionLocation_ExplicitlyDefinedTARGET() {
        testJoinExectionLocation(ExecutionLocationType.TARGET, ExecutionLocationType.TARGET, null, false);
    }

    @Test
    public void testGetJoinExecutionLocation_sameModelAsTransformationNATURAL() {
        testJoinExectionLocation(null, ExecutionLocationType.SOURCE, JoinTypeEnum.NATURAL, true);
    }

    @Test
    public void testGetJoinExecutionLocation_sameModelAsTransformationCROSS() {
        testJoinExectionLocation(null, ExecutionLocationType.SOURCE, JoinTypeEnum.CROSS, true);
    }

    @Test
    public void testGetJoinExecutionLocation_sameModelAsTransformationINNER() {
        testJoinExectionLocation(null, ExecutionLocationType.SOURCE, JoinTypeEnum.INNER, true);
    }

    @Test
    public void testGetJoinExecutionLocation_notSameModelAsTransformationINNER() {
        testJoinExectionLocation(null, ExecutionLocationType.WORK, JoinTypeEnum.INNER, false);
    }

    @Test
    public void testGetJoinExecutionLocation_notSameModelAsTransformationLEFT() {
        testJoinExectionLocation(null, ExecutionLocationType.WORK, JoinTypeEnum.LEFT_OUTER, false);
    }

    @Test
    public void testGetJoinExecutionLocation_sameModelAsTransformationLEFT() {
        testJoinExectionLocation(null, ExecutionLocationType.SOURCE, JoinTypeEnum.LEFT_OUTER, true);
    }

    @Test
    public void testGetJoinExecutionLocation_notSameModelAsTransformationFULL() {
        testJoinExectionLocation(null, ExecutionLocationType.WORK, JoinTypeEnum.FULL, false);
    }

    @Test
    public void testGetJoinExecutionLocation_sameModelAsTransformationFULL() {
        testJoinExectionLocation(null, ExecutionLocationType.SOURCE, JoinTypeEnum.FULL, true);
    }

    @Test
    public void testGetJoinExecutionLocation_sameModelAsTransformationNULL() {
        testJoinExectionLocation(null, ExecutionLocationType.SOURCE, null, true);
    }

    private void testJoinExectionLocation(ExecutionLocationType explicit, ExecutionLocationType expected, JoinTypeEnum joinType, boolean isSameModelAsTransformation) {
        ExecutionLocationJoinExecutionContext context = mock(ExecutionLocationJoinExecutionContext.class);

        when(context.getJoinCondition()).thenReturn("t1.col1=value and t2.col1=t3.col3");
        when(context.isSameModelInTransformation()).thenReturn(isSameModelAsTransformation);
        when(context.getJoinType()).thenReturn(joinType);
        ExecutionLocationType result = fixture.getJoinExecutionLocation(explicit, context);

        assertNotNull(result);
        assertEquals(expected, result);
    }

    // Execution location for LOOKUPs
    @Test
    public void testGetLookupExecutionLocation_ExplicitlyDefinedAsTARGET() {
        testLookupExecutionLocation(ExecutionLocationType.TARGET, ExecutionLocationType.TARGET, false, false);
    }

    @Test
    public void testGetLookupExecutionLocation_ExplicitlyDefinedAsSource() {
        testLookupExecutionLocation(ExecutionLocationType.SOURCE, ExecutionLocationType.SOURCE, false, false);
    }

    @Test
    public void testGetLookupExecutionLocation_ExplicitlyDefinedAsWORK() {
        testLookupExecutionLocation(ExecutionLocationType.WORK, ExecutionLocationType.WORK, false, false);
    }

    @Test
    public void testGetLookupExecutionLocation_notSameModelAsTransformationNotTemporary() {
        testLookupExecutionLocation(null, ExecutionLocationType.SOURCE, false, false);
    }

    @Test
    public void testGetLookupExecutionLocation_sameModelAsTransformationTemporary() {
        testLookupExecutionLocation(null, ExecutionLocationType.SOURCE, true, true);
    }

    @Test
    public void testGetLookupExecutionLocation_notSameModelAsTransformationTemporary() {
        testLookupExecutionLocation(null, ExecutionLocationType.SOURCE, false, true);
    }

    @Test
    public void testGetLookupExecutionLocation_sameModelAsTransformationNotTemporary() {
        testLookupExecutionLocation(null, ExecutionLocationType.WORK, true, false);
    }

    private void testLookupExecutionLocation(ExecutionLocationType explicit, ExecutionLocationType expected, boolean isSameModelAsTransformation, boolean isTemporary) {
        ExecutionLocationLookupExecutionContext context = mock(ExecutionLocationLookupExecutionContext.class);

        when(context.getJoinCondition()).thenReturn("t1.col1=value and t2.col1=t3.col3");
        when(context.isSameModelInTransformation()).thenReturn(isSameModelAsTransformation);


        DataStore lookupDataStore = mock(DataStore.class);
        when(lookupDataStore.isTemporary()).thenReturn(isTemporary);
        DataStoreWithAlias lookupDataStoreAlias = mock(DataStoreWithAlias.class);
        when(lookupDataStoreAlias.getDataStore()).thenReturn(lookupDataStore);
        when(context.getLookupDataStore()).thenReturn(lookupDataStoreAlias);
        ExecutionLocationType result = fixture.getLookupExecutionLocation(explicit, context);

        assertNotNull(result);
        assertEquals(expected, result);
    }
}