package one.jodi.core.targetcolumn.impl;

import one.jodi.core.extensions.contexts.FlagsDataStoreExecutionContext;
import one.jodi.core.extensions.contexts.FlagsTargetColumnExecutionContext;
import one.jodi.core.extensions.contexts.UDFlagsTargetColumnExecutionContext;
import one.jodi.core.extensions.types.TargetColumnFlags;
import one.jodi.core.extensions.types.UserDefinedFlag;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * The class <code>FlagsIDStrategyTest</code> contains tests for
 * the class <code>{@link FlagsIDStrategy}</code>.
 *
 */
public class FlagsIDStrategyTest {
    FlagsIDStrategy fixture;

    public static void main(String[] args) {
        new org.junit.runner.JUnitCore().run(FlagsIDStrategyTest.class);
    }

    /**
     * Perform pre-test initialization.
     *
     * @throws Exception if the initialization fails for some reason
     */
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        fixture = new FlagsIDStrategy();
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
        assertEquals(defaultValues, result);
    }

    /**
     * Validate the ID Strategy which simply returns the original value that is passed in.
     *
     * @throws Exception
     */
    @Test
    public void testGetInsertUpdateFlags() throws Exception {
        FlagsDataStoreExecutionContext tableContext = mock(FlagsDataStoreExecutionContext.class);
        FlagsTargetColumnExecutionContext columnContext = mock(FlagsTargetColumnExecutionContext.class);
        TargetColumnFlags defaultValues = mock(TargetColumnFlags.class);

        TargetColumnFlags result = fixture.getTargetColumnFlags(defaultValues, tableContext, columnContext);
        assertNotNull(result);
        assertEquals(defaultValues, result);
    }
}