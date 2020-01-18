package one.jodi.etl.internalmodel;

import one.jodi.etlmodel.extensions.PackageExtension;

import java.util.List;

public interface ETLPackage extends ETLPackageHeader {

    PackageExtension getPackageExtension();

    List<String> getTargetPackageList();

    ETLStep getGoToOnFinalSuccess();

    ETLStep getFirstStep();

    String getDescription();

    List<InterfaceStep> getInterfaceSteps();
}
