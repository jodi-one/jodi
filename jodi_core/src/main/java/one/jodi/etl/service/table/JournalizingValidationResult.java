package one.jodi.etl.service.table;

import java.util.ArrayList;
import java.util.List;

public class JournalizingValidationResult {
    private boolean isValid;
    private List<String> validationMessages;

    public JournalizingValidationResult(final boolean isValid, final List<String> validationMessages) {
        super();
        this.isValid = isValid;
        this.validationMessages = validationMessages == null ? new ArrayList<>()
                : validationMessages;
    }

    /**
     * @return boolean indicating successful validation
     */
    public boolean isValid() {
        return isValid;
    }

    /**
     * @return List of validation errors
     */
    public List<String> getValidationMessages() {
        return validationMessages;
    }
}
