package one.jodi.core.service;

public interface VariableService {

    String DATE_FORMAT = "EEE MMM d H:m:s zzz y";

    void create(String metadataDirectory);

    void delete(String metadataDirectory);

    void export(String metadataDirectory);
}
