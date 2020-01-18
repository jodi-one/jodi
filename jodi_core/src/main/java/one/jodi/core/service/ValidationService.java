package one.jodi.core.service;

public interface ValidationService {

    /**
     * Validate transformations and throw UnrecoverableException if one or more errors are found.
     *
     * @param journalized
     */
    void validate(boolean journalized);

}
