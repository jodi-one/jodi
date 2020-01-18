package one.jodi.etl.service.repository;

import java.io.IOException;

public interface OdiRepositoryExportService {

    void doExport(String metaDataDirectory) throws IOException;

    String getCipherData();

}
