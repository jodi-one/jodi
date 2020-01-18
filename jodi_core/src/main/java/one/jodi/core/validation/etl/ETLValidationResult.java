package one.jodi.core.validation.etl;

import one.jodi.etl.internalmodel.Transformation;

import java.util.ArrayList;
import java.util.List;

/**
 * The class that represents the result of a ETL Transformation validation.
 *
 */
public class ETLValidationResult {

    private List<String> errorMessages;

    private List<String> warningMessages;

    private Transformation transformation;

    private String sourceFileName;

    public ETLValidationResult(Transformation transformation, String sourceFileName) {
        super();
        this.transformation = transformation;
        errorMessages = new ArrayList<>();
        warningMessages = new ArrayList<>();
        this.sourceFileName = sourceFileName;
    }

    boolean hasErrors() {
        return errorMessages.size() > 0;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public List<String> getWarningMessages() {
        return warningMessages;
    }

    public Transformation getTransformation() {
        return transformation;
    }

    public boolean addErrorMessage(String message) {
        return errorMessages.add(message);
    }

    public boolean addWarningMessage(String message) {
        return warningMessages.add(message);
    }

    public void clearMessages() {
        errorMessages.clear();
        warningMessages.clear();
    }


}
