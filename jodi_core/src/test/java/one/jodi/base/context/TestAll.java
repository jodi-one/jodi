package one.jodi.base.context;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * The class <code>TestAll</code> builds a suite that can be used to run all
 * of the tests within its package as well as within any subpackages of its
 * package.
 */
@RunWith(Suite.class)
@SuiteClasses({ContextImplTest.class})
public class TestAll {
}
