package one.jodi.core.service.impl;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * The class <code>TestAll</code> builds a suite that can be used to run all
 * of the tests within its package as well as within any subpackages of its
 * package.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        TransformationServiceImplTest.class,
        DatastoreServiceImplTest.class,
        PackageServiceProviderTest.class,
        ScenarioServiceImplTest.class,
        XMLMetadataServiceProviderTest.class,
        StreamingXMLMetadataServiceProviderTest.class,
        ModelValidatorImplTest.class,
        OneToOneMappingGenerationImplTest.class,
        TableServiceImplTest.class,
        ProcedureServiceImplTest.class,
        TransformationNameHelperTest.class,
        TransformationFileVisitorTest.class,
        DescSequenceComparatorTest.class})
public class TestAll {
}
