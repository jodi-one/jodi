package one.jodi.core.datastore.impl;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * The class <code>TestAll</code> builds a suite that can be used to run all
 * of the tests within its package as well as within any subpackages of its
 * package.
 *
 */
@RunWith(Suite.class)
@SuiteClasses({ModelCodeContextImplTest.class, ModelCodeExecutionContextDataTest.class})
public class TestAll {
}
