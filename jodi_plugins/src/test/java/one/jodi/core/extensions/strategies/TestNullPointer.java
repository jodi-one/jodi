package one.jodi.core.extensions.strategies;

import one.jodi.core.extensions.contexts.ColumnMappingExecutionContext;
import one.jodi.core.extensions.contexts.TargetColumnExecutionContext;
import org.junit.Test;
import org.mockito.Mockito;

public class TestNullPointer {

    @Test
    public void testNPEGenericFolderNameStrategy() {
        GenericFolderNameStrategy genericFolderNameStrategy = new GenericFolderNameStrategy();
        genericFolderNameStrategy.getFolderName(null, null, false);
    }

    @Test
    public void testNPEGenericExecutionLocationStrategy() {
        GenericExecutionLocationStrategy genericExecutionLocationStrategy = new GenericExecutionLocationStrategy();
        genericExecutionLocationStrategy.getFilterExecutionLocation(null, null);
        genericExecutionLocationStrategy.getJoinExecutionLocation(null, null);
        genericExecutionLocationStrategy.getLookupExecutionLocation(null, null);
        genericExecutionLocationStrategy.getTargetColumnExecutionLocation(null, null, null);
    }

    @Test
    public void testNPEGenericFlagStrategy() {
        GenericFlagsStrategy genericFlagsStrategy = new GenericFlagsStrategy();
        genericFlagsStrategy.getTargetColumnFlags(null, null, null);
        genericFlagsStrategy.getUserDefinedFlags(null, null, null);
    }

    @Test
    public void testGenericColumnMappingStrategy() {
        ColumnMappingExecutionContext mockColumnExecCtxt = Mockito.mock(ColumnMappingExecutionContext.class);
        TargetColumnExecutionContext mockTrgtExecCtxt = Mockito.mock(TargetColumnExecutionContext.class);

        ColumnMappingStrategy strategy = new GenericColumnMappingStrategy();
        try {
            strategy.getMappingExpression(null, null, null);
        } catch (IllegalArgumentException e) {
            // this be good
        }
        try {
            strategy.getMappingExpression("NotNull", null, null);
        } catch (IllegalArgumentException e) {
            // this be good
        }
        try {
            strategy.getMappingExpression("NotNull", mockColumnExecCtxt, null);
        } catch (IllegalArgumentException e) {
            // this be good
        }
        try {
            strategy.getMappingExpression("NotNull", null, mockTrgtExecCtxt);
        } catch (IllegalArgumentException e) {
            // this be good
        }
        try {
            strategy.getMappingExpression(null, mockColumnExecCtxt, mockTrgtExecCtxt);
        } catch (IllegalArgumentException e) {
            // this be good
        }
        try {
            strategy.getMappingExpression(null, mockColumnExecCtxt, null);
        } catch (IllegalArgumentException e) {
            // this be good
        }
    }
}
