package one.jodi.base;

import one.jodi.base.graph.GraphTest;
import one.jodi.base.model.MockTableBaseHelper;
import one.jodi.base.model.TableReferenceHelperTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({one.jodi.base.error.TestAll.class,
        one.jodi.base.util.TestAll.class,
        one.jodi.base.service.annotation.TestAll.class,
        one.jodi.base.tree.TestAll.class,
        GraphTest.class,
        one.jodi.base.config.TestAll.class,
        TableReferenceHelperTest.class,
        MockTableBaseHelper.class})
public class TestAll {

}
