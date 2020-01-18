package one.jodi.etl.builder;

import one.jodi.core.etlmodel.Package;
import one.jodi.etl.internalmodel.ETLPackage;

public interface PackageBuilder {
    ETLPackage transmute(Package pPackage);
}
