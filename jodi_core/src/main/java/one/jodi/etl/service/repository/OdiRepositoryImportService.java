package one.jodi.etl.service.repository;

public interface OdiRepositoryImportService {

    void doImport(String metaDataDirectory, OdiRepositoryImportService.DA_TYPE da_type);

    String getCipherData();

    public enum DA_TYPE {
        DA_INITIAL,
        DA_PATCH_DEV_REPOS,
        DA_PATCH_EXEC_REPOS,
    }

}
