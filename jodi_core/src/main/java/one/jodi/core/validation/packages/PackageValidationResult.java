package one.jodi.core.validation.packages;

import one.jodi.base.util.StringUtils;
import one.jodi.etl.internalmodel.ETLPackage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Class that represents the result of a Package validation.
 */
public class PackageValidationResult {

    /**
     * The validation status.
     */
    private boolean isValid;

    /**
     * The validation messages.
     */
    private List<String> validationMessages;

    /**
     * The target Package instance.
     */
    private ETLPackage targetPackage;

    private Set<String> pendingDependencies;

    /**
     * Instantiates a new package validation result.
     *
     * @param targetPackage the target Package instance
     */
    public PackageValidationResult(ETLPackage targetPackage) {
        this(true,
                new ArrayList<>(), targetPackage);
    }

    /**
     * Instantiates a new package validation result.
     *
     * @param isValid            the validation status
     * @param validationMessages the validation error messages
     * @param targetPackage      the target Package
     */
    public PackageValidationResult(boolean isValid,
                                   List<String> validationMessages, ETLPackage targetPackage) {
        super();
        this.isValid = isValid;
        this.validationMessages = validationMessages == null ? new ArrayList<>() : validationMessages;
        this.targetPackage = targetPackage;
        pendingDependencies = new HashSet<>();
    }

    /**
     * Returns true if the validation result is valid. False otherwise
     *
     * @return the isValid
     */
    public boolean isValid() {
        return isValid;
    }

    /**
     * Set the validation status.
     *
     * @param value the new validation status
     */
    public void setValid(boolean value) {
        this.isValid = value;
    }

    /**
     * Returns true if the validation message list is non empty. False otherwise
     *
     * @return true, if successful
     */
    public boolean hasMessages() {
        return (validationMessages != null && validationMessages.size() > 0);
    }

    /**
     * Gets the validation messages.
     *
     * @return the validationMessages
     */
    public List<String> getValidationMessages() {
        return validationMessages;
    }

    /**
     * Add a message to the list of validation messages.
     *
     * @param message the message
     */
    public void addValidationMessage(String message) {
        validationMessages.add(message);
    }

    /**
     * Gets the target Package.
     *
     * @return the targetPackage
     */
    public ETLPackage getTargetPackage() {
        return targetPackage;
    }

    void addPendingDependency(String packageName) {
        pendingDependencies.add(packageName.toUpperCase());
    }

    void removePendingDependency(String packageName) {
        pendingDependencies.remove(packageName.toUpperCase());
    }

    boolean hasPendingDependencies() {
        return !pendingDependencies.isEmpty();
    }

    String getDependencyNames() {
        return StringUtils.joinStrings(pendingDependencies, ",", " ", "");
    }
}
