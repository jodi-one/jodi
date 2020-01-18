package one.jodi.core.service;

public interface ProcedureService {

    void create(String metadataDirectory, boolean generateScenarios);

    void delete(String metadataDirectory);
}
