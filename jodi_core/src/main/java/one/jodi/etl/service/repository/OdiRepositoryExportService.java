package one.jodi.etl.service.repository;

import java.io.IOException;

public interface OdiRepositoryExportService {

    void doExport(String metaDataDirectory, OdiRepositoryImportService.DA_TYPE backup_type) throws IOException;

    String getCipherData();

}
