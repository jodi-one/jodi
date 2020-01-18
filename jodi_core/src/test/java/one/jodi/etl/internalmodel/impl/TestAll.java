package one.jodi.etl.internalmodel.impl;


import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * The class <code>TestAll</code> builds a suite that can be used to run all
 * of the tests within its package as well as within any subpackages of its
 * package.
 *
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        DatasetImplTest.class,
        KmTypeImplTest.class,
        MappingsImplTest.class,
        SourceImplTest.class,
        TargetcolumnImplTest.class,
        TransformationImplTest.class,
        LookupImplTest.class
})

public class TestAll {
}
