package one.jodi.core.km.impl;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * The class <code>TestAll</code> builds a suite that can be used to run all
 * of the tests within its package as well as within any subpackages of its
 * package.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        KnowledgeModuleDefaultStrategyTest.class,
        KnowledgeModuleContextImplTest.class,
        KnowledgeModuleConfigurationImplTest.class
})

public class TestAll {

    public static void main(String[] args) {
        new org.junit.runner.JUnitCore().run(TestAll.class);
    }
}
