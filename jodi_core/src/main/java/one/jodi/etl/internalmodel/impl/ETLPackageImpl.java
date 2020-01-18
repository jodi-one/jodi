package one.jodi.etl.internalmodel.impl;

import one.jodi.etl.internalmodel.ETLPackage;
import one.jodi.etl.internalmodel.ETLStep;
import one.jodi.etl.internalmodel.InterfaceStep;
import one.jodi.etlmodel.extensions.PackageExtension;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ETLPackageImpl extends ETLPackageHeaderImpl implements ETLPackage {
    private ETLStep firstBeforeStep;
    private ETLStep firstAfterStep;
    private ETLStep firstInterfaceStep;
    private ETLStep firstFailureStep;
    private ETLStep firstStep;
    private PackageExtension packageExtension;
    private ETLStep goToOnFinalSuccess;
    private String description;
    private List<InterfaceStep> interfaceSteps;

    public ETLPackageImpl(String packageName, String folderCode, String projectCode,
                          ETLStep firstBeforeStep, ETLStep firstAfterStep,
                          ETLStep firstFailureStep, ETLStep goToOnFinalSuccess,
                          String packageListItems, PackageExtension packageExtension,
                          ETLStep firstStep, String description,
                          List<InterfaceStep> interfaceSteps,
                          String comments) {
        super(packageName.trim(), folderCode.trim(), projectCode, packageListItems.trim(), comments);
        this.firstBeforeStep = firstBeforeStep;
        this.firstAfterStep = firstAfterStep;
        this.packageExtension = packageExtension;
        this.goToOnFinalSuccess = goToOnFinalSuccess;
        this.firstFailureStep = firstFailureStep;
        this.setFirstStep(firstStep);
        this.description = description;
        this.interfaceSteps = interfaceSteps;
    }

    public ETLPackageImpl(String packageName, String folderCode, String projectCode,
                          ETLStep firstBeforeStep, ETLStep firstAfterStep,
                          ETLStep firstFailureStep, ETLStep goToOnFinalSuccess,
                          String packageListItems, PackageExtension packageExtension,
                          ETLStep firstStep,
                          String comments) {
        super(packageName, folderCode, projectCode, packageListItems, comments);
        this.firstBeforeStep = firstBeforeStep;
        this.firstAfterStep = firstAfterStep;
        this.packageExtension = packageExtension;
        this.goToOnFinalSuccess = goToOnFinalSuccess;
        this.firstFailureStep = firstFailureStep;
        this.setFirstStep(firstStep);
    }

    @Override
    public List<String> getTargetPackageList() {
        return Arrays.asList(getPackageListItems().split(","))
                .stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(s -> s.trim())
                .collect(Collectors.toList());
    }

    public ETLStep getFirstBeforeStep() {
        return firstBeforeStep;
    }

    @Override
    public PackageExtension getPackageExtension() {
        return packageExtension;
    }

    public ETLStep getFirstAfterStep() {
        return firstAfterStep;
    }

    public ETLStep getFirstFailureStep() {
        return firstFailureStep;
    }

    public ETLStep getFirstInterfaceStep() {
        return firstInterfaceStep;
    }

    public void setFirstInterfaceStep(ETLStep firstInterfaceStep) {
        this.firstInterfaceStep = firstInterfaceStep;
    }

    @Override
    public ETLStep getGoToOnFinalSuccess() {
        return goToOnFinalSuccess;
    }

    /**
     * @return the firstStep
     */
    @Override
    public ETLStep getFirstStep() {
        return firstStep;
    }

    /**
     * @param firstStep the firstStep to set
     */
    public void setFirstStep(ETLStep firstStep) {
        this.firstStep = firstStep;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public List<InterfaceStep> getInterfaceSteps() {
        return interfaceSteps;
    }
}
