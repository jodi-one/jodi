package one.jodi.core.context.packages;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        PackageCacheImplTest.class,
        TransformationCacheItemTest.class,
        PackageCacheItemTest.class
})
public class TestAll {
}
