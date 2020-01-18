package one.jodi.odi.common;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OdiConstantsTest {

    @Test
    public void test() {
        assertFalse(OdiConstants.getStaticFieldValues().contains("odi"));
        assertTrue(OdiConstants.getStaticFieldValues().contains(OdiConstants.ODI_CONTEXT));
        assertFalse(OdiConstants.getStaticFieldValues().contains("Unexpected exception"));
    }

}
