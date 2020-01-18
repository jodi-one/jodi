package one.jodi.core.validation.etl;

import one.jodi.etl.internalmodel.Transformation;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;

public class ETLValidationResultTest {

    @Mock
    Transformation transformation;
    String fileName = "FILENAME";

    @Test
    public void test() {
        ETLValidationResult fixture = new ETLValidationResult(transformation, fileName);
        assertEquals(transformation, fixture.getTransformation());
        assertEquals(fileName, fixture.getSourceFileName());
        assertEquals(0, fixture.getErrorMessages().size());
        assertEquals(0, fixture.getWarningMessages().size());

        String message = "MESSAGE";
        fixture.addErrorMessage(message);
        assertEquals(1, fixture.getErrorMessages().size());
        assertEquals(message, fixture.getErrorMessages().get(0));

        fixture.addWarningMessage(message);
        assertEquals(1, fixture.getWarningMessages().size());
        assertEquals(message, fixture.getWarningMessages().get(0));

        fixture.clearMessages();
        assertEquals(0, fixture.getErrorMessages().size());
        assertEquals(0, fixture.getWarningMessages().size());
    }
}
