package one.jodi.core.service;

public interface SequenceService {

    void create(String metadataDirectory);

    void delete(String metadataDirectory);

    void export(String metadataDirectory);
}
