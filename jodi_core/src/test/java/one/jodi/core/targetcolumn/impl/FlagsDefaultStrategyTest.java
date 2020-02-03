package one.jodi.core.targetcolumn.impl;

import one.jodi.InputModelMockHelper;
import one.jodi.core.config.JodiConstants;
import one.jodi.core.config.PropertyValueHolder;
import one.jodi.core.extensions.contexts.FlagsDataStoreExecutionContext;
import one.jodi.core.extensions.contexts.FlagsTargetColumnExecutionContext;
import one.jodi.core.extensions.contexts.UDFlagsTargetColumnExecutionContext;
import one.jodi.core.extensions.types.TargetColumnFlags;
import one.jodi.core.extensions.types.UserDefinedFlag;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The class <code>FlagsDefaultStrategyTest</code> contains tests for
 * the class <code>{@link FlagsDefaultStrategy}</code>.
 */
public class FlagsDefaultStrategyTest {

    static final String kmCode = "SomeKMCode";
    FlagsDefaultStrategy fixture;

    public static void main(String[] args) {
        new org.junit.runner.JUnitCore().run(FlagsDefaultStrategyTest.class);
    }

    /**
     * Perform pre-test initialization.
     *
     * @throws Exception if the initialization fails for some reason
     */
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        fixture = new FlagsDefaultStrategy();
    }

    /**
     * Perform post-test clean-up.
     *
     * @throws Exception if the clean-up fails for some reason
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Run the Collection<UserDefinedFlag>
     * getUserDefinedFlags(FlagsDataStoreExecutionContext
     * ,UDFlagsTargetColumnExecutionContext,Collection<UserDefinedFlag>)
     * method test.
     *
     * @throws Exception
     */
    @Test
    public void testGetUserDefinedFlags() throws Exception {
        FlagsDataStoreExecutionContext tableContext = mock(FlagsDataStoreExecutionContext.class);
        UDFlagsTargetColumnExecutionContext columnContext = mock(UDFlagsTargetColumnExecutionContext.class);
        @SuppressWarnings("unchecked")
        Set<UserDefinedFlag> defaultValues = mock(Set.class);

        Set<UserDefinedFlag> result = fixture.getUserDefinedFlags(
                defaultValues, tableContext, columnContext);
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    /**
     * behavior without explicit overrides, no special columns to disable update flag
     *
     * @throws Exception
     */
    @Test
    public void testGetInsertUpdateFlagsNoExplicit() throws Exception {
        FlagsDataStoreExecutionContext tableContext = mock(FlagsDataStoreExecutionContext.class);
        FlagsTargetColumnExecutionContext columnContext = mock(FlagsTargetColumnExecutionContext.class);
        TargetColumnFlags defaultValues = mock(TargetColumnFlags.class);
        when(columnContext.getTargetColumnName()).thenReturn("SomeName");

        TargetColumnFlags result = fixture.getTargetColumnFlags(null, tableContext, columnContext);
        assertNotNull(result);
        assertNotSame(defaultValues, result);
        assertTrue(result.isInsert());
        assertTrue(result.isUpdate());
        assertFalse(result.isMandatory());
        assertFalse(result.isUpdateKey());
    }

    /**
     * behavior with explicit override of Mandatory, no special columns to disable update flag
     *
     * @throws Exception
     */
    @Test
    public void testGetInsertUpdateFlagsExplicitMandatory() throws Exception {
        FlagsDataStoreExecutionContext tableContext = mock(FlagsDataStoreExecutionContext.class);
        FlagsTargetColumnExecutionContext columnContext = mock(FlagsTargetColumnExecutionContext.class);
        TargetColumnFlags defaultValues = mock(TargetColumnFlags.class);

        when(defaultValues.isMandatory()).thenReturn(true);
        when(defaultValues.isInsert()).thenReturn(null);
        when(defaultValues.isUpdate()).thenReturn(null);
        when(columnContext.getTargetColumnName()).thenReturn("SomeName");

        TargetColumnFlags result = fixture.getTargetColumnFlags(defaultValues, tableContext, columnContext);
        assertNotNull(result);
        assertNotSame(defaultValues, result);
        assertTrue(result.isInsert());
        assertTrue(result.isUpdate());
        assertTrue(result.isMandatory());
        assertFalse(result.isUpdateKey());
    }

    /**
     * behavior explicit override for update key that is ignored because KM name
     * is not a match, no special columns to disable update flag
     *
     * @throws Exception
     */
    @Test
    public void testGetInsertUpdateFlagsExplicitKeyNoMerge() throws Exception {
        FlagsDataStoreExecutionContext tableContext = mock(FlagsDataStoreExecutionContext.class);
        FlagsTargetColumnExecutionContext columnContext = mock(FlagsTargetColumnExecutionContext.class);
        TargetColumnFlags defaultValues = mock(TargetColumnFlags.class);
        @SuppressWarnings("unchecked")
        Map<String, PropertyValueHolder> properties = mock(Map.class);

        when(defaultValues.isUpdateKey()).thenReturn(true);
        when(defaultValues.isInsert()).thenReturn(null);
        when(defaultValues.isUpdate()).thenReturn(null);
        when(columnContext.getTargetColumnName()).thenReturn("SomeName");
        PropertyValueHolder pvhMock = InputModelMockHelper.createMockPropertyValueHolder(kmCode + ".name", "SomeKMName");
        when(properties.get(kmCode + ".name")).thenReturn(pvhMock);
        when(tableContext.getProperties()).thenReturn(properties);
        when(tableContext.getIKMCode()).thenReturn(kmCode);

        TargetColumnFlags result = fixture.getTargetColumnFlags(defaultValues, tableContext, columnContext);
        assertNotNull(result);
        assertNotSame(defaultValues, result);
        assertTrue(result.isInsert());
        assertTrue(result.isUpdate());
        assertFalse(result.isMandatory());
        assertFalse(result.isUpdateKey());
    }

    /**
     * behavior explicit override for update key that is honored because KM name
     * is a match, no special columns to disable update flag
     *
     * @throws Exception
     */
    @Test
    public void testGetInsertUpdateFlagsExplicitKeyMerge() throws Exception {
        FlagsDataStoreExecutionContext tableContext = mock(FlagsDataStoreExecutionContext.class);
        FlagsTargetColumnExecutionContext columnContext = mock(FlagsTargetColumnExecutionContext.class);
        TargetColumnFlags defaultValues = mock(TargetColumnFlags.class);
        @SuppressWarnings("unchecked")
        Map<String, PropertyValueHolder> properties = mock(Map.class);

        when(defaultValues.isUpdateKey()).thenReturn(true);
        when(defaultValues.isInsert()).thenReturn(null);
        when(defaultValues.isUpdate()).thenReturn(null);
        when(columnContext.getTargetColumnName()).thenReturn("SomeName");
        PropertyValueHolder pvhMock = InputModelMockHelper.createMockPropertyValueHolder(kmCode + ".name", "IKM XXXX Incremental YYYYY");
        when(properties.get(kmCode + ".name")).thenReturn(pvhMock);
        when(tableContext.getProperties()).thenReturn(properties);
        when(tableContext.getIKMCode()).thenReturn(kmCode);

        TargetColumnFlags result = fixture.getTargetColumnFlags(defaultValues, tableContext, columnContext);
        assertNotNull(result);
        assertNotSame(defaultValues, result);
        assertTrue(result.isInsert());
        assertFalse(result.isUpdate());
        assertTrue(result.isMandatory());
        assertTrue(result.isUpdateKey());
    }

    /**
     * behavior without explicit overrides, special column to disable update flag
     *
     * @throws Exception
     */
    @Test
    public void testGetInsertUpdateFlagsUpdateColumn() throws Exception {
        FlagsDataStoreExecutionContext tableContext = mock(FlagsDataStoreExecutionContext.class);
        FlagsTargetColumnExecutionContext columnContext = mock(FlagsTargetColumnExecutionContext.class);
        TargetColumnFlags defaultValues = mock(TargetColumnFlags.class);
        @SuppressWarnings("unchecked")
        Map<String, PropertyValueHolder> properties = mock(Map.class);

        when(defaultValues.isUpdateKey()).thenReturn(true);
        when(defaultValues.isInsert()).thenReturn(null);
        when(defaultValues.isUpdate()).thenReturn(null);
        when(columnContext.getTargetColumnName()).thenReturn("ROW_WID");
        PropertyValueHolder pvhMock = InputModelMockHelper.createMockPropertyValueHolder(kmCode + ".name", "SomeKMName");
        when(properties.get(kmCode + ".name")).thenReturn(pvhMock);
        pvhMock = InputModelMockHelper.createMockPropertyValueHolder(JodiConstants.ROW_WID, "ROW_WID");
        when(properties.get(JodiConstants.ROW_WID)).thenReturn(pvhMock);
        when(tableContext.getProperties()).thenReturn(properties);
        when(tableContext.getIKMCode()).thenReturn(kmCode);

        TargetColumnFlags result = fixture.getTargetColumnFlags(defaultValues, tableContext, columnContext);
        assertNotNull(result);
        assertNotSame(defaultValues, result);
        assertTrue(result.isInsert());
        assertFalse(result.isUpdate());
        assertFalse(result.isMandatory());
        assertFalse(result.isUpdateKey());
    }
}