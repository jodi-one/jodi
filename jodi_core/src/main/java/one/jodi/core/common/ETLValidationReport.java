package one.jodi.core.common;

import java.util.List;
import java.util.Map;

public interface ETLValidationReport {

    /**
     * Get the list of errors for which validator has validated.  Results are returned keyed on package sequence.
     *
     * @return errors
     */
    Map<Integer, List<String>> getErrorMessages();

    /**
     * Get the list of errors for which validator has validated.  Results are returned keyed on package sequence.
     *
     * @return warnings
     */
    Map<Integer, List<String>> getWarningMessages();

}
