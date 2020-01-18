package one.jodi;

import one.jodi.core.service.impl.DepthFirstComparatorTest;
import one.jodi.gradle.GradleTests;
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
        one.jodi.base.model.TestAll.class,
        one.jodi.base.context.TestAll.class,
        one.jodi.core.config.TestAll.class,
        one.jodi.core.targetcolumn.impl.TestAll.class,
        one.jodi.core.metadata.impl.TestAll.class,
        one.jodi.core.datastore.impl.TestAll.class,
        one.jodi.core.folder.impl.TestAll.class,
        one.jodi.core.service.impl.TestAll.class,
        one.jodi.core.automapping.impl.TestAll.class,
        one.jodi.core.executionlocation.impl.TestAll.class,
        one.jodi.core.km.impl.TestAll.class,
        one.jodi.core.transformation.impl.TestAll.class,
        one.jodi.core.context.packages.TestAll.class,
        one.jodi.core.validation.TestAll.class,
        one.jodi.etl.internalmodel.impl.TestAll.class,
        one.jodi.etl.builder.impl.TestAll.class,
        GradleTests.class,
        DepthFirstComparatorTest.class,

        // moved to odi11 specific project
        // one.jodi.odi.interfaces.TestAll.class,
        // one.jodi.odi.packages.TestAll.class
})
public class TestAll {
}
