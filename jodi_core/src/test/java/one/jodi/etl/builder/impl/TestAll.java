package one.jodi.etl.builder.impl;

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
        TransmutingVisitorTest.class,
        EnrichingBuilderTest.class
})

public class TestAll {
}
