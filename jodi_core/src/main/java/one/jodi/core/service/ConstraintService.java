package one.jodi.core.service;

public interface ConstraintService {

    void create(String metadataDirectory);

    void delete(String metadataDirectory);

    void export(String metadataDirectory, boolean exportDefinedInDatabase);
}
