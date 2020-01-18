package one.jodi.core.config;

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
@SuiteClasses({ModelPropertiesParserTest.class, KnowledgeModulePropertiesParserTest.class,
        KnowledgeModulePropertiesProviderTest.class})
public class TestAll {

}
