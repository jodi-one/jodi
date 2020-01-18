package one.jodi.etl.internalmodel;

public interface PackageStep extends ETLStep {
    String getSourceFolderCode();

    boolean executeAsynchronously();
}
