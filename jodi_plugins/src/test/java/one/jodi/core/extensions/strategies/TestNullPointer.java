package one.jodi.core.extensions.strategies;

public class TestNullPointer {

    @org.junit.Test
    public void testNPEGenericFolderNameStrategy() {
        GenericFolderNameStrategy genericFolderNameStrategy = new
                GenericFolderNameStrategy();
        genericFolderNameStrategy.getFolderName(null, null, false);
    }

    @org.junit.Test
    public void testNPEGenericExecutionLocationStrategy() {
        GenericExecutionLocationStrategy genericExecutionLocationStrategy = new
                GenericExecutionLocationStrategy();
        genericExecutionLocationStrategy.getFilterExecutionLocation(null, null);
        genericExecutionLocationStrategy.getJoinExecutionLocation(null, null);
        genericExecutionLocationStrategy.getLookupExecutionLocation(null, null);
        genericExecutionLocationStrategy.getTargetColumnExecutionLocation(null,
                null, null);
    }

    @org.junit.Test
    public void testNPEGenericFlagStrategy() {
        GenericFlagsStrategy genericFlagsStrategy = new
                GenericFlagsStrategy();
        genericFlagsStrategy.getTargetColumnFlags(null, null, null);
        genericFlagsStrategy.getUserDefinedFlags(null, null, null);
    }
}
