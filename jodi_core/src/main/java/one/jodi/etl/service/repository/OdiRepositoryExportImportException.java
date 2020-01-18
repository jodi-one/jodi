package one.jodi.etl.service.repository;


@SuppressWarnings("serial")
public class OdiRepositoryExportImportException extends RuntimeException {

    public OdiRepositoryExportImportException(String message) {
        super(message);
    }

    public OdiRepositoryExportImportException(String message, Exception e) {
        super(message, e);
    }
}