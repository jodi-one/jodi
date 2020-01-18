package one.jodi.core.service;

public interface ExtractionTables {

    public void genExtractTables(String sourceModel, String targetModel,
                                 int packageSequenceStart, String xmlOutputDir);

}