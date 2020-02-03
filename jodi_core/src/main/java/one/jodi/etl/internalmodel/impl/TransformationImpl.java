package one.jodi.etl.internalmodel.impl;

import one.jodi.etl.internalmodel.Dataset;
import one.jodi.etl.internalmodel.MappingCommand;
import one.jodi.etl.internalmodel.Mappings;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.model.extensions.TransformationExtension;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Implementation of Transformation interface.
 */
public class TransformationImpl implements Transformation {

    int packageSequence;
    boolean temporary;
    String comments;
    String name;
    Mappings mappings;
    String originalFolderPath = null;
    String folderName;
    LinkedHashSet<Dataset> datasets;
    String packageList;
    TransformationExtension extension;
    boolean useExpressions;
    MappingCommand beginMappingCommand;
    MappingCommand endMappingCommand;
    boolean asynchronous;
    boolean useScenario;

    public TransformationImpl(boolean temporary, String comments, String name,
                              Mappings mappings, String folderName, String packageList,
                              boolean useExpressions) {
        super();
        this.temporary = temporary;
        this.comments = comments.trim();
        this.name = name.trim();
        this.mappings = mappings;
        this.folderName = folderName.trim();
        this.packageList = packageList.trim();
        this.useExpressions = useExpressions;
        this.useExpressions = useScenario;
        datasets = new LinkedHashSet<>();

    }

    public TransformationImpl() {
        super();
        datasets = new LinkedHashSet<>();

    }

    @Override
    public boolean isTemporary() {
        return temporary;
    }

    public void setTemporary(boolean temporary) {
        this.temporary = temporary;
    }

    @Override
    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Mappings getMappings() {
        return mappings;
    }

    public void setMappings(Mappings mappings) {
        this.mappings = mappings;
    }

    public String getOriginalFolderPath() {
        return this.originalFolderPath;
    }

    public void setOriginalFolderPath(final String path) {
        this.originalFolderPath = path;
    }

    @Override
    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    @Override
    public List<Dataset> getDatasets() {
        return new ArrayList<>(datasets);
    }

    public void addDataset(Dataset dataset) {
        datasets.add(dataset);
    }

    public void clearDatasets() {
        datasets.clear();
    }

    @Override
    public String getPackageList() {
        return packageList;
    }

    public void setPackageList(String packageList) {
        this.packageList = packageList;
    }

    @Override
    public TransformationExtension getExtension() {
        return this.extension;
    }

    public void setExtension(TransformationExtension extension) {
        this.extension = extension;
    }

    @Override
    public int getPackageSequence() {
        return packageSequence;
    }

    public void setPackageSequence(int packageSequence) {
        this.packageSequence = packageSequence;
    }

    @Override
    public int getMaxDatasetNumber() {
        int maxDatasetNumber = this.getDatasets().size();
        return maxDatasetNumber;
    }

    @Override
    public boolean useExpressions() {
        return this.useExpressions;
    }

    public void setUseExpressions(boolean useExpressions) {
        this.useExpressions = useExpressions;
    }

    @Override
    public MappingCommand getBeginMappingCommand() {
        return this.beginMappingCommand;
    }

    public void setBeginMappingCommand(MappingCommand beginMappingCommand) {
        this.beginMappingCommand = beginMappingCommand;
    }

    @Override
    public MappingCommand getEndMappingCommand() {
        return this.endMappingCommand;
    }

    public void setEndMappingCommand(MappingCommand endMappingCommand) {
        this.endMappingCommand = endMappingCommand;
    }

    @Override
    public boolean isAsynchronous() {
        return asynchronous;
    }

    public void setAsynchronous(boolean asynchronous) {
        this.asynchronous = asynchronous;
    }
}
